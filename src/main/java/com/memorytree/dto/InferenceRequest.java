package com.memorytree.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InferenceRequest {
    private String prompt;
    private List<String> premises;
    private GenerateConfig config;
    private int maxIntrospectionRounds;
    private boolean enableMemoryRetrieval;
}