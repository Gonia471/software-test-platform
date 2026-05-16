package com.testplatform.controller.apitest;

import com.testplatform.config.UserPrincipal;
import com.testplatform.entity.apitest.ApiAssertion;
import com.testplatform.service.apitest.ApiAssertionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/api-test/assertions")
@RequiredArgsConstructor
public class ApiAssertionController {

    private final ApiAssertionService assertionService;

    @GetMapping("/collection/{collectionId}")
    public List<ApiAssertion> list(@PathVariable Long collectionId) {
        return assertionService.getByCollectionId(collectionId);
    }

    @PostMapping
    public ApiAssertion create(@RequestBody ApiAssertionRequest request) {
        return assertionService.create(createAssertion(request), request.getCollectionId());
    }

    @PutMapping("/{id}")
    public ApiAssertion update(@PathVariable Long id, @RequestBody ApiAssertionRequest request) {
        return assertionService.update(id, createAssertion(request));
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        assertionService.delete(id);
    }

    @PostMapping("/collection/{collectionId}/batch")
    public void saveAll(@PathVariable Long collectionId, @RequestBody List<ApiAssertionRequest> requests) {
        List<ApiAssertion> assertions = requests.stream().map(this::createAssertion).toList();
        assertionService.saveAll(collectionId, assertions);
    }

    private ApiAssertion createAssertion(ApiAssertionRequest request) {
        ApiAssertion assertion = new ApiAssertion();
        assertion.setAssertionType(ApiAssertion.AssertionType.valueOf(request.getAssertionType()));
        assertion.setExpression(request.getExpression());
        assertion.setExpected(request.getExpected());
        assertion.setEnabled(request.getEnabled() != null ? request.getEnabled() : true);
        assertion.setSortOrder(request.getSortOrder());
        return assertion;
    }

    public static class ApiAssertionRequest {
        private Long collectionId;
        private String assertionType;
        private String expression;
        private String expected;
        private Boolean enabled;
        private Integer sortOrder;

        public Long getCollectionId() { return collectionId; }
        public void setCollectionId(Long collectionId) { this.collectionId = collectionId; }
        public String getAssertionType() { return assertionType; }
        public void setAssertionType(String assertionType) { this.assertionType = assertionType; }
        public String getExpression() { return expression; }
        public void setExpression(String expression) { this.expression = expression; }
        public String getExpected() { return expected; }
        public void setExpected(String expected) { this.expected = expected; }
        public Boolean getEnabled() { return enabled; }
        public void setEnabled(Boolean enabled) { this.enabled = enabled; }
        public Integer getSortOrder() { return sortOrder; }
        public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    }
}