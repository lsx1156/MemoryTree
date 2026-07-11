/*
 * Copyright 2026 lsx1156
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.memorytree.memory;

import com.memorytree.dto.MemoryEntry;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WorkingMemory {

    private static final int MAX_SIZE_BYTES = 20 * 1024 * 1024;
    private final Map<String, MemoryEntry> activeEntries = new ConcurrentHashMap<>();
    private final List<String> contextHistory = new ArrayList<>();
    private int currentSizeBytes = 0;

    @PostConstruct
    public void init() {
        seedWorkingMemory();
    }

    private void seedWorkingMemory() {
        MemoryEntry scholarIdentity = MemoryEntry.builder()
                .id("wm_scholar_identity")
                .content("当前角色：学者型思考者。以追求真理为目标，重视逻辑严谨与证据充分，保持开放与怀疑的平衡。")
                .tags(Arrays.asList("工作记忆", "角色设定", "学者"))
                .saliencyScore(0.9)
                .accessCount(0)
                .createdAt(LocalDateTime.now())
                .lastAccessedAt(LocalDateTime.now())
                .build();
        addEntry(scholarIdentity);

        MemoryEntry researcherIdentity = MemoryEntry.builder()
                .id("wm_researcher_identity")
                .content("当前立场：研究者视角。关注问题的提出与验证，重视方法论的选择与反思，对异常现象保持敏感。")
                .tags(Arrays.asList("工作记忆", "角色设定", "研究者"))
                .saliencyScore(0.88)
                .accessCount(0)
                .createdAt(LocalDateTime.now())
                .lastAccessedAt(LocalDateTime.now())
                .build();
        addEntry(researcherIdentity);

        MemoryEntry reasoningGuideline = MemoryEntry.builder()
                .id("wm_reasoning_guide")
                .content("推理原则：先明确前提，再逐步推导，每步标注依据，最后检查一致性。遇到矛盾时先核查前提而非强行解释。")
                .tags(Arrays.asList("工作记忆", "推理原则", "逻辑"))
                .saliencyScore(0.85)
                .accessCount(0)
                .createdAt(LocalDateTime.now())
                .lastAccessedAt(LocalDateTime.now())
                .build();
        addEntry(reasoningGuideline);

        contextHistory.add("系统初始化：进入学者-研究者双角色模式");
        contextHistory.add("记忆系统就绪：工作记忆3条，持久记忆已加载");
    }

    public void addEntry(MemoryEntry entry) {
        if (currentSizeBytes + estimateSize(entry) > MAX_SIZE_BYTES) {
            evictOldest();
        }
        activeEntries.put(entry.getId(), entry);
        currentSizeBytes += estimateSize(entry);
    }

    public void removeEntry(String id) {
        MemoryEntry removed = activeEntries.remove(id);
        if (removed != null) {
            currentSizeBytes -= estimateSize(removed);
        }
    }

    public MemoryEntry getEntry(String id) {
        return activeEntries.get(id);
    }

    public List<MemoryEntry> getAllEntries() {
        return new ArrayList<>(activeEntries.values());
    }

    public void addContext(String context) {
        if (contextHistory.size() >= 100) {
            contextHistory.remove(0);
        }
        contextHistory.add(context);
    }

    public List<String> getContextHistory() {
        return new ArrayList<>(contextHistory);
    }

    public void clearAll() {
        activeEntries.clear();
        contextHistory.clear();
        currentSizeBytes = 0;
    }

    public int getSize() {
        return activeEntries.size();
    }

    public int getSizeBytes() {
        return currentSizeBytes;
    }

    public boolean isEmpty() {
        return activeEntries.isEmpty() && contextHistory.isEmpty();
    }

    private int estimateSize(MemoryEntry entry) {
        int size = 0;
        if (entry.getId() != null) size += entry.getId().length() * 2;
        if (entry.getContent() != null) size += entry.getContent().length() * 2;
        if (entry.getTags() != null) {
            for (String tag : entry.getTags()) {
                size += tag.length() * 2;
            }
        }
        return size + 100;
    }

    private void evictOldest() {
        String oldestId = null;
        long oldestTime = Long.MAX_VALUE;
        
        for (Map.Entry<String, MemoryEntry> entry : activeEntries.entrySet()) {
            if (entry.getValue().getLastAccessedAt() != null) {
                long time = entry.getValue().getLastAccessedAt().toLocalDate().toEpochDay();
                if (time < oldestTime) {
                    oldestTime = time;
                    oldestId = entry.getKey();
                }
            }
        }
        
        if (oldestId != null) {
            removeEntry(oldestId);
        }
    }
}