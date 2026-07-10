package com.memorytree.memory;

import com.memorytree.dto.MemoryEntry;
import com.memorytree.dto.MemoryQuery;

import java.util.List;
import java.util.Optional;

public interface MemoryBackend {
    MemoryEntry store(MemoryEntry entry);
    Optional<MemoryEntry> retrieve(String id);
    List<MemoryEntry> query(MemoryQuery query);
    void delete(String id);
    List<MemoryEntry> getAll();
    int getTotalCount();
    void initialize();
    void buildIndex();
    boolean isInScope(String query);
    void decayAllHeat(double decayRate);
}