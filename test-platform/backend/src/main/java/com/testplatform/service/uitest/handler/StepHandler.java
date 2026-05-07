package com.testplatform.service.uitest.handler;

import com.testplatform.service.uitest.model.ExecutionContext;
import com.testplatform.service.uitest.model.StepDefinition;
import com.testplatform.service.uitest.model.StepResult;

public interface StepHandler {

    boolean supports(StepDefinition step);

    StepResult execute(StepDefinition step, ExecutionContext ctx) throws Exception;
}

