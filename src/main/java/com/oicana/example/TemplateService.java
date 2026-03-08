package com.oicana.example;

import com.fasterxml.jackson.databind.JsonNode;
import com.oicana.BlobInput;
import com.oicana.CompilationMode;
import com.oicana.ExportFormat;
import com.oicana.Template;
import com.oicana.example.dto.BlobInputDto;
import com.oicana.example.dto.CompilationPayload;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TemplateService {
    private static final Logger logger = LoggerFactory.getLogger(TemplateService.class);
    private static final Path TEMPLATE_DIR = Path.of("templates");

    private static final List<String[]> TEMPLATES = List.of(
            new String[]{"accessibility", "0.1.0"},
            new String[]{"certificate", "0.1.0"},
            new String[]{"dependency", "0.1.0"},
            new String[]{"fonts", "0.1.0"},
            new String[]{"invoice", "0.1.0"},
            new String[]{"invoice_zugferd", "0.1.0"},
            new String[]{"minimal", "0.1.0"},
            new String[]{"table", "0.1.0"},
            new String[]{"multi_input", "0.1.0"}
    );

    private final ConcurrentHashMap<String, Template> templateCache = new ConcurrentHashMap<>();
    private final BlobService blobService;

    public TemplateService(BlobService blobService) {
        this.blobService = blobService;
    }

    @PostConstruct
    public void warmUp() {
        logger.info("Warming up templates...");
        for (String[] entry : TEMPLATES) {
            String templateId = entry[0];
            String version = entry[1];
            Path templateFile = TEMPLATE_DIR.resolve(templateId + "-" + version + ".zip");

            if (!Files.exists(templateFile)) {
                logger.error("Template file not found: {}", templateFile);
                continue;
            }

            try {
                long start = System.currentTimeMillis();
                byte[] templateBytes = Files.readAllBytes(templateFile);
                Template template = new Template(templateBytes);
                templateCache.put(templateId, template);
                logger.info("Warmed up {} v{} in {}ms", templateId, version, System.currentTimeMillis() - start);
            } catch (Exception e) {
                logger.error("Failed to warm up template {} v{}: {}", templateId, version, e.getMessage());
            }
        }
        logger.info("Templates warmed up");
    }

    @PreDestroy
    public void cleanup() {
        templateCache.values().forEach(Template::close);
        templateCache.clear();
    }

    public Template getTemplate(String templateId) {
        return templateCache.get(templateId);
    }

    public byte[] compile(String templateId, CompilationPayload payload,
                          ExportFormat exportFormat, CompilationMode mode) {
        Template template = templateCache.get(templateId);
        if (template == null) {
            return null;
        }

        Map<String, String> jsonInputs = new HashMap<>();
        for (var jsonInput : payload.jsonInputs()) {
            jsonInputs.put(jsonInput.key(), jsonInput.value().toString());
        }

        Map<String, BlobInput> blobInputs = new HashMap<>();
        for (BlobInputDto blobInput : payload.blobInputs()) {
            byte[] blobData = blobService.getBlob(blobInput.blobId());
            if (blobData == null) {
                throw new IllegalArgumentException(
                        "Blob with id " + blobInput.blobId() + " not found. "
                                + "Please use an ID of a blob that was previously uploaded.");
            }
            blobInputs.put(blobInput.key(), new BlobInput(blobData));
        }

        long start = System.currentTimeMillis();
        byte[] result = template.compile(jsonInputs, blobInputs, exportFormat, mode);
        logger.info("Compiled '{}' in {}ms", templateId, System.currentTimeMillis() - start);
        return result;
    }

    public void reset(String templateId) {
        Template removed = templateCache.remove(templateId);
        if (removed != null) {
            removed.close();
        }
    }

    public List<String> listTemplateIds() {
        return TEMPLATES.stream().map(entry -> entry[0]).toList();
    }

    public Path getTemplateFile(String templateId) {
        for (String[] entry : TEMPLATES) {
            if (entry[0].equals(templateId)) {
                Path path = TEMPLATE_DIR.resolve(templateId + "-" + entry[1] + ".zip");
                if (Files.exists(path)) {
                    return path;
                }
            }
        }
        return null;
    }
}
