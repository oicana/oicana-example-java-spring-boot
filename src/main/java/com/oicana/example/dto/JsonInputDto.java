package com.oicana.example.dto;

import com.fasterxml.jackson.databind.JsonNode;

public record JsonInputDto(String key, JsonNode value) {}
