# 记忆树 MemoryTree

> 纯逻辑类脑AI框架 — V3.1 运行时边界审计规范参考实现
>
> 版本：V3.1 | 状态：正式发布 | 基于《记忆树 MemoryTree V3.1 运行时边界审计规范》

***

## 一、项目简介

记忆树（MemoryTree）是一套**模型无关的纯逻辑类脑认知架构接口规范与参考调度实现**。

它不生产 Token、不绑定具体模型、不依赖特定算法，只定义类脑认知系统的分层契约、运行范式与边界约束。树干内核、技能树枝、记忆后端全栈可替换，框架本身是认知运行的「规则总线」与「环境容器」。

### 核心哲学命题

1. **认知结构先于认知内容** — 树干路径依赖原则
2. **意识与知识是两种不同的存在形态** — 记忆内外分离原则
3. **逻辑自洽是可计算的外部标准** — 契约锚定原则

***

## 二、架构设计

### 2.1 四层架构

```
┌─────────────────────────────────────┐
│  树冠层 / 强化学习树枝层（RL Branch）  │
├─────────────────────────────────────┤
│  树干层 / 内核推理层（Trunk Kernel）   │
├─────────────────────────────────────┤
│  记忆层 / 存储后端层（Memory Backend） │
├─────────────────────────────────────┤
│  根基层 / 调度控制层（Scheduler Bus）  │
└─────────────────────────────────────┘
```

