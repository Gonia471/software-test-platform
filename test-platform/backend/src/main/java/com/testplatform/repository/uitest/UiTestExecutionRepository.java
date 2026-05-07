package com.testplatform.repository.uitest;

import com.testplatform.entity.uitest.UiTestExecution;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UiTestExecutionRepository extends JpaRepository<UiTestExecution, Long> {

    List<UiTestExecution> findTop100ByOrderByCreatedAtDesc();
}

