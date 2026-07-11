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

package com.memorytree.system;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.reflect.Method;

@Slf4j
@Component
public class HardwareDetector {

    private HardwareInfo cachedInfo;

    public HardwareInfo detect() {
        if (cachedInfo == null) {
            HardwareInfo info = new HardwareInfo();

            info.setCpuCores(Runtime.getRuntime().availableProcessors());
            info.setPhysicalMemoryBytes(getPhysicalMemory());
            info.setAvailableMemoryBytes(getAvailableMemory());
            info.setTotalMemoryBytes(Runtime.getRuntime().totalMemory());
            info.setMaxMemoryBytes(Runtime.getRuntime().maxMemory());
            info.setFreeMemoryBytes(Runtime.getRuntime().freeMemory());
            info.setDiskSpaceBytes(getDiskSpace());

            info.setRecommendedModelSize(getRecommendedModelSize(info));
            info.setRecommendedThreads(getRecommendedThreads(info));

            cachedInfo = info;
            log.info("Hardware detected: CPU={} cores, Memory={} GB, Recommended model: {}B",
                    info.getCpuCores(), formatBytes(info.getPhysicalMemoryBytes()), info.getRecommendedModelSize());
        }

        updateRuntimeUsage(cachedInfo);
        return cachedInfo;
    }

    private void updateRuntimeUsage(HardwareInfo info) {
        info.setJvmTotalMemoryBytes(Runtime.getRuntime().totalMemory());
        info.setJvmFreeMemoryBytes(Runtime.getRuntime().freeMemory());
        info.setJvmUsedMemoryBytes(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
        info.setJvmMaxMemoryBytes(Runtime.getRuntime().maxMemory());

        info.setJvmMemoryUsagePercent((double) info.getJvmUsedMemoryBytes() / info.getJvmMaxMemoryBytes() * 100.0);

        info.setCpuUsagePercent(getCpuUsage());

        info.setSystemFreeMemoryBytes(getAvailableMemory());
        info.setSystemMemoryUsagePercent(
                (double) (info.getPhysicalMemoryBytes() - info.getSystemFreeMemoryBytes())
                        / info.getPhysicalMemoryBytes() * 100.0);

        info.setJvmThreadCount(ManagementFactory.getThreadMXBean().getThreadCount());
    }

    private double getCpuUsage() {
        try {
            OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
            Method method = osBean.getClass().getMethod("getProcessCpuLoad");
            double load = (double) method.invoke(osBean);
            if (load >= 0 && load <= 1.0) {
                return load * 100.0;
            }
        } catch (Exception e) {
            // ignore
        }
        return -1;
    }

    private long getPhysicalMemory() {
        try {
            OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
            Method method = osBean.getClass().getMethod("getTotalPhysicalMemorySize");
            return (long) method.invoke(osBean);
        } catch (Exception e) {
            return 8L * 1024 * 1024 * 1024;
        }
    }

    private long getAvailableMemory() {
        try {
            OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
            Method method = osBean.getClass().getMethod("getFreePhysicalMemorySize");
            return (long) method.invoke(osBean);
        } catch (Exception e) {
            return Runtime.getRuntime().freeMemory();
        }
    }

    private long getDiskSpace() {
        File root = new File("e:\\");
        if (!root.exists()) {
            root = new File("/");
        }
        return root.getFreeSpace();
    }

    private String getRecommendedModelSize(HardwareInfo info) {
        long availableMemoryGB = info.getAvailableMemoryBytes() / (1024 * 1024 * 1024);

        if (availableMemoryGB >= 16) {
            return "14B";
        } else if (availableMemoryGB >= 8) {
            return "7B";
        } else if (availableMemoryGB >= 4) {
            return "3B";
        } else if (availableMemoryGB >= 2) {
            return "0.5B";
        } else {
            return "<0.5B";
        }
    }

    private int getRecommendedThreads(HardwareInfo info) {
        int cpuCores = info.getCpuCores();
        return Math.max(2, cpuCores - 2);
    }

    public static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.2f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }

    @Data
    public static class HardwareInfo {
        private int cpuCores;
        private long physicalMemoryBytes;
        private long availableMemoryBytes;
        private long totalMemoryBytes;
        private long maxMemoryBytes;
        private long freeMemoryBytes;
        private long diskSpaceBytes;
        private String recommendedModelSize;
        private int recommendedThreads;

        private double cpuUsagePercent;
        private double jvmMemoryUsagePercent;
        private double systemMemoryUsagePercent;
        private long jvmTotalMemoryBytes;
        private long jvmFreeMemoryBytes;
        private long jvmUsedMemoryBytes;
        private long jvmMaxMemoryBytes;
        private long systemFreeMemoryBytes;
        private int jvmThreadCount;
    }
}
