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

package com.memorytree.arbiter;

import com.memorytree.dto.ArbitrationResultDTO;
import com.memorytree.dto.ContractBook;
import com.memorytree.dto.ContractClause;
import com.memorytree.enums.ArbitrationResult;
import com.memorytree.enums.SeverityLevel;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class DefaultContractArbiter implements ContractArbiter {

    private final String DEFAULT_CONTRACT_PATH;
    private final String CONTRACT_DIR;
    private ContractBook activeContract;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public DefaultContractArbiter() {
        String appData = System.getProperty("user.home") + "/.memorytree";
        this.CONTRACT_DIR = appData + "/data/contract";
        this.DEFAULT_CONTRACT_PATH = CONTRACT_DIR + "/default_contract.json";
    }

    @PostConstruct
    public void init() {
        File contractFile = new File(DEFAULT_CONTRACT_PATH);
        if (contractFile.exists()) {
            try {
                activeContract = objectMapper.readValue(contractFile, ContractBook.class);
            } catch (IOException e) {
                activeContract = createDefaultContract();
            }
        } else {
            activeContract = createDefaultContract();
            persistContract();
        }
    }

    @Override
    public ArbitrationResultDTO validateDraft(String draft) {
        return validate(draft, true);
    }

    @Override
    public ArbitrationResultDTO validateFinal(String finalResult) {
        return validate(finalResult, false);
    }

    @Override
    public ArbitrationResultDTO validate(String text) {
        return validate(text, false);
    }

    private ArbitrationResultDTO validate(String text, boolean isDraft) {
        if (activeContract == null || activeContract.getClauses() == null) {
            return ArbitrationResultDTO.builder()
                    .result(ArbitrationResult.COMPLIANT)
                    .complianceScore(1.0)
                    .explanation("No contract loaded, defaulting to compliant")
                    .build();
        }

        List<String> matchedRules = new ArrayList<>();
        String violatingClauseId = null;
        String violatingClauseName = null;
        double complianceScore = 1.0;
        boolean failSafeTriggered = false;
        int totalEnabledClauses = 0;
        int violatedCount = 0;

        for (ContractClause clause : activeContract.getClauses()) {
            if (!clause.isEnabled()) {
                continue;
            }
            totalEnabledClauses++;
            
            boolean violated = checkClause(text, clause);
            if (violated) {
                matchedRules.add(clause.getName() + ": " + clause.getRule());
                complianceScore -= clause.getSeverity().getWeight();
                violatedCount++;
                
                if (clause.isFailSafe()) {
                    failSafeTriggered = true;
                    violatingClauseId = clause.getId();
                    violatingClauseName = clause.getName();
                    break;
                }
                
                if (violatingClauseId == null) {
                    violatingClauseId = clause.getId();
                    violatingClauseName = clause.getName();
                }
            }
        }

        complianceScore = Math.max(0.0, Math.min(1.0, complianceScore));
        
        if (totalEnabledClauses > 0) {
            double normalizedScore = (totalEnabledClauses - violatedCount) * 1.0 / totalEnabledClauses;
            complianceScore = (complianceScore + normalizedScore) / 2.0;
        }

        ArbitrationResult result;
        if (failSafeTriggered) {
            result = ArbitrationResult.FAIL_SAFE_TRIGGERED;
        } else if (complianceScore < 0.5) {
            result = ArbitrationResult.NON_COMPLIANT;
        } else if (complianceScore < 0.8) {
            result = ArbitrationResult.LOW_CONFIDENCE;
        } else {
            result = ArbitrationResult.COMPLIANT;
        }

        return ArbitrationResultDTO.builder()
                .result(result)
                .violatingClauseId(violatingClauseId)
                .violatingClauseName(violatingClauseName)
                .matchedRules(matchedRules)
                .complianceScore(complianceScore)
                .explanation(buildExplanation(result, matchedRules, complianceScore))
                .build();
    }

    private boolean checkClause(String text, ContractClause clause) {
        String rule = clause.getRule().toLowerCase();
        String textLower = text.toLowerCase();
        String clauseName = clause.getName().toLowerCase();
        
        if (!clause.isEnabled()) {
            return false;
        }
        
        if (clauseName.contains("逻辑推导") || clauseName.contains("推理")) {
            boolean hasLogicWord = textLower.contains("推导") || textLower.contains("推理") || textLower.contains("结论") 
                    || textLower.contains("因此") || textLower.contains("所以") || textLower.contains("得出")
                    || textLower.contains("证明") || textLower.contains("论证") || textLower.contains("分析")
                    || textLower.contains("推断") || textLower.contains("演绎") || textLower.contains("归纳");
            return !hasLogicWord;
        }
        
        if (clauseName.contains("证据") || clauseName.contains("支持")) {
            boolean hasEvidenceWord = textLower.contains("根据") || textLower.contains("基于") || textLower.contains("证据") 
                    || textLower.contains("前提") || textLower.contains("假设") || textLower.contains("理由")
                    || textLower.contains("依据") || textLower.contains("数据") || textLower.contains("事实")
                    || textLower.contains("研究") || textLower.contains("实验") || textLower.contains("观察");
            return !hasEvidenceWord;
        }
        
        if (clauseName.contains("一致")) {
            return checkConsistency(textLower);
        }
        
        if (rule.contains("不得包含") || rule.contains("不能包含") || rule.contains("不包含")) {
            String forbidden = extractForbiddenWord(rule);
            String[] forbiddenWords = forbidden.split("[、，,\\s]+");
            for (String word : forbiddenWords) {
                word = word.replaceAll("[\"'等]", "").trim();
                if (!word.isEmpty() && textLower.contains(word)) {
                    return true;
                }
            }
            return false;
        }
        
        if (rule.contains("必须包含") || rule.contains("应当包含") || rule.contains("需要包含")) {
            String required = extractRequiredWord(rule);
            String[] requiredWords = required.split("[、，,\\s]+");
            boolean found = false;
            for (String word : requiredWords) {
                word = word.replaceAll("[\"'等]", "").trim();
                if (!word.isEmpty() && textLower.contains(word)) {
                    found = true;
                    break;
                }
            }
            return !found;
        }
        
        if (rule.contains("禁止") || rule.contains("避免")) {
            String[] parts = rule.split("[禁止避免]");
            if (parts.length > 1) {
                String forbidden = parts[1].trim();
                String[] forbiddenWords = forbidden.split("[、，,\\s]+");
                for (String word : forbiddenWords) {
                    word = word.replaceAll("[\"'等]", "").trim();
                    if (!word.isEmpty() && textLower.contains(word)) {
                        return true;
                    }
                }
            }
            return false;
        }
        
        return false;
    }
    
    private boolean checkConsistency(String text) {
        int contradictionCount = 0;
        
        if (text.contains("不是") && text.contains("是")) {
            contradictionCount++;
        }
        if (text.contains("没有") && text.contains("有")) {
            contradictionCount++;
        }
        if (text.contains("不可能") && text.contains("可能")) {
            contradictionCount++;
        }
        if (text.contains("不存在") && text.contains("存在")) {
            contradictionCount++;
        }
        
        return contradictionCount >= 2;
    }

    private String extractForbiddenWord(String rule) {
        if (rule.contains("不得包含")) {
            return rule.substring(rule.indexOf("不得包含") + 4).trim();
        } else if (rule.contains("不能包含")) {
            return rule.substring(rule.indexOf("不能包含") + 4).trim();
        } else if (rule.contains("不包含")) {
            return rule.substring(rule.indexOf("不包含") + 3).trim();
        }
        return rule;
    }

    private String extractRequiredWord(String rule) {
        if (rule.contains("必须包含")) {
            return rule.substring(rule.indexOf("必须包含") + 4).trim();
        } else if (rule.contains("应当包含")) {
            return rule.substring(rule.indexOf("应当包含") + 4).trim();
        } else if (rule.contains("需要包含")) {
            return rule.substring(rule.indexOf("需要包含") + 4).trim();
        }
        return rule;
    }

    private String buildExplanation(ArbitrationResult result, List<String> matchedRules, double complianceScore) {
        StringBuilder sb = new StringBuilder();
        switch (result) {
            case COMPLIANT:
                sb.append(String.format("契约仲裁通过：输出符合所有规则约束。合规分数：%.2f", complianceScore));
                break;
            case NON_COMPLIANT:
                sb.append(String.format("契约仲裁失败：输出违反以下规则，合规分数：%.2f\n", complianceScore));
                for (String rule : matchedRules) {
                    sb.append("- ").append(rule).append("\n");
                }
                break;
            case LOW_CONFIDENCE:
                sb.append(String.format("契约仲裁低置信度：输出部分符合规则，建议审查。合规分数：%.2f", complianceScore));
                if (!matchedRules.isEmpty()) {
                    sb.append("\n涉及规则：");
                    for (String rule : matchedRules) {
                        sb.append("\n- ").append(rule);
                    }
                }
                break;
            case FAIL_SAFE_TRIGGERED:
                sb.append("FAIL-SAFE触发：输出违反安全边界规则，强制终止。");
                break;
        }
        return sb.toString();
    }

    @Override
    public ContractBook loadContract(String filePath) {
        try {
            activeContract = objectMapper.readValue(new File(filePath), ContractBook.class);
            return activeContract;
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public void unloadContract() {
        activeContract = null;
    }

    @Override
    public boolean hasActiveContract() {
        return activeContract != null;
    }

    @Override
    public String getContractInfo() {
        if (activeContract == null) {
            return "No contract loaded";
        }
        return String.format("Contract: %s v%s, %d clauses", 
                activeContract.getName(), 
                activeContract.getVersion(),
                activeContract.getClauses().size());
    }

    @Override
    public ContractBook getContractBook() {
        return activeContract;
    }

    @Override
    public void addClause(ContractClause clause) {
        if (activeContract != null && activeContract.getClauses() != null) {
            activeContract.getClauses().add(clause);
            persistContract();
        }
    }

    @Override
    public void updateClause(String clauseId, ContractClause clause) {
        if (activeContract != null && activeContract.getClauses() != null) {
            for (int i = 0; i < activeContract.getClauses().size(); i++) {
                if (activeContract.getClauses().get(i).getId().equals(clauseId)) {
                    activeContract.getClauses().set(i, clause);
                    persistContract();
                    break;
                }
            }
        }
    }

    @Override
    public void removeClause(String clauseId) {
        if (activeContract != null && activeContract.getClauses() != null) {
            activeContract.getClauses().removeIf(c -> c.getId().equals(clauseId));
            persistContract();
        }
    }

    @Override
    public void saveContract() {
        persistContract();
    }

    @Override
    public void toggleClauseEnabled(String clauseId, boolean enabled) {
        if (activeContract != null && activeContract.getClauses() != null) {
            for (ContractClause clause : activeContract.getClauses()) {
                if (clause.getId().equals(clauseId)) {
                    clause.setEnabled(enabled);
                    persistContract();
                    break;
                }
            }
        }
    }

    private ContractBook createDefaultContract() {
        List<ContractClause> clauses = new ArrayList<>();
        
        clauses.add(ContractClause.builder()
                .id("c1")
                .name("禁止绝对化表述")
                .rule("不得包含'绝对'、'必定'、'毫无疑问'等绝对化词汇")
                .description("防止过度自信的逻辑断言")
                .failSafe(false)
                .severity(SeverityLevel.MINOR)
                .enabled(true)
                .build());
        
        clauses.add(ContractClause.builder()
                .id("c2")
                .name("逻辑推导要求")
                .rule("必须包含逻辑推导过程")
                .description("确保推理链完整可见")
                .failSafe(false)
                .severity(SeverityLevel.MAJOR)
                .enabled(true)
                .build());
        
        clauses.add(ContractClause.builder()
                .id("c3")
                .name("证据支持要求")
                .rule("结论必须有证据或前提支持")
                .description("禁止无根据的断言")
                .failSafe(false)
                .severity(SeverityLevel.MAJOR)
                .enabled(true)
                .build());
        
        clauses.add(ContractClause.builder()
                .id("c4")
                .name("安全边界")
                .rule("禁止输出有害、非法或违反伦理的内容")
                .description("Fail-Safe硬边界")
                .failSafe(true)
                .severity(SeverityLevel.CRITICAL)
                .enabled(true)
                .build());
        
        clauses.add(ContractClause.builder()
                .id("c5")
                .name("一致性检查")
                .rule("前后陈述必须逻辑一致")
                .description("防止自相矛盾")
                .failSafe(false)
                .severity(SeverityLevel.MAJOR)
                .enabled(true)
                .build());

        return ContractBook.builder()
                .id("default")
                .name("默认逻辑契约")
                .version("1.0")
                .clauses(clauses)
                .build();
    }

    private void persistContract() {
        try {
            File dir = new File(CONTRACT_DIR);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(new File(DEFAULT_CONTRACT_PATH), activeContract);
        } catch (IOException e) {
            // ignore
        }
    }
}