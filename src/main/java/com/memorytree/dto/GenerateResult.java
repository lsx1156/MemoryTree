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
public class GenerateResult {
    private String text;
    private List<String> tokens;
    private Map<Integer, double[]> logits;
    private long inferenceTimeMs;
    private double confidenceScore;
    private boolean kvCacheUsed;
    
    private List<IntrospectionRecord> introspectionRecords;
    private int introspectionRounds;
    private List<String> derivationTree;
    private List<Double> logicPurityScores;
    private ArbitrationResultDTO arbitrationResult;
    private List<com.memorytree.system.GatingEvent> gatingEvents;
}