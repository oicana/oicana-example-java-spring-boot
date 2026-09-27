package com.oicana.example.dto;

import tools.jackson.databind.JsonNode;

public record JsonInputDto(String key, JsonNode value) {}
