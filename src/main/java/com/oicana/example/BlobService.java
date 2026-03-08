package com.oicana.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class BlobService {
    private static final Logger logger = LoggerFactory.getLogger(BlobService.class);
    private static final Path BLOB_DIR = Path.of("blobs");
    private static final UUID DEFAULT_BLOB_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    private final ConcurrentHashMap<UUID, byte[]> cache = new ConcurrentHashMap<>();

    public BlobService() {
        loadDefaultBlob();
    }

    public byte[] getBlob(UUID blobId) {
        byte[] cached = cache.get(blobId);
        if (cached != null) {
            return cached;
        }

        Path blobPath = BLOB_DIR.resolve(blobId.toString());
        try {
            byte[] data = Files.readAllBytes(blobPath);
            cache.put(blobId, data);
            logger.info("Loaded blob {} from disk", blobId);
            return data;
        } catch (IOException e) {
            logger.warn("Failed to read blob {} from {}: {}", blobId, blobPath, e.getMessage());
            return null;
        }
    }

    public UUID upload(byte[] data) throws IOException {
        UUID blobId = UUID.randomUUID();
        Path blobPath = BLOB_DIR.resolve(blobId.toString());
        Files.createDirectories(blobPath.getParent());
        Files.write(blobPath, data);
        cache.put(blobId, data);
        logger.info("Stored blob {} to disk and cache", blobId);
        return blobId;
    }

    private void loadDefaultBlob() {
        Path defaultPath = BLOB_DIR.resolve(DEFAULT_BLOB_ID.toString());
        try {
            byte[] data = Files.readAllBytes(defaultPath);
            cache.put(DEFAULT_BLOB_ID, data);
            logger.info("Loaded default blob {}", DEFAULT_BLOB_ID);
        } catch (IOException e) {
            logger.warn("Default blob not found at {}", defaultPath);
        }
    }
}
