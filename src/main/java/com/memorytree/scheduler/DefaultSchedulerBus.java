package com.memorytree.scheduler;

import com.memorytree.dto.InferenceRequest;
import com.memorytree.dto.InferenceResponse;
import com.memorytree.dto.ParallelStatusDTO;
import com.memorytree.enums.InferenceState;
import com.memorytree.enums.LifecycleState;
import com.memorytree.branch.ParallelBranchEvaluator;
import com.memorytree.system.HardwareDetector;
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

    @Autowired(required = false)
    private ParallelBranchEvaluator parallelBranchEvaluator;

    @Autowired
    private HardwareDetector hardwareDetector;

    private int maxParallelBranches = 4;
    private int threadPoolSize = Runtime.getRuntime().availableProcessors() - 2;

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

    @Override
    public void configureParallelism(int maxParallelBranches, int threadPoolSize) {
        this.maxParallelBranches = Math.max(1, maxParallelBranches);
        this.threadPoolSize = Math.max(1, threadPoolSize);
        
        if (parallelBranchEvaluator != null) {
            parallelBranchEvaluator.setMaxParallelBranches(this.maxParallelBranches);
            parallelBranchEvaluator.setThreadPoolSize(this.threadPoolSize);
        }
        
        log.info("Parallelism configured: maxBranches={}, threadPoolSize={}", 
                this.maxParallelBranches, this.threadPoolSize);
    }

    @Override
    public ParallelStatusDTO getParallelismStatus() {
        int activeBranchCount = 0;
        if (parallelBranchEvaluator != null) {
            activeBranchCount = parallelBranchEvaluator.getActiveBranchCount();
        }
        
        return ParallelStatusDTO.builder()
                .maxParallelBranches(maxParallelBranches)
                .threadPoolSize(threadPoolSize)
                .activeBranchCount(activeBranchCount)
                .currentParallelLevel(1)
                .availableProcessors(Runtime.getRuntime().availableProcessors())
                .isParallelEnabled(true)
                .build();
    }
}