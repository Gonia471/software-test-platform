package com.testplatform.service.uitest.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AiPlannedAction {

    /**
     * 动作类型: click / input / wait / unknown
     */
    private String actionType = "unknown";

    /**
     * 定位类型: css / xpath / id / name / linkText
     */
    private String locatorType;

    /**
     * 定位表达式
     */
    private String locatorValue;

    /**
     * 视口坐标（可选）
     */
    private Integer x;
    private Integer y;

    /**
     * 输入类动作文本
     */
    private String text;

    /**
     * AI 置信度（可选）
     */
    private Double confidence;

    /**
     * 说明信息（可选）
     */
    private String reason;
}
