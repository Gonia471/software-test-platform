package com.testplatform.service.apitest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.testplatform.entity.apitest.*;
import com.testplatform.entity.User;
import com.testplatform.repository.apitest.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApiTestExecutionEngine {

    private final ApiCollectionRepository collectionRepository;
    private final ApiAssertionRepository assertionRepository;
    private final ApiPrescriptRepository prescriptRepository;
    private final ScriptLibraryRepository scriptRepository;
    private final ApiTestExecutionRepository executionRepository;
    private final ObjectMapper objectMapper;
    private final ExecutionContext executionContext;
    private final AssertionService assertionService;
    private final PrescriptService prescriptService;
    private final PythonScriptService pythonScriptService;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Transactional
    public ExecutionResult execute(Long collectionId, User user) {
        return execute(collectionId, user, null);
    }

    @Transactional
    public ExecutionResult execute(Long collectionId, User user, Long projectId) {
        ExecutionResult result = new ExecutionResult();
        long startTime = System.currentTimeMillis();

        try {
            ApiCollection collection = collectionRepository.findById(collectionId)
                    .orElseThrow(() -> new IllegalArgumentException("用例不存在: " + collectionId));

            if (collection.getNodeType() != ApiCollection.NodeType.CASE) {
                throw new IllegalArgumentException("只能执行接口用例，不能执行文件夹");
            }

            executionContext.setExecutionId(System.currentTimeMillis());
            result.setCollectionId(collectionId);
            result.setCollectionName(collection.getName());

            List<ExecutionStep> steps = new ArrayList<>();

            List<ApiPrescript> prescripts = prescriptRepository.findByCollectionIdOrderBySortOrderAsc(collectionId);
            for (ApiPrescript prescript : prescripts) {
                if (executionContext.isStopped()) {
                    break;
                }
                ExecutionStep step = executePrescript(prescript);
                steps.add(step);
                if (!step.isSuccess() && Boolean.TRUE.equals(prescript.getStopOnFail())) {
                    executionContext.stop();
                }
            }

            ExecutionStep mainStep = executeMainRequest(collection);
            steps.add(mainStep);
            result.setMainStep(mainStep);

            if (mainStep.isSuccess() && !executionContext.isStopped()) {
                List<AssertionService.ApiAssertionResult> assertions = 
                        assertionService.executeAssertions(collectionId, mainStep.getResponse());
                mainStep.setAssertions(assertions);
                result.setAssertions(assertions);

                boolean allPassed = assertions.stream().allMatch(AssertionService.ApiAssertionResult::isPassed);
                result.setStatus(allPassed ? "SUCCESS" : "FAILED");
                result.setSuccess(allPassed);

                if (!allPassed && Boolean.TRUE.equals(collection.getStopOnFail())) {
                    result.setStatus("FAILED");
                    result.setSuccess(false);
                }
            } else {
                result.setStatus("ERROR");
                result.setSuccess(false);
            }

            ApiTestExecution execution = saveExecution(collection, result, user, projectId);
            result.setExecutionId(execution.getId());

        } catch (Exception e) {
            log.error("执行失败: collectionId={}", collectionId, e);
            result.setStatus("ERROR");
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
        }

        result.setDuration(System.currentTimeMillis() - startTime);
        executionContext.reset();
        return result;
    }

    private ApiTestExecution saveExecution(ApiCollection collection, ExecutionResult result, User user, Long projectId) {
        ApiTestExecution execution = new ApiTestExecution();
        execution.setUser(user);
        execution.setProjectId(projectId);
        execution.setCollectionId(String.valueOf(collection.getId()));
        execution.setCollectionName(collection.getName());
        execution.setStatus("SUCCESS".equals(result.getStatus()) ? 
                ApiTestExecution.TestStatus.SUCCESS : 
                "ERROR".equals(result.getStatus()) ? 
                        ApiTestExecution.TestStatus.ERROR : 
                        ApiTestExecution.TestStatus.FAILED);
        execution.setDuration((int) result.getDuration());
        execution.setHttpStatus(result.getMainStep() != null ? result.getMainStep().getStatus() : 0);

        try {
            execution.setRequestJson(objectMapper.writeValueAsString(result.getMainStep()));
            execution.setResponseJson(objectMapper.writeValueAsString(result.getMainStep()));
            execution.setAssertionsJson(objectMapper.writeValueAsString(result.getAssertions()));
        } catch (Exception ignored) {}

        return executionRepository.save(execution);
    }

    private ExecutionStep executePrescript(ApiPrescript prescript) {
        ExecutionStep step = new ExecutionStep();
        step.setStepType(prescript.getStepType().name());
        step.setSortOrder(prescript.getSortOrder());

        if (prescript.getStepType() == ApiPrescript.StepType.HTTP) {
            step.setMethod(prescript.getMethod());
            step.setUrl(prescript.getUrl());
            PrescriptService.PrescriptResult presult = prescriptService.executeHttpRequest(prescript, executionContext.getAllVariables());
            step.setStatus(presult.getStatus());
            step.setBody(presult.getBody());
            step.setHeaders(presult.getHeaders());
            step.setDuration(presult.getDuration());
            step.setSuccess(presult.isSuccess());
            step.setErrorMessage(presult.getErrorMessage());

            if (presult.isSuccess() && presult.getExtractedVariables() != null) {
                step.setExtractedVariables(presult.getExtractedVariables());
                executionContext.mergeVariables(presult.getExtractedVariables());
            }
        } else if (prescript.getStepType() == ApiPrescript.StepType.FUNCTION) {
            step.setFunctionName(prescript.getFunctionName());

            List<String> params = parseFunctionParams(prescript.getFunctionParamsJson());
            List<String> resolvedParams = params.stream()
                    .map(p -> executionContext.replaceVariables(p))
                    .toList();

            ScriptLibrary script = scriptRepository.findByFunctionName(prescript.getFunctionName())
                    .orElse(null);

            if (script != null) {
                PythonScriptService.FunctionCallResult fresult = 
                        pythonScriptService.executeFunction(script, resolvedParams, executionContext.getAllVariables());
                step.setSuccess(fresult.isSuccess());
                step.setOutput(fresult.getOutput());
                step.setErrorMessage(fresult.getErrorMessage());

                if (fresult.isSuccess() && fresult.getOutputParams() != null) {
                    step.setExtractedVariables(fresult.getOutputParams());
                    executionContext.mergeVariables(fresult.getOutputParams());
                }
            } else {
                step.setSuccess(false);
                step.setErrorMessage("函数不存在: " + prescript.getFunctionName());
            }
        }

        return step;
    }

    private ExecutionStep executeMainRequest(ApiCollection collection) {
        ExecutionStep step = new ExecutionStep();
        step.setStepType("MAIN");
        step.setMethod(collection.getMethod());
        step.setUrl(collection.getUrl());

        try {
            Map<String, String> headers = parseJson(collection.getHeadersJson(), new TypeReference<>() {});
            String url = collection.getUrl();
            String body = collection.getBodyRaw();

            url = executionContext.replaceVariables(url);
            if (body != null) {
                body = executionContext.replaceVariables(body);
            }

            Map<String, String> resolvedHeaders = new HashMap<>();
            if (headers != null) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    resolvedHeaders.put(entry.getKey(), executionContext.replaceVariables(entry.getValue()));
                }
            }

            String method = collection.getMethod() != null ? collection.getMethod() : "GET";

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(60));

            for (Map.Entry<String, String> entry : resolvedHeaders.entrySet()) {
                requestBuilder.header(entry.getKey(), entry.getValue());
            }

            if ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method) || "PATCH".equalsIgnoreCase(method)) {
                if (body != null && !body.isEmpty()) {
                    requestBuilder.method(method, HttpRequest.BodyPublishers.ofString(body));
                } else {
                    requestBuilder.method(method, HttpRequest.BodyPublishers.noBody());
                }
            } else {
                requestBuilder.method(method, HttpRequest.BodyPublishers.noBody());
            }

            long start = System.currentTimeMillis();
            HttpResponse<String> response = httpClient.send(
                    requestBuilder.build(),
                    HttpResponse.BodyHandlers.ofString()
            );
            long duration = System.currentTimeMillis() - start;

            AssertionService.ApiResponse apiResponse = new AssertionService.ApiResponse();
            apiResponse.setStatus(response.statusCode());
            apiResponse.setBody(response.body());
            apiResponse.setDuration(duration);

            Map<String, String> respHeaders = new HashMap<>();
            response.headers().map().forEach((key, values) -> 
                    respHeaders.put(key, String.join(", ", values)));
            apiResponse.setHeaders(respHeaders);

            step.setStatus(response.statusCode());
            step.setBody(response.body());
            step.setHeaders(respHeaders);
            step.setDuration(duration);
            step.setSuccess(response.statusCode() >= 200 && response.statusCode() < 300);
            step.setResponse(apiResponse);

        } catch (Exception e) {
            step.setSuccess(false);
            step.setErrorMessage(e.getMessage());
            log.error("主请求执行失败", e);
        }

        return step;
    }

    private ApiTestExecution saveExecution(ApiCollection collection, ExecutionResult result, User user) {
        ApiTestExecution execution = new ApiTestExecution();
        execution.setUser(user);
        execution.setCollectionId(String.valueOf(collection.getId()));
        execution.setCollectionName(collection.getName());
        execution.setStatus("SUCCESS".equals(result.getStatus()) ? 
                ApiTestExecution.TestStatus.SUCCESS : 
                "ERROR".equals(result.getStatus()) ? 
                        ApiTestExecution.TestStatus.ERROR : 
                        ApiTestExecution.TestStatus.FAILED);
        execution.setDuration((int) result.getDuration());
        execution.setHttpStatus(result.getMainStep() != null ? result.getMainStep().getStatus() : 0);

        try {
            execution.setRequestJson(objectMapper.writeValueAsString(result.getMainStep()));
            execution.setResponseJson(objectMapper.writeValueAsString(result.getMainStep()));
            execution.setAssertionsJson(objectMapper.writeValueAsString(result.getAssertions()));
        } catch (Exception ignored) {}

        return executionRepository.save(execution);
    }

    private List<String> parseFunctionParams(String json) {
        try {
            List<String> params = objectMapper.readValue(json, new TypeReference<>() {});
            return params != null ? params : new ArrayList<>();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private <T> T parseJson(String json, TypeReference<T> typeRef) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, typeRef);
        } catch (Exception e) {
            return null;
        }
    }

    public static class ExecutionResult {
        private Long executionId;
        private Long collectionId;
        private String collectionName;
        private String status;
        private boolean success;
        private long duration;
        private String errorMessage;
        private List<ExecutionStep> steps;
        private ExecutionStep mainStep;
        private List<AssertionService.ApiAssertionResult> assertions;

        public Long getExecutionId() { return executionId; }
        public void setExecutionId(Long executionId) { this.executionId = executionId; }
        public Long getCollectionId() { return collectionId; }
        public void setCollectionId(Long collectionId) { this.collectionId = collectionId; }
        public String getCollectionName() { return collectionName; }
        public void setCollectionName(String collectionName) { this.collectionName = collectionName; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public long getDuration() { return duration; }
        public void setDuration(long duration) { this.duration = duration; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        public List<ExecutionStep> getSteps() { return steps; }
        public void setSteps(List<ExecutionStep> steps) { this.steps = steps; }
        public ExecutionStep getMainStep() { return mainStep; }
        public void setMainStep(ExecutionStep mainStep) { this.mainStep = mainStep; }
        public List<AssertionService.ApiAssertionResult> getAssertions() { return assertions; }
        public void setAssertions(List<AssertionService.ApiAssertionResult> assertions) { this.assertions = assertions; }
    }

    public static class ExecutionStep {
        private String stepType;
        private String method;
        private String url;
        private String functionName;
        private int status;
        private String body;
        private long duration;
        private boolean success;
        private String errorMessage;
        private String output;
        private Map<String, String> headers;
        private Map<String, String> extractedVariables;
        private List<AssertionService.ApiAssertionResult> assertions;
        private AssertionService.ApiResponse response;
        private int sortOrder;

        public String getStepType() { return stepType; }
        public void setStepType(String stepType) { this.stepType = stepType; }
        public String getMethod() { return method; }
        public void setMethod(String method) { this.method = method; }
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public String getFunctionName() { return functionName; }
        public void setFunctionName(String functionName) { this.functionName = functionName; }
        public int getStatus() { return status; }
        public void setStatus(int status) { this.status = status; }
        public String getBody() { return body; }
        public void setBody(String body) { this.body = body; }
        public long getDuration() { return duration; }
        public void setDuration(long duration) { this.duration = duration; }
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        public String getOutput() { return output; }
        public void setOutput(String output) { this.output = output; }
        public Map<String, String> getHeaders() { return headers; }
        public void setHeaders(Map<String, String> headers) { this.headers = headers; }
        public Map<String, String> getExtractedVariables() { return extractedVariables; }
        public void setExtractedVariables(Map<String, String> extractedVariables) { this.extractedVariables = extractedVariables; }
        public List<AssertionService.ApiAssertionResult> getAssertions() { return assertions; }
        public void setAssertions(List<AssertionService.ApiAssertionResult> assertions) { this.assertions = assertions; }
        public AssertionService.ApiResponse getResponse() { return response; }
        public void setResponse(AssertionService.ApiResponse response) { this.response = response; }
        public int getSortOrder() { return sortOrder; }
        public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
    }
}
