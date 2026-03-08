package com.oicana.example;

import com.oicana.CompilationMode;
import com.oicana.ExportFormat;
import com.oicana.OicanaException;
import com.oicana.example.dto.CompilationPayload;
import com.oicana.example.dto.UploadResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@RestController
@Tag(name = "Templates")
public class TemplateController {
    private static final Logger logger = LoggerFactory.getLogger(TemplateController.class);

    private final TemplateService templateService;
    private final BlobService blobService;

    public TemplateController(TemplateService templateService, BlobService blobService) {
        this.templateService = templateService;
        this.blobService = blobService;
    }

    @Operation(
            summary = "Compile a template to PDF",
            responses = {
                    @ApiResponse(responseCode = "200", description = "The compiled PDF document",
                            content = @Content(mediaType = "application/pdf")),
                    @ApiResponse(responseCode = "400", description = "Compilation error"),
                    @ApiResponse(responseCode = "404", description = "Template not found")
            }
    )
    @PostMapping("/templates/{templateId}/compile")
    public ResponseEntity<byte[]> compile(
            @Parameter(description = "Template identifier", example = "table")
            @PathVariable String templateId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "jsonInputs": [
                                {
                                  "key": "data",
                                  "value": {
                                    "description": "from sample data",
                                    "rows": [
                                      {"name": "Frank", "one": "first", "two": "second", "three": "third"}
                                    ]
                                  }
                                }
                              ],
                              "blobInputs": [
                                {"key": "logo", "blobId": "00000000-0000-0000-0000-000000000000"}
                              ]
                            }"""))
            )
            @RequestBody CompilationPayload payload) {
        try {
            byte[] pdf = templateService.compile(templateId, payload,
                    ExportFormat.pdf(), CompilationMode.PRODUCTION);
            if (pdf == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + templateId + ".pdf\"")
                    .body(pdf);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(("{\"detail\":\"" + e.getMessage() + "\"}").getBytes());
        } catch (OicanaException e) {
            logger.error("Template '{}' failed to compile: {}", templateId, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(("{\"detail\":\"" + e.getMessage() + "\"}").getBytes());
        }
    }

    @Operation(
            summary = "Generate a PNG preview of a template",
            responses = {
                    @ApiResponse(responseCode = "200", description = "The rendered PNG image",
                            content = @Content(mediaType = "image/png")),
                    @ApiResponse(responseCode = "400", description = "Compilation error"),
                    @ApiResponse(responseCode = "404", description = "Template not found")
            }
    )
    @PostMapping("/templates/{templateId}/preview")
    public ResponseEntity<byte[]> preview(
            @Parameter(description = "Template identifier", example = "table")
            @PathVariable String templateId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "jsonInputs": [
                                {
                                  "key": "data",
                                  "value": {
                                    "description": "from sample data",
                                    "rows": [
                                      {"name": "Frank", "one": "first", "two": "second", "three": "third"}
                                    ]
                                  }
                                }
                              ],
                              "blobInputs": [
                                {"key": "logo", "blobId": "00000000-0000-0000-0000-000000000000"}
                              ]
                            }"""))
            )
            @RequestBody CompilationPayload payload) {
        try {
            byte[] png = templateService.compile(templateId, payload,
                    ExportFormat.png(1.0f), CompilationMode.DEVELOPMENT);
            if (png == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"" + templateId + ".png\"")
                    .body(png);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(("{\"detail\":\"" + e.getMessage() + "\"}").getBytes());
        } catch (OicanaException e) {
            logger.error("Template '{}' failed to compile: {}", templateId, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(("{\"detail\":\"" + e.getMessage() + "\"}").getBytes());
        }
    }

    @Operation(
            summary = "Clear a template from cache",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Template cache cleared"),
                    @ApiResponse(responseCode = "404", description = "Template not found")
            }
    )
    @PostMapping("/templates/{templateId}/reset")
    public ResponseEntity<Void> reset(
            @Parameter(description = "Template identifier", example = "table")
            @PathVariable String templateId) {
        if (templateService.getTemplate(templateId) == null) {
            return ResponseEntity.notFound().build();
        }
        templateService.reset(templateId);
        logger.info("Template '{}' removed from cache", templateId);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Download a template as ZIP",
            responses = {
                    @ApiResponse(responseCode = "200", description = "The template ZIP file",
                            content = @Content(mediaType = "application/zip")),
                    @ApiResponse(responseCode = "404", description = "Template not found")
            }
    )
    @GetMapping("/templates/{templateId}")
    public ResponseEntity<?> getTemplate(
            @Parameter(description = "Template identifier", example = "table")
            @PathVariable String templateId) {
        Path templateFile = templateService.getTemplateFile(templateId);
        if (templateFile == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/zip"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + templateId + ".zip\"")
                .body(new FileSystemResource(templateFile));
    }

    @Operation(summary = "List all available template IDs")
    @GetMapping("/templates")
    public List<String> listTemplates() {
        return templateService.listTemplateIds();
    }

    @Operation(
            summary = "Upload a blob file",
            description = "Upload a file to be used as a blob input when compiling templates.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Blob uploaded successfully")
            }
    )
    @PostMapping(value = "/blobs", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Tag(name = "Blobs")
    public ResponseEntity<?> uploadBlob(@RequestParam("file") MultipartFile file) {
        try {
            byte[] data = file.getBytes();
            var blobId = blobService.upload(data);
            return ResponseEntity.ok(new UploadResponse(blobId));
        } catch (IOException e) {
            logger.error("Failed to upload blob: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("detail", "Failed to save file"));
        }
    }
}
