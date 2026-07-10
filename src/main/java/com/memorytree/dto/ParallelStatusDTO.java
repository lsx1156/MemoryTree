package com.memorytree.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParallelStatusDTO {
    private int maxParallelBranches;
    private int threadPoolSize;
    private int activeBranchCount;
    private int currentParallelLevel;
    private int availableProcessors;
    private boolean isParallelEnabled;
}
