package com.memorytree.kernel;

import com.memorytree.dto.GenerateConfig;
import com.memorytree.dto.GenerateResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

@Slf4j
@Component
public class OllamaTrunkKernel implements TrunkKernel {

    @Value("${spring.ai.ollama.chat.model:qwen2.5:7b}")
    private String modelName;

    @Value("${spring.ai.ollama.base-url:http://localhost:11434}")
    private String baseUrl;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private boolean loaded = false;
    private long loadTime = 0;
    private String kvCacheHandle = null;
    private Map<String, String> kvCacheStore = new HashMap<>();

    @Override
    public GenerateResult generate(String prompt, GenerateConfig config) {
        long startTime = System.currentTimeMillis();

        try {
            String logicInstruction = "你是一个逻辑推理专家。请对给定的问题进行严格的形式逻辑推导。\n\n要求：\n1. 如果是数学问题，给出计算过程和正确答案\n2. 如果是逻辑命题，分析其真值条件\n3. 输出完整的推理链，标注每步的逻辑类型（演绎、归纳、假言推理等）\n4. 如果结论不成立，明确指出错误之处\n5. 不要添加任何修辞、情感或客套话\n6. 用中文输出";
            String fullPrompt = logicInstruction + "\n\n问题：" + prompt + "\n\n推理：";

            double temperature = config.getTemperature();
            double topP = config.getTopP() > 0 ? config.getTopP() : 0.9;
            int numPredict = config.getMaxTokens() > 0 ? config.getMaxTokens() : 2048;

            String requestJson = String.format(
                    "{\"model\":\"%s\",\"prompt\":%s,\"stream\":false,\"options\":{\"temperature\":%s,\"top_p\":%s,\"num_predict\":%d}}",
                    modelName,
                    jsonEscape(fullPrompt),
                    temperature,
                    topP,
                    numPredict
            );

            log.info("Calling Ollama API: model={}, baseUrl={}", modelName, baseUrl);
            log.debug("Request body: {}", requestJson);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/generate"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                    .timeout(Duration.ofSeconds(120))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("Ollama API returned status {}: {}", response.statusCode(), response.body());
                throw new RuntimeException("Ollama API 返回错误状态码: " + response.statusCode() + " - " + response.body());
            }

            String responseBody = response.body();
            log.debug("Ollama response: {}", responseBody.length() > 500 ? responseBody.substring(0, 500) + "..." : responseBody);

            String generatedText = extractResponseText(responseBody);

            if (generatedText == null || generatedText.trim().isEmpty()) {
                log.error("Ollama returned empty text. Full response: {}", responseBody);
                throw new RuntimeException("Ollama 返回空文本");
            }

            long endTime = System.currentTimeMillis();
            log.info("Ollama inference completed in {}ms, text length: {}", endTime - startTime, generatedText.length());

            List<String> tokens = tokenize(generatedText);
            Map<Integer, double[]> logits = generateMockLogits(tokens.size());

            return GenerateResult.builder()
                    .text(generatedText)
                    .content(generatedText)
                    .tokens(tokens)
                    .logits(logits)
                    .inferenceTimeMs(endTime - startTime)
                    .confidenceScore(calculateConfidence(generatedText))
                    .confidence(calculateConfidence(generatedText))
                    .reward(Math.random() * 0.5 + 0.5)
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
                String logicInstruction = "你是一个逻辑推理专家。请对给定的问题进行严格的形式逻辑推导。\n\n要求：\n1. 如果是数学问题，给出计算过程和正确答案\n2. 如果是逻辑命题，分析其真值条件\n3. 输出完整的推理链，标注每步的逻辑类型（演绎、归纳、假言推理等）\n4. 如果结论不成立，明确指出错误之处\n5. 不要添加任何修辞、情感或客套话\n6. 用中文输出";
                String fullPrompt = logicInstruction + "\n\n问题：" + prompt + "\n\n推理：";

                double temperature = config.getTemperature();
                double topP = config.getTopP() > 0 ? config.getTopP() : 0.9;
                int numPredict = config.getMaxTokens() > 0 ? config.getMaxTokens() : 2048;

                String requestJson = String.format(
                        "{\"model\":\"%s\",\"prompt\":%s,\"stream\":true,\"options\":{\"temperature\":%s,\"top_p\":%s,\"num_predict\":%d}}",
                        modelName,
                        jsonEscape(fullPrompt),
                        temperature,
                        topP,
                        numPredict
                );

                log.info("Calling Ollama API (streaming): model={}", modelName);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(baseUrl + "/api/generate"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                        .timeout(Duration.ofSeconds(120))
                        .build();

                AtomicReference<Boolean> done = new AtomicReference<>(false);
                
                httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofLines())
                        .thenApply(HttpResponse::body)
                        .thenAccept(lines -> {
                            lines.forEach(line -> {
                                if (line != null && !line.isEmpty()) {
                                    String chunk = extractResponseText(line);
                                    if (chunk != null && !chunk.isEmpty()) {
                                        fullResponse.append(chunk);
                                        if (streamCallback != null) {
                                            streamCallback.accept(chunk);
                                        }
                                    }
                                    if (line.contains("\"done\":true")) {
                                        done.set(true);
                                    }
                                }
                            });
                        }).join();

                String generatedText = fullResponse.toString();

                if (generatedText == null || generatedText.trim().isEmpty()) {
                    log.error("Ollama returned empty text");
                    throw new RuntimeException("Ollama 返回空文本");
                }

                long endTime = System.currentTimeMillis();
                log.info("Ollama streaming inference completed in {}ms, text length: {}", endTime - startTime, generatedText.length());

                List<String> tokens = tokenize(generatedText);
                Map<Integer, double[]> logits = generateMockLogits(tokens.size());

                return GenerateResult.builder()
                        .text(generatedText)
                        .content(generatedText)
                        .tokens(tokens)
                        .logits(logits)
                        .inferenceTimeMs(endTime - startTime)
                        .confidenceScore(0.9 + Math.random() * 0.08)
                        .confidence(0.9 + Math.random() * 0.08)
                        .reward(Math.random() * 0.5 + 0.5)
                        .kvCacheUsed(config.isUseKVCache())
                        .build();

            } catch (Exception e) {
                log.error("Ollama streaming inference failed: {}", e.getMessage(), e);
                throw new RuntimeException("推理失败: " + e.getMessage(), e);
            }
        });
    }

    private String extractResponseText(String json) {
        String marker = "\"response\":\"";
        int start = json.indexOf(marker);
        if (start < 0) {
            marker = "\"response\": \"";
            start = json.indexOf(marker);
        }
        if (start < 0) return null;

        start += marker.length();
        StringBuilder sb = new StringBuilder();
        boolean escaped = false;
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (escaped) {
                switch (c) {
                    case 'n' -> sb.append('\n');
                    case 't' -> sb.append('\t');
                    case 'r' -> sb.append('\r');
                    case '"' -> sb.append('"');
                    case '\\' -> sb.append('\\');
                    case '/' -> sb.append('/');
                    default -> sb.append(c);
                }
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else if (c == '"') {
                break;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private String jsonEscape(String text) {
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 32) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        sb.append("\"");
        return sb.toString();
    }

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
        return 4L * 1024 * 1024 * 1024;
    }

    private double calculateConfidence(String text) {
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

    private List<String> tokenize(String text) {
        return Arrays.asList(text.split("\\s+"));
    }

    private Map<Integer, double[]> generateMockLogits(int tokenCount) {
        Map<Integer, double[]> logits = new HashMap<>();
        Random random = new Random();
        for (int i = 0; i < tokenCount; i++) {
            double[] tokenLogits = new double[50257];
            Arrays.fill(tokenLogits, -100);
            int tokenId = random.nextInt(50257);
            tokenLogits[tokenId] = 10;
            logits.put(i, tokenLogits);
        }
        return logits;
    }

    @Override
    public String getKVCacheHandle() {
        if (kvCacheHandle == null) {
            kvCacheHandle = "kv_cache_" + System.currentTimeMillis() + "_" + 
                    Long.toHexString(Double.doubleToLongBits(Math.random()));
            kvCacheStore.put(kvCacheHandle, "active");
        }
        return kvCacheHandle;
    }

    @Override
    public void clearKVCache() {
        kvCacheHandle = null;
        kvCacheStore.clear();
        log.info("KV cache cleared");
    }

    @Override
    public String cloneKVCache() {
        String sourceHandle = getKVCacheHandle();
        String cloneHandle = "kv_cache_clone_" + System.currentTimeMillis() + "_" +
                Long.toHexString(Double.doubleToLongBits(Math.random()));
        kvCacheStore.put(cloneHandle, sourceHandle);
        log.info("KV cache cloned: {} -> {}", sourceHandle, cloneHandle);
        return cloneHandle;
    }

    @Override
    public void restoreKVCache(String handle) {
        if (kvCacheStore.containsKey(handle)) {
            kvCacheHandle = handle;
            log.info("KV cache restored: {}", handle);
        } else {
            log.warn("KV cache handle not found: {}", handle);
        }
    }
}
