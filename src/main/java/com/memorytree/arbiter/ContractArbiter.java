package com.memorytree.arbiter;

import com.memorytree.dto.ArbitrationResultDTO;
import com.memorytree.dto.ContractBook;

public interface ContractArbiter {
    ArbitrationResultDTO validateDraft(String draft);
    ArbitrationResultDTO validateFinal(String finalResult);
    ArbitrationResultDTO validate(String text);
    ContractBook loadContract(String filePath);
    void unloadContract();
    boolean hasActiveContract();
    String getContractInfo();
    ContractBook getContractBook();
    
    void addClause(com.memorytree.dto.ContractClause clause);
    void updateClause(String clauseId, com.memorytree.dto.ContractClause clause);
    void removeClause(String clauseId);
    void saveContract();
    void toggleClauseEnabled(String clauseId, boolean enabled);
}