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
import com.memorytree.dto.ContractClause;
import com.memorytree.enums.ArbitrationResult;
import com.memorytree.enums.SeverityLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class DefaultContractArbiterTest {

    @TempDir
    Path tempDir;

    private DefaultContractArbiter arbiter;

    @BeforeEach
    void setUp() {
        arbiter = new DefaultContractArbiter();

        String contractDir = tempDir.resolve("data/contract").toString();
        String contractPath = contractDir + "/default_contract.json";
        ReflectionTestUtils.setField(arbiter, "CONTRACT_DIR", contractDir);
        ReflectionTestUtils.setField(arbiter, "DEFAULT_CONTRACT_PATH", contractPath);

        arbiter.init();
    }

    @Test
    void validateDraft_compliantText_returnsCompliant() {
        String text = "根据已知前提，我们可以推导出结论。因此，基于证据和逻辑推理，最终的论证结果是成立的。";

        ArbitrationResultDTO result = arbiter.validateDraft(text);

        assertEquals(ArbitrationResult.COMPLIANT, result.getResult(),
                "Text with logic words, evidence words, and no forbidden terms should be compliant");
        assertTrue(result.getComplianceScore() >= 0.8,
                "Compliant text should have score >= 0.8, got: " + result.getComplianceScore());
    }

    @Test
    void validateDraft_textWithAbsoluteWords_triggersViolation() {
        String text = "这是绝对的真理，毫无疑问，必定如此。因为没有推导过程。";

        ArbitrationResultDTO result = arbiter.validateDraft(text);

        assertNotEquals(ArbitrationResult.COMPLIANT, result.getResult(),
                "Text with absolute words and no logic process should not be compliant");
        assertTrue(result.getComplianceScore() < 0.8,
                "Non-compliant text should have score < 0.8, got: " + result.getComplianceScore());
    }

    @Test
    void validateDraft_emptyText_triggersViolations() {
        ArbitrationResultDTO result = arbiter.validateDraft("");

        assertNotEquals(ArbitrationResult.COMPLIANT, result.getResult(),
                "Empty text should not pass contract validation");
    }

    @Test
    void addClause_increasesClauseCount() {
        int initialCount = arbiter.getContractBook().getClauses().size();

        arbiter.addClause(ContractClause.builder()
                .id("test-clause")
                .name("测试规则")
                .rule("必须包含测试关键词")
                .severity(SeverityLevel.MINOR)
                .enabled(true)
                .build());

        assertEquals(initialCount + 1, arbiter.getContractBook().getClauses().size(),
                "Adding a clause should increase the clause count by 1");
    }

    @Test
    void removeClause_decreasesClauseCount() {
        int initialCount = arbiter.getContractBook().getClauses().size();

        arbiter.removeClause("c1");

        assertEquals(initialCount - 1, arbiter.getContractBook().getClauses().size(),
                "Removing a clause should decrease the clause count by 1");
    }

    @Test
    void toggleClauseEnabled_disablesClause() {
        arbiter.toggleClauseEnabled("c1", false);

        ContractClause clause = arbiter.getContractBook().getClauses().stream()
                .filter(c -> c.getId().equals("c1"))
                .findFirst()
                .orElse(null);

        assertNotNull(clause, "Clause c1 should exist");
        assertFalse(clause.isEnabled(), "Clause should be disabled after toggle");
    }

    @Test
    void hasActiveContract_returnsTrueAfterInit() {
        assertTrue(arbiter.hasActiveContract(), "Arbiter should have an active contract after init");
    }

    @Test
    void getContractInfo_returnsFormattedString() {
        String info = arbiter.getContractInfo();
        assertNotNull(info);
        assertTrue(info.contains("Contract:"), "Contract info should contain 'Contract:' prefix");
    }
}
