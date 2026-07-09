package com.memorytree.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InferenceResponse {
    private String id;
    private String finalResult;
    private List<String> derivationTree;
    private List<Double> logicPurityScores;
    private int introspectionRounds;
    private ArbitrationResultDTO arbitrationResult;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private long totalDurationMs;
    private boolean confidenceLow;
}