package com.testplatform.service.apitest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.testplatform.entity.apitest.ApiCollection;
import com.testplatform.entity.apitest.ApiPrescript;
import com.testplatform.repository.apitest.ApiCollectionRepository;
import com.testplatform.repository.apitest.ApiPrescriptRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApiPrescriptService {

    private final ApiPrescriptRepository prescriptRepository;
    private final ApiCollectionRepository collectionRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<ApiPrescript> getByCollectionId(Long collectionId) {
        return prescriptRepository.findByCollectionIdOrderBySortOrderAsc(collectionId);
    }

    @Transactional
    public ApiPrescript create(ApiPrescript prescript, Long collectionId) {
        ApiCollection collection = collectionRepository.findById(collectionId)
                .orElseThrow(() -> new IllegalArgumentException("用例不存在: " + collectionId));

        prescript.setCollection(collection);

        List<ApiPrescript> existing = prescriptRepository.findByCollectionIdOrderBySortOrderAsc(collectionId);
        int maxOrder = existing.stream()
                .mapToInt(p -> p.getSortOrder() != null ? p.getSortOrder() : 0)
                .max()
                .orElse(0);
        prescript.setSortOrder(maxOrder + 1);

        ApiPrescript saved = prescriptRepository.save(prescript);
        log.info("创建前置步骤: collectionId={}, type={}", collectionId, prescript.getStepType());
        return saved;
    }

    @Transactional
    public ApiPrescript update(Long id, ApiPrescript prescriptData) {
        ApiPrescript prescript = prescriptRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("前置步骤不存在: " + id));

        prescript.setStepType(prescriptData.getStepType());
        prescript.setMethod(prescriptData.getMethod());
        prescript.setUrl(prescriptData.getUrl());
        prescript.setHeadersJson(prescriptData.getHeadersJson());
        prescript.setBodyJson(prescriptData.getBodyJson());
        prescript.setExtractParamsJson(prescriptData.getExtractParamsJson());
        prescript.setAssertionsJson(prescriptData.getAssertionsJson());
        prescript.setFunctionName(prescriptData.getFunctionName());
        prescript.setFunctionParamsJson(prescriptData.getFunctionParamsJson());
        prescript.setSetVariablesJson(prescriptData.getSetVariablesJson());
        prescript.setStopOnFail(prescriptData.getStopOnFail());
        prescript.setSortOrder(prescriptData.getSortOrder());

        ApiPrescript saved = prescriptRepository.save(prescript);
        log.info("更新前置步骤: id={}, type={}", id, prescript.getStepType());
        return saved;
    }

    @Transactional
    public void delete(Long id) {
        prescriptRepository.deleteById(id);
        log.info("删除前置步骤: id={}", id);
    }

    @Transactional
    public void deleteByCollectionId(Long collectionId) {
        prescriptRepository.deleteByCollectionId(collectionId);
        log.info("删除用例所有前置步骤: collectionId={}", collectionId);
    }

    @Transactional
    public List<ApiPrescript> saveAll(Long collectionId, List<ApiPrescript> prescripts) {
        ApiCollection collection = collectionRepository.findById(collectionId)
                .orElseThrow(() -> new IllegalArgumentException("用例不存在: " + collectionId));

        prescriptRepository.deleteByCollectionId(collectionId);

        List<ApiPrescript> savedPrescripts = new java.util.ArrayList<>();
        for (int i = 0; i < prescripts.size(); i++) {
            ApiPrescript prescript = prescripts.get(i);
            prescript.setId(null);
            prescript.setCollection(collection);
            prescript.setSortOrder(i);
            savedPrescripts.add(prescriptRepository.save(prescript));
        }

        log.info("保存用例前置步骤: collectionId={}, count={}", collectionId, prescripts.size());
        return savedPrescripts;
    }
}
