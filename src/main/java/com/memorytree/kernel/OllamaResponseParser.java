package com.memorytree.kernel;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 解析 Ollama API 响应：提取生成文本、计数字段、生成 mock logits/token 列表。
 */
public class OllamaResponseParser {

    /**
     * 从响应 JSON 中提取 "response" 字段文本，处理常见转义字符。
     */
    public String extractResponseText(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        String marker = "\"response\":\"";
        int start = json.indexOf(marker);
        if (start < 0) {
            marker = "\"response\": \"";
            start = json.indexOf(marker);
        }
        if (start < 0) return null;

        start += marker.length();
        StringBuilder sb = new StringBuilder();
        boolean escaped = false;
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (escaped) {
                switch (c) {
                    case 'n' -> sb.append('\n');
                    case 't' -> sb.append('\t');
                    case 'r' -> sb.append('\r');
                    case '"' -> sb.append('"');
                    case '\\' -> sb.append('\\');
                    case '/' -> sb.append('/');
                    default -> sb.append(c);
                }
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else if (c == '"') {
                break;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * 从响应 JSON 中提取 long 类型字段（如 total_duration、eval_count）。
     */
    public long extractLongField(String json, String fieldName) {
        if (json == null || json.isEmpty()) {
            return 0;
        }
        String marker = "\"" + fieldName + "\":";
        int start = json.indexOf(marker);
        if (start < 0) return 0;
        start += marker.length();
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c >= '0' && c <= '9') {
                sb.append(c);
            } else if (sb.length() > 0) {
                break;
            }
        }
        try {
            return sb.length() > 0 ? Long.parseLong(sb.toString()) : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * 判断流式响应中某一行是否标记 done=true。
     */
    public boolean isStreamDone(String line) {
        return line != null && line.contains("\"done\":true");
    }

    /**
     * 简单分词（按空白切分）。Ollama API 未暴露真实 tokenizer。
     */
    public List<String> tokenize(String text) {
        if (text == null || text.isEmpty()) {
            return List.of();
        }
        return Arrays.asList(text.split("\\s+"));
    }

    /**
     * 按 token 数量生成 mock logits。Ollama API 不暴露真实 logits。
     */
    public Map<Integer, double[]> generateMockLogits(int tokenCount) {
        Map<Integer, double[]> logits = new HashMap<>();
        Random random = new Random();
        for (int i = 0; i < tokenCount; i++) {
            double[] tokenLogits = new double[50257];
            Arrays.fill(tokenLogits, -100);
            int tokenId = random.nextInt(50257);
            tokenLogits[tokenId] = 10;
            logits.put(i, tokenLogits);
        }
        return logits;
    }

    /**
     * 按 token 数量生成占位 token 列表（eval_count 已知时使用）。
     */
    public List<String> generateMockTokens(long tokenCount) {
        if (tokenCount <= 0) {
            return List.of();
        }
        return IntStream.range(0, (int) tokenCount)
                .mapToObj(i -> "tok_" + i)
                .collect(Collectors.toList());
    }
}
