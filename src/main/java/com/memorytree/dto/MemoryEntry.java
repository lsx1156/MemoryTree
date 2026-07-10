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
public class MemoryEntry {
    private String id;
    private String content;
    private List<String> tags;
    private LocalDateTime createdAt;
    private LocalDateTime lastAccessedAt;
    private int accessCount;
    private double saliencyScore;
    private double heat;
}