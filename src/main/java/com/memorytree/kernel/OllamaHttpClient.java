package com.memorytree.kernel;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 负责与 Ollama HTTP API 通信：构建请求 JSON、发送同步/流式请求。
 */
@Slf4j
public class OllamaHttpClient {

    private final String modelName;
    private final String baseUrl;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OllamaHttpClient(String modelName, String baseUrl) {
        this.modelName = modelName;
        this.baseUrl = baseUrl;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public String getModelName() {
        return modelName;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    /**
     * 构建符合 Ollama /api/generate 的请求 JSON。
     */
    public String buildRequestJson(String prompt, double temperature, double topP, int numPredict, boolean stream) {
        try {
            Map<String, Object> request = new HashMap<>();
            request.put("model", modelName);
            request.put("prompt", prompt);
            request.put("stream", stream);

            Map<String, Object> options = new HashMap<>();
            options.put("temperature", temperature);
            options.put("top_p", topP);
            options.put("num_predict", numPredict);
            request.put("options", options);

            return objectMapper.writeValueAsString(request);
        } catch (Exception e) {
            throw new RuntimeException("Failed to build request JSON", e);
        }
    }

    /**
     * 同步发送 generate 请求，返回完整响应体字符串。
     */
    public HttpResponse<String> sendGenerate(String requestJson) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/generate"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                .timeout(Duration.ofSeconds(120))
                .build();

        log.info("Calling Ollama API: model={}, baseUrl={}", modelName, baseUrl);
        log.debug("Request body: {}", requestJson);

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    /**
     * 异步流式发送 generate 请求，逐行回调消费。
     */
    public CompletableFuture<Void> sendGenerateStream(String requestJson,
                                                      java.util.function.Consumer<String> lineConsumer) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/generate"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                .timeout(Duration.ofSeconds(120))
                .build();

        log.info("Calling Ollama API (streaming): model={}", modelName);

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofLines())
                .thenApply(HttpResponse::body)
                .thenAccept(lines -> lines.forEach(line -> {
                    if (line != null && !line.isEmpty()) {
                        lineConsumer.accept(line);
                    }
                }));
    }
}
