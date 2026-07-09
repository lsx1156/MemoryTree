package com.memorytree.scheduler;

import com.memorytree.dto.*;
import com.memorytree.enums.InferenceState;
import com.memorytree.enums.LifecycleState;

public interface SchedulerBus {
    InferenceResponse executeInference(InferenceRequest request);
    LifecycleState getLifecycleState();
    InferenceState getInferenceState();
    void initialize();
    void terminate();
    void resetWorkingMemory();
    boolean isInitialized();
    String getSystemStatus();
}