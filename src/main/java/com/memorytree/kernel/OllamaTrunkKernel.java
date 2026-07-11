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

package com.memorytree.kernel;

import com.memorytree.dto.GenerateConfig;
import com.memorytree.dto.GenerateResult;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * 基于 Ollama 的 TrunkKernel 实现。
 *
 * <p>本类作为组装入口，将职责委派给四个独立组件：
 * <ul>
 *   <li>{@link OllamaHttpClient} — HTTP 请求发送与请求 JSON 构建</li>
 *   <li>{@link OllamaResponseParser} — 响应 JSON 解析与 mock token/logits 生成</li>
 *   <li>{@link KernelMetricsCalculator} — confidence/reward 计算</li>
 *   <li>{@link MockKVCacheManager} — KV cache 句柄模拟</li>
 * </ul>
 */
@Slf4j
@Component
public class OllamaTrunkKernel implements TrunkKernel {

    private static final String LOGIC_INSTRUCTION =
            "你是一个逻辑推理专家。请对给定的问题进行严格的形式逻辑推导。\n\n要求：\n" +
            "1. 如果是数学问题，给出计算过程和正确答案\n" +
            "2. 如果是逻辑命题，分析其真值条件\n" +
            "3. 输出完整的推理链，标注每步的逻辑类型（演绎、归纳、假言推理等）\n" +
            "4. 如果结论不成立，明确指出错误之处\n" +
            "5. 不要添加任何修辞、情感或客套话\n" +
            "6. 用中文输出";

    @Value("${memorytree.ollama.chat.model:qwen2.5:7b}")
    private String modelName;

    @Value("${memorytree.ollama.base-url:http://localhost:11434}")
    private String baseUrl;

    private OllamaHttpClient httpClient;
    private final OllamaResponseParser responseParser = new OllamaResponseParser();
    private final KernelMetricsCalculator metricsCalculator = new KernelMetricsCalculator();
    private final MockKVCacheManager kvCacheManager = new MockKVCacheManager();

    private boolean loaded = false;
    private long loadTime = 0;

    @PostConstruct
    public void init() {
        this.httpClient = new OllamaHttpClient(modelName, baseUrl);
    }

