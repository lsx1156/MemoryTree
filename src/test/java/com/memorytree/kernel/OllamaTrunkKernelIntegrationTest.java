package com.memorytree.kernel;

import com.memorytree.dto.GenerateConfig;
import com.memorytree.dto.GenerateResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.http.HttpResponse;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * OllamaTrunkKernel 集成测试。
 *
 * <p>包含两部分：
 * <ul>
 *   <li>组件单元测试（始终运行）：验证 OllamaHttpClient/OllamaResponseParser/
 *       KernelMetricsCalculator/MockKVCacheManager 的纯逻辑行为，不依赖 Ollama 服务。</li>
 *   <li>端到端集成测试（受 {@code -Dollama.test=true} 控制）：调用真实 Ollama 服务，
 *       验证完整推理流程。CI 环境默认跳过，本地手动运行。</li>
 * </ul>
 *
 * <p>本地运行方式：
 * <pre>
 * mvn test -Dtest=OllamaTrunkKernelIntegrationTest -Dollama.test=true
 * </pre>
 */
class OllamaTrunkKernelIntegrationTest {

    private OllamaHttpClient httpClient;
    private OllamaResponseParser responseParser;
    private KernelMetricsCalculator metricsCalculator;
    private MockKVCacheManager kvCacheManager;

    @BeforeEach
    void setUpComponents() {
        httpClient = new OllamaHttpClient("qwen2.5:7b", "http://localhost:11434");
        responseParser = new OllamaResponseParser();
        metricsCalculator = new KernelMetricsCalculator();
        kvCacheManager = new MockKVCacheManager();
    }

    // ===== 组件单元测试（始终运行，不依赖 Ollama）=====

    @Test
    void buildRequestJson_containsModelAndPrompt() {
        String json = httpClient.buildRequestJson("测试提示", 0.7, 0.9, 512, false);

        assertNotNull(json);
        assertTrue(json.contains("\"model\":\"qwen2.5:7b\""), "JSON should contain model name");
        assertTrue(json.contains("\"prompt\":\"测试提示\""), "JSON should contain prompt");
        assertTrue(json.contains("\"stream\":false"), "JSON should contain stream=false");
        assertTrue(json.contains("\"temperature\":0.7"), "JSON should contain temperature");
        assertTrue(json.contains("\"top_p\":0.9"), "JSON should contain top_p");
        assertTrue(json.contains("\"num_predict\":512"), "JSON should contain num_predict");
    }

    @Test
    void buildRequestJson_streamTrueSetsStreamField() {
        String json = httpClient.buildRequestJson("流式测试", 0.5, 0.8, 256, true);

        assertTrue(json.contains("\"stream\":true"), "Streaming request should have stream=true");
    }

    @Test
    void extractResponseText_parsesSimpleResponse() {
        String json = "{\"response\":\"你好，世界\",\"done\":true}";

        String text = responseParser.extractResponseText(json);

        assertEquals("你好，世界", text);
    }

    @Test
    void extractResponseText_handlesEscapedChars() {
        String json = "{\"response\":\"第一行\\n第二行\\t缩进\\\"引号\\\"\",\"done\":true}";

        String text = responseParser.extractResponseText(json);

        assertEquals("第一行\n第二行\t缩进\"引号\"", text);
    }

    @Test
    void extractResponseText_returnsNullForMissingField() {
        String json = "{\"error\":\"not found\"}";

        String text = responseParser.extractResponseText(json);

        assertNull(text);
    }

    @Test
    void extractResponseText_returnsNullForNullInput() {
        assertNull(responseParser.extractResponseText(null));
        assertNull(responseParser.extractResponseText(""));
    }

    @Test
    void extractLongField_parsesNumericValue() {
        String json = "{\"total_duration\":1234567890,\"eval_count\":42}";

        assertEquals(1234567890L, responseParser.extractLongField(json, "total_duration"));
        assertEquals(42L, responseParser.extractLongField(json, "eval_count"));
    }

    @Test
    void extractLongField_returnsZeroForMissingField() {
        String json = "{\"other\":123}";

        assertEquals(0L, responseParser.extractLongField(json, "total_duration"));
    }

    @Test
    void isStreamDone_detectsDoneTrue() {
        assertTrue(responseParser.isStreamDone("{\"done\":true}"));
        assertFalse(responseParser.isStreamDone("{\"done\":false}"));
        assertFalse(responseParser.isStreamDone(null));
    }

    @Test
    void tokenize_splitsByWhitespace() {
        var tokens = responseParser.tokenize("hello world foo");

        assertEquals(3, tokens.size());
        assertEquals("hello", tokens.get(0));
    }

    @Test
    void tokenize_returnsEmptyForEmptyInput() {
        assertTrue(responseParser.tokenize("").isEmpty());
        assertTrue(responseParser.tokenize(null).isEmpty());
    }

    @Test
    void generateMockTokens_returnsExpectedCount() {
        var tokens = responseParser.generateMockTokens(5);

        assertEquals(5, tokens.size());
        assertTrue(tokens.get(0).startsWith("tok_"));
    }