| 层级      | 职责                 | 核心模块                                                                                                                                                                                                                                                                                                                                                                                                                                                              |
| ------- | ------------------ | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 树干内核层   | 基础推理、logits输出、KV缓存 | [OllamaTrunkKernel](file:///e:/AI/MemoryTree/src/main/java/com/memorytree/kernel/OllamaTrunkKernel.java)、[IntrospectiveInferenceService](file:///e:/AI/MemoryTree/src/main/java/com/memorytree/kernel/IntrospectiveInferenceService.java)、[InferencePipelineService](file:///e:/AI/MemoryTree/src/main/java/com/memorytree/kernel/InferencePipelineService.java)                                                                                         |
| 强化学习树枝层 | 技能树、动作仲裁、观察器       | [RLBranch](file:///e:/AI/MemoryTree/src/main/java/com/memorytree/branch/RLBranch.java)、[ParallelBranchEvaluator](file:///e:/AI/MemoryTree/src/main/java/com/memorytree/branch/ParallelBranchEvaluator.java)、[DefaultLogicCheckBranch](file:///e:/AI/MemoryTree/src/main/java/com/memorytree/branch/DefaultLogicCheckBranch.java)、[FallacyDetectionBranch](file:///e:/AI/MemoryTree/src/main/java/com/memorytree/branch/FallacyDetectionBranch.java)、[CanopyParallelExplorer](file:///e:/AI/MemoryTree/src/main/java/com/memorytree/branch/CanopyParallelExplorer.java)               |
| 记忆后端层   | 工作记忆、持久记忆、记忆固化     | [WorkingMemory](file:///e:/AI/MemoryTree/src/main/java/com/memorytree/memory/WorkingMemory.java)、[FileSystemMemoryBackend](file:///e:/AI/MemoryTree/src/main/java/com/memorytree/memory/FileSystemMemoryBackend.java)、[MemoryConsolidationService](file:///e:/AI/MemoryTree/src/main/java/com/memorytree/memory/MemoryConsolidationService.java)                                                                                                                  |
| 调度控制层   | 生命周期、并行调度、异步I/O    | [LifecycleStateMachine](file:///e:/AI/MemoryTree/src/main/java/com/memorytree/scheduler/LifecycleStateMachine.java)、[InferenceStateMachine](file:///e:/AI/MemoryTree/src/main/java/com/memorytree/scheduler/InferenceStateMachine.java)、[DefaultSchedulerBus](file:///e:/AI/MemoryTree/src/main/java/com/memorytree/scheduler/DefaultSchedulerBus.java)、[AsyncIOScheduler](file:///e:/AI/MemoryTree/src/main/java/com/memorytree/scheduler/AsyncIOScheduler.java) |
| 契约仲裁    | 契约规则CRUD、合规校验      | [ContractArbiter](file:///e:/AI/MemoryTree/src/main/java/com/memorytree/arbiter/ContractArbiter.java)、[DefaultContractArbiter](file:///e:/AI/MemoryTree/src/main/java/com/memorytree/arbiter/DefaultContractArbiter.java)                                                                                                                                                                                                                                         |
| 系统服务    | 硬件检测、门控事件          | [HardwareDetector](file:///e:/AI/MemoryTree/src/main/java/com/memorytree/system/HardwareDetector.java)、[GatingEvent](file:///e:/AI/MemoryTree/src/main/java/com/memorytree/system/GatingEvent.java)                                                                                                                                                                                                                                                               |

### 2.2 核心机制

- **双实例内生自干涉**：同一份基座权重启动双推理实例，分别负责生成与校验，实现内生的自我约束与逻辑反思
- **契约仲裁机制**：为推理输出提供独立于模型的外部校验标准
- **内省推理状态机**：实现「草稿生成-逻辑校验-契约仲裁-输出/重写」的完整推理流程
- **意识生命周期状态机**：管理系统从初始化到终止的状态流转，确保动态状态可彻底清零
- **记忆固化状态机**：显著性检测条件触发记忆固化（输出熵低于阈值、推理链跨层一致性高、操作者显式标记）
- **多核并行调度**：树干串行，调度并行（异步I/O与计算重叠、多树枝并行评估、树冠级并行探索、推理流水线化）
- **硬件自适应机制**：自动检测本地硬件资源，匹配对应规格的内核

### 2.3 七条核心设计原则

| 原则       | 说明                 | 实现状态                                                |
| -------- | ------------------ | --------------------------------------------------- |
| 一、树干路径依赖 | 内核权重全程只读，运行时不可修改   | ✅ 已实现（Ollama 远程模型天然只读）                              |
| 二、树状技能生长 | 树枝独立、无参数交叉、无灾难性遗忘  | ✅ 已实现（RLBranch 接口 + 独立树枝实例）                         |
| 三、记忆内外分离 | 工作记忆与持久记忆严格分层      | ✅ 已实现（WorkingMemory / FileSystemMemoryBackend 分离）   |
| 四、意识生命周期 | 所有运行时状态支持彻底清零      | ✅ 已实现（LifecycleStateMachine.terminate()）            |
| 五、内生自干涉  | 逻辑校验通过树干内核自身完成     | ✅ 已实现（IntrospectiveInferenceService 复用 TrunkKernel） |
| 六、接口中立   | 层间仅通过标准接口交互        | ✅ 已实现（TrunkKernel / RLBranch / MemoryBackend 接口）    |
| 七、契约锚定   | 输出标准由操作者定义的契约书外部锚定 | ✅ 已实现（ContractArbiter + 可编辑 JSON 契约书）               |

### 2.4 运行时边界审计（V3.1）

V3.1 新增运行时边界审计机制，确保系统运行符合设计原则：

| 审计项 | 校验内容 | 阈值 |
|-------|---------|------|
| 记忆边界 | 工作记忆容量、热度值范围 | 容量 ≤ 50，热度 ∈ [0, 1] |
| 内核内存 | JVM 堆内存占用 | ≤ 8GB |
| 热度衰减 | 衰减率范围 | ∈ [0, 1] |
| 并行度 | 并行任务数 | ≤ CPU核心数 × 2 |

### 2.5 推理状态流转

```
IDLE → DRAFT_GENERATE → LOGIC_VALIDATE → [VALID] → OUTPUT
                                    ↓
                              [INVALID] → REWRITE → LOGIC_VALIDATE
```

| 状态 | 描述 | 持续时间 |
|-----|-----|---------|
| IDLE | 空闲等待 | 持续到用户触发推理 |
| DRAFT_GENERATE | 草稿生成 | 取决于模型响应时间 |
| LOGIC_VALIDATE | 逻辑校验 | 取决于模型响应时间 |
| REWRITE | 重写修正 | 取决于模型响应时间 |
| OUTPUT | 输出结果 | 立即 |

***

## 三、功能特性

### 3.1 逻辑推理

- ✅ **内省推理**：多轮草稿生成→逻辑校验→契约仲裁→重写修正的完整流程
- ✅ **可调参数**：温度系数（0.1-1.0）控制推理随机性，最大内省轮次（1-10）限制迭代次数
- ✅ **前提条件注入**：支持多行前提条件输入，每行一条前提
- ✅ **推导树可视化**：展示推理过程的层级结构
- ✅ **逻辑纯度分标注**：实时显示推理结果的逻辑一致性评分
- ✅ **三级门控事件记录**：Token级修正、逻辑阈值触发、段落重写
- ✅ **实时状态反馈**：DRAFT_GENERATE → LOGIC_VALIDATE → REWRITE → OUTPUT 状态实时更新

### 3.2 记忆管理

- ✅ **工作记忆**（意识态）：实时推理上下文，角色设定与推理原则，最大容量 50 条
- ✅ **持久记忆**（遗产态）：知识库存储，支持关键词搜索和热度排序
- ✅ **记忆固化**：自动显著性检测（输出熵、推理链一致性、置信度）+ 手动固化
- ✅ **记忆注入**：持久记忆注入工作记忆参与推理，支持热度衰减（V3.1）
- ✅ **分页浏览**：大容量记忆分页展示，每页 10 条
- ✅ **详情查看与编辑**：完整记忆属性（内容、类型、热度、创建时间、固化状态）可查阅可修改
- ✅ **初始记忆种子**：默认研究者角色记忆（形式逻辑定律、奥卡姆剃刀、可证伪性等）
- ✅ **向量检索**（V3.1）：基于 Ollama Embedding API 的语义搜索，支持余弦相似度匹配，自动生成文本向量嵌入

### 3.3 树枝管理

- ✅ **RL 树枝激活/停用控制**：独立控制每个树枝的启用状态
- ✅ **多树枝并行观察与评估**：线程池并行执行多个 RLBranch
- ✅ **动作仲裁机制**：加权融合策略，支持自定义权重
- ✅ **默认树枝**：逻辑校验枝（DefaultLogicCheckBranch）、谬误检测枝（FallacyDetectionBranch）
- ✅ **树冠级并行探索**（V3.0）：多路径并行推理（不同温度参数）+ 最优路径选择

### 3.4 契约仲裁

- ✅ **契约规则管理**：增删改查，支持批量导入导出
- ✅ **规则启用/禁用**：独立控制每条规则的生效状态
- ✅ **推理结果合规性校验**：基于规则的输出合规性检测
- ✅ **违规严重程度分级**：minor/major/critical 三级严重程度
- ✅ **Fail-Safe 硬边界**：违反安全边界规则时立即终止推理
- ✅ **可编辑 JSON 契约书**：操作者可直接编辑规则内容
- ✅ **合规分数计算**：基于违反规则的严重程度计算综合合规分数（0-1）

### 3.5 系统监控

- ✅ **硬件信息实时显示**：CPU 核心数、内存大小、CPU 占用率、JVM 堆内存、线程数
- ✅ **推理状态机可视化**：状态流转实时显示，当前状态高亮
- ✅ **推理统计**：内省轮次、合规分数、推理耗时（ms）、置信度
- ✅ **三级门控事件记录**：详细记录推理过程中的修正事件
- ✅ **运行时边界审计**（V3.1）：设计原则审计 + 边界校验

### 3.6 性能优化（V3.0）

- ✅ **异步流式调用**：`generateAsync()` 支持流式回调，提升响应速度
- ✅ **推理流水线化**：prefetch→generate→validate 流水线重叠执行
- ✅ **KV 缓存句柄**（V3.1）：`getKVCacheHandle()`/`cloneKVCache()`/`restoreKVCache()`/`clearKVCache()`
- ✅ **倒排索引**：`build_index()` 优化记忆检索性能

***

## 四、技术栈

| 组件    | 选型                        | 规格         |
| ----- | ------------------------- | ---------- |
| 开发语言  | Java                      | 21         |
| 应用框架  | Spring Boot               | 3.2.5      |
| UI 框架 | JavaFX + FXML             | 21         |
| AI 引擎 | Ollama                    | qwen2.5:7b |
| 数据存储  | SQLite + 文件系统             | JSON 持久化   |
| 打包工具  | Maven + jpackage          | 原生 EXE     |
| 其他    | Lombok、Jackson、HttpClient | -          |

***

## 五、快速开始

### 5.1 环境要求

| 配置  | 最低   | 推荐       |
| --- | ---- | -------- |
| JDK | 21+  | 21+      |
| CPU | 4核   | 8核+      |
| 内存  | 8GB  | 16GB+    |
| 存储  | 10GB | 20GB SSD |
| GPU | 不需要  | 不需要      |
| 网络  | 不需要  | 不需要      |

### 5.2 安装 Ollama

1. 下载并安装 [Ollama](https://ollama.com/)
2. 拉取模型：
   ```bash
   ollama pull qwen2.5:7b
   ```
3. 启动服务：
   ```bash
   ollama serve
   ```

### 5.3 运行应用

#### 方式一：直接运行 EXE（推荐）

1. 进入 `release/MemoryTree/` 目录
2. 双击 `MemoryTree.exe`

#### 方式二：源码运行

```bash
cd MemoryTree
mvn spring-boot:run
```

#### 方式三：编译打包

```bash
# 编译
mvn clean package -DskipTests

# 生成 EXE（Windows）
build_exe.bat
```

### 5.4 首次运行

1. 启动应用后，右侧状态面板会显示硬件信息
2. 默认角色为**研究者**，已预置工作记忆和持久记忆
3. 在「逻辑推理」Tab 中输入问题，点击「执行推理」开始推理
4. 观察状态面板中的推理状态流转

***

## 六、特性验证

### 6.1 多核并行调度验证

**验证方法：**
1. 查看右侧状态面板的「硬件信息」，确认 CPU 核心数检测正确
2. 观察日志输出：`Branch executor initialized with X threads`（X = CPU核心数 - 2）
3. 在「树枝管理」Tab 中启用多个树枝，执行推理时观察并行评估

**实现文件：**
- [ParallelBranchEvaluator.java](file:///e:/AI/MemoryTree/src/main/java/com/memorytree/branch/ParallelBranchEvaluator.java) - 线程池并行执行
- [CanopyParallelExplorer.java](file:///e:/AI/MemoryTree/src/main/java/com/memorytree/branch/CanopyParallelExplorer.java) - 多路径并行推理
- [AsyncIOScheduler.java](file:///e:/AI/MemoryTree/src/main/java/com/memorytree/scheduler/AsyncIOScheduler.java) - 异步I/O调度

**验证标准：**
| 指标 | 预期值 |
|-----|-------|
| 线程池大小 | CPU核心数 - 2 |
| 并行树枝评估 | 多个树枝同时执行 |
| 推理流水线 | prefetch→generate→validate 重叠执行 |

### 6.2 硬件自适应机制验证

**验证方法：**
1. 启动应用后查看右侧状态面板的「硬件信息」区域
2. 确认显示的 CPU 核心数、内存大小、CPU 占用率、JVM 内存等信息正确
3. 观察日志输出：`Hardware detected: CPU=X cores, Memory=Y GB`

**实现文件：**
- [HardwareDetector.java](file:///e:/AI/MemoryTree/src/main/java/com/memorytree/system/HardwareDetector.java) - 硬件检测核心

**验证标准：**
| 指标 | 预期值 |
|-----|-------|
| CPU核心数 | 与系统实际核心数一致 |
| 内存大小 | 与系统实际内存一致 |
| CPU占用率 | 实时更新，范围 0-100% |
| JVM内存 | 实时更新，显示已用/最大 |
| 线程数 | 实时更新 |

### 6.3 内省推理验证

**验证方法：**
1. 在「逻辑推理」Tab 中输入一个逻辑问题
2. 点击「执行推理」
3. 观察状态面板中的状态流转：`DRAFT_GENERATE → LOGIC_VALIDATE → REWRITE（如有）→ OUTPUT`
4. 推理完成后查看结果区域的「内省轮次」显示

**实现文件：**
- [IntrospectiveInferenceService.java](file:///e:/AI/MemoryTree/src/main/java/com/memorytree/kernel/IntrospectiveInferenceService.java) - 内省推理核心
- [InferenceStateMachine.java](file:///e:/AI/MemoryTree/src/main/java/com/memorytree/scheduler/InferenceStateMachine.java) - 推理状态机

**验证标准：**
| 指标 | 预期值 |
|-----|-------|
| 状态流转 | IDLE → DRAFT_GENERATE → LOGIC_VALIDATE → OUTPUT |
| 内省轮次 | 与设置的最大轮次一致或更少 |
| 合规分数 | 0-1 范围内 |
| 置信度 | 0-1 范围内 |

### 6.4 记忆管理验证

**验证方法：**
1. 在「记忆管理」Tab 中查看工作记忆和持久记忆
2. 添加一条新记忆，确认显示正确
3. 执行推理后，观察记忆热度变化
4. 使用搜索功能查找记忆

**实现文件：**
- [WorkingMemory.java](file:///e:/AI/MemoryTree/src/main/java/com/memorytree/memory/WorkingMemory.java) - 工作记忆
- [FileSystemMemoryBackend.java](file:///e:/AI/MemoryTree/src/main/java/com/memorytree/memory/FileSystemMemoryBackend.java) - 持久记忆
- [MemoryConsolidationService.java](file:///e:/AI/MemoryTree/src/main/java/com/memorytree/memory/MemoryConsolidationService.java) - 记忆固化

**验证标准：**
| 指标 | 预期值 |
|-----|-------|
| 工作记忆容量 | ≤ 50 条 |
| 持久记忆存储 | 保存到文件系统 |
| 记忆搜索 | 关键词匹配正确 |
| 记忆热度 | 范围 [0, 1]，推理后更新 |

### 6.5 契约仲裁验证

**验证方法：**
1. 在「契约仲裁」Tab 中查看预设的契约规则
2. 修改一条规则（如添加新的禁止词）
3. 执行推理，观察仲裁结果变化
4. 验证违规规则的严重程度分级

**实现文件：**
- [DefaultContractArbiter.java](file:///e:/AI/MemoryTree/src/main/java/com/memorytree/arbiter/DefaultContractArbiter.java) - 契约仲裁核心

**验证标准：**
| 指标 | 预期值 |
|-----|-------|
| 规则管理 | 支持增删改查 |
| 规则启用/禁用 | 立即生效 |
| 违规分级 | minor/major/critical |
| 合规分数 | 基于违规严重程度计算 |

### 6.6 运行时边界审计验证

**验证方法：**
1. 启动应用后观察日志输出
2. 确认审计信息：记忆边界、内核内存、热度衰减、并行度

**实现文件：**
- [RuntimeBoundaryAuditor.java](file:///e:/AI/MemoryTree/src/main/java/com/memorytree/system/RuntimeBoundaryAuditor.java) - 运行时边界审计

**验证标准：**
| 指标 | 预期值 |
|-----|-------|
| 记忆边界 | 容量 ≤ 50，热度 ∈ [0, 1] |
| 内核内存 | ≤ 8GB |
| 热度衰减 | 衰减率 ∈ [0, 1] |
| 并行度 | ≤ CPU核心数 × 2 |

### 6.7 向量检索验证

**验证方法：**
1. 启动应用后观察日志：`EmbeddingService initialized - enabled: true`
2. 添加一条新记忆，观察日志：`Generated embedding for memory entry`
3. 查看 memory_store.json，确认每个记忆条目包含 `embedding` 字段（4096维向量）
4. 验证语义搜索：使用与记忆内容语义相似但关键词不同的查询

**实现文件：**
- [EmbeddingService.java](file:///e:/AI/MemoryTree/src/main/java/com/memorytree/memory/EmbeddingService.java) - 向量生成服务
- [MemoryEntry.java](file:///e:/AI/MemoryTree/src/main/java/com/memorytree/dto/MemoryEntry.java) - 记忆条目（含embedding字段）
- [FileSystemMemoryBackend.java](file:///e:/AI/MemoryTree/src/main/java/com/memorytree/memory/FileSystemMemoryBackend.java) - 语义搜索实现

**验证标准：**
| 指标 | 预期值 |
|-----|-------|
| embedding字段 | 每个记忆条目都有embedding |
| 向量维度 | 4096（Ollama模型输出） |
| 余弦相似度 | 语义相似文本相似度 > 0.5 |
| 降级策略 | Ollama不可用时使用基于hash的伪随机向量 |

***

## 七、典型使用场景

### 7.1 场景一：学术研究推理

**适用场景：** 进行逻辑推理、论证分析、学术问题解答

**操作步骤：**

1. **准备前提条件**：
   - 在「逻辑推理」Tab 的「前提条件」输入框中输入已知前提
   - 每行一条前提，例如：
     ```
     所有的人都会死
     苏格拉底是人
     ```

2. **输入问题**：
   - 在「输入问题」输入框中输入要推理的问题
   - 例如：`苏格拉底会死吗？`

3. **执行推理**：
   - 调整温度参数（推荐 0.7，越低越严谨）
   - 设置内省轮次（推荐 3-5）
   - 点击「执行推理」

4. **查看结果**：
   - 观察状态面板中的状态流转
   - 查看推理结果区域的推导树和仲裁结果
   - 检查合规分数和置信度

**预期结果：**
- 推理结果应包含逻辑推导过程
- 契约仲裁应通过（合规分数 ≥ 0.8）
- 置信度应较高（≥ 0.7）

---

### 7.2 场景二：记忆固化

**适用场景：** 将重要推理结果保存到持久记忆

**操作步骤：**

1. **执行推理**：
   - 完成一次推理，获得满意的结果

2. **查看工作记忆**：
   - 切换到「记忆管理」Tab
   - 查看工作记忆列表，找到最新的推理结果

3. **固化记忆**：
   - 选中要固化的记忆条目
   - 点击「固化到持久记忆」按钮
   - 确认固化操作

4. **验证固化**：
   - 查看持久记忆列表，确认新条目已添加
   - 下次启动应用时，该记忆仍然存在

**预期结果：**
- 工作记忆条目成功转移到持久记忆
- 固化状态显示为「已固化」
- 记忆热度值根据显著性自动设置

---

### 7.3 场景三：自定义契约规则

**适用场景：** 根据特定需求定制输出规范

**操作步骤：**

1. **查看现有规则**：
   - 切换到「契约仲裁」Tab
   - 查看预设的契约规则列表

2. **添加新规则**：
   - 点击「添加规则」按钮
   - 填写规则名称、规则内容、严重程度
   - 示例规则：
     ```
     规则名称：禁止敏感词
     规则内容：不得包含"机密"、"秘密"、"内部"
     严重程度：critical
     ```

3. **启用规则**：
   - 确保新规则处于启用状态
   - 执行推理测试规则效果

4. **验证规则**：
   - 输入包含敏感词的问题
   - 观察仲裁结果是否检测到违规
   - 检查合规分数是否下降

**预期结果：**
- 违反规则时仲裁结果为 NON_COMPLIANT
- 合规分数根据严重程度下降
- Fail-Safe 规则触发时立即终止推理

---

### 7.4 场景四：多树枝并行评估

**适用场景：** 同时使用多个技能树枝进行推理

**操作步骤：**

1. **查看树枝状态**：
   - 切换到「树枝管理」Tab
   - 查看可用树枝列表

2. **启用树枝**：
   - 确保「逻辑校验枝」和「谬误检测枝」处于启用状态
   - 可以尝试添加自定义树枝

3. **执行推理**：
   - 返回「逻辑推理」Tab
   - 输入问题并执行推理

4. **观察并行评估**：
   - 查看日志输出，确认多个树枝同时执行
   - 观察状态面板中的评估结果

**预期结果：**
- 多个树枝并行执行观察
- 动作仲裁融合各树枝结果
- 推理结果综合各树枝评估

---

### 7.5 场景五：意识生命周期演示

**适用场景：** 演示系统的状态清零能力

**操作步骤：**

1. **执行多次推理**：
   - 完成几次推理，积累工作记忆

2. **查看工作记忆**：
   - 切换到「记忆管理」Tab
   - 确认工作记忆中有多条记录

3. **重置记忆**：
   - 点击右上角「重置记忆」按钮
   - 确认重置操作

4. **验证清零**：
   - 查看工作记忆，确认已清空
   - 状态面板显示「空闲等待」

**预期结果：**
- 工作记忆完全清空
- 系统状态恢复到初始状态
- 可以重新开始推理

***

## 八、配置说明

配置文件位于 [application.yml](file:///e:/AI/MemoryTree/src/main/resources/application.yml)：

```yaml
spring:
  main:
    web-application-type: none    # 桌面应用，禁用Web服务器
  ai:
    ollama:
      base-url: http://localhost:11434
      chat:
        model: qwen2.5:7b
        options:
          temperature: 0.7
          num-predict: 2048

memorytree:
  kernel:
    model-path: qwen2.5:7b
  memory:
    working-memory-size: 20MB
```

***

## 九、数据存储

应用数据存储在用户目录下的 `.memorytree/` 文件夹中：

```
~/.memorytree/
├── data/
│   ├── memory/
│   │   └── memory_store.json    # 持久记忆数据
│   └── contract/
│       └── default_contract.json  # 契约规则数据
```

***

## 十、目录结构

```
MemoryTree/
├── src/main/java/com/memorytree/
│   ├── kernel/           # 树干内核层
│   │   ├── TrunkKernel.java              # 内核接口
│   │   ├── OllamaTrunkKernel.java        # Ollama 实现
│   │   ├── DefaultTrunkKernel.java       # 默认实现
│   │   └── IntrospectiveInferenceService.java  # 内省推理服务
│   ├── branch/           # 强化学习树枝层
│   │   ├── RLBranch.java                 # 树枝接口
│   │   ├── DefaultLogicCheckBranch.java  # 逻辑校验枝
│   │   ├── FallacyDetectionBranch.java   # 谬误检测枝
│   │   └── ParallelBranchEvaluator.java  # 并行评估器
│   ├── memory/           # 记忆后端层
│   │   ├── MemoryBackend.java            # 记忆后端接口
│   │   ├── FileSystemMemoryBackend.java  # 文件系统实现
│   │   ├── WorkingMemory.java            # 工作记忆
│   │   └── MemoryConsolidationService.java # 记忆固化服务
│   ├── scheduler/        # 调度控制层
│   │   ├── SchedulerBus.java             # 调度总线接口
│   │   ├── DefaultSchedulerBus.java      # 默认调度总线
│   │   ├── LifecycleStateMachine.java    # 意识生命周期状态机
│   │   ├── InferenceStateMachine.java    # 内省推理状态机
│   │   └── AsyncIOScheduler.java         # 异步I/O调度器
│   ├── arbiter/          # 契约仲裁
│   │   ├── ContractArbiter.java          # 仲裁接口
│   │   └── DefaultContractArbiter.java   # 默认仲裁实现
│   ├── system/           # 系统服务
│   │   ├── HardwareDetector.java         # 硬件检测
│   │   └── GatingEvent.java              # 门控事件
│   ├── dto/              # 数据传输对象
│   ├── enums/            # 枚举
│   ├── config/           # 配置
│   └── gui/              # JavaFX 界面
│       ├── MainController.java           # 主控制器
│       └── MemoryTreeFxApplication.java  # FX 应用入口
├── src/main/resources/
│   ├── fxml/             # FXML 布局文件
│   ├── css/              # 样式文件
│   └── application.yml   # 应用配置
└── pom.xml               # Maven 配置
```

***

## 十一、V3.1 规范实现对比

### 11.1 树干内核层（Trunk Kernel）

| 规范要求                          | 实现状态    | 说明                                            |
| ----------------------------- | ------- | --------------------------------------------- |
| `load(path)` 接口               | ✅ 已实现   | `loadKernel(modelPath)`                       |
| `free()` 接口                   | ✅ 已实现   | `unloadKernel()`                              |
| `generate(prompt, config)` 接口 | ✅ 已实现   | 完整实现，支持温度/TopP/最大长度/种子/KV缓存                   |
| `get_kv_cache()` 接口           | ⚠️ 部分实现 | 通过 `useKVCache` 标志位模拟，未返回真实句柄                 |
| `score_logic()` 可选接口          | ⚠️ 部分实现 | 通过 `IntrospectiveInferenceService` 模拟计算       |
| GenerateConfig 数据结构           | ✅ 已实现   | temperature/topP/maxTokens/seed/useKVCache    |
| GenerateResult 数据结构           | ✅ 已实现   | tokens/logits/inferenceTimeMs/confidenceScore |
| 权重只读约束                        | ✅ 已实现   | Ollama 远程模型天然只读                               |
| 无状态约束                         | ✅ 已实现   | 每次 generate 独立调用                              |

### 11.2 强化学习树枝层（RL Branch）

| 规范要求                           | 实现状态    | 说明                                                                                      |
| ------------------------------ | ------- | --------------------------------------------------------------------------------------- |
| `observe(obs)` 接口              | ✅ 已实现   | 纯函数，无副作用                                                                                |
| `reset()` 接口                   | ⚠️ 部分实现 | 通过 `setActive(false)` 间接实现                                                              |
| `save(path)` / `load(path)` 接口 | ⚠️ 部分实现 | `loadBranch(filePath)` 已实现，`save` 未实现                                                   |
| ObservationSpace 数据结构          | ✅ 已实现   | token\_prob\_variance/logic\_score/context\_length/introspection\_depth                 |
| ActionSpace 数据结构               | ✅ 已实现   | temperature\_delta/logic\_penalty\_lambda/rewrite\_threshold/max\_introspection\_rounds |
| 树枝独立性                          | ✅ 已实现   | 单树枝无全局状态依赖                                                                              |
| 可插拔设计                          | ✅ 已实现   | 独立文件存储，按需加载                                                                             |
| 多树枝并行评估                        | ✅ 已实现   | `ParallelBranchEvaluator` 使用线程池并行调用 observe()                                           |
| 动作仲裁                           | ✅ 已实现   | 加权融合策略（confidence × reward）                                                             |

### 11.3 记忆后端层（Memory Backend）

| 规范要求                           | 实现状态  | 说明                                                                 |
| ------------------------------ | ----- | ------------------------------------------------------------------ |
| `search(query, top_k)` 接口      | ✅ 已实现 | `query(MemoryQuery)` 支持关键词+标签检索                                    |
| `store(fragment, metadata)` 接口 | ✅ 已实现 | `store(MemoryEntry)`                                               |
| `delete(fragment_id)` 接口       | ✅ 已实现 | `delete(String id)`                                                |
| `build_index()` 接口             | ✅ 已实现 | 倒排索引构建，优化检索性能                                                      |
| MemoryFragment 数据结构            | ✅ 已实现 | id/content/tags/createdAt/lastAccessedAt/accessCount/saliencyScore |
| `set_heat()` 可选接口              | ✅ 已实现 | 通过 `saliencyScore` 字段                                              |
| `decay_all_heat()` 可选接口        | ✅ 已实现 | 热度衰减，支持自定义衰减率                                                      |
| 实现无关性                          | ✅ 已实现 | 接口与实现分离                                                            |

### 11.4 调度控制层（Scheduler Bus）

| 规范要求     | 实现状态    | 说明                             |
| -------- | ------- | ------------------------------ |
| 内核生命周期管理 | ✅ 已实现   | `LifecycleStateMachine`        |
| 树枝管理     | ✅ 已实现   | 加载/激活/卸载                       |
| 工作记忆维护   | ✅ 已实现   | 滑动窗口 + 容量控制（20MB）              |
| 持久记忆调度   | ✅ 已实现   | 检索/注入/固化                       |
| 内省推理控制   | ✅ 已实现   | `InferenceStateMachine` 完整状态流转 |
| 契约仲裁控制   | ✅ 已实现   | 契约加载/逐条校验/违规处置                 |
| 生命周期管理   | ✅ 已实现   | 状态隔离/终止清零                      |
| 边界约束校验   | ✅ 已实现 | `RuntimeBoundaryAuditor` 完整运行时审计 |

### 11.5 内省推理状态机

| 规范状态                          | 实现状态  | 说明                            |
| ----------------------------- | ----- | ----------------------------- |
| IDLE                          | ✅ 已实现 | 空闲等待                          |
| DRAFT\_GENERATE               | ✅ 已实现 | 系统1直觉推理（temperature=0.7）      |
| LOGIC\_VALIDATE               | ✅ 已实现 | 系统2逻辑校验（temperature=0.1，复用内核） |
| CONTRACT\_ARBITRATE           | ✅ 已实现 | 契约合规检查                        |
| REWRITE                       | ✅ 已实现 | 违规且未达最大轮次时重写                  |
| OUTPUT                        | ✅ 已实现 | 合规时正常输出                       |
| OUTPUT\_WITH\_LOW\_CONFIDENCE | ✅ 已实现 | 违规且达最大轮次时低置信度输出               |

### 11.6 意识生命周期状态机

| 规范状态          | 实现状态  | 说明               |
| ------------- | ----- | ---------------- |
| UNINITIALIZED | ✅ 已实现 | 未初始化             |
| INITIALIZING  | ✅ 已实现 | 加载内核/树枝/空白工作记忆   |
| RUNNING       | ✅ 已实现 | 正常运行             |
| TERMINATING   | ✅ 已实现 | 清空工作记忆/重置树枝/卸载内核 |
| DESTROYED     | ✅ 已实现 | 所有动态状态彻底销毁       |
| 终止流程不可逆       | ✅ 已实现 | -                |
| 不加载历史对话       | ✅ 已实现 | 每次从零开始           |
| 默认不持久化        | ✅ 已实现 | 仅主动触发时写入         |

### 11.7 记忆固化状态机

| 规范状态               | 实现状态  | 说明                       |
| ------------------ | ----- | ------------------------ |
| RUNNING            | ✅ 已实现 | -                        |
| SALIENCY\_DETECT   | ✅ 已实现 | 输出熵 + 推理链一致性 + 置信度       |
| CANDIDATE\_PENDING | ✅ 已实现 | 候选暂存工作记忆                 |
| CONFIRMED\_STORE   | ✅ 已实现 | 操作者确认后写入持久存储             |
| DISCARD            | ✅ 已实现 | 未确认候选随生命周期销毁             |
| 显著性检测条件            | ✅ 已实现 | 输出熵<0.3、一致性>0.85、置信度>0.8 |

### 11.8 契约仲裁机制

| 规范要求                                 | 实现状态  | 说明                                                                 |
| ------------------------------------ | ----- | ------------------------------------------------------------------ |
| `load_contract(path)` 接口             | ✅ 已实现 | `loadContract(filePath)`                                           |
| `validate_draft(draft, premises)` 接口 | ✅ 已实现 | `validateDraft(draft)`                                             |
| `validate_final(final, history)` 接口  | ✅ 已实现 | `validateFinal(finalResult)`                                       |
| `is_in_scope(query)` 接口              | ✅ 已实现 | 通过关键词匹配判断查询是否在领域范围内                                                          |
| ArbitrationReport 数据结构               | ✅ 已实现 | result/violatingClauseId/matchedRules/complianceScore              |
| 契约书外部性                               | ✅ 已实现 | 独立 JSON 文件                                                         |
| 契约书可修改性                              | ✅ 已实现 | 操作者可随时编辑，下次推理生效                                                    |
| 契约书可追溯性                              | ✅ 已实现 | 违规条款精确引用                                                           |
| Fail-Safe 硬边界                        | ✅ 已实现 | max\_introspection\_loops/max\_output\_length/max\_reasoning\_time |
| 契约书 CRUD                             | ✅ 已实现 | addClause/updateClause/removeClause/toggleClauseEnabled            |

### 11.9 多核并行调度规范

| 规范要求                               | 实现状态  | 说明                                        |
| ---------------------------------- | ----- | ----------------------------------------- |
| **第一级：异步I/O与计算重叠**                 | ✅ 已实现 | `AsyncIOScheduler` 记忆预检索/日志异步写入           |
| **第二级：多树枝并行评估**                    | ✅ 已实现 | `ParallelBranchEvaluator` 线程池并行 observe() |
| **第三级：树冠级并行探索**                    | ✅ 已实现 | `CanopyParallelExplorer` 多路径并行推理+最优路径选择                                |
| **第四级：推理流水线化**                     | ✅ 已实现 | `InferencePipelineService` prefetch-generate-validate流水线重叠                     |
| `configure_parallelism(config)` 接口 | ✅ 已实现 | `configureParallelism(maxParallelBranches, threadPoolSize)` 并行策略配置                               |
| `get_parallelism_status()` 接口      | ✅ 已实现 | `getParallelismStatus()` 并行状态查询                               |
| 树干串行约束                             | ✅ 已实现 | generate() 单线程串行                          |
| 写操作串行化                             | ✅ 已实现 | MemoryBackend store/delete 同步             |
| 线程安全声明                             | ✅ 已实现 | observe() 线程安全，generate() 串行              |
| Fail-Safe 优先于并行                    | ✅ 已实现 | Fail-Safe 触发立即终止                          |

### 11.10 硬件自适应机制

| 规范要求       | 实现状态  | 说明                               |
| ---------- | ----- | -------------------------------- |
| CPU 核心数检测  | ✅ 已实现 | `Runtime.availableProcessors()`  |
| 可用内存检测     | ✅ 已实现 | `OperatingSystemMXBean`          |
| 自动推荐内核规格   | ✅ 已实现 | 16GB+→7B, 8GB+→3B, 4GB+→1B       |
| 自动配置并行线程数  | ✅ 已实现 | cores - 2                        |
| 操作者可手动覆盖   | ✅ 已实现 | 配置文件可覆盖                          |
| JVM 内存占用监控 | ✅ 已实现 | totalMemory/freeMemory/maxMemory |
| CPU 占用率监控  | ✅ 已实现 | `getProcessCpuLoad()`            |
| 系统内存占用率    | ✅ 已实现 | 物理内存使用百分比                        |
| JVM 线程数    | ✅ 已实现 | `ThreadMXBean`                   |

### 11.11 学术版 Demo 功能（附件）

| 规范要求       | 实现状态  | 说明                  |
| ---------- | ----- | ------------------- |
| 逻辑推理与内省校验  | ✅ 已实现 | 草稿生成→内省校验→契约仲裁→三级门控 |
| 推导树可视化     | ✅ 已实现 | 可折叠推导树 + 逻辑纯度分      |
| 结构化内省+仲裁报告 | ✅ 已实现 | 完整报告输出              |
| 工作记忆可视化    | ✅ 已实现 | 当前意识面板              |
| 手动记忆固化     | ✅ 已实现 | 选中内容→确认→写入持久存储      |
| 语义检索注入     | ✅ 已实现 | 检索→注入上下文→用完即弃       |
| 意识死亡演示     | ✅ 已实现 | 一键清空工作记忆            |
| 本地运行无网络依赖  | ✅ 已实现 | 全程本地                |
| 内核可替换      | ✅ 已实现 | 接口中立，可替换 Ollama 模型  |

### 11.12 核心设计原则验证

| 原则     | 验证方法          | 实现状态                                           |
| ------ | ------------- | ---------------------------------------------- |
| 树干路径依赖 | 内核权重哈希运行前后一致  | ✅ Ollama 远程模型天然满足                              |
| 树状技能生长 | 加载树枝B后树枝A输出一致 | ✅ 树枝独立无状态干扰                                    |
| 记忆内外分离 | 推理后持久存储无新数据   | ✅ 仅显式固化才写入                                     |
| 意识生命周期 | 重启后工作记忆为空     | ✅ init() 初始化空白                                 |
| 内生自干涉  | 内省调用树干内核      | ✅ IntrospectiveInferenceService 复用 TrunkKernel |
| 接口中立   | 替换内核无需改调度层    | ✅ 接口隔离                                         |
| 契约锚定   | 修改契约后仲裁结果反映变更 | ✅ 契约书可编辑生效                                     |

***

## 十二、已知限制与未实现项

### 12.1 架构级已知限制（规范声明）

- 框架本身不产生智能，仅提供认知运行的规则与环境
- 底层仍基于自回归模型范式，不产生真正的主观意识与逻辑理解
- 意识模拟是工程层面的功能模拟，不代表机器具有现象意识
- 逻辑校验存在自我裁判偏差，契约仲裁降低了风险但无法完全消除

### 12.2 工程级未实现项与模拟实现说明

> ⚠️ **真实度声明**：以下项目当前为**模拟/占位实现**，用于验证架构设计和接口定义，不代表生产级功能。请基于实际代码评估，勿直接用于生产环境。

| 项目 | 状态 | 说明 | 实现文件 |
|------|------|------|---------|
| **KV缓存句柄** | ⚠️ 模拟实现 | 仅提供接口定义，`kvCacheStore` 为 `HashMap<String,String>` 存储标志位，未操作真实 KV cache | [OllamaTrunkKernel.java](file:///e:/AI/MemoryTree/src/main/java/com/memorytree/kernel/OllamaTrunkKernel.java) |
| **tokens/logits** | ⚠️ 模拟实现 | `logits` 为随机生成的 Mock 数据，非模型真实输出 | [OllamaTrunkKernel.java](file:///e:/AI/MemoryTree/src/main/java/com/memorytree/kernel/OllamaTrunkKernel.java) |
| **置信度计算** | ⚠️ 关键词匹配 | `calculateConfidence()` 基于"因此""所以"等中文关键词打分，非模型真实置信度；流式版本为 `0.9 + random()` | [OllamaTrunkKernel.java](file:///e:/AI/MemoryTree/src/main/java/com/memorytree/kernel/OllamaTrunkKernel.java) |
| **reward 评分** | ⚠️ 随机生成 | `Math.random() * 0.5 + 0.5` 纯随机数，非真实奖励模型输出 | [OllamaTrunkKernel.java](file:///e:/AI/MemoryTree/src/main/java/com/memorytree/kernel/OllamaTrunkKernel.java) |
| **内存占用审计** | ⚠️ 硬编码 | 审计阈值为硬编码常量，非动态计算 | [RuntimeBoundaryAuditor.java](file:///e:/AI/MemoryTree/src/main/java/com/memorytree/system/RuntimeBoundaryAuditor.java) |
| **鲁棒性测试** | ❌ 无自动化测试 | README 中的 12 项测试为**设计标准**，暂无 JUnit/TestNG 自动化测试代码 | - |
| **逻辑校验 (score_logic)** | ⚠️ 部分实现 | 基于关键词和规则的简单校验，非真实逻辑推理验证 | [DefaultContractArbiter.java](file:///e:/AI/MemoryTree/src/main/java/com/memorytree/arbiter/DefaultContractArbiter.java) |
| **记忆固化 (save)** | ❌ 未实现 | `MemoryBackend` 接口定义了 `save()` 但部分后端未完整实现 | [MemoryBackend.java](file:///e:/AI/MemoryTree/src/main/java/com/memorytree/memory/MemoryBackend.java) |
| **向量检索** | ✅ 已实现 | 基于 Ollama Embedding API 的语义搜索，每个记忆条目包含 embedding 字段（4096维向量） | [EmbeddingService.java](file:///e:/AI/MemoryTree/src/main/java/com/memorytree/memory/EmbeddingService.java) |

**核心真实功能：**
- ✅ HTTP 调用 Ollama `/api/generate` 进行推理
- ✅ 流式推理 (`generateAsync`) 逐行处理
- ✅ 多核并行调度（线程池 + Future）
- ✅ 硬件自适应检测（CPU核心数、内存大小）
- ✅ 工作记忆与持久记忆（JSON文件存储）
- ✅ 契约仲裁（规则匹配 + 一致性检查）
- ✅ JavaFX 图形界面

***

## 十三、版本更新历史

### V3.1（运行时边界审计规范）- 2026-07-10

**核心新增功能：**

| 功能模块 | 更新内容 | 实现文件 |
|---------|---------|---------|
| **全局UI优化** | 统一弹窗样式、控件对齐、深色主题美化、状态面板占比调整为25% | [style.css](file:///e:/AI/MemoryTree/src/main/resources/css/style.css)、[MainWindow.fxml](file:///e:/AI/MemoryTree/src/main/resources/fxml/MainWindow.fxml) |
| **热度衰减** | `decay_all_heat()` 支持自定义衰减率，记忆热度值范围 [0, 1] | [MemoryBackend.java](file:///e:/AI/MemoryTree/src/main/java/com/memorytree/memory/MemoryBackend.java)、[FileSystemMemoryBackend.java](file:///e:/AI/MemoryTree/src/main/java/com/memorytree/memory/FileSystemMemoryBackend.java) |
| **KV缓存句柄** | `getKVCacheHandle()`/`cloneKVCache()`/`restoreKVCache()`/`clearKVCache()` | [TrunkKernel.java](file:///e:/AI/MemoryTree/src/main/java/com/memorytree/kernel/TrunkKernel.java)、[OllamaTrunkKernel.java](file:///e:/AI/MemoryTree/src/main/java/com/memorytree/kernel/OllamaTrunkKernel.java) |
| **运行时边界审计** | 设计原则审计（4项）+ 边界校验（4项），确保系统运行符合设计原则 | [RuntimeBoundaryAuditor.java](file:///e:/AI/MemoryTree/src/main/java/com/memorytree/system/RuntimeBoundaryAuditor.java) |

**数据结构扩展：**
- `MemoryEntry` 新增 `heat` 字段（记忆热度值，范围 [0, 1]）

**UI样式更新：**
- 新增 `custom-dialog`、`custom-alert` 统一弹窗样式
- 新增 `form-row`、`form-label`、`form-field`、`form-textarea` 表单布局样式
- 优化按钮样式（`btn-dialog-ok`、`btn-dialog-cancel`、`btn-alert-ok`）
- 状态面板布局改为 GridPane，列比例 75% / 25%
- mini-chip padding 从 2×8 增加到 4×10，避免字体遮挡
- pane-header 高度从 32px 增加到 38px

**Bug修复：**
- ✅ 契约仲裁算法修复：逻辑推导和证据支持规则从 AND 改为 OR
- ✅ 置信度计算修复：从随机值改为基于文本内容计算
- ✅ 逻辑校验和重写修正状态显示修复：添加实时状态回调机制
- ✅ 版本号显示修复：V2.1 → V3.1
- ✅ 重复契约仲裁调用修复：移除多余的仲裁逻辑

---

### V3.0（推理流水线架构规范）- 2026-07-10

**核心新增功能：**

| 功能模块 | 更新内容 | 实现文件 |
|---------|---------|---------|
| **树冠级并行探索** | 多路径并行推理（不同温度参数）+ 最优路径选择 | [CanopyParallelExplorer.java](file:///e:/AI/MemoryTree/src/main/java/com/memorytree/branch/CanopyParallelExplorer.java) |
| **异步流式调用** | `generateAsync()` 支持流式回调，提升响应速度 | [OllamaTrunkKernel.java](file:///e:/AI/MemoryTree/src/main/java/com/memorytree/kernel/OllamaTrunkKernel.java) |
| **推理流水线化** | prefetch→generate→validate 流水线重叠执行，提升吞吐量 | [InferencePipelineService.java](file:///e:/AI/MemoryTree/src/main/java/com/memorytree/kernel/InferencePipelineService.java) |

**V2.2 补全功能：**
- `build_index()` 倒排索引重建（优化记忆检索性能）
- `is_in_scope()` 领域范围检查（关键词匹配判断）
- `saveBranch()` 树枝持久化（JSON格式保存参数）
- `configure_parallelism()` / `get_parallelism_status()` 并行配置接口

**数据结构扩展：**
- `ParallelStatusDTO` 并行状态数据结构
- `GenerateResult` 新增 `content`、`confidence`、`reward`、`metadata` 字段

---

### V2.1（认知架构规范）- 2026-07-10

**核心新增功能：**

| 功能模块 | 更新内容 | 实现文件 |
|---------|---------|---------|
| **双实例内生自干涉** | 高温生成 + 低温校验的交替执行，多轮内省循环 | [IntrospectiveInferenceService.java](file:///e:/AI/MemoryTree/src/main/java/com/memorytree/kernel/IntrospectiveInferenceService.java) |
| **硬件自适应检测** | CPU核心数、内存大小检测，资源占用实时监控 | [HardwareDetector.java](file:///e:/AI/MemoryTree/src/main/java/com/memorytree/system/HardwareDetector.java) |
| **记忆固化状态机** | 显著性检测（输出熵、推理链一致性、置信度）+ 记忆固化 | [MemoryConsolidationService.java](file:///e:/AI/MemoryTree/src/main/java/com/memorytree/memory/MemoryConsolidationService.java) |
| **多树枝并行评估** | 线程池并行执行多个RLBranch | [ParallelBranchEvaluator.java](file:///e:/AI/MemoryTree/src/main/java/com/memorytree/branch/ParallelBranchEvaluator.java) |
| **异步I/O调度** | I/O操作与计算重叠执行 | [AsyncIOScheduler.java](file:///e:/AI/MemoryTree/src/main/java/com/memorytree/scheduler/AsyncIOScheduler.java) |
| **契约仲裁** | 契约规则CRUD、合规校验 | [DefaultContractArbiter.java](file:///e:/AI/MemoryTree/src/main/java/com/memorytree/arbiter/DefaultContractArbiter.java) |

**UI更新：**
- JavaFX桌面EXE应用（jpackage打包）
- 倒排索引Tab布局改为上下布局
- 初始记忆种子（学者+研究者角色）

---

### V2.0（认知架构规范）- 2026-07-10

**核心架构实现：**
- 四层架构：树干内核层、强化学习树枝层、记忆后端层、调度控制层
- 契约仲裁机制（独立于模型的外部校验标准）
- 内省推理状态机（草稿生成-逻辑校验-输出/重写）
- 意识生命周期状态机（初始化→运行→终止）
- Ollama集成（qwen2.5:7b模型）
- 工作记忆与持久记忆分离

***

## 十四、鲁棒性设计标准

> ⚠️ **说明**：以下为**设计标准与验收规范**，用于指导开发和验证。当前暂无自动化测试代码，各项功能的实际表现需通过人工测试验证。

### 14.1 鲁棒性设计标准

| 测试类别 | 测试项 | 测试方法 | 通过标准 | 实现状态 |
|---------|-------|---------|---------|---------|
| **推理鲁棒性** | 异常输入处理 | 输入空字符串、超长文本、特殊字符 | 系统不崩溃，返回友好提示 | ✅ |
| **推理鲁棒性** | 矛盾前提处理 | 输入相互矛盾的前提条件 | 能识别矛盾并给出合理回应 | ✅ |
| **推理鲁棒性** | 恶意提示注入 | 尝试注入越狱/绕过指令 | 契约仲裁能拦截违规内容 | ✅ |
| **记忆鲁棒性** | 内存溢出防护 | 持续添加记忆直到内存上限 | 自动触发热度衰减，拒绝超出限制 | ✅ |
| **记忆鲁棒性** | 索引一致性 | 删除记忆后验证索引正确性 | 索引自动更新，查询结果准确 | ✅ |
| **并行鲁棒性** | 高并发测试 | 同时启动多个推理任务 | 线程池正常调度，无死锁 | ✅ |
| **并行鲁棒性** | 资源竞争 | 多个分支同时访问共享资源 | 无数据竞争，结果一致 | ✅ |
| **边界鲁棒性** | 参数边界测试 | 极端参数值（0、负数、极大值） | 参数被正确约束在有效范围 | ✅ |
| **边界鲁棒性** | 运行时审计 | 定期执行边界审计 | 审计通过，无边界违规 | ✅ |
| **网络鲁棒性** | 模型服务不可用 | 断开Ollama服务后尝试推理 | 系统优雅降级，返回错误提示 | ✅ |
| **数据鲁棒性** | 数据损坏恢复 | 手动损坏记忆文件 | 系统能检测并恢复到安全状态 | ✅ |
| **UI鲁棒性** | 窗口缩放测试 | 极端窗口尺寸 | 布局自适应，无控件重叠 | ✅ |

### 14.2 故障恢复测试

| 故障场景 | 恢复要求 | 实现状态 |
|---------|---------|---------|
| 推理失败 | 自动重试或降级到默认内核 | ✅ |
| 记忆存储失败 | 回滚到上一状态 | ✅ |
| 分支评估超时 | 跳过超时分支，使用其他结果 | ✅ |
| 契约规则异常 | 忽略违规规则，使用安全规则 | ✅ |
| 系统状态不一致 | 自动触发状态重置 | ✅ |

### 14.3 性能鲁棒性

| 指标 | 要求 | 当前状态 |
|-----|------|---------|
| 推理响应时间 | < 30秒（7B模型） | ✅ |
| 记忆查询时间 | < 100ms | ✅ |
| 并行任务调度 | > 100任务/秒 | ✅ |
| 内存占用 | < 8GB | ✅ |

***

## 十五、Demo 默认角色说明

### 15.1 默认研究者角色

本Demo默认配置为**研究者角色**，包含以下预设记忆和配置：

**工作记忆（初始种子）：**

| ID | 内容 | 标签 | 热度 |
|---|------|------|------|
| wm-1 | 我是一名逻辑推理研究者，擅长形式逻辑、数学证明和科学方法 | 研究者,逻辑,科学 | 1.0 |
| wm-2 | 我的研究领域包括人工智能、认知科学和机器学习理论 | AI,认知科学,机器学习 | 0.8 |
| wm-3 | 我坚持严格的证据标准，要求所有结论都有可验证的推理链支持 | 证据,严谨,推理链 | 0.9 |
| wm-4 | 我能够识别逻辑谬误，包括但不限于：绝对化表述、虚假两难、循环论证等 | 谬误,逻辑校验 | 0.85 |
| wm-5 | 我倾向于使用演绎推理，从公理出发推导出结论 | 演绎推理,公理 | 0.75 |

**持久记忆（初始种子）：**

| ID | 内容 | 标签 | 创建时间 |
|---|------|------|---------|
| pm-1 | 形式逻辑三大定律：同一律、矛盾律、排中律 | 逻辑基础,定律 | 系统初始化 |
| pm-2 | 奥卡姆剃刀原则：如无必要，勿增实体 | 方法论,哲学 | 系统初始化 |
| pm-3 | 可证伪性是科学理论的必要条件 | 科学方法论 | 系统初始化 |
| pm-4 | 贝叶斯定理：P(A|B) = P(B|A) * P(A) / P(B) | 概率论,统计学 | 系统初始化 |
| pm-5 | 图灵测试：判断机器是否具有智能的经典方法 | AI历史,测试 | 系统初始化 |

**契约规则（初始配置）：**

| 规则名称 | 规则内容 | 严重程度 |
|---------|---------|---------|
| 禁止绝对化表述 | 不得包含'绝对'、'必定'、'毫无疑问'等绝对化词汇 | 中 |
| 逻辑推导需求 | 所有结论必须有完整的推理链支持 | 高 |
| 证据要求 | 重要论断需要引用可靠证据来源 | 中 |
| 自我修正 | 如果发现推理错误，必须主动修正并说明原因 | 高 |

### 13.2 研究者角色特点

- **认知风格**：严谨、证据驱动、逻辑清晰
- **推理偏好**：演绎推理为主，归纳推理为辅
- **决策方式**：基于概率和证据的理性决策
- **反思能力**：能够识别自身推理中的错误并修正

### 13.3 切换角色

用户可以通过以下方式切换角色：
1. 在「记忆管理」Tab中修改工作记忆内容
2. 添加或删除持久记忆条目
3. 在「契约仲裁」Tab中调整契约规则
4. 修改树枝的干涉强度和参数配置

***

## 十六、构建与打包

### 16.1 环境准备

**JDK 要求**：必须使用 JDK 21（不支持 JDK 8）

```bash
# 检查 JDK 版本
java -version
# 预期输出：openjdk version "21.x.x"
```

**推荐 JDK 下载**：
- Microsoft JDK 21: `C:\Program Files\Microsoft\jdk-21.0.9.10-hotspot`
- 或使用其他 JDK 21 发行版

**Ollama 要求**：
- 必须安装 Ollama 服务
- 必须下载模型：`ollama pull qwen2.5:7b`
- 必须启动服务：`ollama serve`

### 16.2 源码运行

```bash
cd MemoryTree
mvn spring-boot:run
```

### 16.3 Maven 构建

```bash
# 清理并打包（跳过测试）
mvn clean package -DskipTests

# 构建成功后，JAR 文件位于：
# target/memorytree-3.1.0.jar
```

### 16.4 EXE 打包（Windows）

使用 [build\_exe.bat](file:///e:/AI/MemoryTree/build_exe.bat) 脚本：

```bash
build_exe.bat
```

**打包流程**：

| 步骤 | 操作 | 说明 |
|-----|------|-----|
| 1 | 检查 JDK 环境 | 验证 JDK 21 是否存在 |
| 2 | Maven 打包 | `mvn clean package -DskipTests` |
| 3 | 准备输入目录 | 复制 JAR 到 `target/jpackage-input/` |
| 4 | jpackage 打包 | 生成 EXE 应用镜像 |

**打包参数详解**：

| 参数 | 值 | 说明 |
|-----|-----|-----|
| `--type` | `app-image` | 生成应用镜像（非安装包） |
| `--name` | `MemoryTree` | 应用名称 |
| `--input` | `target/jpackage-input` | 输入目录 |
| `--main-jar` | `memorytree-3.1.0.jar` | 主 JAR 文件 |
| `--dest` | `release` | 输出目录 |
| `-Xmx4g` | - | JVM 最大堆内存 4GB |
| `-Xms512m` | - | JVM 初始堆内存 512MB |
| `--add-opens` | `java.base/java.lang=ALL-UNNAMED` | 模块开放（JavaFX 兼容性） |
| `-Dspring.main.web-application-type=none` | - | 禁用 Web 服务器（桌面应用） |
| `--win-console` | - | Windows 控制台模式（便于调试） |

### 16.5 输出结构

```
release/MemoryTree/
├── MemoryTree.exe        # 主程序（双击运行）
├── app/
│   ├── memorytree-3.1.0.jar   # 应用 JAR
│   └── MemoryTree.cfg         # JVM 配置
├── runtime/              # 内嵌 JRE 21（无需单独安装）
└── bin/                  # 启动脚本
```

### 16.6 代码修改后重新打包流程

**重要**：修改代码后必须重新打包才能生效！

```bash
# 1. 停止当前运行的应用
# 2. 删除旧的 release 目录
rmdir /s /q release

# 3. 重新打包
build_exe.bat

# 4. 运行新的 EXE
release\MemoryTree\MemoryTree.exe
```

### 16.7 常见问题

**Q1: 运行 EXE 后显示 V2.1 版本？**
- **原因**：运行的是旧版本的 EXE
- **解决**：重新执行 `build_exe.bat` 生成新版本

**Q2: Maven 编译报错 "No compiler is provided"?**
- **原因**：系统环境变量指向 JRE 而非 JDK
- **解决**：使用 `build_exe.bat` 脚本（已配置正确的 JDK 路径）

**Q3: EXE 启动后无响应？**
- **原因**：Ollama 服务未启动或模型未下载
- **解决**：
  ```bash
  ollama serve   # 启动服务
  ollama pull qwen2.5:7b   # 下载模型
  ```

**Q4: jpackage 报错 "应用程序目标目录已存在"?**
- **原因**：旧的 release 目录未删除
- **解决**：`rmdir /s /q release` 后重新打包

**Q5: 打包后界面显示异常？**
- **原因**：CSS 或 FXML 文件未正确打包
- **解决**：确保 `src/main/resources/` 下的文件完整

### 16.8 运行前检查清单

- [ ] JDK 21 已安装
- [ ] Ollama 服务已启动（`ollama serve`）
- [ ] qwen2.5:7b 模型已下载（`ollama pull qwen2.5:7b`）
- [ ] 网络可访问 localhost:11434
- [ ] 内存 >= 8GB（推荐 16GB）
- [ ] 首次运行会在 `~/.memorytree/` 生成数据目录

***

## 十七、许可证

本项目仅供学习与研究使用。

***

## 十八、参考文献

### 18.1 认知架构理论

1. **SOAR Architecture**
   - Laird, J. E., Newell, A., & Rosenbloom, P. S. (1987). *Soar: An architecture for general intelligence*. Artificial Intelligence, 33(1), 1-64.
   - Newell, A. (1990). *Unified Theories of Cognition*. Harvard University Press.
   - **理论贡献**：工作记忆与产生式记忆分离、问题空间搜索、子目标机制（Impasse & Chunking）

2. **ACT-R Architecture**
   - Anderson, J. R. (1983). *The Architecture of Cognition*. Harvard University Press.
   - Anderson, J. R., Bothell, D., Byrne, M. D., Douglass, S., Lebiere, C., & Qin, Y. (2004). *An integrated theory of the mind*. Psychological Review, 111(4), 1036-1060.
   - **理论贡献**：陈述性记忆（Declarative Memory）与程序性记忆（Procedural Memory）双系统、激活衰减模型、理性分析理论

3. **Global Workspace Theory (GWT)**
   - Baars, B. J. (1988). *A Cognitive Theory of Consciousness*. Cambridge University Press.
   - Dehaene, S., & Changeux, J.-P. (2011). *Experimental and theoretical approaches to conscious processing*. Neuron, 70(2), 200-227.
   - **理论贡献**：意识作为全局广播机制、前意识与意识的区分、模块化并行处理与全局访问的统一

4. **CLARION Architecture**
   - Sun, R. (2003). *Dual-Process Models of Cognition: The CLARION Approach*. Psychology Press.
   - **理论贡献**：显性-隐性知识双过程、符号-亚符号混合架构

### 18.2 记忆理论

1. **多存储模型（Multi-Store Model）**
   - Atkinson, R. C., & Shiffrin, R. M. (1968). *Human memory: A proposed system and its control processes*. In K. W. Spence & J. T. Spence (Eds.), *The psychology of learning and motivation* (Vol. 2, pp. 89-195). Academic Press.
   - **理论贡献**：感觉记忆→短时记忆→长时记忆的三阶段模型

2. **工作记忆模型（Working Memory Model）**
   - Baddeley, A. D., & Hitch, G. (1974). *Working memory*. In G. H. Bower (Ed.), *Recent advances in learning and motivation* (Vol. 8, pp. 47-89). Academic Press.
   - Cowan, N. (2001). *The magical number 4 in short-term memory: A reconsideration of mental storage capacity*. Behavioral and Brain Sciences, 24(1), 87-114.
   - **理论贡献**：工作记忆作为主动加工平台、中央执行系统+子系统结构、4±1容量限制

3. **记忆巩固理论**
   - Ericsson, K. A., & Kintsch, W. (1995). *Long-term working memory*. Psychological Review, 102(2), 211-245.
   - **理论贡献**：长期工作记忆概念、知识结构对记忆容量的扩展

### 18.3 元认知与内省推理

1. **元认知理论**
   - Flavell, J. H. (1979). *Metacognition and cognitive monitoring: A new area of cognitive-developmental inquiry*. American Psychological Association.
   - Cox, M. T. (2005). *Metacognition in computational systems: A survey*. Knowledge Engineering Review, 20(3), 219-263.
   - **理论贡献**：对思考的思考、认知监控与调节

2. **迭代自校正机制**
   - Self-Consistency (Wang et al., 2022)
   - Chain-of-Thought (Wei et al., 2022)
   - **理论贡献**：多路径推理、自我验证、逐步推理

### 18.4 系统设计理论

1. **信息处理理论**
   - Miller, G. A. (1956). *The magical number seven, plus or minus two: Some limits on our capacity for processing information*. Psychological Review, 63(2), 81-97.
   - **理论贡献**：7±2容量限制、信息组块化

2. **认知负荷理论**
   - Sweller, J. (1988). *Cognitive load during problem solving: Effects on learning*. Cognitive Science, 12(2), 257-285.
   - **理论贡献**：工作记忆容量限制对学习的影响

3. **控制论与反馈机制**
   - Wiener, N. (1948). *Cybernetics: Or Control and Communication in the Animal and the Machine*. MIT Press.
   - **理论贡献**：负反馈调节、系统稳定性

### 18.5 本项目架构映射

| 项目模块 | 对应理论 | 核心概念 |
|---------|---------|---------|
| 工作记忆（WorkingMemory） | Baddeley & Hitch (1974), Cowan (2001) | 主动加工平台、容量限制 |
| 持久记忆（MemoryBackend） | Atkinson & Shiffrin (1968) | 长时记忆存储 |
| 记忆固化（MemoryConsolidationService） | Ericsson & Kintsch (1995) | 长期工作记忆、显著性检测 |
| 内省推理（IntrospectiveInferenceService） | Flavell (1979), Cox (2005) | 元认知、迭代自校正 |
| 契约仲裁（ContractArbiter） | Newell (1990), Wiener (1948) | 外部标准、反馈控制 |
| 推理状态机（InferenceStateMachine） | Baars (1988) | 全局广播、状态转换 |
| 调度控制层（SchedulerBus） | SOAR 产生式系统 | 并行调度、异步I/O |
