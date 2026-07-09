package com.memorytree.branch;

import com.memorytree.dto.ActionSpace;
import com.memorytree.dto.ObservationSpace;
import com.memorytree.enums.BranchType;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class FallacyDetectionBranch implements RLBranch {

    private boolean loaded = false;
    private boolean active = true;
    private String filePath = "";

    @Override
    public ActionSpace observe(ObservationSpace observation) {
        double interferenceStrength = 0.7;
        double temperatureModifier = 0.2;
        boolean triggerRewrite = false;
        double confidenceThreshold = 0.8;

        String draft = observation.getCurrentDraft();
        if (draft != null) {
            if (draft.contains("显然") || draft.contains("毫无疑问") || 
                draft.contains("必定") || draft.contains("绝对")) {
                interferenceStrength = 0.9;
                triggerRewrite = true;
            }
            if (draft.contains("因为...所以") && draft.length() < 50) {
                interferenceStrength = 0.85;
            }
        }

        Map<String, Object> constraints = new HashMap<>();
        constraints.put("detect_fallacies", true);
        constraints.put("require_evidence", true);
        constraints.put("ban_absolute_terms", true);

        return ActionSpace.builder()
                .temperatureModifier(temperatureModifier)
                .interferenceStrength(interferenceStrength)
                .triggerRewrite(triggerRewrite)
                .confidenceThreshold(confidenceThreshold)
                .constraints(constraints)
                .build();
    }

    @Override
    public void loadBranch(String filePath) {
        this.filePath = filePath;
        this.loaded = true;
    }

    @Override
    public void unloadBranch() {
        this.loaded = false;
        this.filePath = "";
    }

    @Override
    public boolean isLoaded() {
        return loaded;
    }

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public BranchType getType() {
        return BranchType.LOGICAL_FALLACY_DETECTION;
    }

    @Override
    public String getBranchInfo() {
        return String.format("Fallacy Detection Branch - %s (loaded: %b, active: %b)", 
                filePath, loaded, active);
    }
}