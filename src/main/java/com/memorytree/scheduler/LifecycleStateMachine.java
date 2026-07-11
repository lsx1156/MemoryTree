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

package com.memorytree.scheduler;

import com.memorytree.branch.RLBranch;
import com.memorytree.kernel.TrunkKernel;
import com.memorytree.memory.WorkingMemory;
import com.memorytree.enums.LifecycleState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.List;

@Slf4j
@Component
public class LifecycleStateMachine {

    @Autowired
    private TrunkKernel trunkKernel;

    @Autowired
    private List<RLBranch> branches;

    @Autowired
    private WorkingMemory workingMemory;

    private volatile LifecycleState currentState = LifecycleState.UNINITIALIZED;

    @PostConstruct
    public void init() {
        transitionTo(LifecycleState.INITIALIZING);
        
        trunkKernel.loadKernel("default");
        
        for (RLBranch branch : branches) {
            branch.loadBranch("default");
        }
        
        transitionTo(LifecycleState.RUNNING);
        log.info("MemoryTree system initialized successfully");
    }

    public void terminate() {
        if (currentState != LifecycleState.RUNNING) {
            log.warn("Cannot terminate - system not in RUNNING state: {}", currentState);
            return;
        }

        transitionTo(LifecycleState.TERMINATING);
        
        workingMemory.clearAll();
        
        for (RLBranch branch : branches) {
            branch.setActive(false);
            branch.unloadBranch();
        }
        
        trunkKernel.unloadKernel();
        
        transitionTo(LifecycleState.DESTROYED);
        log.info("MemoryTree system terminated - all dynamic states cleared");
    }

    public void reset() {
        if (currentState == LifecycleState.RUNNING) {
            log.info("Resetting working memory");
            workingMemory.clearAll();
            for (RLBranch branch : branches) {
                branch.setActive(true);
            }
        }
    }

    public LifecycleState getCurrentState() {
        return currentState;
    }

    public boolean isRunning() {
        return currentState == LifecycleState.RUNNING;
    }

    private void transitionTo(LifecycleState newState) {
        log.debug("Lifecycle state transition: {} -> {}", currentState, newState);
        currentState = newState;
    }

    public String getStatusSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("Lifecycle State: ").append(currentState).append("\n");
        sb.append("Kernel Loaded: ").append(trunkKernel.isLoaded()).append("\n");
        sb.append("Active Branches: ").append(branches.stream().filter(RLBranch::isActive).count()).append("/").append(branches.size()).append("\n");
        sb.append("Working Memory Size: ").append(workingMemory.getSizeBytes() / 1024).append(" KB\n");
        sb.append("Working Memory Entries: ").append(workingMemory.getSize());
        return sb.toString();
    }
}