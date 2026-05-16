package com.testplatform.service.apitest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.testplatform.entity.apitest.ApiAssertion;
import com.testplatform.entity.apitest.ApiCollection;
import com.testplatform.repository.apitest.ApiAssertionRepository;
import com.testplatform.repository.apitest.ApiCollectionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApiAssertionService {

    private final ApiAssertionRepository assertionRepository;
    private final ApiCollectionRepository collectionRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<ApiAssertion> getByCollectionId(Long collectionId) {
        return assertionRepository.findByCollectionIdOrderBySortOrderAsc(collectionId);
    }

    @Transactional
    public ApiAssertion create(ApiAssertion assertion, Long collectionId) {
        ApiCollection collection = collectionRepository.findById(collectionId)
                .orElseThrow(() -> new IllegalArgumentException("用例不存在: " + collectionId));

        assertion.setCollection(collection);

        List<ApiAssertion> existing = assertionRepository.findByCollectionIdOrderBySortOrderAsc(collectionId);
        int maxOrder = existing.stream()
                .mapToInt(a -> a.getSortOrder() != null ? a.getSortOrder() : 0)
                .max()
                .orElse(0);
        assertion.setSortOrder(maxOrder + 1);

        ApiAssertion saved = assertionRepository.save(assertion);
        log.info("创建断言配置: collectionId={}, type={}", collectionId, assertion.getAssertionType());
        return saved;
    }

    @Transactional
    public ApiAssertion update(Long id, ApiAssertion assertionData) {
        ApiAssertion assertion = assertionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("断言不存在: " + id));

        assertion.setAssertionType(assertionData.getAssertionType());
        assertion.setExpression(assertionData.getExpression());
        assertion.setExpected(assertionData.getExpected());
        assertion.setEnabled(assertionData.getEnabled());
        assertion.setSortOrder(assertionData.getSortOrder());

        ApiAssertion saved = assertionRepository.save(assertion);
        log.info("更新断言配置: id={}, type={}", id, assertion.getAssertionType());
        return saved;
    }

    @Transactional
    public void delete(Long id) {
        assertionRepository.deleteById(id);
        log.info("删除断言配置: id={}", id);
    }

    @Transactional
    public void deleteByCollectionId(Long collectionId) {
        assertionRepository.deleteByCollectionId(collectionId);
        log.info("删除用例所有断言: collectionId={}", collectionId);
    }

    @Transactional
    public void saveAll(Long collectionId, List<ApiAssertion> assertions) {
        ApiCollection collection = collectionRepository.findById(collectionId)
                .orElseThrow(() -> new IllegalArgumentException("用例不存在: " + collectionId));

        assertionRepository.deleteByCollectionId(collectionId);

        for (int i = 0; i < assertions.size(); i++) {
            ApiAssertion assertion = assertions.get(i);
            assertion.setId(null);
            assertion.setCollection(collection);
            assertion.setSortOrder(i);
            assertionRepository.save(assertion);
        }

        log.info("保存用例断言配置: collectionId={}, count={}", collectionId, assertions.size());
    }
}