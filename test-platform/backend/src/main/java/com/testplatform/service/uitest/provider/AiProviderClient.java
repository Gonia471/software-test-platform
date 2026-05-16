package com.testplatform.service.uitest.provider;

import com.testplatform.service.uitest.model.AiPlannedAction;
import com.testplatform.service.uitest.model.AiPlanningRequest;

public interface AiProviderClient {

    String getProviderName();

    boolean isAvailable();

    AiPlannedAction plan(AiPlanningRequest request) throws Exception;
}
