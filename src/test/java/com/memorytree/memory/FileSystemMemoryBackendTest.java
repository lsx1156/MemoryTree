package com.memorytree.memory;

import com.memorytree.dto.MemoryEntry;
import com.memorytree.dto.MemoryQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class FileSystemMemoryBackendTest {

    @TempDir
    Path tempDir;

    private FileSystemMemoryBackend backend;
    private EmbeddingService embeddingService;

    @BeforeEach
    void setUp() {
        embeddingService = new EmbeddingService();
        ReflectionTestUtils.setField(embeddingService, "ollamaBaseUrl", "http://localhost:11434");
        ReflectionTestUtils.setField(embeddingService, "embeddingModel", "qwen2.5:7b");
        ReflectionTestUtils.setField(embeddingService, "embeddingEnabled", false);

        backend = new FileSystemMemoryBackend(embeddingService);

        String memoryDir = tempDir.resolve("data/memory").toString();
        String memoryFile = memoryDir + "/memory_store.json";
        ReflectionTestUtils.setField(backend, "MEMORY_DIR", memoryDir);
        ReflectionTestUtils.setField(backend, "MEMORY_FILE", memoryFile);

        backend.initialize();

        for (MemoryEntry entry : backend.getAll()) {
            backend.delete(entry.getId());
        }
    }

    @Test
    void store_assignsIdAndTimestamps() {
        MemoryEntry entry = MemoryEntry.builder()
                .content("测试记忆内容")
                .tags(Arrays.asList("测试", "单元测试"))
                .saliencyScore(0.8)
                .accessCount(0)
                .build();

        MemoryEntry stored = backend.store(entry);

        assertNotNull(stored.getId(), "Stored entry should have an ID");
        assertNotNull(stored.getCreatedAt(), "Stored entry should have createdAt");
        assertNotNull(stored.getLastAccessedAt(), "Stored entry should have lastAccessedAt");
        assertNotNull(stored.getEmbedding(), "Stored entry should have embedding generated");
        assertFalse(stored.getEmbedding().isEmpty(), "Embedding should not be empty");
        assertEquals(0.8, stored.getHeat(), 0.001, "Heat should be initialized from saliencyScore");
    }

    @Test
    void store_existingId_replacesEntry() {
        MemoryEntry entry = MemoryEntry.builder()
                .id("test-001")
                .content("原始内容")
                .tags(Arrays.asList("测试"))
                .saliencyScore(0.5)
                .build();
        backend.store(entry);

        MemoryEntry updated = MemoryEntry.builder()
                .id("test-001")
                .content("更新后的内容")
                .tags(Arrays.asList("测试", "更新"))
                .saliencyScore(0.9)
                .build();
        backend.store(updated);

        assertEquals(1, backend.getTotalCount(), "Store with same ID should replace, not add");
        Optional<MemoryEntry> retrieved = backend.retrieve("test-001");
        assertTrue(retrieved.isPresent());
        assertEquals("更新后的内容", retrieved.get().getContent());
    }

    @Test
    void query_byKeyword_returnsMatchingEntries() {
        backend.store(MemoryEntry.builder()
                .content("人工智能是计算机科学的分支")
                .tags(Arrays.asList("AI"))
                .saliencyScore(0.8)
                .build());
        backend.store(MemoryEntry.builder()
                .content("生物学是研究生命的科学")
                .tags(Arrays.asList("生物"))
                .saliencyScore(0.7)
                .build());

        MemoryQuery query = MemoryQuery.builder()
                .keyword("计算机")
                .limit(10)
                .build();

        List<MemoryEntry> results = backend.query(query);
        assertEquals(1, results.size(), "Keyword search should return only matching entries");
        assertTrue(results.get(0).getContent().contains("计算机"));
    }

    @Test
    void query_byTags_returnsMatchingEntries() {
        backend.store(MemoryEntry.builder()
                .content("逻辑推理基础")
                .tags(Arrays.asList("逻辑", "推理"))
                .saliencyScore(0.8)
                .build());
        backend.store(MemoryEntry.builder()
                .content("数学证明方法")
                .tags(Arrays.asList("数学", "证明"))
                .saliencyScore(0.7)
                .build());

        MemoryQuery query = MemoryQuery.builder()
                .tags(Arrays.asList("逻辑"))
                .limit(10)
                .build();

        List<MemoryEntry> results = backend.query(query);
        assertEquals(1, results.size(), "Tag search should return only matching entries");
        assertTrue(results.get(0).getTags().contains("逻辑"));
    }

    @Test
    void retrieve_incrementsAccessCount() {
        backend.store(MemoryEntry.builder()
                .id("access-test")
                .content("测试访问计数")
                .tags(Arrays.asList("测试"))
                .saliencyScore(0.5)
                .build());

        backend.retrieve("access-test");
        backend.retrieve("access-test");

        Optional<MemoryEntry> retrieved = backend.retrieve("access-test");
        assertTrue(retrieved.isPresent());
        assertEquals(3, retrieved.get().getAccessCount(), "Access count should be incremented to 3");
    }

    @Test
    void delete_removesEntry() {
        backend.store(MemoryEntry.builder()
                .id("delete-test")
                .content("待删除的记忆")
                .tags(Arrays.asList("测试"))
                .saliencyScore(0.5)
                .build());
        assertEquals(1, backend.getTotalCount());

        backend.delete("delete-test");
        assertEquals(0, backend.getTotalCount(), "Entry should be deleted");
        assertTrue(backend.retrieve("delete-test").isEmpty(), "Deleted entry should not be retrievable");
    }

    @Test
    void semanticSearch_returnsResultsBySimilarity() {
        String content = "机器学习是人工智能的核心技术";
        backend.store(MemoryEntry.builder()
                .content(content)
                .tags(Arrays.asList("AI", "ML"))
                .saliencyScore(0.9)
                .build());

        List<MemoryEntry> results = backend.semanticSearch(content, 10);
        assertFalse(results.isEmpty(), "Semantic search with identical text should return results");
        assertEquals(content, results.get(0).getContent(), "Most similar result should be the identical text");
    }

    @Test
    void semanticSearch_emptyQuery_returnsEmptyList() {
        List<MemoryEntry> results = backend.semanticSearch("", 10);
        assertTrue(results.isEmpty(), "Empty query should return empty list");
        assertTrue(backend.semanticSearch(null, 10).isEmpty(), "Null query should return empty list");
    }

    @Test
    void getAll_returnsAllEntries() {
        backend.store(MemoryEntry.builder().content("记忆A").tags(Arrays.asList("a")).saliencyScore(0.5).build());
        backend.store(MemoryEntry.builder().content("记忆B").tags(Arrays.asList("b")).saliencyScore(0.5).build());
        backend.store(MemoryEntry.builder().content("记忆C").tags(Arrays.asList("c")).saliencyScore(0.5).build());

        List<MemoryEntry> all = backend.getAll();
        assertEquals(3, all.size(), "getAll should return all stored entries");
    }

    @Test
    void decayAllHeat_reducesHeatValues() {
        backend.store(MemoryEntry.builder()
                .id("decay-test")
                .content("测试热度衰减")
                .tags(Arrays.asList("测试"))
                .saliencyScore(1.0)
                .build());

        backend.decayAllHeat(0.5);

        Optional<MemoryEntry> retrieved = backend.retrieve("decay-test");
        assertTrue(retrieved.isPresent());
        assertEquals(0.5, retrieved.get().getHeat(), 0.01, "Heat should be reduced by 50% after decay rate 0.5");
    }
}
