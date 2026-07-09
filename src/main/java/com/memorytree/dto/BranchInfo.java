package com.memorytree.dto;

import com.memorytree.enums.BranchType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BranchInfo {
    private String id;
    private String name;
    private BranchType type;
    private boolean active;
    private String description;
    private String filePath;
}