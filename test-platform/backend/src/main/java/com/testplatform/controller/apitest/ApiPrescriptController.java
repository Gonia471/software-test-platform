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
    public List<ApiPrescript> list(@PathVariable Long collectionId) {
        return prescriptService.getByCollectionId(collectionId);
    }

    @PostMapping
    public ApiPrescript create(@RequestBody ApiPrescriptRequest request) {
        return prescriptService.create(createPrescript(request), request.getCollectionId());
    }

    @PutMapping("/{id}")
    public ApiPrescript update(@PathVariable Long id, @RequestBody ApiPrescriptRequest request) {
        return prescriptService.update(id, createPrescript(request));
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        prescriptService.delete(id);
    }

    @PostMapping("/collection/{collectionId}/batch")
    public void saveAll(@PathVariable Long collectionId, @RequestBody List<ApiPrescriptRequest> requests) {
        List<ApiPrescript> prescripts = requests.stream().map(this::createPrescript).toList();
        prescriptService.saveAll(collectionId, prescripts);
    }

    private ApiPrescript createPrescript(ApiPrescriptRequest request) {
        ApiPrescript prescript = new ApiPrescript();
        prescript.setStepType(ApiPrescript.StepType.valueOf(request.getStepType()));
        prescript.setMethod(request.getMethod());
        prescript.setUrl(request.getUrl());
        prescript.setHeadersJson(request.getHeadersJson());
        prescript.setBodyJson(request.getBodyJson());
        prescript.setExtractParamsJson(request.getExtractParamsJson());
        prescript.setFunctionName(request.getFunctionName());
        prescript.setFunctionParamsJson(request.getFunctionParamsJson());
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
        private String functionName;
        private String functionParamsJson;
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
        public String getFunctionName() { return functionName; }
        public void setFunctionName(String functionName) { this.functionName = functionName; }
        public String getFunctionParamsJson() { return functionParamsJson; }
        public void setFunctionParamsJson(String functionParamsJson) { this.functionParamsJson = functionParamsJson; }
        public Boolean getStopOnFail() { return stopOnFail; }
        public void setStopOnFail(Boolean stopOnFail) { this.stopOnFail = stopOnFail; }
        public Integer getSortOrder() { return sortOrder; }
        public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    }
}