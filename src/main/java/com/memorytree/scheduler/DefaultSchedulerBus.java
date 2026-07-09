package com.memorytree.scheduler;

import com.memorytree.dto.InferenceRequest;
import com.memorytree.dto.InferenceResponse;
import com.memorytree.enums.InferenceState;
import com.memorytree.enums.LifecycleState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DefaultSchedulerBus implements SchedulerBus {

    @Autowired
    private LifecycleStateMachine lifecycleStateMachine;

    @Autowired
    private InferenceStateMachine inferenceStateMachine;

    @Override
    public InferenceResponse executeInference(InferenceRequest request) {
        if (!lifecycleStateMachine.isRunning()) {
            throw new IllegalStateException("System not in RUNNING state");
        }
        
        log.info("Executing inference: {}", request.getPrompt());
        return inferenceStateMachine.executeInference(request);
    }

    @Override
    public LifecycleState getLifecycleState() {
        return lifecycleStateMachine.getCurrentState();
    }

    @Override
    public InferenceState getInferenceState() {
        return inferenceStateMachine.getCurrentState();
    }

    @Override
    public void initialize() {
        if (lifecycleStateMachine.getCurrentState() == LifecycleState.UNINITIALIZED) {
            lifecycleStateMachine.init();
        }
    }

    @Override
    public void terminate() {
        lifecycleStateMachine.terminate();
    }

    @Override
    public void resetWorkingMemory() {
        lifecycleStateMachine.reset();
    }

    @Override
    public boolean isInitialized() {
        return lifecycleStateMachine.getCurrentState() == LifecycleState.RUNNING;
    }

    @Override
    public String getSystemStatus() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== MemoryTree System Status ===\n");
        sb.append(lifecycleStateMachine.getStatusSummary());
        sb.append("\nInference State: ").append(inferenceStateMachine.getCurrentState());
        sb.append("\nActive Inferences: ").append(inferenceStateMachine.getActiveInferenceCount());
        return sb.toString();
    }
}