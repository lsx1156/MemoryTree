# 贡献指南

感谢你对 MemoryTree 项目的关注！本文档描述了参与贡献的流程、代码规范与测试要求。

## 目录

- [快速贡献](#快速贡献)
- [开发环境搭建](#开发环境搭建)
- [代码规范](#代码规范)
- [测试要求](#测试要求)
- [提交规范](#提交规范)
- [Pull Request 流程](#pull-request-流程)
- [Issue 模板](#issue-模板)
- [项目结构](#项目结构)

---

## 快速贡献

1. **Fork** 本仓库到你的 GitHub 账户
2. **Clone** 到本地：`git clone https://github.com/<你的用户名>/MemoryTree.git`
3. **创建分支**：`git checkout -b feature/your-feature-name`
4. **编写代码**并确保通过所有测试
5. **提交**：`git commit -m "feat: 简要描述"`
6. **推送**：`git push origin feature/your-feature-name`
7. **发起 PR**：在 GitHub 上创建 Pull Request 到 `master` 分支

---

## 开发环境搭建

### 必需环境

| 组件 | 版本要求 | 说明 |
|------|---------|------|
| JDK | 21+ | 推荐 Microsoft OpenJDK 21 |
| Maven | 3.8+ | 项目依赖管理与构建 |
| Ollama | 最新版 | 本地 LLM 推理服务，需下载 `qwen2.5:7b` 模型 |

### 可选环境

| 组件 | 用途 |
|------|------|
| JavaFX SDK 21 | 桌面 GUI 开发（项目已内嵌依赖） |
| jpackage | EXE 打包（JDK 21 自带） |

### 初始化步骤

```bash
# 1. 克隆仓库
git clone https://github.com/lsx1156/MemoryTree.git
cd MemoryTree

# 2. 启动 Ollama 服务（另开终端）
ollama serve
ollama pull qwen2.5:7b

# 3. 编译项目（Windows 下设置 JAVA_HOME 指向 JDK 21）
set JAVA_HOME=C:\Program Files\Microsoft\jdk-21.0.9.10-hotspot
set PATH=%JAVA_HOME%\bin;%PATH%
mvn compile

# 4. 运行测试
mvn test
# 或使用便捷脚本
test.bat

# 5. 启动应用
mvn spring-boot:run
```

---

## 代码规范

### Java 代码

1. **包结构**：
   - `com.memorytree.kernel` — 主干内核（推理引擎）
   - `com.memorytree.branch` — 强化学习树枝
   - `com.memorytree.memory` — 记忆后端
   - `com.memorytree.arbiter` — 契约仲裁
   - `com.memorytree.scheduler` — 调度控制
   - `com.memorytree.dto` — 数据传输对象
   - `com.memorytree.enums` — 枚举
   - `com.memorytree.config` — Spring 配置
   - `com.memorytree.gui` — JavaFX 界面
   - `com.memorytree.system` — 系统工具

2. **命名约定**：
   - 类名：PascalCase（如 `OllamaTrunkKernel`）
   - 方法/字段：camelCase（如 `calculateConfidence`）
   - 常量：UPPER_SNAKE_CASE（如 `LOGIC_INSTRUCTION`）
   - 包名：全小写

3. **Spring Boot 3.x 兼容**：
   - 使用 `jakarta.annotation` 而非 `javax.annotation`
   - 配置项放在 `memorytree.*` 命名空间下

4. **Lombok 使用**：
   - DTO 类使用 `@Data @Builder @NoArgsConstructor @AllArgsConstructor`
   - 日志使用 `@Slf4j`

5. **异常处理**：
   - 禁止吞没异常（`catch (Exception e) { // ignore }`）
   - 必须记录日志或向上抛出
   - 边界层使用 `GlobalExceptionHandler` 统一处理

6. **模拟实现标注**：
   - mock 数据必须添加 `// MOCK` 注释，说明原因
   - 示例：`// MOCK: Ollama API does not expose real logits.`

### FXML / CSS

1. **深色主题**：所有窗口（含弹窗）背景色 `#1e1e1e`
2. **标签颜色**：`#c9d1d9`（正文）、`#e6edf3`（标题）
3. **布局**：使用 `HBox.hgrow="ALWAYS"` / `VBox.vgrow="ALWAYS"` 填充空间，避免固定宽度
4. **隐藏组件**：必须同时 `setVisible(false)` 和 `setManaged(false)`
5. **TextArea**：覆盖 ScrollPane、viewport、content 背景色为 `#21262d`
6. **焦点**：禁用焦点高亮 `-fx-focus-color: transparent; -fx-faint-focus-color: transparent;`

### 配置文件

1. `application.yml` 中 AI 模型配置放在 `memorytree.ollama` 命名空间下
2. 时间格式统一为 `yyyy-MM-dd HH:mm:ss`（禁止数组时间戳或纳秒）
3. 显著性值显示两位小数（如 `0.90`）

---

## 测试要求

### 测试分类

| 类型 | 运行时机 | 依赖 | 示例 |
|------|---------|------|------|
| 单元测试 | 每次 `mvn test` | 无外部依赖 | `DefaultContractArbiterTest` |
| 组件测试 | 每次 `mvn test` | 无外部依赖 | `OllamaTrunkKernelIntegrationTest`（组件部分） |
| 集成测试 | 手动 `-Dollama.test=true` | 运行中的 Ollama 服务 | `OllamaTrunkKernelIntegrationTest`（`@EnabledIfSystemProperty`） |

### 测试规范

1. **测试文件位置**：`src/test/java/com/memorytree/<包名>/`
2. **命名**：`<被测类名>Test.java` 或 `<被测类名>IntegrationTest.java`
3. **框架**：JUnit 5（`org.junit.jupiter.api`）
4. **无 Spring 上下文**：使用 `ReflectionTestUtils.setField()` 注入 `@Value` 字段，避免启动 Spring 上下文
5. **临时目录**：使用 `@TempDir` 注解，避免污染文件系统
6. **断言**：使用 JUnit 5 原生断言，不引入 AssertJ 等额外依赖

### 运行测试

```bash
# 运行全部测试（不含 Ollama 集成测试）
mvn test

# 运行指定测试类
mvn test -Dtest=DefaultContractArbiterTest

# 运行 Ollama 集成测试（需先启动 ollama serve）
mvn test -Dtest=OllamaTrunkKernelIntegrationTest -Dollama.test=true

# Windows 便捷脚本
test.bat
```

### PR 测试覆盖率要求

- 新增功能必须附带单元测试
- Bug 修复必须附带回归测试
- 测试必须全部通过（`mvn test` 零失败）

---

## 提交规范

采用 [约定式提交](https://www.conventionalcommits.org/zh-hans/v1.0.0/) 格式：

```
<类型>[可选范围]: <描述>

[可选正文]

[可选脚注]
```

### 常用类型

| 类型 | 说明 | 示例 |
|------|------|------|
| `feat` | 新功能 | `feat: 添加向量检索功能` |
| `fix` | Bug 修复 | `fix: 修复契约仲裁逻辑推导规则` |
| `docs` | 文档更新 | `docs: 更新 README 使用说明` |
| `style` | 代码格式 | `style: 统一缩进为4空格` |
| `refactor` | 重构 | `refactor: 拆分 OllamaTrunkKernel 为4个组件` |
| `test` | 测试相关 | `test: 添加 EmbeddingService 单元测试` |
| `chore` | 构建/工具 | `chore: 添加 GitHub Actions CI` |

### 提交示例

```
feat: 添加语义检索功能

- 新增 EmbeddingService 调用 Ollama /api/embeddings 生成 4096 维向量
- MemoryBackend 新增 semanticSearch 接口
- FileSystemMemoryBackend 实现 cosine 相似度检索

Closes #42
```

---

## Pull Request 流程

1. **创建分支**：从 `master` 创建特性分支，命名格式：
   - `feature/<功能名>` — 新功能
   - `fix/<问题名>` — Bug 修复
   - `docs/<文档名>` — 文档更新
   - `refactor/<重构名>` — 代码重构

2. **保持分支精简**：
   - 一个 PR 只解决一个问题
   - 避免无关的代码改动
   - 控制 diff 行数在可审查范围内（建议 < 500 行）

3. **自检清单**（提交 PR 前逐项确认）：
   - [ ] 代码通过 `mvn compile` 编译
   - [ ] 测试通过 `mvn test`（零失败）
   - [ ] 新增功能附带测试
   - [ ] 代码符合本文档的规范要求
   - [ ] 提交信息符合约定式提交格式
   - [ ] CHANGELOG.md 已更新（如涉及用户可见变更）

4. **PR 标题**：使用约定式提交格式，如 `feat: 添加向量检索功能`

5. **PR 描述**应包含：
   - 变更摘要（做了什么、为什么）
   - 测试方式（如何验证）
   - 关联 Issue（如 `Closes #42`）

6. **代码审查**：
   - 至少需要 1 位维护者审查通过
   - 审查意见修改后，在同一分支提交并推送（不要新开 PR）
   - 确保 CI 检查通过

7. **合并**：
   - 默认使用 Squash merge，保持提交历史整洁
   - 合并后删除特性分支

---

## Issue 模板

### Bug 报告

```markdown
**Bug 描述**
简要描述遇到的问题。

**复现步骤**
1. 启动应用：`mvn spring-boot:run`
2. 在推理输入框输入 '...'
3. 点击推理按钮
4. 观察到错误

**期望行为**
描述你期望发生什么。

**实际行为**
描述实际发生了什么。

**环境信息**
- OS: [如 Windows 11]
- JDK: [如 21.0.9]
- Ollama: [如 0.1.34]
- 模型: [如 qwen2.5:7b]
- MemoryTree 版本: [如 V3.1.2]

**日志/截图**
如有错误日志或截图，请粘贴在此处。
```

### 功能请求

```markdown
**功能描述**
简要描述你希望添加的功能。

**动机**
说明为什么需要这个功能，解决什么问题。

**建议方案**
如果有的话，描述你期望的实现方式。

**替代方案**
是否考虑过其他实现方式？
```

---

## 项目结构

```
MemoryTree/
├── src/
│   ├── main/
│   │   ├── java/com/memorytree/
│   │   │   ├── kernel/          # 主干内核（推理引擎）
│   │   │   ├── branch/          # 强化学习树枝
│   │   │   ├── memory/          # 记忆后端
│   │   │   ├── arbiter/         # 契约仲裁
│   │   │   ├── scheduler/       # 调度控制
│   │   │   ├── dto/             # 数据传输对象
│   │   │   ├── enums/           # 枚举
│   │   │   ├── config/          # Spring 配置
│   │   │   ├── gui/             # JavaFX 界面
│   │   │   └── system/          # 系统工具
│   │   └── resources/
│   │       ├── application.yml  # Spring Boot 配置
│   │       ├── fxml/            # JavaFX 布局文件
│   │       └── css/             # 样式表
│   └── test/
│       └── java/com/memorytree/ # 测试代码
├── .github/workflows/ci.yml     # GitHub Actions CI
├── pom.xml                      # Maven 配置
├── test.bat                     # 测试便捷脚本
├── CHANGELOG.md                 # 更新日志
├── README.md                    # 项目说明
├── LICENSE                      # MIT 开源协议
└── CONTRIBUTING.md              # 本文档
```

---

## 联系方式

- **GitHub Issues**：[https://github.com/lsx1156/MemoryTree/issues](https://github.com/lsx1156/MemoryTree/issues)
- **仓库地址**：[https://github.com/lsx1156/MemoryTree](https://github.com/lsx1156/MemoryTree)

如有疑问，欢迎通过 Issue 提出讨论。感谢你的贡献！