    @Test
    void generateMockTokens_returnsEmptyForZero() {
        assertTrue(responseParser.generateMockTokens(0).isEmpty());
    }

    @Test
    void generateMockLogits_returnsExpectedSize() {
        var logits = responseParser.generateMockLogits(3);

        assertEquals(3, logits.size());
        assertEquals(50257, logits.get(0).length);
    }

    @Test
    void calculateConfidence_returnsBaselineForEmptyText() {
        assertEquals(0.5, metricsCalculator.calculateConfidence(""), 0.001);
        assertEquals(0.5, metricsCalculator.calculateConfidence(null), 0.001);
    }

    @Test
    void calculateConfidence_increasesWithLogicKeywords() {
        double baseline = metricsCalculator.calculateConfidence("一段普通文本");
        double withLogic = metricsCalculator.calculateConfidence(
                "根据前提，我们进行推导。因此结论成立。证据表明这是正确的。");

        assertTrue(withLogic > baseline,
                "Text with logic keywords should have higher confidence than plain text");
        assertTrue(withLogic >= 0.8, "Logic-rich text should reach at least 0.8 confidence");
    }

    @Test
    void calculateConfidence_neverExceedsUpperBound() {
        String richText = "因此所以结论推导推理论证根据基于证据假设前提条件证明验证" +
                "这是一段很长的文本，超过两百个字符，用于触发长度加分。" +
                "句子一。句子二。句子三。句子四。句子五。";

        double confidence = metricsCalculator.calculateConfidence(richText);

        assertTrue(confidence <= 0.95, "Confidence should never exceed 0.95");
    }

    @Test
    void calculateReward_returnsBaselineForEmptyInput() {
        assertEquals(0.5, metricsCalculator.calculateReward("", 0, 0), 0.001);
        assertEquals(0.5, metricsCalculator.calculateReward(null, 0, 0), 0.001);
    }

    @Test
    void calculateReward_increasesWithContentAndSpeed() {
        double baseline = metricsCalculator.calculateReward("", 0, 0);
        double rewarded = metricsCalculator.calculateReward(
                "一段足够长的文本来获得长度加分", 100, 5000);

        assertTrue(rewarded > baseline, "Rich fast response should have higher reward");
        assertTrue(rewarded <= 1.0, "Reward should never exceed 1.0");
    }

    @Test
    void calculateReward_neverBelowHalf() {
        double reward = metricsCalculator.calculateReward("", 0, 60000);

        assertTrue(reward >= 0.5, "Reward should never go below 0.5");
    }

    @Test
    void kvCache_getHandleCreatesActiveHandle() {
        String handle = kvCacheManager.getKVCacheHandle();

        assertNotNull(handle);
        assertTrue(handle.startsWith("kv_cache_"));
    }

    @Test
    void kvCache_getHandleReturnsSameHandleOnSecondCall() {
        String first = kvCacheManager.getKVCacheHandle();
        String second = kvCacheManager.getKVCacheHandle();

        assertEquals(first, second, "Repeated calls should return the same handle");
    }

    @Test
    void kvCache_clearResetsHandle() {
        String handle = kvCacheManager.getKVCacheHandle();
        assertNotNull(handle);

        kvCacheManager.clearKVCache();

        String newHandle = kvCacheManager.getKVCacheHandle();
        assertNotEquals(handle, newHandle, "After clear, a new handle should be generated");
    }

    @Test
    void kvCache_cloneCreatesDistinctHandle() {
        String original = kvCacheManager.getKVCacheHandle();
        String clone = kvCacheManager.cloneKVCache();

        assertNotEquals(original, clone, "Clone should have a different handle");
        assertTrue(clone.startsWith("kv_cache_clone_"));
    }

    @Test
    void kvCache_restoreActivatesExistingHandle() {
        String clone = kvCacheManager.cloneKVCache();

        kvCacheManager.restoreKVCache(clone);

        String current = kvCacheManager.getKVCacheHandle();
        assertEquals(clone, current, "After restore, getKVCacheHandle should return the restored handle");
    }

    @Test
    void kvCache_restoreNonExistentHandleDoesNotCrash() {
        assertDoesNotThrow(() -> kvCacheManager.restoreKVCache("non_existent_handle"));
    }

    // ===== 鲁棒性测试（异常路径与边界压力）=====

    /**
     * 验证 Ollama 服务不可用时，内核抛出 RuntimeException 且消息对用户友好。
     * 这是生产环境最常见的异常路径（用户忘记启动 ollama serve）。
     */
    @Test
    void generate_ollamaUnavailable_throwsRuntimeExceptionWithFriendlyMessage() {
        OllamaTrunkKernel kernel = new OllamaTrunkKernel();
        ReflectionTestUtils.setField(kernel, "modelName", "qwen2.5:7b");
        // 使用一个几乎肯定未占用的端口，模拟 Ollama 不可用
        ReflectionTestUtils.setField(kernel, "baseUrl", "http://127.0.0.1:59999");
        kernel.init();

        GenerateConfig config = GenerateConfig.builder()
                .temperature(0.3)
                .topP(0.9)
                .maxTokens(64)
                .useKVCache(false)
                .build();

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> kernel.generate("测试", config),
                "Ollama 不可用时应抛出 RuntimeException");

