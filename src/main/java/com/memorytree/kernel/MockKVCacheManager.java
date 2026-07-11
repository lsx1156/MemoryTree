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

package com.memorytree.kernel;

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * 模拟 KV cache 句柄管理。Ollama API 不暴露真实 KV cache 句柄，
 * 本类仅维护占位句柄以满足 TrunkKernel 接口契约。
 */
@Slf4j
public class MockKVCacheManager {

    private String kvCacheHandle = null;
    private final Map<String, String> kvCacheStore = new HashMap<>();

    public String getKVCacheHandle() {
        if (kvCacheHandle == null) {
            kvCacheHandle = "kv_cache_" + System.currentTimeMillis() + "_" +
                    Long.toHexString(Double.doubleToLongBits(Math.random()));
            kvCacheStore.put(kvCacheHandle, "active");
        }
        return kvCacheHandle;
    }

    public void clearKVCache() {
        kvCacheHandle = null;
        kvCacheStore.clear();
        log.info("KV cache cleared");
    }

    public String cloneKVCache() {
        String sourceHandle = getKVCacheHandle();
        String cloneHandle = "kv_cache_clone_" + System.currentTimeMillis() + "_" +
                Long.toHexString(Double.doubleToLongBits(Math.random()));
        kvCacheStore.put(cloneHandle, sourceHandle);
        log.info("KV cache cloned: {} -> {}", sourceHandle, cloneHandle);
        return cloneHandle;
    }

    public void restoreKVCache(String handle) {
        if (kvCacheStore.containsKey(handle)) {
            kvCacheHandle = handle;
            log.info("KV cache restored: {}", handle);
        } else {
            log.warn("KV cache handle not found: {}", handle);
        }
    }
}
