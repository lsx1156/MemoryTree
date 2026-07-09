package com.memorytree.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActionSpace {
    private double temperatureModifier;
    private double interferenceStrength;
    private boolean triggerRewrite;
    private double confidenceThreshold;
    private Map<String, Object> constraints;
    private com.memorytree.enums.BranchType branchType;
    private Double temperature;
    private Double topP;
    private Integer maxTokens;
    private Double penalty;
    private double reward;
    private double confidence;
}