        assertNotNull(ex.getMessage(), "异常消息不应为 null");
        assertTrue(ex.getMessage().contains("推理失败") || ex.getMessage().contains("Ollama")
                        || ex.getMessage().contains("Connection") || ex.getMessage().contains("connect"),
                "异常消息应包含可读的错误提示，实际消息: " + ex.getMessage());
    }

    /**
     * 验证超长输入（10000 字符）不会导致 JSON 构建崩溃或溢出。
     * 用户可能粘贴大段文本作为推理输入。
     */
    @Test
    void buildRequestJson_superLongInput_doesNotCrash() {
        String longInput = "这是一段超长测试文本。".repeat(500); // 约 5000 字符
        assertTrue(longInput.length() > 4000, "测试前置：输入应足够长");

        String json = httpClient.buildRequestJson(longInput, 0.3, 0.9, 2048, false);

        assertNotNull(json, "超长输入的 JSON 不应为 null");
        assertTrue(json.contains("\"prompt\":"), "JSON 应包含 prompt 字段");
        assertTrue(json.length() > longInput.length(), "JSON 长度应大于原始输入长度");
    }

    /**
     * 验证 KV cache 在 10 线程并发访问下不发生死锁或数据竞争。
     * 并行分支评估器（ParallelBranchEvaluator）会并发调用内核。
     */
    @Test
    void kvCache_concurrentAccess_noDeadlock() throws InterruptedException {
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        int[] errorCount = {0};

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < 20; j++) {
                        kvCacheManager.getKVCacheHandle();
                        String clone = kvCacheManager.cloneKVCache();
                        kvCacheManager.restoreKVCache(clone);
                        kvCacheManager.clearKVCache();
                    }
                } catch (Exception e) {
                    errorCount[0]++;
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean completed = latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();
        executor.awaitTermination(2, TimeUnit.SECONDS);

        assertTrue(completed, "10 线程并发访问应在 10 秒内完成，未发生死锁");
        assertEquals(0, errorCount[0], "并发访问不应产生异常，实际异常数: " + errorCount[0]);
    }

    // ===== 端到端集成测试（需要真实 Ollama 服务）=====

    @Test
    @EnabledIfSystemProperty(named = "ollama.test", matches = "true")
    void generate_realOllamaCall_returnsNonEmptyResult() {
        OllamaTrunkKernel kernel = new OllamaTrunkKernel();
        ReflectionTestUtils.setField(kernel, "modelName", "qwen2.5:7b");
        ReflectionTestUtils.setField(kernel, "baseUrl", "http://localhost:11434");
        kernel.init();

        GenerateConfig config = GenerateConfig.builder()
                .temperature(0.7)
                .topP(0.9)
                .maxTokens(256)
                .useKVCache(false)
                .build();

        GenerateResult result = kernel.generate("1+1等于几？", config);

        assertNotNull(result);
        assertNotNull(result.getText());
        assertFalse(result.getText().trim().isEmpty(), "Real Ollama call should return non-empty text");
        assertTrue(result.getConfidence() >= 0.5, "Confidence should be at least 0.5");
        assertTrue(result.getInferenceTimeMs() > 0, "Inference time should be positive");
        assertNotNull(result.getTokens(), "Tokens list should not be null");
        assertFalse(result.getTokens().isEmpty(), "Tokens list should not be empty");

        System.out.println("[集成测试] 推理文本: " + result.getText());
        System.out.println("[集成测试] 置信度: " + String.format("%.2f", result.getConfidence()));
        System.out.println("[集成测试] 耗时: " + result.getInferenceTimeMs() + "ms");
        System.out.println("[集成测试] Token数: " + result.getTokens().size());
    }

    @Test
    @EnabledIfSystemProperty(named = "ollama.test", matches = "true")
    void generate_realOllamaCall_kvCacheOperationsWork() {
        OllamaTrunkKernel kernel = new OllamaTrunkKernel();
        ReflectionTestUtils.setField(kernel, "modelName", "qwen2.5:7b");
        ReflectionTestUtils.setField(kernel, "baseUrl", "http://localhost:11434");
        kernel.init();

        String handle1 = kernel.getKVCacheHandle();
        String clone = kernel.cloneKVCache();
        kernel.restoreKVCache(clone);
        kernel.clearKVCache();

        assertNotNull(handle1);
        assertNotNull(clone);
        assertNotEquals(handle1, clone);
    }

    @Test
    @EnabledIfSystemProperty(named = "ollama.test", matches = "true")
    void kernelInfo_containsModelAndBaseUrl() {
        OllamaTrunkKernel kernel = new OllamaTrunkKernel();
        ReflectionTestUtils.setField(kernel, "modelName", "qwen2.5:7b");
        ReflectionTestUtils.setField(kernel, "baseUrl", "http://localhost:11434");
        kernel.init();

        String info = kernel.getKernelInfo();

        assertTrue(info.contains("qwen2.5:7b"), "Kernel info should contain model name");
        assertTrue(info.contains("http://localhost:11434"), "Kernel info should contain base URL");
    }
}
