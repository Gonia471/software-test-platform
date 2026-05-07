package com.testplatform.service.uitest.handler;

import com.testplatform.service.uitest.model.ExecutionContext;
import com.testplatform.service.uitest.model.StepDefinition;
import com.testplatform.service.uitest.model.StepResult;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class StepDispatcher {

    private final List<StepHandler> handlers;

    public StepDispatcher(List<StepHandler> handlers) {
        this.handlers = handlers;
    }

    public StepResult dispatch(StepDefinition step, ExecutionContext ctx) throws Exception {
        for (StepHandler handler : handlers) {
            if (handler.supports(step)) {
                return handler.execute(step, ctx);
            }
        }
        throw new IllegalArgumentException("不支持的步骤类型: " + step.getType() + " / " + step.getAction());
    }
}

