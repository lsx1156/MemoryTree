package com.memorytree.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateConfig {
    private double temperature;
    private double topP;
    private int maxLength;
    private int maxTokens;
    private long seed;
    private boolean useKVCache;
}