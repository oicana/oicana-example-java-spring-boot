package com.oicana.example.dto;

import java.util.List;

public record CompilationPayload(
        List<JsonInputDto> jsonInputs,
        List<BlobInputDto> blobInputs
) {
    public CompilationPayload {
        if (jsonInputs == null) jsonInputs = List.of();
        if (blobInputs == null) blobInputs = List.of();
    }
}
