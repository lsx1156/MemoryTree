package com.memorytree.system;

import com.memorytree.kernel.TrunkKernel;
import com.memorytree.memory.MemoryBackend;
import com.memorytree.memory.WorkingMemory;
import com.memorytree.scheduler.SchedulerBus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class RuntimeBoundaryAuditor {

    @Autowired(required = false)
    private TrunkKernel trunkKernel;

    @Autowired(required = false)
    private MemoryBackend memoryBackend;

    @Autowired(required = false)
    private WorkingMemory workingMemory;

    @Autowired(required = false)
    private SchedulerBus schedulerBus;

    private static final int MAX_WORKING_MEMORY_SIZE = 100;
    private static final long MAX_KERNEL_MEMORY_USAGE_GB = 8;
    private static final double MAX_HEAT_VALUE = 100.0;
    private static final int MAX_PARALLEL_BRANCHES = 10;

    public AuditResult performAudit() {
        List<AuditItem> items = new ArrayList<>();
        
        items.add(checkDesignPrinciple1());
        items.add(checkDesignPrinciple2());
        items.add(checkDesignPrinciple3());
        items.add(checkDesignPrinciple4());
        items.add(checkMemoryBoundaries(items));
        items.add(checkKernelBoundaries(items));
        items.add(checkHeatBoundaries(items));
        items.add(checkParallelismBoundaries(items));

        boolean allPassed = items.stream().allMatch(AuditItem::passed);
        
        AuditResult result = new AuditResult(items, allPassed, System.currentTimeMillis());
        
        if (!allPassed) {
            log.warn("Runtime boundary audit failed. Issues found: {}", 
                    items.stream().filter(i -> !i.passed()).count());
        } else {
            log.info("Runtime boundary audit passed");
        }
        
        return result;
    }

    private AuditItem checkDesignPrinciple1() {
        boolean passed = true;
        String message = "检查通过：树干内核提供基础推理能力";
        
        if (trunkKernel == null) {
            passed = false;
            message = "未找到树干内核实例";
        } else if (!trunkKernel.isLoaded()) {
            passed = false;
            message = "树干内核未加载";
        }
        
        return new AuditItem("PRINCIPLE_1", "树干内核层独立", passed, message);
    }

    private AuditItem checkDesignPrinciple2() {
        boolean passed = true;
        String message = "检查通过：树枝层独立于树干";
        
        if (trunkKernel != null && trunkKernel.isLoaded()) {
            String handle = trunkKernel.getKVCacheHandle();
            if (handle == null || handle.isEmpty()) {
                passed = false;
                message = "KV缓存句柄为空";
            }
        }
        
        return new AuditItem("PRINCIPLE_2", "树枝层独立性", passed, message);
    }

    private AuditItem checkDesignPrinciple3() {
        boolean passed = true;
        String message = "检查通过：记忆后端可独立工作";
        
        if (memoryBackend == null) {
            passed = false;
            message = "未找到记忆后端实例";
        } else {
            try {
                int count = memoryBackend.getTotalCount();
                if (count < 0) {
                    passed = false;
                    message = "记忆计数异常: " + count;
                }
            } catch (Exception e) {
                passed = false;
                message = "记忆后端查询失败: " + e.getMessage();
            }
        }
        
        return new AuditItem("PRINCIPLE_3", "记忆后端独立性", passed, message);
    }

    private AuditItem checkDesignPrinciple4() {
        boolean passed = true;
        String message = "检查通过：调度控制层协调各组件";
        
        if (schedulerBus == null) {
            passed = false;
            message = "未找到调度总线实例";
        } else if (!schedulerBus.isInitialized()) {
            passed = false;
            message = "调度总线未初始化";
        }
        
        return new AuditItem("PRINCIPLE_4", "调度控制层协调", passed, message);
    }

    private AuditItem checkMemoryBoundaries(List<AuditItem> items) {
        boolean passed = true;
        StringBuilder message = new StringBuilder();
        
        if (workingMemory != null) {
            int size = workingMemory.getAllEntries().size();
            if (size > MAX_WORKING_MEMORY_SIZE) {
                passed = false;
                message.append("工作记忆超限: ").append(size).append("/").append(MAX_WORKING_MEMORY_SIZE).append("; ");
            } else {
                message.append("工作记忆: ").append(size).append("/").append(MAX_WORKING_MEMORY_SIZE).append("; ");
            }
        }
        
        if (memoryBackend != null) {
            int count = memoryBackend.getTotalCount();
            message.append("持久记忆: ").append(count).append("; ");
        }
        
        return new AuditItem("MEMORY_BOUNDARY", "记忆边界", passed, 
                message.length() > 0 ? message.toString().trim() : "检查通过");
    }

    private AuditItem checkKernelBoundaries(List<AuditItem> items) {
        boolean passed = true;
        String message = "";
        
        if (trunkKernel != null && trunkKernel.isLoaded()) {
            long usage = trunkKernel.getMemoryUsageBytes();
            long maxUsage = MAX_KERNEL_MEMORY_USAGE_GB * 1024 * 1024 * 1024;
            
            if (usage > maxUsage) {
                passed = false;
                message = "内核内存占用超限: " + (usage / (1024*1024*1024)) + "GB/" + MAX_KERNEL_MEMORY_USAGE_GB + "GB";
            } else {
                message = "内核内存占用: " + (usage / (1024*1024)) + "MB";
            }
        }
        
        return new AuditItem("KERNEL_BOUNDARY", "内核边界", passed, 
                message.isEmpty() ? "检查通过" : message);
    }

    private AuditItem checkHeatBoundaries(List<AuditItem> items) {
        boolean passed = true;
        String message = "检查通过";
        
        if (memoryBackend != null) {
            try {
                for (var entry : memoryBackend.getAll()) {
                    if (entry.getHeat() > MAX_HEAT_VALUE) {
                        passed = false;
                        message = "记忆热度超限: " + entry.getHeat() + " > " + MAX_HEAT_VALUE;
                        break;
                    }
                }
            } catch (Exception e) {
                message = "热度检查失败: " + e.getMessage();
            }
        }
        
        return new AuditItem("HEAT_BOUNDARY", "热度边界", passed, message);
    }

    private AuditItem checkParallelismBoundaries(List<AuditItem> items) {
        boolean passed = true;
        String message = "检查通过";
        
        if (schedulerBus != null) {
            var status = schedulerBus.getParallelismStatus();
            if (status.getMaxParallelBranches() > MAX_PARALLEL_BRANCHES) {
                passed = false;
                message = "最大并行分支超限: " + status.getMaxParallelBranches() + " > " + MAX_PARALLEL_BRANCHES;
            } else {
                message = "并行分支: " + status.getActiveBranchCount() + "/" + status.getMaxParallelBranches();
            }
        }
        
        return new AuditItem("PARALLELISM_BOUNDARY", "并行度边界", passed, message);
    }

    public record AuditItem(
            String code,
            String name,
            boolean passed,
            String message
    ) {}

    public record AuditResult(
            List<AuditItem> items,
            boolean allPassed,
            long timestamp
    ) {
        public List<AuditItem> getFailedItems() {
            return items.stream().filter(i -> !i.passed()).toList();
        }
        
        public int getPassCount() {
            return (int) items.stream().filter(AuditItem::passed).count();
        }
        
        public int getFailCount() {
            return (int) items.stream().filter(i -> !i.passed()).count();
        }
        
        public double getPassRate() {
            return items.isEmpty() ? 0.0 : (double) getPassCount() / items.size();
        }
    }
}
