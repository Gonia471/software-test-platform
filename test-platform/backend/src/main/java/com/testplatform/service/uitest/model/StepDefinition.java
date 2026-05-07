package com.testplatform.service.uitest.model;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
public class StepDefinition {

    /**
     * 前端生成的步骤 id，例如 step-123。
     */
    private String id;

    /**
     * 动作大类：browser / element / wait / assert / ai
     */
    private String type;

    /**
     * 具体动作 key：openPage / clickElement / sleep 等。
     */
    private String action;

    /**
     * 步骤描述（可选）。
     */
    private String description;

    /**
     * 参数对象，直接与前端的 parameters 对应。
     */
    private Map<String, Object> parameters = new HashMap<>();
}

