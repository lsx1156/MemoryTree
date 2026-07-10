package com.memorytree.branch;

import com.memorytree.dto.ActionSpace;
import com.memorytree.dto.ObservationSpace;
import com.memorytree.enums.BranchType;

/**
 * 强化学习分支接口，表示记忆树中可并行评估的推理分支。
 *
 * <p>RLBranch 封装了"观察→决策"的强化学习循环，每个分支独立维护：
 * <ul>
 *   <li>分支加载/卸载状态</li>
 *   <li>激活/停用开关</li>
 *   <li>分支类型（逻辑、记忆、探索等）</li>
 * </ul>
 *
 * <p>多个 RLBranch 可由 {@code ParallelBranchEvaluator} 并行调度，实现多路推理评估。
 *
 * @see ActionSpace
 * @see ObservationSpace
 * @see BranchType
 */
public interface RLBranch {

    /**
     * 根据观察空间输出动作空间（决策）。
     *
     * @param observation 当前环境观察（prompt、记忆、上下文等）
     * @return 决策动作空间
     */
    ActionSpace observe(ObservationSpace observation);

    /**
     * 从文件加载分支配置/权重。
     *
     * @param filePath 配置文件路径
     */
    void loadBranch(String filePath);

    /**
     * 保存当前分支配置/权重到文件。
     *
     * @param filePath 目标文件路径
     */
    void saveBranch(String filePath);

    /**
     * 卸载当前分支，释放资源。
     */
    void unloadBranch();

    /**
     * 判断分支是否已加载。
     *
     * @return 已加载返回 true
     */
    boolean isLoaded();

    /**
     * 判断分支是否处于激活状态（参与并行评估）。
     *
     * @return 激活返回 true
     */
    boolean isActive();

    /**
     * 设置分支激活状态。
     *
     * @param active true 表示激活，false 表示停用
     */
    void setActive(boolean active);

    /**
     * 获取分支类型。
     *
     * @return 分支类型枚举值
     */
    BranchType getType();

    /**
     * 获取分支元信息（类型、状态、加载时间等）。
     *
     * @return 格式化信息字符串
     */
    String getBranchInfo();
}
