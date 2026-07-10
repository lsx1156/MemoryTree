package com.memorytree.kernel;

import com.memorytree.dto.GenerateConfig;
import com.memorytree.dto.GenerateResult;

public interface TrunkKernel {
    GenerateResult generate(String prompt, GenerateConfig config);
    double[] getLogits(String prompt);
    void loadKernel(String modelPath);
    void unloadKernel();
    boolean isLoaded();
    String getKernelInfo();
    long getMemoryUsageBytes();
    
    String getKVCacheHandle();
    void clearKVCache();
    String cloneKVCache();
    void restoreKVCache(String handle);
}