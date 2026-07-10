package com.memorytree.memory;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

@Slf4j
@Service
public class EmbeddingService {

    @Value("${memorytree.ollama.base-url:http://localhost:11434}")
    private String ollamaBaseUrl;

    @Value("${memorytree.ollama.embedding.model:qwen2.5:7b}")
    private String embeddingModel;

    @Value("${memorytree.embedding.enabled:true}")
    private boolean embeddingEnabled;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final Random random = new Random();

    @PostConstruct
    public void init() {
        log.info("EmbeddingService initialized - enabled: {}, model: {}", embeddingEnabled, embeddingModel);
    }

    public List<Double> generateEmbedding(String text) {
        if (!embeddingEnabled || text == null || text.trim().isEmpty()) {
            return generateFallbackEmbedding(text);
        }

        try {
            String jsonBody = String.format("{\"model\": \"%s\", \"prompt\": \"%s\"}", embeddingModel, escapeJson(text));
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(ollamaBaseUrl + "/api/embeddings"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .timeout(java.time.Duration.ofSeconds(30))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return parseEmbeddingResponse(response.body());
            } else {
                log.warn("Embedding API failed with status {}: {}", response.statusCode(), response.body());
                return generateFallbackEmbedding(text);
            }

        } catch (IOException | InterruptedException e) {
            log.warn("Embedding API call failed: {}", e.getMessage());
            return generateFallbackEmbedding(text);
        }
    }

    public List<Double> generateFallbackEmbedding(String text) {
        if (text == null || text.trim().isEmpty()) {
            return new ArrayList<>();
        }

        int seed = text.hashCode();
        Random seededRandom = new Random(seed);
        List<Double> embedding = new ArrayList<>();
        
        for (int i = 0; i < 4096; i++) {
            embedding.add(seededRandom.nextGaussian() * 0.1);
        }
        
        return embedding;
    }

    public double cosineSimilarity(List<Double> vector1, List<Double> vector2) {
        if (vector1 == null || vector2 == null || vector1.isEmpty() || vector2.isEmpty()) {
            return 0.0;
        }

        int minLength = Math.min(vector1.size(), vector2.size());
        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (int i = 0; i < minLength; i++) {
            dotProduct += vector1.get(i) * vector2.get(i);
            norm1 += Math.pow(vector1.get(i), 2);
            norm2 += Math.pow(vector2.get(i), 2);
        }

        if (norm1 == 0 || norm2 == 0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    private String escapeJson(String text) {
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }

    private List<Double> parseEmbeddingResponse(String responseBody) {
        try {
            int startIndex = responseBody.indexOf("[");
            int endIndex = responseBody.lastIndexOf("]");

            if (startIndex == -1 || endIndex == -1 || startIndex >= endIndex) {
                log.warn("Failed to parse embedding response: {}", responseBody);
                return new ArrayList<>();
            }

            String arrayStr = responseBody.substring(startIndex, endIndex + 1);
            String[] elements = arrayStr.substring(1, arrayStr.length() - 1).split(",");
            
            List<Double> embedding = new ArrayList<>();
            for (String element : elements) {
                embedding.add(Double.parseDouble(element.trim()));
            }

            log.debug("Generated embedding with {} dimensions", embedding.size());
            return embedding;

        } catch (Exception e) {
            log.warn("Failed to parse embedding: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    public boolean isEnabled() {
        return embeddingEnabled;
    }

    public String getModel() {
        return embeddingModel;
    }
}