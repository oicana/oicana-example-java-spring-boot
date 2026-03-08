package com.oicana.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oicana.CompilationMode;
import com.oicana.ExportFormat;
import com.oicana.OicanaException;
import com.oicana.Template;
import com.oicana.example.dto.CreateCertificateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@RestController
@Tag(name = "Certificates")
public class CertificateController {
    private static final Logger logger = LoggerFactory.getLogger(CertificateController.class);
    private static final String TEMPLATE_ID = "certificate";

    private final TemplateService templateService;
    private final ObjectMapper objectMapper;

    public CertificateController(TemplateService templateService, ObjectMapper objectMapper) {
        this.templateService = templateService;
        this.objectMapper = objectMapper;
    }

    @Operation(
            summary = "Create a PDF certificate",
            description = "Compiles the certificate template with the given name and returns a PDF.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "The compiled PDF certificate",
                            content = @Content(mediaType = "application/pdf")),
                    @ApiResponse(responseCode = "400", description = "Compilation error"),
                    @ApiResponse(responseCode = "500", description = "Certificate template not available")
            }
    )
    @PostMapping("/certificates")
    public ResponseEntity<byte[]> createCertificate(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(examples = @ExampleObject(value = """
                            {"name": "Jane Doe"}"""))
            )
            @RequestBody CreateCertificateRequest request) {
        Template template = templateService.getTemplate(TEMPLATE_ID);
        if (template == null) {
            return ResponseEntity.internalServerError().build();
        }

        try {
            long start = System.currentTimeMillis();
            String jsonValue = objectMapper.writeValueAsString(request);
            Map<String, String> jsonInputs = Map.of("certificate", jsonValue);
            byte[] pdf = template.compile(jsonInputs, Map.of(), ExportFormat.pdf(), CompilationMode.PRODUCTION);
            logger.info("Certificate compiled in {}ms", System.currentTimeMillis() - start);

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss"));
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"certificate_" + timestamp + ".pdf\"")
                    .body(pdf);
        } catch (OicanaException e) {
            logger.error("Failed to compile certificate: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(("{\"detail\":\"" + e.getMessage() + "\"}").getBytes());
        } catch (Exception e) {
            logger.error("Failed to create certificate: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
}
