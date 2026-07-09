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
public class IntrospectionRecord {
    private int round;
    private String action;
    private String generatedText;
    private long inferenceTimeMs;
    private String validationResult;
    private long validationTimeMs;
    private boolean valid;
    private double confidenceScore;
    private List<String> issues;
    private String rewrittenText;
    private long rewriteTimeMs;
    private boolean isFinal;
}