    @Override
    public GenerateResult generate(String prompt, GenerateConfig config) {
        long startTime = System.currentTimeMillis();

        try {
            String fullPrompt = LOGIC_INSTRUCTION + "\n\n问题：" + prompt + "\n\n推理：";

            double temperature = config.getTemperature();
            double topP = config.getTopP() > 0 ? config.getTopP() : 0.9;
            int numPredict = config.getMaxTokens() > 0 ? config.getMaxTokens() : 2048;

            String requestJson = httpClient.buildRequestJson(fullPrompt, temperature, topP, numPredict, false);

            HttpResponse<String> response = httpClient.sendGenerate(requestJson);

            if (response.statusCode() != 200) {
                log.error("Ollama API returned status {}: {}", response.statusCode(), response.body());
                throw new RuntimeException("Ollama API 返回错误状态码: " + response.statusCode() + " - " + response.body());
            }

            String responseBody = response.body();
            log.debug("Ollama response: {}", responseBody.length() > 500 ? responseBody.substring(0, 500) + "..." : responseBody);

            String generatedText = responseParser.extractResponseText(responseBody);

            if (generatedText == null || generatedText.trim().isEmpty()) {
                log.error("Ollama returned empty text. Full response: {}", responseBody);
                throw new RuntimeException("Ollama 返回空文本");
            }

            long endTime = System.currentTimeMillis();
            long ollamaTotalDuration = responseParser.extractLongField(responseBody, "total_duration");
            long ollamaEvalCount = responseParser.extractLongField(responseBody, "eval_count");
            long inferenceTimeMs = ollamaTotalDuration > 0 ? ollamaTotalDuration / 1_000_000 : (endTime - startTime);
            log.info("Ollama inference completed in {}ms, tokens: {}, text length: {}", inferenceTimeMs, ollamaEvalCount, generatedText.length());

            List<String> tokens = ollamaEvalCount > 0
                    ? responseParser.generateMockTokens(ollamaEvalCount)
                    : responseParser.tokenize(generatedText);
            Map<Integer, double[]> logits = responseParser.generateMockLogits(tokens.size());

            double confidence = metricsCalculator.calculateConfidence(generatedText);
            double reward = metricsCalculator.calculateReward(generatedText, ollamaEvalCount, inferenceTimeMs);

            return GenerateResult.builder()
                    .text(generatedText)
                    .content(generatedText)
                    .tokens(tokens)
                    .logits(logits)
                    .inferenceTimeMs(inferenceTimeMs)
                    .confidenceScore(confidence)
                    .confidence(confidence)
                    .reward(reward)
                    .kvCacheUsed(config.isUseKVCache())
                    .build();

        } catch (RuntimeException e) {
            log.error("Ollama inference failed: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error("Ollama inference failed: {}", e.getMessage(), e);
            throw new RuntimeException("推理失败: " + e.getMessage(), e);
        }
    }

    public CompletableFuture<GenerateResult> generateAsync(String prompt, GenerateConfig config, Consumer<String> streamCallback) {
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            StringBuilder fullResponse = new StringBuilder();

            try {
                String fullPrompt = LOGIC_INSTRUCTION + "\n\n问题：" + prompt + "\n\n推理：";

                double temperature = config.getTemperature();
                double topP = config.getTopP() > 0 ? config.getTopP() : 0.9;
                int numPredict = config.getMaxTokens() > 0 ? config.getMaxTokens() : 2048;

                String requestJson = httpClient.buildRequestJson(fullPrompt, temperature, topP, numPredict, true);

                httpClient.sendGenerateStream(requestJson, line -> {
                    String chunk = responseParser.extractResponseText(line);
                    if (chunk != null && !chunk.isEmpty()) {
                        fullResponse.append(chunk);
                        if (streamCallback != null) {
                            streamCallback.accept(chunk);
                        }
                    }
                }).join();

                String generatedText = fullResponse.toString();

                if (generatedText == null || generatedText.trim().isEmpty()) {
                    log.error("Ollama returned empty text");
                    throw new RuntimeException("Ollama 返回空文本");
                }

                long endTime = System.currentTimeMillis();
                log.info("Ollama streaming inference completed in {}ms, text length: {}", endTime - startTime, generatedText.length());

                List<String> tokens = responseParser.tokenize(generatedText);
                Map<Integer, double[]> logits = responseParser.generateMockLogits(tokens.size());

                double confidence = metricsCalculator.calculateConfidence(generatedText);
                double reward = metricsCalculator.calculateReward(generatedText, tokens.size(), endTime - startTime);

                return GenerateResult.builder()
                        .text(generatedText)
                        .content(generatedText)
                        .tokens(tokens)
                        .logits(logits)
                        .inferenceTimeMs(endTime - startTime)
                        .confidenceScore(confidence)
                        .confidence(confidence)
                        .reward(reward)
                        .kvCacheUsed(config.isUseKVCache())
                        .build();

            } catch (Exception e) {
                log.error("Ollama streaming inference failed: {}", e.getMessage(), e);
                throw new RuntimeException("推理失败: " + e.getMessage(), e);
            }
        });
    }

    // MOCK: Ollama API does not expose real logits. This generates placeholder data for interface compliance.
    @Override
    public double[] getLogits(String prompt) {
        double[] logits = new double[50257];
        Arrays.fill(logits, -100);
        Random random = new Random(prompt.hashCode());
        for (int i = 0; i < 10; i++) {
            int idx = random.nextInt(50257);
            logits[idx] = random.nextDouble() * 10;
        }
        return logits;
    }

    @Override
    public void loadKernel(String modelPath) {
        this.loaded = true;
        this.loadTime = System.currentTimeMillis();
        log.info("Ollama kernel loaded: {} at {}", modelPath, baseUrl);
    }

    @Override
    public void unloadKernel() {
        this.loaded = false;
        log.info("Ollama kernel unloaded");
    }

    @Override
    public boolean isLoaded() {
        return loaded;
    }

    @Override
    public String getKernelInfo() {
        return String.format("Ollama Kernel - Model: %s, BaseURL: %s, Loaded: %b, LoadTime: %d",
                modelName, baseUrl, loaded, loadTime);
    }

    @Override
    public long getMemoryUsageBytes() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }

    @Override
    public String getKVCacheHandle() {
        return kvCacheManager.getKVCacheHandle();
    }

    @Override
    public void clearKVCache() {
        kvCacheManager.clearKVCache();
    }

    @Override
    public String cloneKVCache() {
        return kvCacheManager.cloneKVCache();
    }

    @Override
    public void restoreKVCache(String handle) {
        kvCacheManager.restoreKVCache(handle);
    }
}
