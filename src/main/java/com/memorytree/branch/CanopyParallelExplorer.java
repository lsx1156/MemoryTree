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

package com.memorytree.branch;

import com.memorytree.dto.GenerateConfig;
import com.memorytree.dto.GenerateResult;
import com.memorytree.kernel.OllamaTrunkKernel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

@Slf4j
@Component
public class CanopyParallelExplorer {

    @Autowired
    private OllamaTrunkKernel trunkKernel;

    private final ExecutorService explorerExecutor;
    private int maxParallelPaths = 3;
    private int timeoutSeconds = 30;

    public CanopyParallelExplorer() {
        int cpuCores = Runtime.getRuntime().availableProcessors();
        this.explorerExecutor = Executors.newFixedThreadPool(Math.min(cpuCores - 1, 4));
    }

    public ParallelExploreResult exploreParallel(String prompt, GenerateConfig baseConfig, int numPaths) {
        int actualPaths = Math.min(numPaths, maxParallelPaths);
        
        if (!trunkKernel.isLoaded()) {
            log.warn("Kernel not loaded, falling back to single path");
            GenerateResult result = trunkKernel.generate(prompt, baseConfig);
            return new ParallelExploreResult(List.of(result), 0, result);
        }

        List<Future<GenerateResult>> futures = new ArrayList<>();
        
        for (int i = 0; i < actualPaths; i++) {
            final int pathIndex = i;
            GenerateConfig pathConfig = createPathConfig(baseConfig, pathIndex);
            
            futures.add(explorerExecutor.submit(() -> {
                try {
                    GenerateResult result = trunkKernel.generate(prompt, pathConfig);
                    result.setMetadata("path_index", pathIndex);
                    return result;
                } catch (Exception e) {
                    log.error("Path {} exploration failed: {}", pathIndex, e.getMessage());
                    return GenerateResult.builder()
                            .content("")
                            .confidence(0.0)
                            .reward(0.0)
                            .build();
                }
            }));
        }

        List<GenerateResult> allResults = new ArrayList<>();
        long startTime = System.currentTimeMillis();
        
        for (Future<GenerateResult> future : futures) {
            try {
                GenerateResult result = future.get(timeoutSeconds, TimeUnit.SECONDS);
                if (result != null && result.getContent() != null && !result.getContent().isEmpty()) {
                    allResults.add(result);
                }
            } catch (TimeoutException e) {
                log.warn("Path exploration timed out");
            } catch (InterruptedException | ExecutionException e) {
                log.error("Path exploration execution failed: {}", e.getMessage());
            }
        }
        
        long duration = System.currentTimeMillis() - startTime;

        if (allResults.isEmpty()) {
            log.warn("All parallel paths failed, returning empty result");
            return new ParallelExploreResult(allResults, duration, null);
        }

        GenerateResult bestResult = selectBestPath(allResults);
        
        log.info("Parallel exploration completed: {} paths in {}ms, best confidence: {}", 
                allResults.size(), duration, bestResult.getConfidence());
        
        return new ParallelExploreResult(allResults, duration, bestResult);
    }

    private GenerateConfig createPathConfig(GenerateConfig baseConfig, int pathIndex) {
        double[] temperatures = {0.7, 1.0, 0.5};
        double[] topPs = {0.9, 0.8, 0.95};
        
        double temp = temperatures[pathIndex % temperatures.length];
        double topP = topPs[pathIndex % topPs.length];
        
        return GenerateConfig.builder()
                .temperature(temp)
                .topP(topP)
                .maxTokens(baseConfig.getMaxTokens())
                .build();
    }

    private GenerateResult selectBestPath(List<GenerateResult> results) {
        if (results == null || results.isEmpty()) {
            return null;
        }

        GenerateResult best = results.get(0);
        double bestScore = calculatePathScore(best);

        for (int i = 1; i < results.size(); i++) {
            GenerateResult current = results.get(i);
            double score = calculatePathScore(current);
            
            if (score > bestScore) {
                best = current;
                bestScore = score;
            }
        }

        return best;
    }

    private double calculatePathScore(GenerateResult result) {
        double confidence = result.getConfidence() != null ? result.getConfidence() : 0.5;
        double reward = result.getReward() != null ? result.getReward() : 0;
        int contentLength = result.getContent() != null ? result.getContent().length() : 0;
        
        double lengthBonus = Math.min(contentLength / 500.0, 1.0);
        
        return confidence * 0.6 + reward * 0.2 + lengthBonus * 0.2;
    }

    public void setMaxParallelPaths(int maxParallelPaths) {
        this.maxParallelPaths = Math.max(1, maxParallelPaths);
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = Math.max(5, timeoutSeconds);
    }

    public void shutdown() {
        explorerExecutor.shutdown();
    }

    public record ParallelExploreResult(
            List<GenerateResult> allPaths,
            long durationMs,
            GenerateResult bestPath
    ) {}
}
