# 更新日志

本项目遵循 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/) 格式，
并采用 [语义化版本](https://semver.org/lang/zh-CN/) 规范。

## [3.1.4] - 2026-07-11

### 新增

- **3 项鲁棒性测试**：
  - `generate_ollamaUnavailable_throwsRuntimeExceptionWithFriendlyMessage` — 模拟 Ollama 不可用，验证友好异常
  - `buildRequestJson_superLongInput_doesNotCrash` — 5000 字符超长输入不崩溃
  - `kvCache_concurrentAccess_noDeadlock` — 10 线程 × 20 轮并发无死锁
- 测试总数从 55 增至 58，鲁棒性覆盖 8/12 项

### 变更

- README 第十四章鲁棒性表格实事求是更新：6 项 ❌ → ✅，2 项 ❌ → ⚠️，附测试依据说明
- README 版本号更新至 V3.1.4

---

## [未发布]

### 新增

- **OllamaTrunkKernel 拆分**：将 419 行的主类拆分为 4 个职责单一的组件类
  - `OllamaHttpClient` — HTTP 请求发送与请求 JSON 构建
  - `OllamaResponseParser` — 响应 JSON 解析与 mock token/logits 生成
  - `KernelMetricsCalculator` — confidence/reward 指标计算
  - `MockKVCacheManager` — KV cache 句柄模拟
- **集成测试**：新增 `OllamaTrunkKernelIntegrationTest`，包含 26 个组件单元测试（始终运行）与 3 个端到端集成测试（`-Dollama.test=true` 时运行真实 Ollama 调用）
- **公共接口 Javadoc**：为 `TrunkKernel`、`MemoryBackend`、`RLBranch`、`ContractArbiter` 四个核心接口添加完整 Javadoc 文档
- **JSON 构建改用 ObjectMapper**：`OllamaTrunkKernel` 中请求 JSON 从 `String.format` 拼接改为 `ObjectMapper.writeValueAsString`，消除手动转义与注入风险
- **JavaFX 全局异常处理**：通过 `Thread.setDefaultUncaughtExceptionHandler` 接入 `GlobalExceptionHandler`，未捕获异常以 Alert 弹窗展示
- **SeverityLevel 枚举**：`ContractClause.severity` 从 `double` 改为 `SeverityLevel` 枚举（MINOR=0.3, MAJOR=0.6, CRITICAL=1.0），消除魔法数字
- **GitHub Actions CI**：新增 `.github/workflows/ci.yml`，push/PR 自动运行 `mvn test`
- **test.bat 测试脚本**：配置 JDK 21 环境后运行 `mvn test` 的便捷脚本

### 变更

- `OllamaTrunkKernel` 从 419 行降至 ~248 行，仅负责组件组装与流程编排
- 测试总数从 26 项增至 55 项（新增 29 项组件/集成测试）

---

## [3.1.2] - 2026-07-11

### 新增

- **26 项单元测试**：覆盖 `EmbeddingService`（8项）、`FileSystemMemoryBackend`（10项）、`DefaultContractArbiter`（8项）核心路径

### 修复

- **store() 未初始化 heat**：`store()` 方法新增逻辑——heat 为 0 且 saliencyScore > 0 时从 saliencyScore 初始化 heat
- **清理 application.yml spring-ai 残留**：`spring.ai.ollama` 迁移至 `memorytree.ollama` 自定义命名空间，移除 `org.springframework.ai` 日志配置
- **提取 Ollama 真实指标**：从 `/api/generate` 响应解析 `eval_count`（真实 token 数）、`total_duration`（真实推理耗时），替代 mock 值
- **reward 改为质量启发式**：`calculateReward()` 基于响应长度、token 数量、推理耗时计算，替代 `Math.random()`
- **流式置信度统一**：流式版本从随机值改为与同步版本相同的 `calculateConfidence()` 关键词启发式
- **内存占用改为真实值**：从硬编码 `4L * 1024 * 1024 * 1024` 改为 `Runtime.getRuntime().totalMemory() - freeMemory()`
- **mock 项明确标注**：logits 和 KV cache 添加 `// MOCK` 注释

---

## [3.1.1] - 2026-07-11

### 新增

- **MIT LICENSE**：项目原先无 LICENSE 文件，法律上不可被任何人合法使用；现已添加 MIT 开源协议

### 移除

- **spring-ai 0.8.1 预发布依赖**：代码实际通过 `java.net.http.HttpClient` 直接调用 Ollama REST API，未使用任何 `org.springframework.ai` 类；移除该未使用的预发布依赖，同时移除 `spring-milestones` 仓库

### 修复

- **persist() 异常吞没**：`FileSystemMemoryBackend.persist()` 原先 `catch (IOException e) { // ignore }` 隐藏了潜在错误；现改为记录 ERROR 日志并抛出 `RuntimeException`
- **README 鲁棒性测试标记修正**：第十四章鲁棒性测试表格原标记为 ✅，与"无自动化测试代码"声明自相矛盾；现统一改为 ❌

---

## [3.1.0] - 2026-07-10

### 新增

- **热度衰减**：`decay_all_heat()` 支持自定义衰减率，记忆热度值范围 [0, 1]
- **KV 缓存句柄**：`getKVCacheHandle()` / `cloneKVCache()` / `restoreKVCache()` / `clearKVCache()`
- **运行时边界审计**：设计原则审计（4项）+ 边界校验（4项），确保系统运行符合设计原则
- **向量检索**：基于 Ollama Embedding API 的语义搜索，支持余弦相似度匹配
- `MemoryEntry` 新增 `heat` 字段（记忆热度值，范围 [0, 1]）
- `MemoryEntry` 新增 `embedding` 字段（4096 维文本嵌入向量）
- UI 新增 `custom-dialog`、`custom-alert` 统一弹窗样式

### 变更

- 全局 UI 优化：统一弹窗样式、控件对齐、深色主题美化、状态面板占比调整为 25%
- mini-chip padding 从 2×8 增加到 4×10，避免字体遮挡
- pane-header 高度从 32px 增加到 38px

### 修复

- 契约仲裁算法修复：逻辑推导和证据支持规则从 AND 改为 OR
- 置信度计算修复：从随机值改为基于文本内容计算
- 逻辑校验和重写修正状态显示修复：添加实时状态回调机制
- 版本号显示修复：V2.1 → V3.1
- 重复契约仲裁调用修复：移除多余的仲裁逻辑

---

## [3.0.0] - 2026-07-10

### 新增

- **树冠级并行探索**：多路径并行推理（不同温度参数）+ 最优路径选择
- **异步流式调用**：`generateAsync()` 支持流式回调，提升响应速度
- **推理流水线化**：prefetch → generate → validate 流水线重叠执行，提升吞吐量
- `build_index()` 倒排索引重建（优化记忆检索性能）
- `is_in_scope()` 领域范围检查（关键词匹配判断）
- `saveBranch()` 树枝持久化（JSON 格式保存参数）
- `configure_parallelism()` / `get_parallelism_status()` 并行配置接口
- `ParallelStatusDTO` 并行状态数据结构
- `GenerateResult` 新增 `content`、`confidence`、`reward`、`metadata` 字段

---

## [2.1.0] - 2026-07-10

### 新增

- **双实例内生自干涉**：高温生成 + 低温校验的交替执行，多轮内省循环
- **硬件自适应检测**：CPU 核心数、内存大小检测，资源占用实时监控
- **记忆固化状态机**：显著性检测（输出熵、推理链一致性、置信度）+ 记忆固化
- **多树枝并行评估**：线程池并行执行多个 RLBranch
- **异步 I/O 调度**：I/O 操作与计算重叠执行
- **契约仲裁**：契约规则 CRUD、合规校验
- JavaFX 桌面 EXE 应用（jpackage 打包）
- 初始记忆种子（学者 + 研究者角色）

---

## [2.0.0] - 2026-07-10

### 新增

- **四层核心架构**：
  - 树干内核层（TrunkKernel）
  - 强化学习树枝层（RLBranch）
  - 记忆后端层（MemoryBackend）
  - 调度控制层（SchedulerBus）
- **契约仲裁机制**：独立于模型的外部校验标准

---

[未发布]: https://github.com/lsx1156/MemoryTree/compare/v3.1.2...HEAD
[3.1.2]: https://github.com/lsx1156/MemoryTree/releases/tag/v3.1.2
[3.1.1]: https://github.com/lsx1156/MemoryTree/releases/tag/v3.1.1
[3.1.0]: https://github.com/lsx1156/MemoryTree/releases/tag/v3.1.0
[3.0.0]: https://github.com/lsx1156/MemoryTree/releases/tag/v3.0.0
[2.1.0]: https://github.com/lsx1156/MemoryTree/releases/tag/v2.1.0
[2.0.0]: https://github.com/lsx1156/MemoryTree/releases/tag/v2.0.0
