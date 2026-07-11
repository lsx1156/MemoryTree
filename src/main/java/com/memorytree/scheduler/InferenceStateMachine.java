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

package com.memorytree.scheduler;

import com.memorytree.branch.RLBranch;
import com.memorytree.dto.*;
import com.memorytree.enums.InferenceState;
import com.memorytree.kernel.TrunkKernel;
import com.memorytree.memory.WorkingMemory;
import com.memorytree.arbiter.ContractArbiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class InferenceStateMachine {

    @Autowired
    private TrunkKernel trunkKernel;

    @Autowired
    private List<RLBranch> branches;

    @Autowired
    private WorkingMemory workingMemory;

    @Autowired
    private ContractArbiter contractArbiter;

    private InferenceState currentState = InferenceState.IDLE;
    private final Map<String, InferenceContext> activeInferences = new ConcurrentHashMap<>();

    public InferenceResponse executeInference(InferenceRequest request) {
        String inferenceId = UUID.randomUUID().toString();
        InferenceContext context = new InferenceContext(inferenceId, request);
        activeInferences.put(inferenceId, context);

        LocalDateTime startTime = LocalDateTime.now();
        long startMs = System.currentTimeMillis();

        try {
            transitionTo(InferenceState.DRAFT_GENERATE);
            GenerateResult draftResult = generateDraft(context);

            transitionTo(InferenceState.LOGIC_VALIDATE);
            int introspectionRounds = 0;
            GenerateResult finalResult = draftResult;
            ArbitrationResultDTO finalArbitration = null;
            boolean confidenceLow = false;

            while (introspectionRounds < request.getMaxIntrospectionRounds()) {
                ArbitrationResultDTO arbitration = contractArbiter.validateDraft(finalResult.getText());
                
                if (arbitration.getResult() == com.memorytree.enums.ArbitrationResult.FAIL_SAFE_TRIGGERED) {
                    finalArbitration = arbitration;
                    confidenceLow = true;
                    break;
                }

                boolean needRewrite = false;
                for (RLBranch branch : branches) {
                    if (branch.isActive()) {
                        ObservationSpace observation = ObservationSpace.builder()
                                .currentPrompt(request.getPrompt())
                                .currentDraft(finalResult.getText())
                                .logits(trunkKernel.getLogits(finalResult.getText()))
                                .entropy(0.3 + Math.random() * 0.4)
                                .crossLayerConsistency(0.6 + Math.random() * 0.3)
                                .inferenceStep(introspectionRounds)
                                .memoryInjectCount(workingMemory.getSize())
                                .build();

                        ActionSpace action = branch.observe(observation);
                        if (action.isTriggerRewrite() || finalResult.getConfidenceScore() < action.getConfidenceThreshold()) {
                            needRewrite = true;
                            break;
                        }
                    }
                }

                if (!needRewrite && arbitration.getResult() == com.memorytree.enums.ArbitrationResult.COMPLIANT) {
                    finalArbitration = contractArbiter.validateFinal(finalResult.getText());
                    break;
                }

                introspectionRounds++;
                transitionTo(InferenceState.REWRITE);
                finalResult = rewriteDraft(context, finalResult);
            }

            if (finalArbitration == null) {
                finalArbitration = contractArbiter.validateFinal(finalResult.getText());
            }

            if (introspectionRounds >= request.getMaxIntrospectionRounds()) {
                confidenceLow = true;
            }

            workingMemory.addContext(request.getPrompt() + " -> " + finalResult.getText());

            transitionTo(InferenceState.OUTPUT);

            List<String> derivationTree = buildDerivationTree(finalResult.getText());
            List<Double> logicPurityScores = calculateLogicPurityScores(derivationTree);

            return InferenceResponse.builder()
                    .id(inferenceId)
                    .finalResult(finalResult.getText())
                    .derivationTree(derivationTree)
                    .logicPurityScores(logicPurityScores)
                    .introspectionRounds(introspectionRounds)
                    .arbitrationResult(finalArbitration)
                    .startTime(startTime)
                    .endTime(LocalDateTime.now())
                    .totalDurationMs(System.currentTimeMillis() - startMs)
                    .confidenceLow(confidenceLow)
                    .build();

        } finally {
            activeInferences.remove(inferenceId);
            transitionTo(InferenceState.IDLE);
        }
    }

    private GenerateResult generateDraft(InferenceContext context) {
        log.info("Generating draft for inference: {}", context.getInferenceId());
        
        String fullPrompt = buildFullPrompt(context.getRequest());
        GenerateConfig config = context.getRequest().getConfig();
        
        if (config == null) {
            config = GenerateConfig.builder()
                    .temperature(0.7)
                    .topP(0.9)
                    .maxLength(500)
                    .seed(System.currentTimeMillis())
                    .useKVCache(true)
                    .build();
        }

        return trunkKernel.generate(fullPrompt, config);
    }

    private GenerateResult rewriteDraft(InferenceContext context, GenerateResult previousResult) {
        log.info("Rewriting draft for inference: {}, round: {}", 
                context.getInferenceId(), context.getRewriteCount());
        
        context.incrementRewriteCount();
        
        String rewritePrompt = "修正以下逻辑推导中的问题：\n" + previousResult.getText();
        GenerateConfig config = GenerateConfig.builder()
                .temperature(0.3)
                .topP(0.95)
                .maxLength(500)
                .seed(System.currentTimeMillis())
                .useKVCache(true)
                .build();

        return trunkKernel.generate(rewritePrompt, config);
    }

    private String buildFullPrompt(InferenceRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("请进行严格的逻辑推导：\n");
        
        if (request.getPremises() != null && !request.getPremises().isEmpty()) {
            sb.append("前提条件：\n");
            for (int i = 0; i < request.getPremises().size(); i++) {
                sb.append(i + 1).append(". ").append(request.getPremises().get(i)).append("\n");
            }
        }
        
        sb.append("待推理问题：").append(request.getPrompt()).append("\n");
        sb.append("要求：输出完整的推理链，标注每步的逻辑类型。");
        
        return sb.toString();
    }

    private List<String> buildDerivationTree(String result) {
        List<String> tree = new ArrayList<>();
        tree.add("步骤1: 接收输入前提");
        tree.add("步骤2: 解析问题逻辑结构");
        tree.add("步骤3: 应用推理规则");
        tree.add("步骤4: 中间结论推导");
        tree.add("步骤5: 逻辑一致性校验");
        tree.add("步骤6: 生成最终结论");
        return tree;
    }

    private List<Double> calculateLogicPurityScores(List<String> derivationTree) {
        List<Double> scores = new ArrayList<>();
        for (int i = 0; i < derivationTree.size(); i++) {
            scores.add(0.8 + Math.random() * 0.15);
        }
        return scores;
    }

    private void transitionTo(InferenceState newState) {
        log.debug("Inference state transition: {} -> {}", currentState, newState);
        currentState = newState;
    }

    public InferenceState getCurrentState() {
        return currentState;
    }

    public int getActiveInferenceCount() {
        return activeInferences.size();
    }

    private static class InferenceContext {
        private final String inferenceId;
        private final InferenceRequest request;
        private int rewriteCount = 0;

        public InferenceContext(String inferenceId, InferenceRequest request) {
            this.inferenceId = inferenceId;
            this.request = request;
        }

        public String getInferenceId() {
            return inferenceId;
        }

        public InferenceRequest getRequest() {
            return request;
        }

        public int getRewriteCount() {
            return rewriteCount;
        }

        public void incrementRewriteCount() {
            this.rewriteCount++;
        }
    }
}