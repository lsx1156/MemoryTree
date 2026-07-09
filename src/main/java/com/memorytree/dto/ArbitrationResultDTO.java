package com.memorytree.dto;

import com.memorytree.enums.ArbitrationResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArbitrationResultDTO {
    private ArbitrationResult result;
    private String violatingClauseId;
    private String violatingClauseName;
    private List<String> matchedRules;
    private double complianceScore;
    private String explanation;
}