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

import com.memorytree.dto.ObservationSpace;
import com.memorytree.dto.ActionSpace;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

@Slf4j
@Component
public class ParallelBranchEvaluator {

    private ExecutorService branchExecutor;
    private int maxParallelBranches = 4;
    private int threadPoolSize = Math.max(1, Runtime.getRuntime().availableProcessors() - 2);

    public ParallelBranchEvaluator() {
        this.branchExecutor = Executors.newFixedThreadPool(threadPoolSize);
        log.info("Branch executor initialized with {} threads", threadPoolSize);
    }

    public List<ActionSpace> parallelObserve(List<RLBranch> branches, ObservationSpace observation) {
        if (branches == null || branches.isEmpty()) {
            return new ArrayList<>();
        }

        List<Future<ActionSpace>> futures = new ArrayList<>();
        List<ActionSpace> results = new ArrayList<>();

        for (RLBranch branch : branches) {
            if (!branch.isActive()) {
                continue;
            }

            futures.add(branchExecutor.submit(() -> {
                try {
                    return branch.observe(observation);
                } catch (Exception e) {
                    log.error("Branch {} observation failed: {}", branch.getType(), e.getMessage());
                    return ActionSpace.builder()
                            .branchType(branch.getType())
                            .reward(0)
                            .confidence(0)
                            .build();
                }
            }));
        }

        for (Future<ActionSpace> future : futures) {
            try {
                ActionSpace result = future.get(5, TimeUnit.SECONDS);
                if (result != null) {
                    results.add(result);
                }
            } catch (TimeoutException e) {
                log.warn("Branch observation timed out");
            } catch (InterruptedException | ExecutionException e) {
                log.error("Branch observation execution failed: {}", e.getMessage());
            }
        }

        log.info("Parallel branch evaluation completed: {} branches evaluated", results.size());
        return results;
    }

    public ActionSpace arbitrateActions(List<ActionSpace> actions) {
        if (actions == null || actions.isEmpty()) {
            return null;
        }

        double totalWeight = actions.stream()
                .mapToDouble(a -> a.getConfidence() * (a.getReward() + 1))
                .sum();

        if (totalWeight == 0) {
            return actions.get(0);
        }

        double avgTemperature = 0;
        double avgTopP = 0;
        double avgMaxTokens = 0;
        double avgPenalty = 0;

        for (ActionSpace action : actions) {
            double weight = (action.getConfidence() * (action.getReward() + 1)) / totalWeight;
            
            if (action.getTemperature() != null) {
                avgTemperature += action.getTemperature() * weight;
            }
            if (action.getTopP() != null) {
                avgTopP += action.getTopP() * weight;
            }
            if (action.getMaxTokens() != null) {
                avgMaxTokens += action.getMaxTokens() * weight;
            }
            if (action.getPenalty() != null) {
                avgPenalty += action.getPenalty() * weight;
            }
        }

        return ActionSpace.builder()
                .branchType(actions.get(0).getBranchType())
                .temperature(avgTemperature)
                .topP(avgTopP)
                .maxTokens((int) avgMaxTokens)
                .penalty(avgPenalty)
                .reward(actions.stream().mapToDouble(ActionSpace::getReward).average().orElse(0))
                .confidence(actions.stream().mapToDouble(ActionSpace::getConfidence).average().orElse(0))
                .build();
    }

    public void shutdown() {
        branchExecutor.shutdown();
    }

    public void setMaxParallelBranches(int maxParallelBranches) {
        this.maxParallelBranches = Math.max(1, maxParallelBranches);
    }

    public void setThreadPoolSize(int threadPoolSize) {
        if (threadPoolSize > 0 && threadPoolSize != this.threadPoolSize) {
            this.threadPoolSize = threadPoolSize;
            ExecutorService oldExecutor = this.branchExecutor;
            this.branchExecutor = Executors.newFixedThreadPool(threadPoolSize);
            oldExecutor.shutdown();
            log.info("Branch executor reconfigured with {} threads", threadPoolSize);
        }
    }

    public int getActiveBranchCount() {
        if (branchExecutor instanceof ThreadPoolExecutor) {
            return ((ThreadPoolExecutor) branchExecutor).getActiveCount();
        }
        return 0;
    }
}