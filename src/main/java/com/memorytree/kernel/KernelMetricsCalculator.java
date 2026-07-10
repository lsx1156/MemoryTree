package com.memorytree.kernel;

/**
 * 计算推理结果的置信度（confidence）与奖励（reward）。
 * 当前实现为基于关键词的启发式估算，Ollama API 不直接暴露模型置信度。
 */
public class KernelMetricsCalculator {

    /**
     * 基于逻辑/证据/推导关键词估算置信度，范围 [0.5, 0.95]。
     */
    public double calculateConfidence(String text) {
        if (text == null || text.isEmpty()) {
            return 0.5;
        }

        double score = 0.5;

        if (text.contains("因此") || text.contains("所以") || text.contains("结论")) {
            score += 0.1;
        }
        if (text.contains("推导") || text.contains("推理") || text.contains("论证")) {
            score += 0.1;
        }
        if (text.contains("根据") || text.contains("基于") || text.contains("证据")) {
            score += 0.1;
        }
        if (text.contains("假设") || text.contains("前提") || text.contains("条件")) {
            score += 0.05;
        }
        if (text.contains("证明") || text.contains("验证")) {
            score += 0.1;
        }
        if (text.length() > 200) {
            score += 0.05;
        }

        String[] sentences = text.split("[。！？;]");
        if (sentences.length > 3) {
            score += 0.05;
        }

        return Math.min(0.95, Math.max(0.5, score));
    }

    /**
     * 基于文本长度、token 数量、耗时估算 reward，范围 [0.5, 1.0]。
     */
    public double calculateReward(String text, long tokenCount, long inferenceTimeMs) {
        double reward = 0.5;
        if (text != null && !text.isEmpty()) {
            reward += Math.min(0.2, text.length() / 1000.0);
        }
        if (tokenCount > 50) {
            reward += 0.15;
        }
        if (inferenceTimeMs > 0 && inferenceTimeMs < 30000) {
            reward += 0.15;
        }
        return Math.min(1.0, Math.max(0.5, reward));
    }
}
