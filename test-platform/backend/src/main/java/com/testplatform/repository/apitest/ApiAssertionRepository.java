package com.testplatform.repository.apitest;

import com.testplatform.entity.apitest.ApiAssertion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApiAssertionRepository extends JpaRepository<ApiAssertion, Long> {

    List<ApiAssertion> findByCollectionIdOrderBySortOrderAsc(Long collectionId);

    List<ApiAssertion> findByCollectionIdAndEnabledTrueOrderBySortOrderAsc(Long collectionId);

    void deleteByCollectionId(Long collectionId);
}
