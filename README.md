# 记忆树 MemoryTree

> 纯逻辑类脑AI框架 — V2.1 认知架构规范参考实现
>
> 版本：V2.1 | 状态：正式发布 | 基于《记忆树 MemoryTree V2.1 认知架构规范》

---

## 一、项目简介

记忆树（MemoryTree）是一套**模型无关的纯逻辑类脑认知架构接口规范与参考调度实现**。

它不生产 Token、不绑定具体模型、不依赖特定算法，只定义类脑认知系统的分层契约、运行范式与边界约束。树干内核、技能树枝、记忆后端全栈可替换，框架本身是认知运行的「规则总线」与「环境容器」。

### 核心哲学命题

1. **认知结构先于认知内容** — 树干路径依赖原则
2. **意识与知识是两种不同的存在形态** — 记忆内外分离原则
3. **逻辑自洽是可计算的外部标准** — 契约锚定原则

---

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

| 层级 | 职责 | 核心模块 |
|------|------|----------|
| 树干内核层 | 基础推理、logits输出、KV缓存 | [OllamaTrunkKernel](file:///e:/AI/MemoryTree/src/main/java/com/memorytree/kernel/OllamaTrunkKernel.java)、[IntrospectiveInferenceService](file:///e:/AI/MemoryTree/src/main/java/com/memorytree/kernel/IntrospectiveInferenceService.java) |
| 强化学习树枝层 | 技能树、动作仲裁、观察器 | [RLBranch](file:///e:/AI/MemoryTree/src/main/java/com/memorytree/branch/RLBranch.java)、[ParallelBranchEvaluator](file:///e:/AI/MemoryTree/src/main/java/com/memorytree/branch/ParallelBranchEvaluator.java)、[DefaultLogicCheckBranch](file:///e:/AI/MemoryTree/src/main/java/com/memorytree/branch/DefaultLogicCheckBranch.java)、[FallacyDetectionBranch](file:///e:/AI/MemoryTree/src/main/java/com/memorytree/branch/FallacyDetectionBranch.java) |
| 记忆后端层 | 工作记忆、持久记忆、记忆固化 | [WorkingMemory](file:///e:/AI/MemoryTree/src/main/java/com/memorytree/memory/WorkingMemory.java)、[FileSystemMemoryBackend](file:///e:/AI/MemoryTree/src/main/java/com/memorytree/memory/FileSystemMemoryBackend.java)、[MemoryConsolidationService](file:///e:/AI/MemoryTree/src/main/java/com/memorytree/memory/MemoryConsolidationService.java) |
| 调度控制层 | 生命周期、并行调度、异步I/O | [LifecycleStateMachine](file:///e:/AI/MemoryTree/src/main/java/com/memorytree/scheduler/LifecycleStateMachine.java)、[InferenceStateMachine](file:///e:/AI/MemoryTree/src/main/java/com/memorytree/scheduler/InferenceStateMachine.java)、[DefaultSchedulerBus](file:///e:/AI/MemoryTree/src/main/java/com/memorytree/scheduler/DefaultSchedulerBus.java)、[AsyncIOScheduler](file:///e:/AI/MemoryTree/src/main/java/com/memorytree/scheduler/AsyncIOScheduler.java) |
| 契约仲裁 | 契约规则CRUD、合规校验 | [ContractArbiter](file:///e:/AI/MemoryTree/src/main/java/com/memorytree/arbiter/ContractArbiter.java)、[DefaultContractArbiter](file:///e:/AI/MemoryTree/src/main/java/com/memorytree/arbiter/DefaultContractArbiter.java) |
| 系统服务 | 硬件检测、门控事件 | [HardwareDetector](file:///e:/AI/MemoryTree/src/main/java/com/memorytree/system/HardwareDetector.java)、[GatingEvent](file:///e:/AI/MemoryTree/src/main/java/com/memorytree/system/GatingEvent.java) |

### 2.2 核心机制

- **双实例内生自干涉**：同一份基座权重启动双推理实例，分别负责生成与校验，实现内生的自我约束与逻辑反思
- **契约仲裁机制**：为推理输出提供独立于模型的外部校验标准
- **内省推理状态机**：实现「草稿生成-逻辑校验-契约仲裁-输出/重写」的完整推理流程
- **意识生命周期状态机**：管理系统从初始化到终止的状态流转，确保动态状态可彻底清零
- **记忆固化状态机**：显著性检测条件触发记忆固化（输出熵低于阈值、推理链跨层一致性高、操作者显式标记）
- **多核并行调度**：树干串行，调度并行（异步I/O与计算重叠、多树枝并行评估、树冠级并行探索、推理流水线化）
- **硬件自适应机制**：自动检测本地硬件资源，匹配对应规格的内核

### 2.3 七条核心设计原则

| 原则 | 说明 | 实现状态 |
|------|------|----------|
| 一、树干路径依赖 | 内核权重全程只读，运行时不可修改 | ✅ 已实现（Ollama 远程模型天然只读） |
| 二、树状技能生长 | 树枝独立、无参数交叉、无灾难性遗忘 | ✅ 已实现（RLBranch 接口 + 独立树枝实例） |
| 三、记忆内外分离 | 工作记忆与持久记忆严格分层 | ✅ 已实现（WorkingMemory / FileSystemMemoryBackend 分离） |
| 四、意识生命周期 | 所有运行时状态支持彻底清零 | ✅ 已实现（LifecycleStateMachine.terminate()） |
| 五、内生自干涉 | 逻辑校验通过树干内核自身完成 | ✅ 已实现（IntrospectiveInferenceService 复用 TrunkKernel） |
| 六、接口中立 | 层间仅通过标准接口交互 | ✅ 已实现（TrunkKernel / RLBranch / MemoryBackend 接口） |
| 七、契约锚定 | 输出标准由操作者定义的契约书外部锚定 | ✅ 已实现（ContractArbiter + 可编辑 JSON 契约书） |

---

## 三、功能特性

### 3.1 逻辑推理

- ✅ 内省推理（多轮草稿生成-校验-重写）
- ✅ 可调温度系数与最大内省轮次
- ✅ 前提条件注入
- ✅ 推导树可视化
- ✅ 逻辑纯度分标注
- ✅ 三级门控事件记录（Token级修正、逻辑阈值触发、段落重写）

### 3.2 记忆管理

- ✅ **工作记忆**（意识态）：实时推理上下文，角色设定与推理原则，20MB 容量限制
- ✅ **持久记忆**（遗产态）：知识库存储，支持关键词搜索
- ✅ **记忆固化**：自动显著性检测 + 手动固化
- ✅ **记忆注入**：持久记忆注入工作记忆参与推理
- ✅ **分页浏览**：大容量记忆分页展示
- ✅ **详情查看与编辑**：完整记忆属性可查阅可修改
- ✅ **初始记忆种子**：默认学者与研究者角色记忆

### 3.3 树枝管理

- ✅ RL 树枝激活/停用控制
- ✅ 多树枝并行观察与评估
- ✅ 动作仲裁机制（加权融合策略）
- ✅ 默认树枝：逻辑校验枝、谬误检测枝

### 3.4 契约仲裁

- ✅ 契约规则管理（增删改查）
- ✅ 规则启用/禁用
- ✅ 推理结果合规性校验
- ✅ 违规严重程度分级
- ✅ Fail-Safe 硬边界
- ✅ 可被操作者直接编辑的 JSON 契约书

### 3.5 系统监控

- ✅ 硬件信息实时显示（CPU、内存、占用率、JVM 线程数）
- ✅ 推理状态机可视化
- ✅ 推理统计（内省轮次、合规分数、耗时、置信度）
- ✅ 三级门控事件记录

---

## 四、技术栈

| 组件 | 选型 | 规格 |
|------|------|------|
| 开发语言 | Java | 21 |
| 应用框架 | Spring Boot | 3.2.5 |
| UI 框架 | JavaFX + FXML | 21 |
| AI 引擎 | Ollama | qwen2.5:7b |
| 数据存储 | SQLite + 文件系统 | JSON 持久化 |
| 打包工具 | Maven + jpackage | 原生 EXE |
| 其他 | Lombok、Jackson、HttpClient | - |

---

## 五、快速开始

### 5.1 环境要求

| 配置 | 最低 | 推荐 |
|------|------|------|
| JDK | 21+ | 21+ |
| CPU | 4核 | 8核+ |
| 内存 | 8GB | 16GB+ |
| 存储 | 10GB | 20GB SSD |
| GPU | 不需要 | 不需要 |
| 网络 | 不需要 | 不需要 |

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

---

## 六、配置说明

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

---

## 七、数据存储

应用数据存储在用户目录下的 `.memorytree/` 文件夹中：

```
~/.memorytree/
├── data/
│   ├── memory/
│   │   └── memory_store.json    # 持久记忆数据
│   └── contract/
│       └── default_contract.json  # 契约规则数据
```

---

## 八、目录结构

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

---

## 九、V2.1 规范实现对比

### 9.1 树干内核层（Trunk Kernel）

| 规范要求 | 实现状态 | 说明 |
|----------|----------|------|
| `load(path)` 接口 | ✅ 已实现 | `loadKernel(modelPath)` |
| `free()` 接口 | ✅ 已实现 | `unloadKernel()` |
| `generate(prompt, config)` 接口 | ✅ 已实现 | 完整实现，支持温度/TopP/最大长度/种子/KV缓存 |
| `get_kv_cache()` 接口 | ⚠️ 部分实现 | 通过 `useKVCache` 标志位模拟，未返回真实句柄 |
| `score_logic()` 可选接口 | ⚠️ 部分实现 | 通过 `IntrospectiveInferenceService` 模拟计算 |
| GenerateConfig 数据结构 | ✅ 已实现 | temperature/topP/maxTokens/seed/useKVCache |
| GenerateResult 数据结构 | ✅ 已实现 | tokens/logits/inferenceTimeMs/confidenceScore |
| 权重只读约束 | ✅ 已实现 | Ollama 远程模型天然只读 |
| 无状态约束 | ✅ 已实现 | 每次 generate 独立调用 |

### 9.2 强化学习树枝层（RL Branch）

| 规范要求 | 实现状态 | 说明 |
|----------|----------|------|
| `observe(obs)` 接口 | ✅ 已实现 | 纯函数，无副作用 |
| `reset()` 接口 | ⚠️ 部分实现 | 通过 `setActive(false)` 间接实现 |
| `save(path)` / `load(path)` 接口 | ⚠️ 部分实现 | `loadBranch(filePath)` 已实现，`save` 未实现 |
| ObservationSpace 数据结构 | ✅ 已实现 | token_prob_variance/logic_score/context_length/introspection_depth |
| ActionSpace 数据结构 | ✅ 已实现 | temperature_delta/logic_penalty_lambda/rewrite_threshold/max_introspection_rounds |
| 树枝独立性 | ✅ 已实现 | 单树枝无全局状态依赖 |
| 可插拔设计 | ✅ 已实现 | 独立文件存储，按需加载 |
| 多树枝并行评估 | ✅ 已实现 | `ParallelBranchEvaluator` 使用线程池并行调用 observe() |
| 动作仲裁 | ✅ 已实现 | 加权融合策略（confidence × reward） |

### 9.3 记忆后端层（Memory Backend）

| 规范要求 | 实现状态 | 说明 |
|----------|----------|------|
| `search(query, top_k)` 接口 | ✅ 已实现 | `query(MemoryQuery)` 支持关键词+标签检索 |
| `store(fragment, metadata)` 接口 | ✅ 已实现 | `store(MemoryEntry)` |
| `delete(fragment_id)` 接口 | ✅ 已实现 | `delete(String id)` |
| `build_index()` 接口 | ❌ 未实现 | 当前使用线性扫描，未建索引 |
| MemoryFragment 数据结构 | ✅ 已实现 | id/content/tags/createdAt/lastAccessedAt/accessCount/saliencyScore |
| `set_heat()` 可选接口 | ✅ 已实现 | 通过 `saliencyScore` 字段 |
| `decay_all_heat()` 可选接口 | ❌ 未实现 | 热度衰减未实现 |
| 实现无关性 | ✅ 已实现 | 接口与实现分离 |

### 9.4 调度控制层（Scheduler Bus）

| 规范要求 | 实现状态 | 说明 |
|----------|----------|------|
| 内核生命周期管理 | ✅ 已实现 | `LifecycleStateMachine` |
| 树枝管理 | ✅ 已实现 | 加载/激活/卸载 |
| 工作记忆维护 | ✅ 已实现 | 滑动窗口 + 容量控制（20MB） |
| 持久记忆调度 | ✅ 已实现 | 检索/注入/固化 |
| 内省推理控制 | ✅ 已实现 | `InferenceStateMachine` 完整状态流转 |
| 契约仲裁控制 | ✅ 已实现 | 契约加载/逐条校验/违规处置 |
| 生命周期管理 | ✅ 已实现 | 状态隔离/终止清零 |
| 边界约束校验 | ⚠️ 部分实现 | 运行时审计未完全实现 |

### 9.5 内省推理状态机

| 规范状态 | 实现状态 | 说明 |
|----------|----------|------|
| IDLE | ✅ 已实现 | 空闲等待 |
| DRAFT_GENERATE | ✅ 已实现 | 系统1直觉推理（temperature=0.7） |
| LOGIC_VALIDATE | ✅ 已实现 | 系统2逻辑校验（temperature=0.1，复用内核） |
| CONTRACT_ARBITRATE | ✅ 已实现 | 契约合规检查 |
| REWRITE | ✅ 已实现 | 违规且未达最大轮次时重写 |
| OUTPUT | ✅ 已实现 | 合规时正常输出 |
| OUTPUT_WITH_LOW_CONFIDENCE | ✅ 已实现 | 违规且达最大轮次时低置信度输出 |

### 9.6 意识生命周期状态机

| 规范状态 | 实现状态 | 说明 |
|----------|----------|------|
| UNINITIALIZED | ✅ 已实现 | 未初始化 |
| INITIALIZING | ✅ 已实现 | 加载内核/树枝/空白工作记忆 |
| RUNNING | ✅ 已实现 | 正常运行 |
| TERMINATING | ✅ 已实现 | 清空工作记忆/重置树枝/卸载内核 |
| DESTROYED | ✅ 已实现 | 所有动态状态彻底销毁 |
| 终止流程不可逆 | ✅ 已实现 | - |
| 不加载历史对话 | ✅ 已实现 | 每次从零开始 |
| 默认不持久化 | ✅ 已实现 | 仅主动触发时写入 |

### 9.7 记忆固化状态机

| 规范状态 | 实现状态 | 说明 |
|----------|----------|------|
| RUNNING | ✅ 已实现 | - |
| SALIENCY_DETECT | ✅ 已实现 | 输出熵 + 推理链一致性 + 置信度 |
| CANDIDATE_PENDING | ✅ 已实现 | 候选暂存工作记忆 |
| CONFIRMED_STORE | ✅ 已实现 | 操作者确认后写入持久存储 |
| DISCARD | ✅ 已实现 | 未确认候选随生命周期销毁 |
| 显著性检测条件 | ✅ 已实现 | 输出熵<0.3、一致性>0.85、置信度>0.8 |

### 9.8 契约仲裁机制

| 规范要求 | 实现状态 | 说明 |
|----------|----------|------|
| `load_contract(path)` 接口 | ✅ 已实现 | `loadContract(filePath)` |
| `validate_draft(draft, premises)` 接口 | ✅ 已实现 | `validateDraft(draft)` |
| `validate_final(final, history)` 接口 | ✅ 已实现 | `validateFinal(finalResult)` |
| `is_in_scope(query)` 接口 | ❌ 未实现 | 领域范围检查未实现 |
| ArbitrationReport 数据结构 | ✅ 已实现 | result/violatingClauseId/matchedRules/complianceScore |
| 契约书外部性 | ✅ 已实现 | 独立 JSON 文件 |
| 契约书可修改性 | ✅ 已实现 | 操作者可随时编辑，下次推理生效 |
| 契约书可追溯性 | ✅ 已实现 | 违规条款精确引用 |
| Fail-Safe 硬边界 | ✅ 已实现 | max_introspection_loops/max_output_length/max_reasoning_time |
| 契约书 CRUD | ✅ 已实现 | addClause/updateClause/removeClause/toggleClauseEnabled |

### 9.9 多核并行调度规范

| 规范要求 | 实现状态 | 说明 |
|----------|----------|------|
| **第一级：异步I/O与计算重叠** | ✅ 已实现 | `AsyncIOScheduler` 记忆预检索/日志异步写入 |
| **第二级：多树枝并行评估** | ✅ 已实现 | `ParallelBranchEvaluator` 线程池并行 observe() |
| **第三级：树冠级并行探索** | ❌ 未实现 | 多路径并行探索未实现 |
| **第四级：推理流水线化** | ❌ 未实现 | 远期探索方向 |
| `configure_parallelism(config)` 接口 | ❌ 未实现 | 并行策略配置接口未实现 |
| `get_parallelism_status()` 接口 | ❌ 未实现 | 并行状态查询接口未实现 |
| 树干串行约束 | ✅ 已实现 | generate() 单线程串行 |
| 写操作串行化 | ✅ 已实现 | MemoryBackend store/delete 同步 |
| 线程安全声明 | ✅ 已实现 | observe() 线程安全，generate() 串行 |
| Fail-Safe 优先于并行 | ✅ 已实现 | Fail-Safe 触发立即终止 |

### 9.10 硬件自适应机制

| 规范要求 | 实现状态 | 说明 |
|----------|----------|------|
| CPU 核心数检测 | ✅ 已实现 | `Runtime.availableProcessors()` |
| 可用内存检测 | ✅ 已实现 | `OperatingSystemMXBean` |
| 自动推荐内核规格 | ✅ 已实现 | 16GB+→7B, 8GB+→3B, 4GB+→1B |
| 自动配置并行线程数 | ✅ 已实现 | cores - 2 |
| 操作者可手动覆盖 | ✅ 已实现 | 配置文件可覆盖 |
| JVM 内存占用监控 | ✅ 已实现 | totalMemory/freeMemory/maxMemory |
| CPU 占用率监控 | ✅ 已实现 | `getProcessCpuLoad()` |
| 系统内存占用率 | ✅ 已实现 | 物理内存使用百分比 |
| JVM 线程数 | ✅ 已实现 | `ThreadMXBean` |

### 9.11 学术版 Demo 功能（附件）

| 规范要求 | 实现状态 | 说明 |
|----------|----------|------|
| 逻辑推理与内省校验 | ✅ 已实现 | 草稿生成→内省校验→契约仲裁→三级门控 |
| 推导树可视化 | ✅ 已实现 | 可折叠推导树 + 逻辑纯度分 |
| 结构化内省+仲裁报告 | ✅ 已实现 | 完整报告输出 |
| 工作记忆可视化 | ✅ 已实现 | 当前意识面板 |
| 手动记忆固化 | ✅ 已实现 | 选中内容→确认→写入持久存储 |
| 语义检索注入 | ✅ 已实现 | 检索→注入上下文→用完即弃 |
| 意识死亡演示 | ✅ 已实现 | 一键清空工作记忆 |
| 本地运行无网络依赖 | ✅ 已实现 | 全程本地 |
| 内核可替换 | ✅ 已实现 | 接口中立，可替换 Ollama 模型 |

### 9.12 核心设计原则验证

| 原则 | 验证方法 | 实现状态 |
|------|----------|----------|
| 树干路径依赖 | 内核权重哈希运行前后一致 | ✅ Ollama 远程模型天然满足 |
| 树状技能生长 | 加载树枝B后树枝A输出一致 | ✅ 树枝独立无状态干扰 |
| 记忆内外分离 | 推理后持久存储无新数据 | ✅ 仅显式固化才写入 |
| 意识生命周期 | 重启后工作记忆为空 | ✅ init() 初始化空白 |
| 内生自干涉 | 内省调用树干内核 | ✅ IntrospectiveInferenceService 复用 TrunkKernel |
| 接口中立 | 替换内核无需改调度层 | ✅ 接口隔离 |
| 契约锚定 | 修改契约后仲裁结果反映变更 | ✅ 契约书可编辑生效 |

---

## 十、已知限制与未实现项

### 10.1 架构级已知限制（规范声明）

- 框架本身不产生智能，仅提供认知运行的规则与环境
- 底层仍基于自回归模型范式，不产生真正的主观意识与逻辑理解
- 意识模拟是工程层面的功能模拟，不代表机器具有现象意识
- 逻辑校验存在自我裁判偏差，契约仲裁降低了风险但无法完全消除

### 10.2 工程级未实现项

| 未实现项 | 规范章节 | 影响 | 计划 |
|----------|----------|------|------|
| `build_index()` 记忆索引重建 | 3.3.1 | 大容量记忆检索效率 | V2.2 |
| `decay_all_heat()` 热度衰减 | 3.3.3 | 认知热度冷启动 | V2.2 |
| `is_in_scope()` 领域范围检查 | 5.4 | 超范围查询未拒绝 | V2.2 |
| `configure_parallelism()` 并行配置 | 7.3 | 并行策略不可配置 | V2.2 |
| `get_parallelism_status()` 并行状态 | 7.3 | 并行运行状态不可查 | V2.2 |
| 树冠级并行探索 | 7.2 第三级 | 复杂推理鲁棒性 | V2.3 |
| 推理流水线化 | 7.2 第四级 | 远期探索方向 | V3.0 |
| KV 缓存真实句柄 | 3.1.1 | 缓存复用效率 | V2.2 |
| `save()` 树枝持久化 | 3.2.2 | 树枝参数不可保存 | V2.2 |
| 运行时边界审计 | 3.4.1 | 设计原则运行时审计 | V2.2 |

---

## 十一、V2.1 更新内容

- ✅ 双实例内生自干涉推理（IntrospectiveInferenceService）
- ✅ 硬件自适应检测与推荐（HardwareDetector）
- ✅ 记忆固化状态机（MemoryConsolidationService）
- ✅ 多树枝并行评估（ParallelBranchEvaluator）
- ✅ 推导树逻辑纯度分标注
- ✅ 三级门控可视化（GatingEvent）
- ✅ 异步I/O与计算重叠调度（AsyncIOScheduler）
- ✅ 记忆管理分页与编辑功能
- ✅ 契约仲裁规则 CRUD（DefaultContractArbiter）
- ✅ 桌面 EXE 应用（JavaFX + jpackage）
- ✅ 初始记忆种子（学者+研究者角色）
- ✅ 硬件资源占用实时监控

---

## 十二、Demo 成功标准验证

| 测试项 | 通过标准 | 实现状态 |
|--------|----------|----------|
| 内省推理 | 输入含已知谬误的推理链，第二阶段能识别并修正 | ✅ |
| 契约仲裁 | 修改契约规则后，输出合规判定反映规则变更 | ✅ |
| 生命周期 | 重启后工作记忆完全清零，持久记忆完整保留 | ✅ |
| 内核替换 | 替换模型文件后，系统正常运行且推理结果反映模型差异 | ✅ |
| 本地运行 | 全程无网络请求，所有数据存储于本地 | ✅ |

---

## 十三、构建与打包

### 13.1 Maven 构建

```bash
mvn clean package -DskipTests
```

### 13.2 EXE 打包

使用 [build_exe.bat](file:///e:/AI/MemoryTree/build_exe.bat)：

```bash
build_exe.bat
```

打包参数：
- `--type app-image` — 生成应用镜像
- `--main-jar memorytree-2.1.0.jar` — 主 JAR 文件
- `-Xmx4g` — JVM 最大堆内存 4GB
- `--add-opens` — 模块开放（java.lang / java.util / java.lang.reflect）
- `-Dspring.main.web-application-type=none` — 禁用 Web 服务器
- `--win-console` — Windows 控制台模式

### 13.3 输出位置

```
release/MemoryTree/
├── MemoryTree.exe        # 主程序
├── app/
│   ├── memorytree-2.1.0.jar
│   └── MemoryTree.cfg
└── runtime/              # 内嵌 JRE 21
```

---

## 十四、许可证

本项目仅供学习与研究使用。

---

## 十五、文档参考

- 《记忆树 MemoryTree V2.1 认知架构规范》
- 《记忆树 MemoryTree V2.0 认知架构规范》
- 《记忆树（MemoryTree）纯逻辑类脑AI框架 项目整理》

> 注：部分内容可能由 AI 生成
