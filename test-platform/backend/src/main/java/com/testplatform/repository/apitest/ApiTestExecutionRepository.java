package com.testplatform.repository.apitest;

import com.testplatform.entity.apitest.ApiTestExecution;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.time.Instant;

@Repository
public interface ApiTestExecutionRepository extends JpaRepository<ApiTestExecution, Long> {

    List<ApiTestExecution> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<ApiTestExecution> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    List<ApiTestExecution> findByCollectionIdOrderByCreatedAtDesc(String collectionId);

    @Query("SELECT e FROM ApiTestExecution e WHERE e.user.id = :userId ORDER BY e.createdAt DESC")
    List<ApiTestExecution> findRecentByUserId(@Param("userId") Long userId, Pageable pageable);

    long countByUserIdAndStatus(Long userId, ApiTestExecution.TestStatus status);

    long countByUserId(Long userId);

    @Query("SELECT e FROM ApiTestExecution e WHERE e.user.id = :userId AND e.createdAt >= :since ORDER BY e.createdAt DESC")
    List<ApiTestExecution> findByUserIdAndCreatedAtAfter(
            @Param("userId") Long userId,
            @Param("since") Instant since
    );

    @Query("SELECT e FROM ApiTestExecution e WHERE e.createdAt >= :since ORDER BY e.createdAt DESC")
    List<ApiTestExecution> findByCreatedAtAfterOrderByCreatedAtDesc(@Param("since") Instant since);
}
