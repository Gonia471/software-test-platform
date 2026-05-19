package com.testplatform.controller.apitest;

import com.testplatform.entity.apitest.ApiPrescript;
import com.testplatform.service.apitest.ApiPrescriptService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/api-test/prescripts")
@RequiredArgsConstructor
public class ApiPrescriptController {

    private final ApiPrescriptService prescriptService;

    @GetMapping("/collection/{collectionId}")
    public List<ApiPrescriptResponse> list(@PathVariable Long collectionId) {
        return prescriptService.getByCollectionId(collectionId).stream()
                .map(this::toResponse)
                .toList();
    }

    @PostMapping
    public ApiPrescriptResponse create(@RequestBody ApiPrescriptRequest request) {
        return toResponse(prescriptService.create(createPrescript(request), request.getCollectionId()));
    }

    @PutMapping("/{id}")
    public ApiPrescriptResponse update(@PathVariable Long id, @RequestBody ApiPrescriptRequest request) {
        return toResponse(prescriptService.update(id, createPrescript(request)));
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        prescriptService.delete(id);
    }

    @PostMapping("/collection/{collectionId}/batch")
    public List<ApiPrescriptResponse> saveAll(@PathVariable Long collectionId, @RequestBody List<ApiPrescriptRequest> requests) {
        List<ApiPrescript> prescripts = requests.stream().map(this::createPrescript).toList();
        return prescriptService.saveAll(collectionId, prescripts).stream()
                .map(this::toResponse)
                .toList();
    }

    private ApiPrescriptResponse toResponse(ApiPrescript prescript) {
        ApiPrescriptResponse response = new ApiPrescriptResponse();
        response.setId(prescript.getId());
        response.setCollectionId(prescript.getCollection() != null ? prescript.getCollection().getId() : null);
        response.setStepType(prescript.getStepType() != null ? prescript.getStepType().name() : null);
        response.setMethod(prescript.getMethod());
        response.setUrl(prescript.getUrl());
        response.setHeadersJson(prescript.getHeadersJson());
        response.setBodyJson(prescript.getBodyJson());
        response.setExtractParamsJson(prescript.getExtractParamsJson());
        response.setAssertionsJson(prescript.getAssertionsJson());
        response.setFunctionName(prescript.getFunctionName());
        response.setFunctionParamsJson(prescript.getFunctionParamsJson());
        response.setSetVariablesJson(prescript.getSetVariablesJson());
        response.setStopOnFail(prescript.getStopOnFail());
        response.setSortOrder(prescript.getSortOrder());
        return response;
    }

    private ApiPrescript createPrescript(ApiPrescriptRequest request) {
        ApiPrescript prescript = new ApiPrescript();
        prescript.setStepType(ApiPrescript.StepType.valueOf(request.getStepType()));
        prescript.setMethod(request.getMethod());
        prescript.setUrl(request.getUrl());
        prescript.setHeadersJson(request.getHeadersJson());
        prescript.setBodyJson(request.getBodyJson());
        prescript.setExtractParamsJson(request.getExtractParamsJson());
        prescript.setAssertionsJson(request.getAssertionsJson());
        prescript.setFunctionName(request.getFunctionName());
        prescript.setFunctionParamsJson(request.getFunctionParamsJson());
        prescript.setSetVariablesJson(request.getSetVariablesJson());
        prescript.setStopOnFail(request.getStopOnFail() != null ? request.getStopOnFail() : false);
        prescript.setSortOrder(request.getSortOrder());
        return prescript;
    }

    public static class ApiPrescriptRequest {
        private Long collectionId;
        private String stepType;
        private String method;
        private String url;
        private String headersJson;
        private String bodyJson;
        private String extractParamsJson;
        private String assertionsJson;
        private String functionName;
        private String functionParamsJson;
        private String setVariablesJson;
        private Boolean stopOnFail;
        private Integer sortOrder;

        public Long getCollectionId() { return collectionId; }
        public void setCollectionId(Long collectionId) { this.collectionId = collectionId; }
        public String getStepType() { return stepType; }
        public void setStepType(String stepType) { this.stepType = stepType; }
        public String getMethod() { return method; }
        public void setMethod(String method) { this.method = method; }
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public String getHeadersJson() { return headersJson; }
        public void setHeadersJson(String headersJson) { this.headersJson = headersJson; }
        public String getBodyJson() { return bodyJson; }
        public void setBodyJson(String bodyJson) { this.bodyJson = bodyJson; }
        public String getExtractParamsJson() { return extractParamsJson; }
        public void setExtractParamsJson(String extractParamsJson) { this.extractParamsJson = extractParamsJson; }
        public String getAssertionsJson() { return assertionsJson; }
        public void setAssertionsJson(String assertionsJson) { this.assertionsJson = assertionsJson; }
        public String getFunctionName() { return functionName; }
        public void setFunctionName(String functionName) { this.functionName = functionName; }
        public String getFunctionParamsJson() { return functionParamsJson; }
        public void setFunctionParamsJson(String functionParamsJson) { this.functionParamsJson = functionParamsJson; }
        public String getSetVariablesJson() { return setVariablesJson; }
        public void setSetVariablesJson(String setVariablesJson) { this.setVariablesJson = setVariablesJson; }
        public Boolean getStopOnFail() { return stopOnFail; }
        public void setStopOnFail(Boolean stopOnFail) { this.stopOnFail = stopOnFail; }
        public Integer getSortOrder() { return sortOrder; }
        public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    }

    public static class ApiPrescriptResponse {
        private Long id;
        private Long collectionId;
        private String stepType;
        private String method;
        private String url;
        private String headersJson;
        private String bodyJson;
        private String extractParamsJson;
        private String assertionsJson;
        private String functionName;
        private String functionParamsJson;
        private String setVariablesJson;
        private Boolean stopOnFail;
        private Integer sortOrder;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Long getCollectionId() { return collectionId; }
        public void setCollectionId(Long collectionId) { this.collectionId = collectionId; }
        public String getStepType() { return stepType; }
        public void setStepType(String stepType) { this.stepType = stepType; }
        public String getMethod() { return method; }
        public void setMethod(String method) { this.method = method; }
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public String getHeadersJson() { return headersJson; }
        public void setHeadersJson(String headersJson) { this.headersJson = headersJson; }
        public String getBodyJson() { return bodyJson; }
        public void setBodyJson(String bodyJson) { this.bodyJson = bodyJson; }
        public String getExtractParamsJson() { return extractParamsJson; }
        public void setExtractParamsJson(String extractParamsJson) { this.extractParamsJson = extractParamsJson; }
        public String getAssertionsJson() { return assertionsJson; }
        public void setAssertionsJson(String assertionsJson) { this.assertionsJson = assertionsJson; }
        public String getFunctionName() { return functionName; }
        public void setFunctionName(String functionName) { this.functionName = functionName; }
        public String getFunctionParamsJson() { return functionParamsJson; }
        public void setFunctionParamsJson(String functionParamsJson) { this.functionParamsJson = functionParamsJson; }
        public String getSetVariablesJson() { return setVariablesJson; }
        public void setSetVariablesJson(String setVariablesJson) { this.setVariablesJson = setVariablesJson; }
        public Boolean getStopOnFail() { return stopOnFail; }
        public void setStopOnFail(Boolean stopOnFail) { this.stopOnFail = stopOnFail; }
        public Integer getSortOrder() { return sortOrder; }
        public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    }
}
