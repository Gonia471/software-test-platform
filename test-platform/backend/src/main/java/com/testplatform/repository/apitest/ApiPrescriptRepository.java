package com.testplatform.repository.apitest;

import com.testplatform.entity.apitest.ApiPrescript;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApiPrescriptRepository extends JpaRepository<ApiPrescript, Long> {

    List<ApiPrescript> findByCollectionIdOrderBySortOrderAsc(Long collectionId);

    void deleteByCollectionId(Long collectionId);
}
