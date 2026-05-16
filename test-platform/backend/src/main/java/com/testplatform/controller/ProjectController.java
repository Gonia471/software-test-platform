package com.testplatform.controller;

import com.testplatform.dto.*;
import com.testplatform.entity.User;
import com.testplatform.service.ProjectExecutionService;
import com.testplatform.service.ProjectService;
import com.testplatform.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;
    private final ProjectExecutionService projectExecutionService;

    @GetMapping
    public ResponseEntity<List<ProjectDto>> getUserProjects() {
        User user = SecurityUtils.getCurrentUser();
        return ResponseEntity.ok(projectService.getUserProjects(user));
    }

    @PostMapping("/{id}/run")
    public ResponseEntity<Map<String, String>> runProject(@PathVariable Long id) {
        User user = SecurityUtils.getCurrentUser();
        projectExecutionService.runProject(id, user);
        return ResponseEntity.ok(Map.of("message", "项目合集已启动执行"));
    }

    @PostMapping
    public ResponseEntity<ProjectDto> createProject(
            @Valid @RequestBody CreateProjectRequest request) {
        User user = SecurityUtils.getCurrentUser();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(projectService.createProject(request, user));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProjectDto> getProject(@PathVariable Long id) {
        User user = SecurityUtils.getCurrentUser();
        return ResponseEntity.ok(projectService.getProject(id, user));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProjectDto> updateProject(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProjectRequest request) {
        User user = SecurityUtils.getCurrentUser();
        return ResponseEntity.ok(projectService.updateProject(id, request, user));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(@PathVariable Long id) {
        User user = SecurityUtils.getCurrentUser();
        projectService.deleteProject(id, user);
        return ResponseEntity.noContent().build();
    }
}