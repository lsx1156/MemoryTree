package com.memorytree.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContractClause {
    private String id;
    private String name;
    private String rule;
    private String description;
    private boolean failSafe;
    private double severity;
    private boolean enabled;
}