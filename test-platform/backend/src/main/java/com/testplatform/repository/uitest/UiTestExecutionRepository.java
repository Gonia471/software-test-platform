package com.testplatform.repository.uitest;

import com.testplatform.entity.uitest.UiTestExecution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface UiTestExecutionRepository extends JpaRepository<UiTestExecution, Long> {

    List<UiTestExecution> findTop100ByOrderByCreatedAtDesc();

    boolean existsByInstanceId(Long instanceId);

    @Modifying
    @Transactional
    @Query("DELETE FROM UiTestExecution e WHERE e.testCaseId = :testCaseId")
    void deleteByTestCaseId(Long testCaseId);
}

