package com.memorytree.kernel;

import com.memorytree.dto.GenerateConfig;
import com.memorytree.dto.GenerateResult;
import com.memorytree.dto.IntrospectionRecord;
import com.memorytree.system.GatingEvent;
import com.memorytree.branch.CanopyParallelExplorer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Slf4j
@Service
public class IntrospectiveInferenceService {

    private final TrunkKernel trunkKernel;
    
    @Autowired(required = false)
    private CanopyParallelExplorer canopyParallelExplorer;
    
    private boolean enableParallelExploration = true;
    private int parallelPathCount = 3;

    public IntrospectiveInferenceService(TrunkKernel trunkKernel) {
        this.trunkKernel = trunkKernel;
    }

    public GenerateResult performIntrospectiveInference(String prompt, String premises, 
                                                        int maxIntrospectionRounds, 
                                                        double generateTemperature,
                                                        double validateTemperature) {
        return performIntrospectiveInference(prompt, premises, maxIntrospectionRounds, 
                generateTemperature, validateTemperature, null);
    }

    public GenerateResult performIntrospectiveInference(String prompt, String premises, 
                                                        int maxIntrospectionRounds, 
                                                        double generateTemperature,
                                                        double validateTemperature,
                                                        Consumer<String> stateCallback) {
        List<IntrospectionRecord> records = new ArrayList<>();
        List<GatingEvent> gatingEvents = new ArrayList<>();
        GenerateResult currentResult = null;
        boolean needsRewrite = false;
        int round = 0;

        while (round < maxIntrospectionRounds) {
            round++;
            log.info("Introspection round {} of {}", round, maxIntrospectionRounds);

            IntrospectionRecord record = new IntrospectionRecord();
            record.setRound(round);

            if (round == 1 || needsRewrite) {
                record.setAction("DRAFT_GENERATE");
                
                GenerateConfig generateConfig = GenerateConfig.builder()
                        .temperature(generateTemperature)
                        .seed(round * 1000)
                        .useKVCache(true)
                        .maxTokens(2048)
                        .build();

                String fullPrompt = buildGeneratePrompt(prompt, premises);
                
                if (round == 1 && enableParallelExploration && canopyParallelExplorer != null) {
                    CanopyParallelExplorer.ParallelExploreResult exploreResult = 
                            canopyParallelExplorer.exploreParallel(fullPrompt, generateConfig, parallelPathCount);
                    
                    currentResult = exploreResult.bestPath();
                    record.setGeneratedText(currentResult.getText());
                    record.setInferenceTimeMs(exploreResult.durationMs());
                    record.setMetadata("parallel_paths", exploreResult.allPaths().size());
                    
                    log.info("Parallel exploration completed: {} paths in {}ms, best confidence: {}", 
                            exploreResult.allPaths().size(), exploreResult.durationMs(), 
                            currentResult.getConfidence());
                } else {
                    currentResult = trunkKernel.generate(fullPrompt, generateConfig);
                    record.setGeneratedText(currentResult.getText());
                    record.setInferenceTimeMs(currentResult.getInferenceTimeMs());
                    
                    log.info("Draft generated in {}ms", currentResult.getInferenceTimeMs());
                }
                
                if (stateCallback != null) {
                    stateCallback.accept("DRAFT_GENERATE");
                }
            }

            record.setAction("LOGIC_VALIDATE");
            
            GenerateConfig validateConfig = GenerateConfig.builder()
                    .temperature(validateTemperature)
                    .seed(round * 2000)
                    .useKVCache(true)
                    .maxTokens(1024)
                    .build();

            String validatePrompt = buildValidatePrompt(prompt, premises, currentResult.getText());
            GenerateResult validationResult = trunkKernel.generate(validatePrompt, validateConfig);
            
            record.setValidationResult(validationResult.getText());
            record.setValidationTimeMs(validationResult.getInferenceTimeMs());

            ValidationAnalysis analysis = analyzeValidationResult(validationResult.getText());
            record.setValid(analysis.isValid());
            record.setConfidenceScore(analysis.getConfidence());
            record.setIssues(analysis.getIssues());

            log.info("Validation result: valid={}, confidence={}, issues={}", 
                    analysis.isValid(), analysis.getConfidence(), analysis.getIssues());

            if (stateCallback != null) {
                stateCallback.accept("LOGIC_VALIDATE");
            }

            if (analysis.isValid()) {
                record.setAction("OUTPUT");
                record.setFinal(true);

                gatingEvents.add(GatingEvent.logicThreshold(
                        "推理通过逻辑校验", 0.6, analysis.getConfidence(), true));
                records.add(record);
                break;
            }

            if (round < maxIntrospectionRounds) {
                needsRewrite = true;
                record.setAction("REWRITE");

                gatingEvents.add(GatingEvent.paragraphRewrite(
                        "第" + round + "轮推理不通过，触发重写"));
                gatingEvents.add(GatingEvent.logicThreshold(
                        "置信度低于阈值", 0.6, analysis.getConfidence(), false));

                GenerateConfig rewriteConfig = GenerateConfig.builder()
                        .temperature(generateTemperature * 0.8)
                        .seed(round * 3000)
                        .useKVCache(true)
                        .maxTokens(2048)
                        .build();

                String rewritePrompt = buildRewritePrompt(prompt, premises,
                        currentResult.getText(), analysis.getIssues());
                currentResult = trunkKernel.generate(rewritePrompt, rewriteConfig);
                record.setRewrittenText(currentResult.getText());
                record.setRewriteTimeMs(currentResult.getInferenceTimeMs());

                log.info("Rewrite performed in {}ms", currentResult.getInferenceTimeMs());
                
                if (stateCallback != null) {
                    stateCallback.accept("REWRITE");
                }
                
                records.add(record);
            } else {
                record.setFinal(true);
                record.setAction("OUTPUT");
                log.warn("Max introspection rounds reached, outputting final result");
                records.add(record);
                break;
            }
        }

        if (currentResult != null) {
            currentResult.setIntrospectionRecords(records);
            currentResult.setIntrospectionRounds(round);
            double avgConfidence = records.stream()
                    .mapToDouble(IntrospectionRecord::getConfidenceScore)
                    .average().orElse(0.5);
            currentResult.setConfidenceScore(avgConfidence);
            
            List<String> derivationTree = extractDerivationTree(currentResult.getText());
            currentResult.setDerivationTree(derivationTree);
            
            List<Double> purityScores = calculateLogicPurityScores(derivationTree, currentResult.getText());
            currentResult.setLogicPurityScores(purityScores);
            
            currentResult.setGatingEvents(gatingEvents);
        }

        return currentResult;
    }

