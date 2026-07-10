package com.memorytree.branch;

import com.memorytree.dto.ActionSpace;
import com.memorytree.dto.ObservationSpace;
import com.memorytree.enums.BranchType;

public interface RLBranch {
    ActionSpace observe(ObservationSpace observation);
    void loadBranch(String filePath);
    void saveBranch(String filePath);
    void unloadBranch();
    boolean isLoaded();
    boolean isActive();
    void setActive(boolean active);
    BranchType getType();
    String getBranchInfo();
}