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
public class ObservationSpace {
    private String currentPrompt;
    private String inputPrompt;
    private String currentDraft;
    private double[] logits;
    private double entropy;
    private double crossLayerConsistency;
    private int inferenceStep;
    private Map<String, Object> branchStates;
    private int memoryInjectCount;
    private double temperature;
}