    private List<String> extractDerivationTree(String text) {
        List<String> tree = new ArrayList<>();
        if (text == null) {
            return tree;
        }

        String[] lines = text.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            
            if (line.matches("^[\\d一二三四五六七八九十]+[.、]\\s.*") ||
                line.matches("^[\\-*•]\\s.*") ||
                line.matches("^\\([\\d一二三四五六七八九十]+\\)\\s.*")) {
                tree.add(line);
            } else if (line.length() < 100) {
                tree.add(line);
            }
        }

        if (tree.isEmpty()) {
            tree.add(text.substring(0, Math.min(text.length(), 200)));
        }

        return tree;
    }

    private List<Double> calculateLogicPurityScores(List<String> derivationTree, String fullText) {
        List<Double> scores = new ArrayList<>();
        
        for (String step : derivationTree) {
            double score = 0.5;
            
            if (step.contains("因为") || step.contains("由于") || step.contains("基于")) {
                score += 0.15;
            }
            if (step.contains("所以") || step.contains("因此") || step.contains("故")) {
                score += 0.15;
            }
            if (step.contains("根据") || step.contains("依据")) {
                score += 0.1;
            }
            if (step.contains("证明") || step.contains("推导") || step.contains("得出")) {
                score += 0.1;
            }
            if (step.contains("假设") || step.contains("假定")) {
                score -= 0.1;
            }
            if (step.contains("可能") || step.contains("也许") || step.contains("大概")) {
                score -= 0.15;
            }
            if (step.matches(".*[=<>≤≥≠≈].*")) {
                score += 0.15;
            }
            if (step.matches(".*\\d+.*")) {
                score += 0.1;
            }
            
            score = Math.max(0.1, Math.min(1.0, score));
            scores.add(Math.round(score * 100) / 100.0);
        }

        return scores;
    }

    private String buildGeneratePrompt(String prompt, String premises) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一个逻辑推理专家。请对给定的问题进行严格的形式逻辑推导。\n\n");
        sb.append("要求：\n");
        sb.append("1. 如果是数学问题，给出计算过程和正确答案\n");
        sb.append("2. 如果是逻辑命题，分析其真值条件\n");
        sb.append("3. 输出完整的推理链，标注每步的逻辑类型（演绎、归纳、假言推理等）\n");
        sb.append("4. 如果结论不成立，明确指出错误之处\n");
        sb.append("5. 不要添加任何修辞、情感或客套话\n");
        sb.append("6. 用中文输出\n\n");
        
        if (premises != null && !premises.trim().isEmpty()) {
            sb.append("前提条件：\n");
            String[] premiseLines = premises.split("\n");
            for (int i = 0; i < premiseLines.length; i++) {
                sb.append(i + 1).append(". ").append(premiseLines[i]).append("\n");
            }
            sb.append("\n");
        }
        
        sb.append("问题：").append(prompt).append("\n\n");
        sb.append("推理：");
        
        return sb.toString();
    }

    private String buildValidatePrompt(String prompt, String premises, String generatedText) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一个逻辑校验专家。请对以下推理结果进行严格的逻辑校验。\n\n");
        sb.append("校验标准：\n");
        sb.append("1. 前提条件是否被正确使用\n");
        sb.append("2. 推理链是否完整，有无逻辑跳跃\n");
        sb.append("3. 结论是否从前提必然得出\n");
        sb.append("4. 是否存在逻辑谬误（如循环论证、偷换概念等）\n");
        sb.append("5. 数学计算是否正确\n\n");
        
        if (premises != null && !premises.trim().isEmpty()) {
            sb.append("前提条件：\n").append(premises).append("\n\n");
        }
        
        sb.append("问题：").append(prompt).append("\n\n");
        sb.append("待校验的推理结果：\n").append(generatedText).append("\n\n");
        sb.append("请输出校验结果，格式如下：\n");
        sb.append("VALID: true/false\n");
        sb.append("CONFIDENCE: 0.0-1.0\n");
        sb.append("ISSUES: [问题列表，用逗号分隔]\n");
        sb.append("EXPLANATION: [详细说明]\n");
        
        return sb.toString();
    }

    private String buildRewritePrompt(String prompt, String premises, 
                                       String originalText, List<String> issues) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一个逻辑推理专家。请根据以下问题和前提重新进行推理。\n\n");
        sb.append("原推理中发现以下问题：\n");
        for (int i = 0; i < issues.size(); i++) {
            sb.append(i + 1).append(". ").append(issues.get(i)).append("\n");
        }
        sb.append("\n");
        
        if (premises != null && !premises.trim().isEmpty()) {
            sb.append("前提条件：\n").append(premises).append("\n\n");
        }
        
        sb.append("问题：").append(prompt).append("\n\n");
        sb.append("原推理结果：\n").append(originalText).append("\n\n");
        sb.append("请修正以上问题，重新输出推理结果：\n");
        
        return sb.toString();
    }

    private ValidationAnalysis analyzeValidationResult(String validationText) {
        ValidationAnalysis analysis = new ValidationAnalysis();
        analysis.setValid(true);
        analysis.setConfidence(0.5);
        analysis.setIssues(new ArrayList<>());

        if (validationText == null || validationText.isEmpty()) {
            return analysis;
        }

        String[] lines = validationText.split("\n");
        for (String line : lines) {
            line = line.trim();
            
            if (line.toUpperCase().startsWith("VALID:")) {
                String value = line.substring(6).trim().toLowerCase();
                analysis.setValid(value.equals("true"));
            } else if (line.toUpperCase().startsWith("CONFIDENCE:")) {
                try {
                    analysis.setConfidence(Double.parseDouble(line.substring(11).trim()));
                } catch (NumberFormatException e) {
                    analysis.setConfidence(0.5);
                }
            } else if (line.toUpperCase().startsWith("ISSUES:")) {
                String issuesStr = line.substring(7).trim();
                if (!issuesStr.isEmpty() && !issuesStr.equals("[]")) {
                    String[] issueArray = issuesStr.replace("[", "").replace("]", "").split(",");
                    for (String issue : issueArray) {
                        String trimmedIssue = issue.trim();
                        if (!trimmedIssue.isEmpty()) {
                            analysis.getIssues().add(trimmedIssue);
                        }
                    }
                    analysis.setValid(false);
                }
            }
        }

        if (!analysis.getIssues().isEmpty()) {
            analysis.setValid(false);
        }

        return analysis;
    }

    private static class ValidationAnalysis {
        private boolean valid;
        private double confidence;
        private List<String> issues;

        public ValidationAnalysis() {
            this.issues = new ArrayList<>();
        }

        public boolean isValid() {
            return valid;
        }

        public void setValid(boolean valid) {
            this.valid = valid;
        }

        public double getConfidence() {
            return confidence;
        }

        public void setConfidence(double confidence) {
            this.confidence = confidence;
        }

        public List<String> getIssues() {
            return issues;
        }

        public void setIssues(List<String> issues) {
            this.issues = issues;
        }
    }
}