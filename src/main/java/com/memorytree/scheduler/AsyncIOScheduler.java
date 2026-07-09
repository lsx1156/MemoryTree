package com.memorytree.scheduler;

import com.memorytree.dto.GenerateResult;
import com.memorytree.memory.MemoryBackend;
import com.memorytree.memory.WorkingMemory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

@Slf4j
@Component
public class AsyncIOScheduler {

    private final ExecutorService ioExecutor;
    private final WorkingMemory workingMemory;
    private final MemoryBackend memoryBackend;

    public AsyncIOScheduler(WorkingMemory workingMemory, MemoryBackend memoryBackend) {
        this.workingMemory = workingMemory;
        this.memoryBackend = memoryBackend;
        this.ioExecutor = Executors.newFixedThreadPool(2);
        log.info("Async IO scheduler initialized");
    }

    public Future<List<String>> preRetrieveMemory(String query) {
        return ioExecutor.submit(() -> {
            log.debug("Pre-retrieving memory for query: {}", query);
            try {
                Thread.sleep(100);
                List<String> results = new ArrayList<>();
                memoryBackend.getAll().forEach(entry -> {
                    if (entry.getContent() != null && 
                        (entry.getContent().contains(query) ||
                         entry.getTags() != null && entry.getTags().stream().anyMatch(t -> t.contains(query)))) {
                        results.add(entry.getContent());
                    }
                });
                log.debug("Memory pre-retrieval completed: {} results", results.size());
                return results;
            } catch (Exception e) {
                log.error("Memory pre-retrieval failed: {}", e.getMessage());
                return new ArrayList<>();
            }
        });
    }

    public void asyncWriteLog(String logContent) {
        ioExecutor.submit(() -> {
            try {
                log.debug("Writing log asynchronously");
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    public CompletableFuture<Void> asyncStoreMemory(GenerateResult result) {
        return CompletableFuture.runAsync(() -> {
            log.debug("Async storing memory");
            try {
                Thread.sleep(80);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, ioExecutor);
    }

    public void shutdown() {
        ioExecutor.shutdown();
    }
}