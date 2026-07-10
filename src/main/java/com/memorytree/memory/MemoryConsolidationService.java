package com.memorytree.memory;

import com.memorytree.dto.MemoryEntry;
import com.memorytree.dto.GenerateResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class MemoryConsolidationService {

    private static final double SIGNIFICANCE_ENTROPY_THRESHOLD = 0.3;
    private static final double CONSISTENCY_THRESHOLD = 0.85;

    private final WorkingMemory workingMemory;
    private final MemoryBackend memoryBackend;

    public MemoryConsolidationService(WorkingMemory workingMemory, MemoryBackend memoryBackend) {
        this.workingMemory = workingMemory;
        this.memoryBackend = memoryBackend;
    }

    public ConsolidationResult checkSignificance(GenerateResult result) {
        ConsolidationResult consolidationResult = new ConsolidationResult();
        consolidationResult.setSignificant(false);
        consolidationResult.setReasons(new ArrayList<>());

        if (result == null || result.getText() == null) {
            return consolidationResult;
        }

        double entropy = calculateEntropy(result.getText());
        consolidationResult.setEntropy(entropy);

        double consistency = calculateConsistency(result);
        consolidationResult.setConsistency(consistency);

        if (entropy < SIGNIFICANCE_ENTROPY_THRESHOLD) {
            consolidationResult.getReasons().add("输出熵低于阈值 (" + String.format("%.2f", entropy) + "<" + SIGNIFICANCE_ENTROPY_THRESHOLD + ")");
        }

        if (consistency > CONSISTENCY_THRESHOLD) {
            consolidationResult.getReasons().add("推理链一致性高 (" + String.format("%.2f", consistency) + ">" + CONSISTENCY_THRESHOLD + ")");
        }

        if (result.getConfidenceScore() > 0.8) {
            consolidationResult.getReasons().add("置信度高 (" + String.format("%.2f", result.getConfidenceScore()) + ")");
        }

        if (!consolidationResult.getReasons().isEmpty()) {
            consolidationResult.setSignificant(true);
        }

        log.info("Significance check: entropy={}, consistency={}, significant={}", 
                entropy, consistency, consolidationResult.isSignificant());

        return consolidationResult;
    }

    public MemoryEntry consolidate(GenerateResult result, List<String> tags) {
        MemoryEntry entry = MemoryEntry.builder()
                .content(result.getText())
                .tags(tags != null ? tags : new ArrayList<>())
                .build();

        workingMemory.addEntry(entry);
        memoryBackend.store(entry);

        log.info("Memory consolidated: id={}, length={}", entry.getId(), result.getText().length());
        return entry;
    }

    public MemoryEntry manualConsolidate(String content, List<String> tags) {
        MemoryEntry entry = MemoryEntry.builder()
                .content(content)
                .tags(tags != null ? tags : new ArrayList<>())
                .build();

        workingMemory.addEntry(entry);
        memoryBackend.store(entry);

        log.info("Manual memory consolidated: id={}", entry.getId());
        return entry;
    }

    public void tryConsolidate(String content, double confidenceScore, int rounds) {
        if (content == null || content.isEmpty()) {
            return;
        }

        MemoryEntry entry = MemoryEntry.builder()
                .content(content)
                .tags(java.util.Arrays.asList("对话记录", "推理结果"))
                .saliencyScore(confidenceScore)
                .build();
        
        workingMemory.addEntry(entry);
        log.info("Conversation saved to working memory: id={}, confidence={}", entry.getId(), confidenceScore);

        GenerateResult mockResult = new GenerateResult();
        mockResult.setText(content);
        mockResult.setConfidenceScore(confidenceScore);

        ConsolidationResult result = checkSignificance(mockResult);
        if (result.isSignificant()) {
            memoryBackend.store(entry);
            log.info("Conversation consolidated to persistent memory: id={}", entry.getId());
        }
    }

    private double calculateEntropy(String text) {
        if (text == null || text.isEmpty()) {
            return 1.0;
        }

        int[] freq = new int[256];
        for (char c : text.toCharArray()) {
            if (c < 256) {
                freq[c]++;
            }
        }

        double entropy = 0;
        int total = text.length();
        for (int f : freq) {
            if (f > 0) {
                double p = (double) f / total;
                entropy -= p * Math.log(p) / Math.log(2);
            }
        }

        return entropy / 8.0;
    }

    private double calculateConsistency(GenerateResult result) {
        if (result.getIntrospectionRecords() == null || result.getIntrospectionRecords().isEmpty()) {
            return result.getConfidenceScore() > 0 ? result.getConfidenceScore() : 0.5;
        }

        long validCount = result.getIntrospectionRecords().stream()
                .filter(r -> r.isValid())
                .count();

        return (double) validCount / result.getIntrospectionRecords().size();
    }

    public static class ConsolidationResult {
        private boolean significant;
        private double entropy;
        private double consistency;
        private List<String> reasons;

        public ConsolidationResult() {
            this.reasons = new ArrayList<>();
        }

        public boolean isSignificant() {
            return significant;
        }

        public void setSignificant(boolean significant) {
            this.significant = significant;
        }

        public double getEntropy() {
            return entropy;
        }

        public void setEntropy(double entropy) {
            this.entropy = entropy;
        }

        public double getConsistency() {
            return consistency;
        }

        public void setConsistency(double consistency) {
            this.consistency = consistency;
        }

        public List<String> getReasons() {
            return reasons;
        }

        public void setReasons(List<String> reasons) {
            this.reasons = reasons;
        }
    }
}