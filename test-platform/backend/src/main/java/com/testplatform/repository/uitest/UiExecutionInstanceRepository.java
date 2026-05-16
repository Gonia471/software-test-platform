package com.testplatform.repository.uitest;

import com.testplatform.entity.uitest.UiExecutionInstance;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UiExecutionInstanceRepository extends JpaRepository<UiExecutionInstance, Long> {
    java.util.Optional<UiExecutionInstance> findFirstByEnabledTrueOrderByIdAsc();
}

