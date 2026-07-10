package com.memorytree.kernel;

import com.memorytree.dto.GenerateConfig;
import com.memorytree.dto.GenerateResult;

/**
 * 记忆树框架的"主干内核"接口，封装底层语言模型的推理能力。
 *
 * <p>TrunkKernel 是整个推理流程的入口，负责：
 * <ul>
 *   <li>接收 prompt 与生成配置，返回推理结果（文本、token、logits、置信度等）</li>
 *   <li>管理模型加载/卸载生命周期</li>
 *   <li>提供 KV cache 句柄操作（克隆、恢复、清空），用于推理上下文复用</li>
 *   <li>上报内存占用与内核元信息</li>
 * </ul>
 *
 * <p>默认实现为 {@link OllamaTrunkKernel}，通过 Ollama HTTP API 调用本地模型。
 *
 * @see OllamaTrunkKernel
 * @see GenerateResult
 * @see GenerateConfig
 */
public interface TrunkKernel {

    /**
     * 根据给定 prompt 与配置执行同步推理。
     *
     * @param prompt  用户输入的推理问题
     * @param config  生成参数（temperature、top_p、max_tokens 等）
     * @return 推理结果，包含生成文本、token 列表、置信度、耗时等
     * @throws RuntimeException 当模型未加载、API 调用失败或返回空文本时抛出
     */
    GenerateResult generate(String prompt, GenerateConfig config);

    /**
     * 获取给定 prompt 对应的 logits 分布。
     *
     * <p>当前实现为 mock 数据（Ollama API 不暴露真实 logits），仅用于接口契约兼容。
     *
     * @param prompt 输入文本
     * @return logits 数组，长度为词表大小（50257）
     */
    double[] getLogits(String prompt);

    /**
     * 加载指定路径的模型。
     *
     * @param modelPath 模型标识或路径（Ollama 实现中为模型名称）
     */
    void loadKernel(String modelPath);

    /**
     * 卸载当前已加载的模型，释放相关资源。
     */
    void unloadKernel();

    /**
     * 判断内核是否已加载模型。
     *
     * @return 已加载返回 true，否则 false
     */
    boolean isLoaded();

    /**
     * 获取内核元信息，包含模型名称、BaseURL、加载状态、加载时间等。
     *
     * @return 格式化的人类可读信息字符串
     */
    String getKernelInfo();

    /**
     * 获取当前内核的内存占用（JVM 堆使用量）。
     *
     * @return 已使用堆内存字节数
     */
    long getMemoryUsageBytes();

    /**
     * 获取当前 KV cache 句柄。若不存在则创建新句柄。
     *
     * <p>当前实现为 mock（Ollama API 不暴露真实 KV cache 句柄）。
     *
     * @return KV cache 句柄字符串
     */
    String getKVCacheHandle();

    /**
     * 清空所有 KV cache 句柄，释放占位资源。
     */
    void clearKVCache();

    /**
     * 克隆当前 KV cache 句柄，生成独立副本。
     *
     * <p>用于分支推理场景：在不影响主上下文的前提下，基于当前状态创建可独立修改的副本。
     *
     * @return 克隆后的新 KV cache 句柄
     */
    String cloneKVCache();

    /**
     * 恢复到指定的 KV cache 句柄。
     *
     * @param handle 之前通过 {@link #getKVCacheHandle()} 或 {@link #cloneKVCache()} 获取的句柄
     */
    void restoreKVCache(String handle);
}
