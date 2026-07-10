package com.memorytree.memory;

import com.memorytree.dto.MemoryEntry;
import com.memorytree.dto.MemoryQuery;

import java.util.List;
import java.util.Optional;

/**
 * 记忆持久化后端接口，负责存储、检索与管理记忆条目。
 *
 * <p>MemoryBackend 是记忆树框架的存储层抽象，支持：
 * <ul>
 *   <li>基于 ID 的精确存取</li>
 *   <li>基于 {@link MemoryQuery} 的结构化查询（标签、类型等）</li>
 *   <li>基于文本嵌入的语义检索（cosine 相似度）</li>
 *   <li>记忆热度衰减，模拟人类记忆遗忘机制</li>
 * </ul>
 *
 * <p>默认实现为 {@code FileSystemMemoryBackend}，以 JSON 文件持久化。
 *
 * @see MemoryEntry
 * @see MemoryQuery
 */
public interface MemoryBackend {

    /**
     * 存储一条记忆条目。若 ID 已存在则覆盖。
     *
     * @param entry 待存储的记忆条目（ID 为空时由后端自动生成）
     * @return 存储后的条目（包含生成的 ID 与时间戳）
     */
    MemoryEntry store(MemoryEntry entry);

    /**
     * 根据 ID 精确检索记忆条目。
     *
     * @param id 记忆条目 ID
     * @return 命中则返回 {@link Optional#of(Object)}，未命中返回 {@link Optional#empty()}
     */
    Optional<MemoryEntry> retrieve(String id);

    /**
     * 根据结构化查询条件检索记忆条目。
     *
     * @param query 查询条件（标签、类型、时间范围等）
     * @return 命中条目列表，可能为空
     */
    List<MemoryEntry> query(MemoryQuery query);

    /**
     * 基于文本嵌入的语义检索。
     *
     * <p>将查询文本转为向量，与所有记忆条目的 embedding 计算 cosine 相似度，
     * 返回相似度超过阈值的前 N 条结果。
     *
     * @param queryText 查询文本
     * @param limit     最大返回条数
     * @return 按相似度降序排列的条目列表
     */
    List<MemoryEntry> semanticSearch(String queryText, int limit);

    /**
     * 根据 ID 删除记忆条目。
     *
     * @param id 待删除条目的 ID
     */
    void delete(String id);

    /**
     * 获取所有记忆条目。
     *
     * @return 全部条目列表
     */
    List<MemoryEntry> getAll();

    /**
     * 获取当前存储的记忆条目总数。
     *
     * @return 条目数量
     */
    int getTotalCount();

    /**
     * 初始化后端：创建存储目录、加载已有数据、写入默认记忆等。
     */
    void initialize();

    /**
     * 构建检索索引（如嵌入向量索引），加速后续查询。
     */
    void buildIndex();

    /**
     * 判断当前查询是否在记忆作用域内（基于时间衰减或相关性阈值）。
     *
     * @param query 查询文本
     * @return 在作用域内返回 true
     */
    boolean isInScope(String query);

    /**
     * 对所有记忆条目执行热度衰减。
     *
     * <p>模拟人类记忆遗忘机制：每次调用将所有条目的 heat 字段按 decayRate 比例衰减。
     *
     * @param decayRate 衰减率，范围 [0, 1]，例如 0.95 表示保留 95% 热度
     */
    void decayAllHeat(double decayRate);
}
