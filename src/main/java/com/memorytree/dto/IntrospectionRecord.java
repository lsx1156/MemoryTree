package com.memorytree.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

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
    private Map<String, Object> metadata;
    
    public void setMetadata(String key, Object value) {
        if (this.metadata == null) {
            this.metadata = new java.util.HashMap<>();
        }
        this.metadata.put(key, value);
    }
}