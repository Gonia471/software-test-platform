package com.testplatform.repository.uitest;

import com.testplatform.entity.uitest.UiTestCase;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UiTestCaseRepository extends JpaRepository<UiTestCase, Long> {
}

