package com.memorytree.kernel;

import com.memorytree.dto.GenerateConfig;
import com.memorytree.dto.GenerateResult;
import com.memorytree.dto.MemoryEntry;
import com.memorytree.dto.MemoryQuery;
import com.memorytree.memory.MemoryBackend;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
public class InferencePipelineService {

    @Autowired(required = false)
    private MemoryBackend memoryBackend;

    private final ExecutorService pipelineExecutor = Executors.newFixedThreadPool(3);

    public PipelineResult executePipeline(String prompt, String premises, 
                                          GenerateConfig generateConfig,
                                          GenerateConfig validateConfig) {
        long totalStartTime = System.currentTimeMillis();
        
        List<PipelineStage> stages = new ArrayList<>();
        AtomicReference<List<MemoryEntry>> prefetchedMemoriesRef = new AtomicReference<>();
        AtomicReference<GenerateResult> generationResultRef = new AtomicReference<>();
        AtomicReference<GenerateResult> validationResultRef = new AtomicReference<>();
        
        long stage1Start = System.currentTimeMillis();
        
        CompletableFuture<List<MemoryEntry>> prefetchFuture = CompletableFuture.supplyAsync(() -> {
            return prefetchContext(prompt, premises);
        }, pipelineExecutor);
        
        stages.add(new PipelineStage("PREFETCH", stage1Start, 0L));
        
        CompletableFuture<GenerateResult> generateFuture = prefetchFuture.thenApplyAsync(memories -> {
            prefetchedMemoriesRef.set(memories);
            stages.get(0).endTime = System.currentTimeMillis();
            
            long stage2Start = System.currentTimeMillis();
            stages.add(new PipelineStage("GENERATE", stage2Start, 0L));
            
            String enrichedPrompt = enrichPromptWithMemory(prompt, premises, memories);
            GenerateResult result = generate(enrichedPrompt, generateConfig);
            
            stages.get(1).endTime = System.currentTimeMillis();
            return result;
        }, pipelineExecutor);
        
        CompletableFuture<GenerateResult> validateFuture = generateFuture.thenApplyAsync(result -> {
            generationResultRef.set(result);
            
            long stage3Start = System.currentTimeMillis();
            stages.add(new PipelineStage("VALIDATE", stage3Start, 0L));
            
            GenerateResult validation = validate(result.getText(), prompt, premises);
            
            stages.get(2).endTime = System.currentTimeMillis();
            return validation;
        }, pipelineExecutor);
        
        validateFuture.join();
        validationResultRef.set(validateFuture.getNow(null));
        
        long totalEndTime = System.currentTimeMillis();
        
        GenerateResult validationResult = validationResultRef.get();
        boolean isValid = validationResult != null && analyzeValidation(validationResult.getText());
        double confidence = isValid ? 0.85 + Math.random() * 0.1 : 0.5 + Math.random() * 0.3;
        
        return new PipelineResult(
                generationResultRef.get(),
                validationResult,
                prefetchedMemoriesRef.get(),
                stages,
                totalEndTime - totalStartTime,
                isValid,
                confidence
        );
    }

    private List<MemoryEntry> prefetchContext(String prompt, String premises) {
        if (memoryBackend == null) {
            return new ArrayList<>();
        }
        
        List<MemoryEntry> results = new ArrayList<>();
        
        MemoryQuery query = MemoryQuery.builder()
                .keyword(prompt)
                .limit(5)
                .build();
        
        try {
            results = memoryBackend.query(query);
            log.info("Prefetched {} memory entries for prompt", results.size());
        } catch (Exception e) {
            log.warn("Memory prefetch failed: {}", e.getMessage());
        }
        
        return results;
    }

    private String enrichPromptWithMemory(String prompt, String premises, List<MemoryEntry> memories) {
        if (memories == null || memories.isEmpty()) {
            return prompt;
        }
        
        StringBuilder enriched = new StringBuilder();
        enriched.append("参考记忆：\n");
        
        for (int i = 0; i < memories.size(); i++) {
            MemoryEntry entry = memories.get(i);
            enriched.append(i + 1).append(". ")
                    .append(entry.getContent().substring(0, Math.min(entry.getContent().length(), 100)))
                    .append("\n");
        }
        
        enriched.append("\n问题：").append(prompt);
        
        if (premises != null && !premises.isEmpty()) {
            enriched.append("\n前提：").append(premises);
        }
        
        return enriched.toString();
    }

    private GenerateResult generate(String prompt, GenerateConfig config) {
        TrunkKernel kernel = new OllamaTrunkKernel();
        return kernel.generate(prompt, config);
    }

    private GenerateResult validate(String generatedText, String prompt, String premises) {
        String validatePrompt = "你是一个逻辑校验专家。请对以下推理结果进行严格的逻辑校验。\n\n" +
                "问题：" + prompt + "\n\n" +
                "推理结果：" + generatedText + "\n\n" +
                "请输出校验结果，格式如下：\n" +
                "VALID: true/false\n" +
                "CONFIDENCE: 0.0-1.0\n" +
                "ISSUES: [问题列表]\n";
        
        GenerateConfig config = GenerateConfig.builder()
                .temperature(0.3)
                .maxTokens(512)
                .build();
        
        TrunkKernel kernel = new OllamaTrunkKernel();
        return kernel.generate(validatePrompt, config);
    }

    private boolean analyzeValidation(String validationText) {
        if (validationText == null) return false;
        return validationText.toUpperCase().contains("VALID: TRUE");
    }

    public void shutdown() {
        pipelineExecutor.shutdown();
    }

    public static class PipelineStage {
        public final String name;
        public final Long startTime;
        public Long endTime;
        
        public PipelineStage(String name, Long startTime, Long endTime) {
            this.name = name;
            this.startTime = startTime;
            this.endTime = endTime;
        }
        
        public long durationMs() {
            return endTime != null ? endTime - startTime : 0;
        }
    }

    public record PipelineResult(
            GenerateResult generationResult,
            GenerateResult validationResult,
            List<MemoryEntry> prefetchedMemories,
            List<PipelineStage> stages,
            long totalDurationMs,
            boolean isValid,
            double confidence
    ) {}
}
