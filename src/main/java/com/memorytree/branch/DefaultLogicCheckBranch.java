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
public class DefaultLogicCheckBranch implements RLBranch {

    private boolean loaded = false;
    private boolean active = true;
    private String filePath = "";
    private double interferenceStrength = 0.6;
    private double temperatureModifier = 0.3;
    private double confidenceThreshold = 0.75;
    private double entropyThreshold = 0.8;
    private double consistencyThreshold = 0.5;

    @Override
    public ActionSpace observe(ObservationSpace observation) {
        double currentInterference = this.interferenceStrength;
        double currentTemperature = this.temperatureModifier;
        boolean triggerRewrite = false;

        if (observation.getEntropy() > this.entropyThreshold) {
            currentInterference = 0.8;
            currentTemperature = 0.1;
        }

        if (observation.getCrossLayerConsistency() < this.consistencyThreshold) {
            triggerRewrite = true;
        }

        Map<String, Object> constraints = new HashMap<>();
        constraints.put("max_length", 100);
        constraints.put("min_confidence", this.confidenceThreshold);
        constraints.put("logic_purity", true);

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
                if (config.containsKey("entropyThreshold")) {
                    this.entropyThreshold = ((Number) config.get("entropyThreshold")).doubleValue();
                }
                if (config.containsKey("consistencyThreshold")) {
                    this.consistencyThreshold = ((Number) config.get("consistencyThreshold")).doubleValue();
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
            config.put("entropyThreshold", this.entropyThreshold);
            config.put("consistencyThreshold", this.consistencyThreshold);
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
        return BranchType.DEFAULT_LOGIC_CHECK;
    }

    @Override
    public String getBranchInfo() {
        return String.format("Default Logic Check Branch - %s (loaded: %b, active: %b)", 
                filePath, loaded, active);
    }
}