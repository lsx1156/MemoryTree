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
    void configureParallelism(int maxParallelBranches, int threadPoolSize);
    ParallelStatusDTO getParallelismStatus();
}