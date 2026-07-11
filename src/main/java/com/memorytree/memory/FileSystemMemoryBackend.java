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
import com.memorytree.dto.MemoryQuery;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import java.time.format.DateTimeFormatter;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Component
public class FileSystemMemoryBackend implements MemoryBackend {

    private final String MEMORY_DIR;
    private final String MEMORY_FILE;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    private final ObjectMapper objectMapper = createObjectMapper();
    
    private ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        javaTimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DATE_TIME_FORMATTER));
        javaTimeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(DATE_TIME_FORMATTER));
        mapper.registerModule(javaTimeModule);
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
    private List<MemoryEntry> memoryStore = new ArrayList<>();
    private Map<String, List<String>> invertedIndex = new HashMap<>();
    private List<String> scopeKeywords = new ArrayList<>();
    private final EmbeddingService embeddingService;

    public FileSystemMemoryBackend(EmbeddingService embeddingService) {
        this.embeddingService = embeddingService;
        String appData = System.getProperty("user.home") + "/.memorytree";
        this.MEMORY_DIR = appData + "/data/memory";
        this.MEMORY_FILE = MEMORY_DIR + "/memory_store.json";
    }

    @PostConstruct
    @Override
    public void initialize() {
        File dir = new File(MEMORY_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File file = new File(MEMORY_FILE);
        if (file.exists()) {
            try {
                memoryStore = objectMapper.readValue(file, new TypeReference<List<MemoryEntry>>() {});
            } catch (IOException e) {
                memoryStore = new ArrayList<>();
            }
        }
        
        if (memoryStore.isEmpty()) {
            seedDefaultMemories();
        }
        
        buildIndex();
    }
    
    private void seedDefaultMemories() {
        List<MemoryEntry> defaultMemories = new ArrayList<>();
        
        defaultMemories.add(MemoryEntry.builder()
                .id("scholar_001")
                .content("学者思维模式：追求知识的深度与广度，重视证据与逻辑，保持怀疑精神与开放心态。学者的核心素养包括：文献综述能力、批判性思维、方法论自觉、学术规范意识。")
                .tags(java.util.Arrays.asList("学者", "思维模式", "学术素养"))
                .saliencyScore(0.85)
                .accessCount(0)
                .build());
        
        defaultMemories.add(MemoryEntry.builder()
                .id("scholar_002")
                .content("学术研究方法论：提出问题→文献综述→建立假设→设计实验→收集数据→分析验证→得出结论。每一步都需要严谨的逻辑和可复现的操作。")
                .tags(java.util.Arrays.asList("学者", "研究方法", "方法论"))
                .saliencyScore(0.9)
                .accessCount(0)
                .build());
        
        defaultMemories.add(MemoryEntry.builder()
                .id("scholar_003")
                .content("逻辑推理的基本类型：演绎推理（从一般到特殊）、归纳推理（从特殊到一般）、溯因推理（从结果推原因）、类比推理（基于相似性）。演绎推理保证结论的必然性，其余三种提供或然性结论。")
                .tags(java.util.Arrays.asList("学者", "逻辑", "推理"))
                .saliencyScore(0.8)
                .accessCount(0)
                .build());
        
        defaultMemories.add(MemoryEntry.builder()
                .id("researcher_001")
                .content("研究者工作准则：1. 问题导向而非方法导向；2. 保持对反常现象的敏感；3. 记录每一个失败的实验；4. 定期复盘研究方向；5. 与同行交流获取反馈；6. 维护研究笔记的连续性与可追溯性。")
                .tags(java.util.Arrays.asList("研究者", "工作准则", "科研习惯"))
                .saliencyScore(0.88)
                .accessCount(0)
                .build());
        
        defaultMemories.add(MemoryEntry.builder()
                .id("researcher_002")
                .content("科学发现的常见路径：偶然观察→好奇心驱动→系统探索→理论构建→验证证伪。重大发现往往始于对'异常'的关注，而非预设的研究计划。")
                .tags(java.util.Arrays.asList("研究者", "科学发现", "创新"))
                .saliencyScore(0.75)
                .accessCount(0)
                .build());
        
        defaultMemories.add(MemoryEntry.builder()
                .id("researcher_003")
                .content("研究者的认知偏差防范：确认偏差（只寻找支持证据）、锚定效应（过度依赖第一印象）、可得性启发（高估易想起的事件）、幸存者偏差（只关注成功案例）。好的研究者主动寻找证伪证据。")
                .tags(java.util.Arrays.asList("研究者", "认知偏差", "批判性思维"))
                .saliencyScore(0.82)
                .accessCount(0)
                .build());
        
        defaultMemories.add(MemoryEntry.builder()
                .id("common_001")
                .content("知识树结构：最底层是事实与数据，中间层是理论与模型，最高层是范式与世界观。学者的任务是在各层之间建立严谨的逻辑连接，研究者的任务是扩展或修正知识树的边界。")
                .tags(java.util.Arrays.asList("学者", "研究者", "知识结构"))
                .saliencyScore(0.7)
                .accessCount(0)
                .build());
        
        for (MemoryEntry entry : defaultMemories) {
            entry.setCreatedAt(java.time.LocalDateTime.now());
            entry.setLastAccessedAt(java.time.LocalDateTime.now());
            entry.setHeat(entry.getSaliencyScore());
            store(entry);
        }
    }

    @Override
    public MemoryEntry store(MemoryEntry entry) {
        if (entry.getId() == null || entry.getId().isEmpty()) {
            entry.setId(UUID.randomUUID().toString());
        }
        entry.setCreatedAt(LocalDateTime.now());
        entry.setLastAccessedAt(LocalDateTime.now());

        if (entry.getHeat() == 0.0 && entry.getSaliencyScore() > 0) {
            entry.setHeat(entry.getSaliencyScore());
        }

        if (entry.getEmbedding() == null || entry.getEmbedding().isEmpty()) {
            entry.setEmbedding(embeddingService.generateEmbedding(entry.getContent()));
            log.debug("Generated embedding for memory entry: id={}, dimensions={}",
                    entry.getId(), entry.getEmbedding().size());
        }
        
        Optional<MemoryEntry> existing = memoryStore.stream()
                .filter(e -> e.getId().equals(entry.getId()))
                .findFirst();
        
        if (existing.isPresent()) {
            memoryStore.remove(existing.get());
        }
        memoryStore.add(entry);
        persist();
        
        return entry;
    }

    @Override
    public Optional<MemoryEntry> retrieve(String id) {
        return memoryStore.stream()
                .filter(e -> e.getId().equals(id))
                .findFirst()
                .map(entry -> {
                    entry.setLastAccessedAt(LocalDateTime.now());
                    entry.setAccessCount(entry.getAccessCount() + 1);
                    persist();
                    return entry;
                });
    }

    @Override
    public List<MemoryEntry> query(MemoryQuery query) {
        return memoryStore.stream()
                .filter(entry -> {
                    if (query.getKeyword() != null && !query.getKeyword().isEmpty()) {
                        if (!entry.getContent().toLowerCase().contains(query.getKeyword().toLowerCase())) {
                            return false;
                        }
                    }
                    if (query.getTags() != null && !query.getTags().isEmpty()) {
                        if (!entry.getTags().containsAll(query.getTags())) {
                            return false;
                        }
                    }
                    return true;
                })
                .limit(query.getLimit())
                .peek(entry -> {
                    entry.setLastAccessedAt(LocalDateTime.now());
                    entry.setAccessCount(entry.getAccessCount() + 1);
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<MemoryEntry> semanticSearch(String queryText, int limit) {
        if (queryText == null || queryText.trim().isEmpty()) {
            return new ArrayList<>();
        }

        List<Double> queryEmbedding = embeddingService.generateEmbedding(queryText);
        
        List<MemoryEntry> results = memoryStore.stream()
                .filter(entry -> entry.getEmbedding() != null && !entry.getEmbedding().isEmpty())
                .map(entry -> {
                    double similarity = embeddingService.cosineSimilarity(queryEmbedding, entry.getEmbedding());
                    return Map.entry(entry, similarity);
                })
                .filter(entrySimilarity -> entrySimilarity.getValue() >= 0.1)
                .sorted(Map.Entry.<MemoryEntry, Double>comparingByValue().reversed())
                .limit(limit)
                .map(Map.Entry::getKey)
                .peek(entry -> {
                    entry.setLastAccessedAt(LocalDateTime.now());
                    entry.setAccessCount(entry.getAccessCount() + 1);
                })
                .collect(Collectors.toList());

        log.info("Semantic search completed: query='{}', results={}", queryText, results.size());
        return results;
    }

    @Override
    public void delete(String id) {
        memoryStore.removeIf(e -> e.getId().equals(id));
        persist();
    }

    @Override
    public List<MemoryEntry> getAll() {
        return new ArrayList<>(memoryStore);
    }

    @Override
    public int getTotalCount() {
        return memoryStore.size();
    }

    private void persist() {
        try {
            objectMapper.writeValue(new File(MEMORY_FILE), memoryStore);
        } catch (IOException e) {
            log.error("Failed to persist memory store to file: {}", MEMORY_FILE, e);
            throw new RuntimeException("Memory persistence failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void buildIndex() {
        invertedIndex.clear();
        scopeKeywords.clear();
        
        for (MemoryEntry entry : memoryStore) {
            String content = entry.getContent().toLowerCase();
            String[] words = content.split("[\\s\\p{Punct}]+");
            
            for (String word : words) {
                if (word.length() >= 2) {
                    invertedIndex.computeIfAbsent(word, k -> new ArrayList<>()).add(entry.getId());
                }
            }
            
            if (entry.getTags() != null) {
                for (String tag : entry.getTags()) {
                    String tagLower = tag.toLowerCase();
                    invertedIndex.computeIfAbsent(tagLower, k -> new ArrayList<>()).add(entry.getId());
                    if (!scopeKeywords.contains(tagLower)) {
                        scopeKeywords.add(tagLower);
                    }
                }
            }
        }
    }

    @Override
    public boolean isInScope(String query) {
        if (query == null || query.trim().isEmpty()) {
            return true;
        }
        
        String queryLower = query.toLowerCase();
        String[] queryWords = queryLower.split("[\\s\\p{Punct}]+");
        
        int matchedScopeCount = 0;
        int matchedIndexCount = 0;
        
        for (String word : queryWords) {
            if (word.length() >= 2) {
                if (scopeKeywords.contains(word)) {
                    matchedScopeCount++;
                }
                if (invertedIndex.containsKey(word)) {
                    matchedIndexCount++;
                }
            }
        }
        
        if (queryWords.length == 0) {
            return true;
        }
        
        double scopeMatchRatio = (double) matchedScopeCount / queryWords.length;
        double indexMatchRatio = (double) matchedIndexCount / queryWords.length;
        
        return scopeMatchRatio >= 0.3 || indexMatchRatio >= 0.5;
    }

    @Override
    public void decayAllHeat(double decayRate) {
        double effectiveRate = Math.max(0.0, Math.min(1.0, decayRate));
        
        for (MemoryEntry entry : memoryStore) {
            entry.setHeat(entry.getHeat() * (1 - effectiveRate));
            
            if (entry.getHeat() < 0.01) {
                entry.setHeat(0);
            }
        }
        
        log.info("Heat decayed for {} entries with rate {}", memoryStore.size(), effectiveRate);
    }
}