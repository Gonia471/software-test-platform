package com.testplatform.repository.uitest;

import com.testplatform.entity.uitest.UiExecutionStep;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UiExecutionStepRepository extends JpaRepository<UiExecutionStep, Long> {

    List<UiExecutionStep> findByExecutionIdOrderByStepIndexAsc(Long executionId);
}

