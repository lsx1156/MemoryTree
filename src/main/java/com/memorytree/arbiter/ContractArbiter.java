package com.memorytree.arbiter;

import com.memorytree.dto.ArbitrationResultDTO;
import com.memorytree.dto.ContractBook;
import com.memorytree.dto.ContractClause;

/**
 * 契约仲裁器接口，对推理输出进行规则校验与合规性评估。
 *
 * <p>ContractArbiter 是记忆树框架的"守门人"，负责：
 * <ul>
 *   <li>加载/卸载契约规则集（{@link ContractBook}）</li>
 *   <li>对草稿（draft）与最终输出（final）分别校验</li>
 *   <li>动态增删改查契约条款（{@link ContractClause}）</li>
 *   <li>返回合规分数、违反条款、匹配规则等结构化结果</li>
 * </ul>
 *
 * <p>校验结果分为四级：
 * <ul>
 *   <li>{@code COMPLIANT} — 合规，可放行</li>
 *   <li>{@code LOW_CONFIDENCE} — 低置信度，建议人工审查</li>
 *   <li>{@code NON_COMPLIANT} — 不合规，需重写</li>
 *   <li>{@code FAIL_SAFE_TRIGGERED} — 触发安全边界，强制终止</li>
 * </ul>
 *
 * <p>默认实现为 {@link DefaultContractArbiter}，基于关键词匹配的规则引擎。
 *
 * @see DefaultContractArbiter
 * @see ContractBook
 * @see ContractClause
 * @see ArbitrationResultDTO
 */
public interface ContractArbiter {

    /**
     * 校验草稿文本（宽松模式，用于 DRAFT_GENERATE 阶段）。
     *
     * @param draft 草稿文本
     * @return 仲裁结果，包含合规分数与违反条款信息
     */
    ArbitrationResultDTO validateDraft(String draft);

    /**
     * 校验最终输出文本（严格模式，用于 OUTPUT 阶段）。
     *
     * @param finalResult 最终输出文本
     * @return 仲裁结果
     */
    ArbitrationResultDTO validateFinal(String finalResult);

    /**
     * 通用文本校验，等同于 {@link #validateFinal(String)}。
     *
     * @param text 待校验文本
     * @return 仲裁结果
     */
    ArbitrationResultDTO validate(String text);

    /**
     * 从文件加载契约规则集。
     *
     * @param filePath 契约 JSON 文件路径
     * @return 加载后的 {@link ContractBook}，失败返回 null
     */
    ContractBook loadContract(String filePath);

    /**
     * 卸载当前契约规则集。
     */
    void unloadContract();

    /**
     * 判断是否存在已加载的活跃契约。
     *
     * @return 存在活跃契约返回 true
     */
    boolean hasActiveContract();

    /**
     * 获取当前契约的摘要信息。
     *
     * @return 格式化字符串，包含名称、版本、条款数
     */
    String getContractInfo();

    /**
     * 获取当前契约规则集对象。
     *
     * @return 当前 {@link ContractBook}，未加载时返回 null
     */
    ContractBook getContractBook();

    /**
     * 向当前契约新增一条条款。
     *
     * @param clause 待新增条款
     */
    void addClause(ContractClause clause);

    /**
     * 更新指定 ID 的条款。
     *
     * @param clauseId 待更新条款 ID
     * @param clause   新条款内容
     */
    void updateClause(String clauseId, ContractClause clause);

    /**
     * 删除指定 ID 的条款。
     *
     * @param clauseId 待删除条款 ID
     */
    void removeClause(String clauseId);

    /**
     * 将当前契约规则集持久化到磁盘。
     */
    void saveContract();

    /**
     * 切换指定条款的启用/停用状态。
     *
     * @param clauseId 条款 ID
     * @param enabled  true 启用，false 停用
     */
    void toggleClauseEnabled(String clauseId, boolean enabled);
}
