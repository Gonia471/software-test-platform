package com.testplatform.service.apitest;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class ExecutionContext {

    private Map<String, String> variables = new HashMap<>();
    private long executionId;
    private boolean stopped = false;

    public void setExecutionId(long executionId) {
        this.executionId = executionId;
        this.variables.clear();
        this.stopped = false;
        log.debug("创建执行上下文: {}", executionId);
    }

    public void setVariable(String name, String value) {
        variables.put(name, value);
        log.debug("设置变量: {} = {}", name, value);
    }

    public String getVariable(String name) {
        return variables.get(name);
    }

    public String replaceVariables(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        String result = text;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            if (result.contains(placeholder)) {
                result = result.replace(placeholder, entry.getValue() != null ? entry.getValue() : "");
            }
        }
        return result;
    }

    public Map<String, String> getAllVariables() {
        return new HashMap<>(variables);
    }

    public void stop() {
        this.stopped = true;
        log.debug("执行上下文停止: {}", executionId);
    }

    public boolean isStopped() {
        return stopped;
    }

    public void reset() {
        variables.clear();
        stopped = false;
    }

    public void mergeVariables(Map<String, String> newVariables) {
        if (newVariables != null) {
            variables.putAll(newVariables);
            log.debug("合并变量: {}", newVariables);
        }
    }

    public String toDebugString() {
        return "ExecutionContext{" +
                "executionId=" + executionId +
                ", variables=" + variables +
                ", stopped=" + stopped +
                '}';
    }
}
