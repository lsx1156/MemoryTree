package com.memorytree.branch;

import com.memorytree.dto.ActionSpace;
import com.memorytree.dto.ObservationSpace;
import com.memorytree.enums.BranchType;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class DefaultLogicCheckBranch implements RLBranch {

    private boolean loaded = false;
    private boolean active = true;
    private String filePath = "";

    @Override
    public ActionSpace observe(ObservationSpace observation) {
        double interferenceStrength = 0.6;
        double temperatureModifier = 0.3;
        boolean triggerRewrite = false;
        double confidenceThreshold = 0.75;

        if (observation.getEntropy() > 0.8) {
            interferenceStrength = 0.8;
            temperatureModifier = 0.1;
        }

        if (observation.getCrossLayerConsistency() < 0.5) {
            triggerRewrite = true;
        }

        Map<String, Object> constraints = new HashMap<>();
        constraints.put("max_length", 100);
        constraints.put("min_confidence", 0.7);
        constraints.put("logic_purity", true);

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
        return BranchType.DEFAULT_LOGIC_CHECK;
    }

    @Override
    public String getBranchInfo() {
        return String.format("Default Logic Check Branch - %s (loaded: %b, active: %b)", 
                filePath, loaded, active);
    }
}