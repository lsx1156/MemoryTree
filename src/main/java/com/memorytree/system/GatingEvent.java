package com.memorytree.system;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GatingEvent {
    private String type;
    private String description;
    private String detail;
    private double threshold;
    private double actualValue;
    private boolean passed;
    private long timestamp;

    public static GatingEvent tokenLevel(String detail, double threshold, double actualValue, boolean passed) {
        return GatingEvent.builder()
                .type("TOKEN_LEVEL")
                .description("Token级修正")
                .detail(detail)
                .threshold(threshold)
                .actualValue(actualValue)
                .passed(passed)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    public static GatingEvent logicThreshold(String detail, double threshold, double actualValue, boolean passed) {
        return GatingEvent.builder()
                .type("LOGIC_THRESHOLD")
                .description("逻辑阈值触发")
                .detail(detail)
                .threshold(threshold)
                .actualValue(actualValue)
                .passed(passed)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    public static GatingEvent paragraphRewrite(String detail) {
        return GatingEvent.builder()
                .type("PARAGRAPH_REWRITE")
                .description("段落重写记录")
                .detail(detail)
                .passed(true)
                .timestamp(System.currentTimeMillis())
                .build();
    }
}