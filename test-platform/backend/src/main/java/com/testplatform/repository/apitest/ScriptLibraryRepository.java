package com.testplatform.repository.apitest;

import com.testplatform.entity.apitest.ScriptLibrary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ScriptLibraryRepository extends JpaRepository<ScriptLibrary, Long> {

    Optional<ScriptLibrary> findByFunctionName(String functionName);

    boolean existsByFunctionName(String functionName);

    @Query("SELECT s FROM ScriptLibrary s JOIN FETCH s.creator ORDER BY s.createdAt DESC")
    List<ScriptLibrary> findAllByOrderByCreatedAtDesc();
}
