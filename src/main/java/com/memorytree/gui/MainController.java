package com.memorytree.gui;

import com.memorytree.arbiter.ContractArbiter;
import com.memorytree.branch.ParallelBranchEvaluator;
import com.memorytree.branch.RLBranch;
import com.memorytree.dto.*;
import com.memorytree.kernel.IntrospectiveInferenceService;
import com.memorytree.kernel.TrunkKernel;
import com.memorytree.memory.MemoryBackend;
import com.memorytree.memory.MemoryConsolidationService;
import com.memorytree.system.HardwareDetector;
import com.memorytree.memory.WorkingMemory;
import com.memorytree.scheduler.AsyncIOScheduler;
import com.memorytree.scheduler.SchedulerBus;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class MainController {

    @Autowired
    private TrunkKernel trunkKernel;

    @Autowired
    private SchedulerBus schedulerBus;

    @Autowired
    private MemoryBackend memoryBackend;

    @Autowired
    private WorkingMemory workingMemory;

    @Autowired
    private ContractArbiter contractArbiter;

    @Autowired
    private IntrospectiveInferenceService introspectiveInferenceService;

    @Autowired
    private HardwareDetector hardwareDetector;

    @Autowired(required = false)
    private List<RLBranch> branches;

    @Autowired(required = false)
    private ParallelBranchEvaluator parallelBranchEvaluator;

    @Autowired(required = false)
    private MemoryConsolidationService memoryConsolidationService;

    @Autowired(required = false)
    private AsyncIOScheduler asyncIOScheduler;

    @FXML
    private Label systemStatusLabel;

    @FXML
    private Button resetMemoryBtn;

    @FXML
    private Button tabInferenceBtn;

    @FXML
    private Button tabMemoryBtn;

    @FXML
    private Button tabBranchesBtn;

    @FXML
    private Button tabContractBtn;

    @FXML
    private Label workingMemorySizeLabel;

    @FXML
    private Label persistentMemoryCountLabel;

    @FXML
    private Label activeBranchCountLabel;

    @FXML
    private Label hardwareCpuLabel;

    @FXML
    private Label hardwareCpuUsageLabel;

    @FXML
    private Label hardwareMemoryLabel;

    @FXML
    private Label hardwareMemoryUsageLabel;

    @FXML
    private Label hardwareJvmUsageLabel;

    @FXML
    private Label hardwareModelLabel;

    @FXML
    private Label hardwareThreadLabel;

    @FXML
    private TabPane mainTabPane;

    @FXML
    private Tab tabInference;

    @FXML
    private Tab tabMemory;

    @FXML
    private Tab tabBranches;

    @FXML
    private Tab tabContract;

    @FXML
    private TextArea promptInput;

    @FXML
    private TextArea premisesInput;

    @FXML
    private Slider temperatureSlider;

    @FXML
    private Label temperatureValueLabel;

    @FXML
    private Slider introspectionSlider;

    @FXML
    private Label introspectionValueLabel;

    @FXML
    private Button inferenceBtn;

    @FXML
    private HBox stateIdle;

    @FXML
    private HBox stateDraft;

    @FXML
    private HBox stateValidate;

    @FXML
    private HBox stateRewrite;

    @FXML
    private HBox stateOutput;

    @FXML
    private TextArea resultContent;

    @FXML
    private Label resultSubtitle;

    @FXML
    private Label statRoundsLabel;

    @FXML
    private Label statTimeLabel;

    @FXML
    private Label statConfidenceLabel;

    @FXML
    private ListView<String> derivationTreeView;

    @FXML
    private VBox arbitrationContainer;

    @FXML
    private ListView<String> workingMemoryList;

    @FXML
    private ListView<String> persistentMemoryList;

    @FXML
    private TextField memorySearchField;

    @FXML
    private Label workingPageLabel;

    @FXML
    private Label persistentPageLabel;

    @FXML
    private Button workingPrevBtn;

    @FXML
    private Button workingNextBtn;

    @FXML
    private Button persistentPrevBtn;

    @FXML
    private Button persistentNextBtn;

    @FXML
    private ListView<String> branchListView;

    @FXML
    private TextField branchNameField;

    @FXML
    private TextField branchTypeField;

    @FXML
    private VBox contractRulesContainer;

    private static final int MEMORY_PAGE_SIZE = 5;
    private int workingMemoryPage = 0;
    private int persistentMemoryPage = 0;

    private boolean isInferencing = false;

    @FXML
    public void initialize() {
        temperatureSlider.valueProperty().addListener((obs, old, val) -> 
            temperatureValueLabel.setText(String.format("%.1f", val.doubleValue())));
        
        introspectionSlider.valueProperty().addListener((obs, old, val) -> 
            introspectionValueLabel.setText(String.valueOf(val.intValue())));

        updateSystemStatus();
        loadBranches();
        loadPersistentMemory();
        updateMemoryStats();
        updateHardwareInfo();
        
        loadContractClauses();

        java.util.Timer timer = new java.util.Timer();
        timer.scheduleAtFixedRate(new java.util.TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> {
                    updateSystemStatus();
                    updateMemoryStats();
                    updateHardwareInfo();
                });
            }
        }, 1000, 2000);
    }

    public void initializeAfterStageShow() {
        checkOllamaConnection();
    }

    private void checkOllamaConnection() {
        new Thread(() -> {
            try {
                Thread.sleep(2000);
                Platform.runLater(() -> {
                    if (trunkKernel.isLoaded()) {
                        systemStatusLabel.setText("运行中");
                        systemStatusLabel.setStyle("-fx-background-color: rgba(35, 134, 54, 0.2); -fx-text-fill: #3fb950; -fx-padding: 6 12; -fx-background-radius: 20;");
                    } else {
                        systemStatusLabel.setText("就绪");
                        systemStatusLabel.setStyle("-fx-background-color: rgba(240, 136, 62, 0.2); -fx-text-fill: #f0883e; -fx-padding: 6 12; -fx-background-radius: 20;");
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    systemStatusLabel.setText("未连接");
                    systemStatusLabel.setStyle("-fx-background-color: rgba(248, 81, 73, 0.2); -fx-text-fill: #f85149; -fx-padding: 6 12; -fx-background-radius: 20;");
                });
            }
        }).start();
    }

    private void updateSystemStatus() {
        String status = schedulerBus.getSystemStatus();
        if (status.contains("RUNNING")) {
            systemStatusLabel.setText("运行中");
            systemStatusLabel.setStyle("-fx-background-color: rgba(35, 134, 54, 0.2); -fx-text-fill: #3fb950; -fx-padding: 6 12; -fx-background-radius: 20;");
        } else {
            systemStatusLabel.setText("初始化中");
            systemStatusLabel.setStyle("-fx-background-color: rgba(240, 136, 62, 0.2); -fx-text-fill: #f0883e; -fx-padding: 6 12; -fx-background-radius: 20;");
        }
    }

    private void updateMemoryStats() {
        List<MemoryEntry> workingEntries = workingMemory.getAllEntries();
        long size = workingEntries.stream()
                .mapToLong(e -> (e.getContent() != null ? e.getContent().length() * 2 : 0) + 100)
                .sum();
        String sizeText;
        if (size < 1024) {
            sizeText = size + " B";
        } else {
            sizeText = String.format("%.1f KB", size / 1024.0);
        }
        workingMemorySizeLabel.setText(workingEntries.size() + "条 / " + sizeText);

        persistentMemoryCountLabel.setText(memoryBackend.getTotalCount() + " 条");

        if (branches != null) {
            int activeCount = (int) branches.stream().filter(RLBranch::isActive).count();
            activeBranchCountLabel.setText(activeCount + " 个");
        }
    }

    private void updateHardwareInfo() {
        HardwareDetector.HardwareInfo info = hardwareDetector.detect();
        hardwareCpuLabel.setText("CPU: " + info.getCpuCores() + " 核");

        String cpuUsage = info.getCpuUsagePercent() >= 0
                ? String.format("%.1f%%", info.getCpuUsagePercent())
                : "N/A";
        hardwareCpuUsageLabel.setText("CPU占用: " + cpuUsage);

        hardwareMemoryLabel.setText("内存: " + HardwareDetector.formatBytes(info.getPhysicalMemoryBytes()));

        hardwareMemoryUsageLabel.setText(String.format("系统内存占用: %.1f%%", info.getSystemMemoryUsagePercent()));

        hardwareJvmUsageLabel.setText(String.format("JVM占用: %s / %s (%.1f%%)",
                HardwareDetector.formatBytes(info.getJvmUsedMemoryBytes()),
                HardwareDetector.formatBytes(info.getJvmMaxMemoryBytes()),
                info.getJvmMemoryUsagePercent()));

        hardwareModelLabel.setText("推荐模型: " + info.getRecommendedModelSize());
        hardwareThreadLabel.setText("线程数: " + info.getJvmThreadCount());
    }

    @FXML
    public void switchTabInference() {
        showTab("inference");
    }

    @FXML
    public void switchTabMemory() {
        showTab("memory");
        loadWorkingMemory();
        loadPersistentMemory();
    }

    @FXML
    public void switchTabBranches() {
        showTab("branches");
        loadBranches();
    }

    @FXML
    public void switchTabContract() {
        showTab("contract");
        loadContractClauses();
    }

    private void showTab(String tab) {
        tabInferenceBtn.getStyleClass().remove("active");
        tabMemoryBtn.getStyleClass().remove("active");
        tabBranchesBtn.getStyleClass().remove("active");
        tabContractBtn.getStyleClass().remove("active");

        switch (tab) {
            case "inference": 
                mainTabPane.getSelectionModel().select(tabInference);
                tabInferenceBtn.getStyleClass().add("active"); 
                break;
            case "memory": 
                mainTabPane.getSelectionModel().select(tabMemory);
                tabMemoryBtn.getStyleClass().add("active"); 
                break;
            case "branches": 
                mainTabPane.getSelectionModel().select(tabBranches);
                tabBranchesBtn.getStyleClass().add("active"); 
                break;
            case "contract": 
                mainTabPane.getSelectionModel().select(tabContract);
                tabContractBtn.getStyleClass().add("active"); 
                break;
        }
    }

    @FXML
    public void handleInference() {
        String prompt = promptInput.getText().trim();
        if (prompt.isEmpty()) {
            showAlert("请输入推理问题");
            return;
        }

        if (isInferencing) {
            showAlert("正在推理中，请等待完成");
            return;
        }

        executeInference(prompt);
    }

    private void executeInference(String prompt) {
        isInferencing = true;
        inferenceBtn.setText("推理中...");
        inferenceBtn.setDisable(true);

        resetStateMachine();
        setState("idle", "active");

        String premises = premisesInput.getText().trim();
        int introspectionRounds = (int) introspectionSlider.getValue();
        double temperature = temperatureSlider.getValue();

        setState("draft", "active");

        new Thread(() -> {
            try {
                log.info("[推理] 步骤1: 异步I/O记忆预检索");
                if (asyncIOScheduler != null) {
                    try {
                        asyncIOScheduler.preRetrieveMemory(prompt);
                    } catch (Exception ex) {
                        log.warn("[推理] 异步记忆预检索失败（非致命）: {}", ex.getMessage());
                    }
                }

                log.info("[推理] 步骤2: 多树枝并行评估");
                if (parallelBranchEvaluator != null && branches != null && !branches.isEmpty()) {
                    try {
                        ObservationSpace observation = ObservationSpace.builder()
                                .inputPrompt(prompt)
                                .temperature(temperature)
                                .build();
                        parallelBranchEvaluator.parallelObserve(branches, observation);
                    } catch (Exception ex) {
                        log.warn("[推理] 树枝评估失败（非致命）: {}", ex.getMessage());
                    }
                }

                log.info("[推理] 步骤3: 内省推理 (轮次={}, 温度={})", introspectionRounds, temperature);
                GenerateResult result = null;
                try {
                    result = introspectiveInferenceService.performIntrospectiveInference(
                            prompt, premises, introspectionRounds, temperature, 0.1);
                } catch (Exception ex) {
                    log.error("[推理] 内省推理失败: {}", ex.getMessage(), ex);
                    throw new RuntimeException("内省推理失败: " + (ex.getMessage() != null ? ex.getMessage() : ex.getClass().getName()), ex);
                }

                log.info("[推理] 步骤4: 契约仲裁");
                if (result != null && result.getText() != null) {
                    try {
                        ArbitrationResultDTO arbResult = contractArbiter.validate(result.getText());
                        result.setArbitrationResult(arbResult);
                        log.info("[推理] 契约仲裁完成: {}", arbResult != null ? arbResult.getResult() : "null");
                    } catch (Exception ex) {
                        log.warn("[推理] 契约仲裁失败（非致命）: {}", ex.getMessage());
                    }
                }

                log.info("[推理] 步骤5: 记忆固化检测");
                if (memoryConsolidationService != null && result != null && result.getText() != null) {
                    try {
                        memoryConsolidationService.tryConsolidate(result.getText(),
                                result.getConfidenceScore(), result.getIntrospectionRounds());
                    } catch (Exception ex) {
                        log.warn("[推理] 记忆固化失败（非致命）: {}", ex.getMessage());
                    }
                }

                log.info("[推理] 步骤6: 输出结果");
                final GenerateResult finalResult = result;

                Platform.runLater(() -> {
                    setState("draft", "completed");
                    setState("validate", "active");
                    setState("validate", "completed");

                    if (finalResult != null && finalResult.getIntrospectionRecords() != null) {
                        for (IntrospectionRecord record : finalResult.getIntrospectionRecords()) {
                            if ("REWRITE".equals(record.getAction())) {
                                setState("rewrite", "active");
                                setState("rewrite", "completed");
                            }
                        }
                    }

                    setState("output", "active");

                    displayResult(finalResult);

                    inferenceBtn.setText("执行逻辑推理");
                    inferenceBtn.setDisable(false);
                    isInferencing = false;
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    StringBuilder errorMsg = new StringBuilder();
                    errorMsg.append("推理失败: ");
                    if (e.getMessage() != null) {
                        errorMsg.append(e.getMessage());
                    } else {
                        errorMsg.append(e.getClass().getName());
                    }
                    
                    Throwable cause = e.getCause();
                    int depth = 0;
                    while (cause != null && depth < 3) {
                        errorMsg.append("\n\n原因").append(depth + 1).append(": ");
                        if (cause.getMessage() != null) {
                            errorMsg.append(cause.getMessage());
                        } else {
                            errorMsg.append(cause.getClass().getName());
                        }
                        cause = cause.getCause();
                        depth++;
                    }
                    
                    errorMsg.append("\n\n请检查：\n1. Ollama 服务是否已启动 (ollama serve)\n2. 模型是否已下载 (ollama pull qwen2.5:7b)\n3. 网络连接是否正常");
                    resultContent.setText(errorMsg.toString());
                    inferenceBtn.setText("执行逻辑推理");
                    inferenceBtn.setDisable(false);
                    isInferencing = false;
                    setState("idle", "active");
                });
            }
        }).start();
    }

    private void resetStateMachine() {
        updateStateVisual(stateIdle, "idle");
        updateStateVisual(stateDraft, "idle");
        updateStateVisual(stateValidate, "idle");
        updateStateVisual(stateRewrite, "idle");
        updateStateVisual(stateOutput, "idle");
    }

    private void setState(String stateName, String status) {
        HBox state = switch (stateName) {
            case "idle" -> stateIdle;
            case "draft" -> stateDraft;
            case "validate" -> stateValidate;
            case "rewrite" -> stateRewrite;
            case "output" -> stateOutput;
            default -> null;
        };
        if (state != null) {
            updateStateVisual(state, status);
        }
    }

    private void updateStateVisual(HBox state, String status) {
        Region dot = (Region) state.getChildren().get(0);
        VBox textContainer = (VBox) state.getChildren().get(1);
        Label nameLabel = (Label) textContainer.getChildren().get(0);

        state.getStyleClass().remove("active");
        dot.setStyle("-fx-background-color: #6c6c6c; -fx-background-radius: 50%; -fx-min-width: 8; -fx-min-height: 8;");
        nameLabel.setStyle("-fx-text-fill: #bbbbbb; -fx-font-size: 11px; -fx-font-weight: 600;");
        state.setOpacity(0.5);

        switch (status) {
            case "active":
                dot.setStyle("-fx-background-color: #007acc; -fx-background-radius: 50%; -fx-min-width: 8; -fx-min-height: 8; -fx-effect: dropshadow(gaussian, rgba(0, 122, 204, 0.6), 4, 0, 0, 0);");
                nameLabel.setStyle("-fx-text-fill: #4fc1ff; -fx-font-size: 11px; -fx-font-weight: 600;");
                state.setOpacity(1);
                state.getStyleClass().add("active");
                break;
            case "completed":
                dot.setStyle("-fx-background-color: #4ec9b0; -fx-background-radius: 50%; -fx-min-width: 8; -fx-min-height: 8; -fx-effect: dropshadow(gaussian, rgba(78, 201, 176, 0.6), 4, 0, 0, 0);");
                nameLabel.setStyle("-fx-text-fill: #4ec9b0; -fx-font-size: 11px; -fx-font-weight: 600;");
                state.setOpacity(1);
                break;
        }
    }

    private void displayResult(GenerateResult result) {
        if (result == null) {
            resultContent.setText("");
            return;
        }

        resultContent.setText(result.getText() != null ? result.getText() : "");

        ObservableList<String> treeItems = FXCollections.observableArrayList();
        if (result.getDerivationTree() != null) {
            for (int i = 0; i < result.getDerivationTree().size(); i++) {
                String item = result.getDerivationTree().get(i);
                if (result.getLogicPurityScores() != null && i < result.getLogicPurityScores().size()) {
                    item += " (纯度: " + String.format("%.2f", result.getLogicPurityScores().get(i)) + ")";
                }
                treeItems.add((i + 1) + ". " + item);
            }
        } else if (result.getIntrospectionRecords() != null) {
            for (IntrospectionRecord record : result.getIntrospectionRecords()) {
                String prefix = switch (record.getAction()) {
                    case "DRAFT_GENERATE" -> "📝 草稿生成";
                    case "LOGIC_VALIDATE" -> "✅ 逻辑校验";
                    case "REWRITE" -> "🔄 重写";
                    case "OUTPUT" -> "📤 输出";
                    default -> "";
                };
                treeItems.add("[" + record.getRound() + "] " + prefix);
            }
        }
        derivationTreeView.setItems(treeItems);

        arbitrationContainer.getChildren().clear();
        if (result.getArbitrationResult() != null) {
            ArbitrationResultDTO arb = result.getArbitrationResult();
            Label statusLabel = new Label("结果: " + formatArbitrationResult(arb.getResult()));
            statusLabel.setStyle(getArbitrationStyle(arb.getResult()));
            arbitrationContainer.getChildren().add(statusLabel);
            arbitrationContainer.getChildren().add(new Label("合规分数: " + String.format("%.2f", arb.getComplianceScore())));
            arbitrationContainer.getChildren().add(new Label("说明: " + (arb.getExplanation() != null ? arb.getExplanation() : "无")));
            if (arb.getViolatingClauseName() != null) {
                Label violationLabel = new Label("违反规则: " + arb.getViolatingClauseName());
                violationLabel.setStyle("-fx-text-fill: #f87171;");
                arbitrationContainer.getChildren().add(violationLabel);
            }
        } else {
            ArbitrationResultDTO arb = contractArbiter.validate(result.getText());
            if (arb != null) {
                Label statusLabel = new Label("结果: " + formatArbitrationResult(arb.getResult()));
                statusLabel.setStyle(getArbitrationStyle(arb.getResult()));
                arbitrationContainer.getChildren().add(statusLabel);
                arbitrationContainer.getChildren().add(new Label("合规分数: " + String.format("%.2f", arb.getComplianceScore())));
                arbitrationContainer.getChildren().add(new Label("说明: " + (arb.getExplanation() != null ? arb.getExplanation() : "无")));
            }
        }

        int rounds = result.getIntrospectionRounds() > 0 ? result.getIntrospectionRounds() : 1;
        statRoundsLabel.setText("内省: " + rounds + "轮");
        statTimeLabel.setText("耗时: " + result.getInferenceTimeMs() + "ms");
        String confLevel = result.getConfidenceScore() < 0.6 ? "低" :
                (result.getConfidenceScore() < 0.8 ? "中" : "高");
        statConfidenceLabel.setText("置信度: " + confLevel + " (" + String.format("%.2f", result.getConfidenceScore()) + ")");
        resultSubtitle.setText("推理完成");
    }

    private String formatArbitrationResult(com.memorytree.enums.ArbitrationResult result) {
        if (result == null) return "未知";
        return switch (result) {
            case COMPLIANT -> "合规通过";
            case NON_COMPLIANT -> "不合规";
            case LOW_CONFIDENCE -> "低置信度";
            case FAIL_SAFE_TRIGGERED -> "FAIL-SAFE触发";
        };
    }

    private String getArbitrationStyle(com.memorytree.enums.ArbitrationResult result) {
        if (result == null) return "-fx-text-fill: #8b949e;";
        return switch (result) {
            case COMPLIANT -> "-fx-text-fill: #3fb950; -fx-font-weight: bold;";
            case NON_COMPLIANT -> "-fx-text-fill: #f85149; -fx-font-weight: bold;";
            case LOW_CONFIDENCE -> "-fx-text-fill: #f0883e; -fx-font-weight: bold;";
            case FAIL_SAFE_TRIGGERED -> "-fx-text-fill: #f85149; -fx-font-weight: bold;";
        };
    }

    @FXML
    public void handleResetMemory() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("确认重置");
        alert.setHeaderText(null);
        alert.setContentText("确定要重置工作记忆吗？这将清空所有意识态记忆。");
        alert.showAndWait().ifPresent(response -> {
            if (response == javafx.scene.control.ButtonType.OK) {
                schedulerBus.resetWorkingMemory();
                loadWorkingMemory();
                updateMemoryStats();
                showAlert("工作记忆已重置");
            }
        });
    }

    @FXML
    public void handleClearWorking() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("确认清空");
        alert.setHeaderText(null);
        alert.setContentText("确定要清空工作记忆吗？");
        alert.showAndWait().ifPresent(response -> {
            if (response == javafx.scene.control.ButtonType.OK) {
                workingMemory.clearAll();
                loadWorkingMemory();
                updateMemoryStats();
                showAlert("工作记忆已清空");
            }
        });
    }

    private void loadWorkingMemory() {
        List<MemoryEntry> allEntries = workingMemory.getAllEntries();
        int totalPages = Math.max(1, (int) Math.ceil((double) allEntries.size() / MEMORY_PAGE_SIZE));
        if (workingMemoryPage >= totalPages) workingMemoryPage = totalPages - 1;
        if (workingMemoryPage < 0) workingMemoryPage = 0;

        int fromIndex = workingMemoryPage * MEMORY_PAGE_SIZE;
        int toIndex = Math.min(fromIndex + MEMORY_PAGE_SIZE, allEntries.size());
        
        ObservableList<String> items = FXCollections.observableArrayList();
        if (allEntries.isEmpty()) {
            items.add("工作记忆为空");
        } else {
            for (int i = fromIndex; i < toIndex; i++) {
                MemoryEntry entry = allEntries.get(i);
                String preview = entry.getContent();
                if (preview.length() > 60) {
                    preview = preview.substring(0, 60) + "...";
                }
                items.add(entry.getId() + " | " + preview);
            }
        }
        workingMemoryList.setItems(items);
        workingPageLabel.setText("第 " + (workingMemoryPage + 1) + "/" + totalPages + " 页 (共 " + allEntries.size() + " 条)");
        workingPrevBtn.setDisable(workingMemoryPage == 0);
        workingNextBtn.setDisable(workingMemoryPage >= totalPages - 1);
    }

    private void loadPersistentMemory() {
        String keyword = memorySearchField.getText().trim();
        List<MemoryEntry> allEntries;
        
        if (!keyword.isEmpty()) {
            MemoryQuery query = MemoryQuery.builder()
                    .keyword(keyword)
                    .limit(100)
                    .build();
            allEntries = memoryBackend.query(query);
        } else {
            allEntries = memoryBackend.getAll();
        }

        int totalPages = Math.max(1, (int) Math.ceil((double) allEntries.size() / MEMORY_PAGE_SIZE));
        if (persistentMemoryPage >= totalPages) persistentMemoryPage = totalPages - 1;
        if (persistentMemoryPage < 0) persistentMemoryPage = 0;

        int fromIndex = persistentMemoryPage * MEMORY_PAGE_SIZE;
        int toIndex = Math.min(fromIndex + MEMORY_PAGE_SIZE, allEntries.size());
        
        ObservableList<String> items = FXCollections.observableArrayList();
        if (allEntries.isEmpty()) {
            items.add(keyword.isEmpty() ? "暂无持久记忆" : "未找到匹配 '" + keyword + "' 的记忆");
        } else {
            for (int i = fromIndex; i < toIndex; i++) {
                MemoryEntry entry = allEntries.get(i);
                String preview = entry.getContent();
                if (preview.length() > 60) {
                    preview = preview.substring(0, 60) + "...";
                }
                items.add(entry.getId() + " | " + preview);
            }
        }
        persistentMemoryList.setItems(items);
        persistentPageLabel.setText("第 " + (persistentMemoryPage + 1) + "/" + totalPages + " 页 (共 " + allEntries.size() + " 条)");
        persistentPrevBtn.setDisable(persistentMemoryPage == 0);
        persistentNextBtn.setDisable(persistentMemoryPage >= totalPages - 1);
    }

    @FXML
    public void prevWorkingPage() {
        if (workingMemoryPage > 0) {
            workingMemoryPage--;
            loadWorkingMemory();
        }
    }

    @FXML
    public void nextWorkingPage() {
        List<MemoryEntry> allEntries = workingMemory.getAllEntries();
        int totalPages = (int) Math.ceil((double) allEntries.size() / MEMORY_PAGE_SIZE);
        if (workingMemoryPage < totalPages - 1) {
            workingMemoryPage++;
            loadWorkingMemory();
        }
    }

    @FXML
    public void prevPersistentPage() {
        if (persistentMemoryPage > 0) {
            persistentMemoryPage--;
            loadPersistentMemory();
        }
    }

    @FXML
    public void nextPersistentPage() {
        List<MemoryEntry> allEntries = memoryBackend.getAll();
        int totalPages = (int) Math.ceil((double) allEntries.size() / MEMORY_PAGE_SIZE);
        if (persistentMemoryPage < totalPages - 1) {
            persistentMemoryPage++;
            loadPersistentMemory();
        }
    }

    @FXML
    public void handleAddWorking() {
        Dialog<MemoryEntry> dialog = new Dialog<>();
        dialog.setTitle("添加工作记忆");
        dialog.setHeaderText(null);

        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialogPane.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        dialogPane.getStyleClass().add("custom-dialog");

        VBox content = new VBox(12);
        content.setPadding(new Insets(20));
        content.getStyleClass().add("dialog-body");

        TextArea contentArea = new TextArea();
        contentArea.setWrapText(true);
        contentArea.setPrefHeight(150);
        contentArea.setPromptText("请输入记忆内容");
        contentArea.getStyleClass().add("form-textarea");

        TextField tagsField = new TextField();
        tagsField.setPromptText("标签 (逗号分隔)");
        tagsField.getStyleClass().add("form-field");

        HBox contentRow = new HBox(8);
        contentRow.getStyleClass().add("form-row");
        Label contentLabel = new Label("内容:");
        contentLabel.getStyleClass().add("form-label");
        contentRow.getChildren().addAll(contentLabel, contentArea);
        contentArea.setMaxWidth(Double.MAX_VALUE);

        HBox tagsRow = new HBox(8);
        tagsRow.getStyleClass().add("form-row");
        Label tagsLabel = new Label("标签:");
        tagsLabel.getStyleClass().add("form-label");
        tagsRow.getChildren().addAll(tagsLabel, tagsField);

        content.getChildren().addAll(contentRow, tagsRow);
        dialogPane.setContent(content);

        Button okBtn = (Button) dialogPane.lookupButton(ButtonType.OK);
        okBtn.getStyleClass().add("btn-dialog-ok");
        Button cancelBtn = (Button) dialogPane.lookupButton(ButtonType.CANCEL);
        cancelBtn.getStyleClass().add("btn-dialog-cancel");

        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                String text = contentArea.getText().trim();
                if (text.isEmpty()) {
                    showAlert("内容不能为空");
                    return null;
                }
                List<String> tags = Arrays.stream(tagsField.getText().split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.toList());
                return MemoryEntry.builder().content(text).tags(tags).build();
            }
            return null;
        });

        dialog.showAndWait().ifPresent(entry -> {
            workingMemory.addEntry(entry);
            loadWorkingMemory();
            updateMemoryStats();
            showAlert("工作记忆已添加");
        });
    }

    @FXML
    public void handleViewWorking() {
        int idx = workingMemoryList.getSelectionModel().getSelectedIndex();
        if (idx < 0) {
            showAlert("请先选择一条工作记忆");
            return;
        }
        List<MemoryEntry> allEntries = workingMemory.getAllEntries();
        int fromIndex = workingMemoryPage * MEMORY_PAGE_SIZE;
        if (idx + fromIndex < allEntries.size()) {
            showMemoryDetailDialog(allEntries.get(idx + fromIndex), true);
        }
    }

    @FXML
    public void handleEditWorking() {
        int idx = workingMemoryList.getSelectionModel().getSelectedIndex();
        if (idx < 0) {
            showAlert("请先选择一条工作记忆");
            return;
        }
        List<MemoryEntry> allEntries = workingMemory.getAllEntries();
        int fromIndex = workingMemoryPage * MEMORY_PAGE_SIZE;
        if (idx + fromIndex < allEntries.size()) {
            showEditMemoryDialog(allEntries.get(idx + fromIndex), true);
        }
    }

    @FXML
    public void handleRemoveWorking() {
        int idx = workingMemoryList.getSelectionModel().getSelectedIndex();
        if (idx < 0) {
            showAlert("请先选择一条工作记忆");
            return;
        }
        List<MemoryEntry> allEntries = workingMemory.getAllEntries();
        int fromIndex = workingMemoryPage * MEMORY_PAGE_SIZE;
        if (idx + fromIndex < allEntries.size()) {
            MemoryEntry entry = allEntries.get(idx + fromIndex);
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("确认移除");
            alert.setHeaderText(null);
            alert.setContentText("确定要移除这条工作记忆吗？");
            alert.showAndWait().ifPresent(response -> {
                if (response == javafx.scene.control.ButtonType.OK) {
                    workingMemory.removeEntry(entry.getId());
                    loadWorkingMemory();
                    updateMemoryStats();
                    showAlert("已移除");
                }
            });
        }
    }

    @FXML
    public void handleAddPersistent() {
        Dialog<MemoryEntry> dialog = new Dialog<>();
        dialog.setTitle("添加持久记忆");
        dialog.setHeaderText(null);

        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialogPane.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        dialogPane.getStyleClass().add("custom-dialog");

        VBox content = new VBox(12);
        content.setPadding(new Insets(20));
        content.getStyleClass().add("dialog-body");

        TextArea contentArea = new TextArea();
        contentArea.setWrapText(true);
        contentArea.setPrefHeight(150);
        contentArea.setPromptText("请输入记忆内容");
        contentArea.getStyleClass().add("form-textarea");

        TextField tagsField = new TextField();
        tagsField.setPromptText("标签 (逗号分隔)");
        tagsField.getStyleClass().add("form-field");

        HBox contentRow = new HBox(8);
        contentRow.getStyleClass().add("form-row");
        Label contentLabel = new Label("内容:");
        contentLabel.getStyleClass().add("form-label");
        contentRow.getChildren().addAll(contentLabel, contentArea);
        contentArea.setMaxWidth(Double.MAX_VALUE);

        HBox tagsRow = new HBox(8);
        tagsRow.getStyleClass().add("form-row");
        Label tagsLabel = new Label("标签:");
        tagsLabel.getStyleClass().add("form-label");
        tagsRow.getChildren().addAll(tagsLabel, tagsField);

        content.getChildren().addAll(contentRow, tagsRow);
        dialogPane.setContent(content);

        Button okBtn = (Button) dialogPane.lookupButton(ButtonType.OK);
        okBtn.getStyleClass().add("btn-dialog-ok");
        Button cancelBtn = (Button) dialogPane.lookupButton(ButtonType.CANCEL);
        cancelBtn.getStyleClass().add("btn-dialog-cancel");

        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                String text = contentArea.getText().trim();
                if (text.isEmpty()) {
                    showAlert("内容不能为空");
                    return null;
                }
                List<String> tags = Arrays.stream(tagsField.getText().split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.toList());
                return MemoryEntry.builder().content(text).tags(tags).build();
            }
            return null;
        });

        dialog.showAndWait().ifPresent(entry -> {
            memoryBackend.store(entry);
            loadPersistentMemory();
            updateMemoryStats();
            showAlert("持久记忆已添加");
        });
    }

    @FXML
    public void handleViewPersistent() {
        int idx = persistentMemoryList.getSelectionModel().getSelectedIndex();
        if (idx < 0) {
            showAlert("请先选择一条持久记忆");
            return;
        }
        List<MemoryEntry> allEntries = memoryBackend.getAll();
        int fromIndex = persistentMemoryPage * MEMORY_PAGE_SIZE;
        if (idx + fromIndex < allEntries.size()) {
            showMemoryDetailDialog(allEntries.get(idx + fromIndex), false);
        }
    }

    @FXML
    public void handleEditPersistent() {
        int idx = persistentMemoryList.getSelectionModel().getSelectedIndex();
        if (idx < 0) {
            showAlert("请先选择一条持久记忆");
            return;
        }
        List<MemoryEntry> allEntries = memoryBackend.getAll();
        int fromIndex = persistentMemoryPage * MEMORY_PAGE_SIZE;
        if (idx + fromIndex < allEntries.size()) {
            showEditMemoryDialog(allEntries.get(idx + fromIndex), false);
        }
    }

    @FXML
    public void handleInjectMemory() {
        int idx = persistentMemoryList.getSelectionModel().getSelectedIndex();
        if (idx < 0) {
            showAlert("请先选择一条持久记忆");
            return;
        }
        List<MemoryEntry> allEntries = memoryBackend.getAll();
        int fromIndex = persistentMemoryPage * MEMORY_PAGE_SIZE;
        if (idx + fromIndex < allEntries.size()) {
            MemoryEntry entry = allEntries.get(idx + fromIndex);
            workingMemory.addEntry(entry);
            loadWorkingMemory();
            updateMemoryStats();
            showAlert("已注入到工作记忆");
        }
    }

    @FXML
    public void handleDeletePersistent() {
        int idx = persistentMemoryList.getSelectionModel().getSelectedIndex();
        if (idx < 0) {
            showAlert("请先选择一条持久记忆");
            return;
        }
        List<MemoryEntry> allEntries = memoryBackend.getAll();
        int fromIndex = persistentMemoryPage * MEMORY_PAGE_SIZE;
        if (idx + fromIndex < allEntries.size()) {
            MemoryEntry entry = allEntries.get(idx + fromIndex);
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("确认删除");
            alert.setHeaderText(null);
            alert.setContentText("确定要删除这条持久记忆吗？");
            alert.showAndWait().ifPresent(response -> {
                if (response == javafx.scene.control.ButtonType.OK) {
                    memoryBackend.delete(entry.getId());
                    loadPersistentMemory();
                    updateMemoryStats();
                    showAlert("已删除");
                }
            });
        }
    }

    private void showMemoryDetailDialog(MemoryEntry entry, boolean isWorking) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("记忆详情");
        dialog.setHeaderText(null);

        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getButtonTypes().add(ButtonType.CLOSE);
        dialogPane.setPrefWidth(560);
        dialogPane.setPrefHeight(420);

        dialogPane.setStyle("-fx-background-color: #1e1e1e;");
        dialogPane.lookupButton(ButtonType.CLOSE).setStyle(
            "-fx-background-color: #0e639c; -fx-text-fill: white; -fx-background-radius: 4; -fx-padding: 6 16;"
        );

        VBox content = new VBox(12);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: #1e1e1e;");

        Label typeLabel = new Label("类型: " + (isWorking ? "工作记忆" : "持久记忆"));
        typeLabel.setStyle("-fx-text-fill: #58a6ff; -fx-font-weight: bold; -fx-font-size: 14;");

        Label idLabel = new Label("ID: " + entry.getId());
        idLabel.setStyle("-fx-text-fill: #c9d1d9; -fx-font-size: 12;");

        Label tagsLabel = new Label("标签: " + (entry.getTags() != null ? String.join(", ", entry.getTags()) : "无"));
        tagsLabel.setStyle("-fx-text-fill: #c9d1d9; -fx-font-size: 12;");

        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        Label saliencyLabel = new Label("显著性: " + String.format("%.2f", entry.getSaliencyScore()));
        saliencyLabel.setStyle("-fx-text-fill: #c9d1d9; -fx-font-size: 12;");

        Label accessLabel = new Label("访问次数: " + entry.getAccessCount());
        accessLabel.setStyle("-fx-text-fill: #c9d1d9; -fx-font-size: 12;");

        Label createdLabel = new Label("创建时间: " + (entry.getCreatedAt() != null ? entry.getCreatedAt().format(formatter) : "未知"));
        createdLabel.setStyle("-fx-text-fill: #c9d1d9; -fx-font-size: 12;");

        Label accessedLabel = new Label("最后访问: " + (entry.getLastAccessedAt() != null ? entry.getLastAccessedAt().format(formatter) : "未知"));
        accessedLabel.setStyle("-fx-text-fill: #c9d1d9; -fx-font-size: 12;");

        Label contentTitle = new Label("内容:");
        contentTitle.setStyle("-fx-text-fill: #e6edf3; -fx-font-weight: bold; -fx-font-size: 13;");

        TextArea contentArea = new TextArea(entry.getContent());
        contentArea.setEditable(false);
        contentArea.setWrapText(true);
        contentArea.setPrefHeight(180);
        contentArea.setStyle(
            "-fx-control-inner-background: #21262d; " +
            "-fx-text-fill: #e6edf3; " +
            "-fx-background-color: #30363d; " +
            "-fx-background-radius: 6; " +
            "-fx-border-color: #30363d; " +
            "-fx-border-radius: 6;"
        );

        content.getChildren().addAll(typeLabel, idLabel, tagsLabel, saliencyLabel, accessLabel, createdLabel, accessedLabel, contentTitle, contentArea);
        dialogPane.setContent(content);

        dialog.showAndWait();
    }

    private void showEditMemoryDialog(MemoryEntry entry, boolean isWorking) {
        Dialog<MemoryEntry> dialog = new Dialog<>();
        dialog.setTitle("编辑记忆");
        dialog.setHeaderText(null);

        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialogPane.setPrefWidth(560);
        dialogPane.setPrefHeight(420);

        dialogPane.setStyle("-fx-background-color: #1e1e1e;");
        dialogPane.lookupButton(ButtonType.OK).setStyle(
            "-fx-background-color: #238636; -fx-text-fill: white; -fx-background-radius: 4; -fx-padding: 6 16;"
        );
        dialogPane.lookupButton(ButtonType.CANCEL).setStyle(
            "-fx-background-color: #30363d; -fx-text-fill: #c9d1d9; -fx-background-radius: 4; -fx-padding: 6 16;"
        );

        VBox content = new VBox(12);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: #1e1e1e;");

        Label idLabel = new Label("ID:");
        idLabel.setStyle("-fx-text-fill: #e6edf3; -fx-font-weight: bold;");
        TextField idField = new TextField(entry.getId());
        idField.setEditable(false);
        idField.setStyle(
            "-fx-control-inner-background: #21262d; " +
            "-fx-text-fill: #8b949e; " +
            "-fx-background-color: #30363d; " +
            "-fx-background-radius: 6; " +
            "-fx-border-color: #30363d; " +
            "-fx-border-radius: 6;"
        );

        Label contentLabel = new Label("内容:");
        contentLabel.setStyle("-fx-text-fill: #e6edf3; -fx-font-weight: bold;");
        TextArea contentArea = new TextArea(entry.getContent());
        contentArea.setWrapText(true);
        contentArea.setPrefHeight(180);
        contentArea.setStyle(
            "-fx-control-inner-background: #21262d; " +
            "-fx-text-fill: #e6edf3; " +
            "-fx-background-color: #30363d; " +
            "-fx-background-radius: 6; " +
            "-fx-border-color: #30363d; " +
            "-fx-border-radius: 6;"
        );

        Label tagsLabel = new Label("标签 (逗号分隔):");
        tagsLabel.setStyle("-fx-text-fill: #e6edf3; -fx-font-weight: bold;");
        TextField tagsField = new TextField(entry.getTags() != null ? String.join(", ", entry.getTags()) : "");
        tagsField.setPromptText("标签1, 标签2, 标签3");
        tagsField.setStyle(
            "-fx-control-inner-background: #21262d; " +
            "-fx-text-fill: #e6edf3; " +
            "-fx-prompt-text-fill: #6e7681; " +
            "-fx-background-color: #30363d; " +
            "-fx-background-radius: 6; " +
            "-fx-border-color: #30363d; " +
            "-fx-border-radius: 6;"
        );

        content.getChildren().addAll(
                idLabel, idField,
                contentLabel, contentArea,
                tagsLabel, tagsField
        );

        dialogPane.setContent(content);

        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                String newContent = contentArea.getText().trim();
                if (newContent.isEmpty()) {
                    showAlert("内容不能为空");
                    return null;
                }

                List<String> newTags = new ArrayList<>();
                String tagsText = tagsField.getText().trim();
                if (!tagsText.isEmpty()) {
                    for (String tag : tagsText.split(",")) {
                        String t = tag.trim();
                        if (!t.isEmpty()) newTags.add(t);
                    }
                }

                entry.setContent(newContent);
                entry.setTags(newTags);
                entry.setLastAccessedAt(java.time.LocalDateTime.now());

                return entry;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(updated -> {
            if (isWorking) {
                workingMemory.removeEntry(updated.getId());
                workingMemory.addEntry(updated);
                loadWorkingMemory();
            } else {
                memoryBackend.store(updated);
                loadPersistentMemory();
            }
            updateMemoryStats();
            showAlert("记忆已更新");
        });
    }

    private void loadBranches() {
        ObservableList<String> items = FXCollections.observableArrayList();
        if (branches == null || branches.isEmpty()) {
            items.add("无可用树枝");
        } else {
            for (RLBranch branch : branches) {
                items.add(branch.getBranchInfo() + " | " + branch.getType() + " | " + (branch.isActive() ? "活跃" : "停用"));
            }
        }
        branchListView.setItems(items);
    }

    @FXML
    public void handleAddBranch() {
        String name = branchNameField.getText().trim();
        String type = branchTypeField.getText().trim();
        if (name.isEmpty() || type.isEmpty()) {
            showAlert("请填写树枝名称和类型");
            return;
        }
        showAlert("树枝添加功能需要在服务端配置");
    }

    @FXML
    public void handleToggleBranch() {
        int idx = branchListView.getSelectionModel().getSelectedIndex();
        if (idx < 0 || branches == null || idx >= branches.size()) {
            showAlert("请先选择一条树枝");
            return;
        }
        RLBranch branch = branches.get(idx);
        branch.setActive(!branch.isActive());
        loadBranches();
        updateMemoryStats();
        showAlert(branch.getBranchInfo() + "已" + (branch.isActive() ? "激活" : "停用"));
    }

    @FXML
    public void handleDeleteBranch() {
        int idx = branchListView.getSelectionModel().getSelectedIndex();
        if (idx < 0 || branches == null || idx >= branches.size()) {
            showAlert("请先选择一条树枝");
            return;
        }
        showAlert("树枝删除功能需要在服务端配置");
    }

    private void loadContractClauses() {
        contractRulesContainer.getChildren().clear();
        ContractBook book = contractArbiter.getContractBook();
        if (book == null || book.getClauses() == null || book.getClauses().isEmpty()) {
            Label label = new Label("无契约规则");
            label.setStyle("-fx-text-fill: #858585;");
            contractRulesContainer.getChildren().add(label);
            return;
        }

        for (ContractClause clause : book.getClauses()) {
            VBox clauseBox = new VBox(5);
            clauseBox.setStyle("-fx-background-color: #252526; -fx-background-radius: 4; -fx-padding: 10; -fx-border-color: #3c3c3c; -fx-border-width: 1;");

            HBox header = new HBox();
            header.setSpacing(8);

            CheckBox enabledCheckBox = new CheckBox();
            enabledCheckBox.setSelected(clause.isEnabled());
            enabledCheckBox.setOnAction(e -> {
                contractArbiter.toggleClauseEnabled(clause.getId(), enabledCheckBox.isSelected());
            });

            Label nameLabel = new Label(clause.getName());
            nameLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #cccccc;");

            Region spacer = new Region();
            HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

            Button editBtn = new Button("编辑");
            editBtn.setStyle("-fx-background-color: #0e639c; -fx-text-fill: white; -fx-background-radius: 3; -fx-padding: 3 8; -fx-font-size: 11;");
            editBtn.setOnAction(e -> showEditClauseDialog(clause));

            Button deleteBtn = new Button("删除");
            deleteBtn.setStyle("-fx-background-color: #c93c37; -fx-text-fill: white; -fx-background-radius: 3; -fx-padding: 3 8; -fx-font-size: 11;");
            deleteBtn.setOnAction(e -> {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("确认删除");
                alert.setHeaderText(null);
                alert.setContentText("确定要删除规则 '" + clause.getName() + "' 吗？");
                alert.showAndWait().ifPresent(response -> {
                    if (response == javafx.scene.control.ButtonType.OK) {
                        contractArbiter.removeClause(clause.getId());
                        loadContractClauses();
                    }
                });
            });

            header.getChildren().addAll(enabledCheckBox, nameLabel, spacer, editBtn, deleteBtn);

            Label ruleLabel = new Label("规则: " + clause.getRule());
            ruleLabel.setStyle("-fx-text-fill: #d4d4d4; -fx-font-size: 12;");

            Label descriptionLabel = new Label("描述: " + clause.getDescription());
            descriptionLabel.setStyle("-fx-text-fill: #858585; -fx-font-size: 11;");

            clauseBox.getChildren().addAll(header, ruleLabel, descriptionLabel);
            contractRulesContainer.getChildren().add(clauseBox);
        }
    }

    @FXML
    public void handleAddContractRule() {
        showEditClauseDialog(null);
    }

    private void showEditClauseDialog(ContractClause existingClause) {
        Dialog<ContractClause> dialog = new Dialog<>();
        dialog.setTitle(existingClause == null ? "添加契约规则" : "编辑契约规则");
        dialog.setHeaderText(null);

        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialogPane.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        dialogPane.getStyleClass().add("custom-dialog");

        VBox content = new VBox(12);
        content.setPadding(new Insets(20));
        content.getStyleClass().add("dialog-body");

        TextField nameField = new TextField();
        nameField.setPromptText("规则名称");
        nameField.getStyleClass().add("form-field");

        TextField ruleField = new TextField();
        ruleField.setPromptText("规则内容");
        ruleField.getStyleClass().add("form-field");

        TextArea descriptionArea = new TextArea();
        descriptionArea.setPromptText("规则描述");
        descriptionArea.setPrefHeight(80);
        descriptionArea.getStyleClass().add("form-textarea");

        if (existingClause != null) {
            nameField.setText(existingClause.getName());
            ruleField.setText(existingClause.getRule());
            descriptionArea.setText(existingClause.getDescription());
        }

        HBox nameRow = new HBox(8);
        nameRow.getStyleClass().add("form-row");
        Label nameLabel = new Label("规则名称:");
        nameLabel.getStyleClass().add("form-label");
        nameRow.getChildren().addAll(nameLabel, nameField);

        HBox ruleRow = new HBox(8);
        ruleRow.getStyleClass().add("form-row");
        Label ruleLabel = new Label("规则内容:");
        ruleLabel.getStyleClass().add("form-label");
        ruleRow.getChildren().addAll(ruleLabel, ruleField);

        HBox descRow = new HBox(8);
        descRow.getStyleClass().add("form-row");
        Label descLabel = new Label("规则描述:");
        descLabel.getStyleClass().add("form-label");
        descRow.getChildren().addAll(descLabel, descriptionArea);
        descriptionArea.setMaxWidth(Double.MAX_VALUE);

        content.getChildren().addAll(nameRow, ruleRow, descRow);

        dialogPane.setContent(content);

        Button okBtn = (Button) dialogPane.lookupButton(ButtonType.OK);
        okBtn.getStyleClass().add("btn-dialog-ok");
        Button cancelBtn = (Button) dialogPane.lookupButton(ButtonType.CANCEL);
        cancelBtn.getStyleClass().add("btn-dialog-cancel");

        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                String name = nameField.getText().trim();
                String rule = ruleField.getText().trim();
                String description = descriptionArea.getText().trim();

                if (name.isEmpty() || rule.isEmpty()) {
                    showAlert("请填写规则名称和规则内容");
                    return null;
                }

                ContractClause clause = ContractClause.builder()
                        .id(existingClause != null ? existingClause.getId() : "c" + System.currentTimeMillis())
                        .name(name)
                        .rule(rule)
                        .description(description)
                        .failSafe(false)
                        .severity(0.5)
                        .enabled(existingClause != null ? existingClause.isEnabled() : true)
                        .build();

                if (existingClause != null) {
                    contractArbiter.updateClause(existingClause.getId(), clause);
                } else {
                    contractArbiter.addClause(clause);
                }

                return clause;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(clause -> {
            loadContractClauses();
        });
    }

    private void showAlert(String message) {
        Stage stage = new Stage();
        stage.setTitle("提示");
        stage.initStyle(StageStyle.UNDECORATED);

        VBox container = new VBox();
        container.getStyleClass().add("custom-alert");

        VBox content = new VBox();
        content.getStyleClass().add("alert-content");
        Label messageLabel = new Label(message);
        messageLabel.getStyleClass().add("alert-message");
        content.getChildren().add(messageLabel);

        HBox footer = new HBox();
        footer.setPadding(new Insets(12));
        footer.setAlignment(Pos.CENTER);

        Button okBtn = new Button("确定");
        okBtn.getStyleClass().add("btn-alert-ok");
        okBtn.setOnAction(e -> stage.close());
        footer.getChildren().add(okBtn);

        container.getChildren().addAll(content, footer);
        container.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());

        Scene scene = new Scene(container, 320, 140);
        stage.setScene(scene);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.showAndWait();
    }
}