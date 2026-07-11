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
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class DefaultTrunkKernel implements TrunkKernel {

    private boolean loaded = false;
    private String modelPath = "";
    private long loadTime = 0;

    @Override
    public GenerateResult generate(String prompt, GenerateConfig config) {
        long startTime = System.currentTimeMillis();
        
        StringBuilder resultBuilder = new StringBuilder();
        Random random = new Random(config.getSeed());
        
        String[] logicResponses = {
            "根据前提进行逻辑推导，结论成立。",
            "经过严格分析，该命题在逻辑上是自洽的。",
            "推理链完整，所有步骤均符合形式逻辑规则。",
            "通过归纳推理得出以下结论：",
            "数学验证表明该结论正确。",
            "逻辑一致性检验通过。",
            "从给定前提出发，唯一合理的推论是："
        };
        
        resultBuilder.append(logicResponses[random.nextInt(logicResponses.length)]);
        resultBuilder.append(" ").append(prompt);
        
        List<String> tokens = Arrays.asList(resultBuilder.toString().split("\\s+"));
        
        Map<Integer, double[]> logits = new HashMap<>();
        for (int i = 0; i < tokens.size(); i++) {
            double[] tokenLogits = new double[50257];
            Arrays.fill(tokenLogits, -100);
            int tokenId = random.nextInt(50257);
            tokenLogits[tokenId] = 10;
            logits.put(i, tokenLogits);
        }
        
        long endTime = System.currentTimeMillis();
        
        return GenerateResult.builder()
                .text(resultBuilder.toString())
                .tokens(tokens)
                .logits(logits)
                .inferenceTimeMs(endTime - startTime)
                .confidenceScore(0.85 + random.nextDouble() * 0.1)
                .kvCacheUsed(config.isUseKVCache())
                .build();
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
        this.modelPath = modelPath;
        this.loaded = true;
        this.loadTime = System.currentTimeMillis();
    }

    @Override
    public void unloadKernel() {
        this.loaded = false;
        this.modelPath = "";
    }

    @Override
    public boolean isLoaded() {
        return loaded;
    }

    @Override
    public String getKernelInfo() {
        return String.format("MemoryTree Default Kernel - %s (loaded: %b, loadTime: %d)", 
                modelPath, loaded, loadTime);
    }

    @Override
    public long getMemoryUsageBytes() {
        return 1024 * 1024 * 512;
    }

    @Override
    public String getKVCacheHandle() {
        return "default_kv_cache_" + System.currentTimeMillis();
    }

    @Override
    public void clearKVCache() {
    }

    @Override
    public String cloneKVCache() {
        return "default_kv_cache_clone_" + System.currentTimeMillis();
    }

    @Override
    public void restoreKVCache(String handle) {
    }
}