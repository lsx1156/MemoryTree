package com.memorytree.branch;

import com.memorytree.dto.ActionSpace;
import com.memorytree.dto.ObservationSpace;
import com.memorytree.enums.BranchType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
public class FallacyDetectionBranch implements RLBranch {

    private boolean loaded = false;
    private boolean active = true;
    private String filePath = "";
    private double interferenceStrength = 0.7;
    private double temperatureModifier = 0.2;
    private double confidenceThreshold = 0.8;
    private String[] absoluteTerms = {"显然", "毫无疑问", "必定", "绝对"};
    private String shortEvidencePattern = "因为...所以";
    private int shortEvidenceMinLength = 50;

    @Override
    public ActionSpace observe(ObservationSpace observation) {
        double currentInterference = this.interferenceStrength;
        double currentTemperature = this.temperatureModifier;
        boolean triggerRewrite = false;

        String draft = observation.getCurrentDraft();
        if (draft != null) {
            for (String term : this.absoluteTerms) {
                if (draft.contains(term)) {
                    currentInterference = 0.9;
                    triggerRewrite = true;
                    break;
                }
            }
            if (draft.contains(this.shortEvidencePattern) && draft.length() < this.shortEvidenceMinLength) {
                currentInterference = 0.85;
            }
        }

        Map<String, Object> constraints = new HashMap<>();
        constraints.put("detect_fallacies", true);
        constraints.put("require_evidence", true);
        constraints.put("ban_absolute_terms", true);

        return ActionSpace.builder()
                .temperatureModifier(currentTemperature)
                .interferenceStrength(currentInterference)
                .triggerRewrite(triggerRewrite)
                .confidenceThreshold(this.confidenceThreshold)
                .constraints(constraints)
                .build();
    }

    @Override
    public void loadBranch(String filePath) {
        this.filePath = filePath;
        try {
            File file = new File(filePath);
            if (file.exists()) {
                ObjectMapper mapper = new ObjectMapper();
                Map<String, Object> config = mapper.readValue(file, Map.class);
                if (config.containsKey("interferenceStrength")) {
                    this.interferenceStrength = ((Number) config.get("interferenceStrength")).doubleValue();
                }
                if (config.containsKey("temperatureModifier")) {
                    this.temperatureModifier = ((Number) config.get("temperatureModifier")).doubleValue();
                }
                if (config.containsKey("confidenceThreshold")) {
                    this.confidenceThreshold = ((Number) config.get("confidenceThreshold")).doubleValue();
                }
            }
        } catch (IOException e) {
            // ignore
        }
        this.loaded = true;
    }

    @Override
    public void saveBranch(String filePath) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> config = new HashMap<>();
            config.put("branchType", this.getType().name());
            config.put("interferenceStrength", this.interferenceStrength);
            config.put("temperatureModifier", this.temperatureModifier);
            config.put("confidenceThreshold", this.confidenceThreshold);
            config.put("active", this.active);
            mapper.writerWithDefaultPrettyPrinter().writeValue(new File(filePath), config);
            this.filePath = filePath;
            this.loaded = true;
        } catch (IOException e) {
            // ignore
        }
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