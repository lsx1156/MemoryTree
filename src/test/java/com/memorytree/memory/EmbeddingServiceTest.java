package com.memorytree.memory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EmbeddingServiceTest {

    private EmbeddingService embeddingService;

    @BeforeEach
    void setUp() {
        embeddingService = new EmbeddingService();
        ReflectionTestUtils.setField(embeddingService, "ollamaBaseUrl", "http://localhost:11434");
        ReflectionTestUtils.setField(embeddingService, "embeddingModel", "qwen2.5:7b");
        ReflectionTestUtils.setField(embeddingService, "embeddingEnabled", false);
    }

    @Test
    void cosineSimilarity_identicalVectors_returnsOne() {
        List<Double> vector = Arrays.asList(1.0, 2.0, 3.0, 4.0);
        double similarity = embeddingService.cosineSimilarity(vector, vector);
        assertEquals(1.0, similarity, 0.0001, "Identical vectors should have cosine similarity of 1.0");
    }

    @Test
    void cosineSimilarity_orthogonalVectors_returnsZero() {
        List<Double> v1 = Arrays.asList(1.0, 0.0);
        List<Double> v2 = Arrays.asList(0.0, 1.0);
        double similarity = embeddingService.cosineSimilarity(v1, v2);
        assertEquals(0.0, similarity, 0.0001, "Orthogonal vectors should have cosine similarity of 0.0");
    }

    @Test
    void cosineSimilarity_nullOrEmpty_returnsZero() {
        assertEquals(0.0, embeddingService.cosineSimilarity(null, Arrays.asList(1.0)), "Null v1 should return 0");
        assertEquals(0.0, embeddingService.cosineSimilarity(Arrays.asList(1.0), null), "Null v2 should return 0");
        assertEquals(0.0, embeddingService.cosineSimilarity(Collections.emptyList(), Arrays.asList(1.0)), "Empty v1 should return 0");
    }

    @Test
    void cosineSimilarity_differentLengths_usesMinLength() {
        List<Double> v1 = Arrays.asList(1.0, 2.0, 3.0);
        List<Double> v2 = Arrays.asList(1.0, 2.0, 3.0, 4.0, 5.0);
        double similarity = embeddingService.cosineSimilarity(v1, v2);
        assertEquals(1.0, similarity, 0.0001, "Vectors with shared prefix should have similarity close to 1.0");
    }

    @Test
    void generateFallbackEmbedding_returns4096Dimensions() {
        List<Double> embedding = embeddingService.generateFallbackEmbedding("test text");
        assertEquals(4096, embedding.size(), "Fallback embedding should have 4096 dimensions");
    }

    @Test
    void generateFallbackEmbedding_isDeterministic() {
        List<Double> e1 = embeddingService.generateFallbackEmbedding("hello world");
        List<Double> e2 = embeddingService.generateFallbackEmbedding("hello world");
        assertEquals(e1, e2, "Same text should produce identical fallback embeddings");
    }

    @Test
    void generateFallbackEmbedding_differentTexts_produceDifferentEmbeddings() {
        List<Double> e1 = embeddingService.generateFallbackEmbedding("text one");
        List<Double> e2 = embeddingService.generateFallbackEmbedding("text two different");
        assertNotEquals(e1, e2, "Different texts should produce different embeddings");
    }

    @Test
    void generateFallbackEmbedding_emptyText_returnsEmptyList() {
        List<Double> embedding = embeddingService.generateFallbackEmbedding("");
        assertTrue(embedding.isEmpty(), "Empty text should return empty embedding list");
        assertTrue(embeddingService.generateFallbackEmbedding(null).isEmpty(), "Null text should return empty embedding list");
    }
}
