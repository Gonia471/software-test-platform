package com.testplatform.controller.apitest;

import com.testplatform.config.UserPrincipal;
import com.testplatform.entity.apitest.ScriptLibrary;
import com.testplatform.service.apitest.PythonScriptService;
import com.testplatform.service.apitest.ScriptLibraryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/api-test/scripts")
@RequiredArgsConstructor
public class ScriptLibraryController {

    private final ScriptLibraryService scriptService;

    @GetMapping
    public List<ScriptResponse> list() {
        return scriptService.getAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @GetMapping("/{id}")
    public ScriptResponse get(@PathVariable Long id) {
        return scriptService.getById(id).map(this::toResponse).orElse(null);
    }

    @GetMapping("/function/{functionName}")
    public ScriptResponse getByFunctionName(@PathVariable String functionName) {
        return scriptService.getByFunctionName(functionName).map(this::toResponse).orElse(null);
    }

    @PostMapping("/compile")
    public PythonScriptService.CompileResult compile(@RequestBody Map<String, String> request) {
        return scriptService.compile(request.get("content"));
    }

    @PostMapping("/test")
    public PythonScriptService.TestResult test(@RequestBody Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        List<String> params = (List<String>) request.get("params");
        return scriptService.test((String) request.get("content"), params);
    }

    @PostMapping
    public ScriptResponse create(
            @RequestBody ScriptRequest request,
            @AuthenticationPrincipal UserPrincipal user) {
        ScriptLibrary script = new ScriptLibrary();
        script.setScriptName(request.getScriptName());
        script.setDescription(request.getDescription());
        script.setContent(request.getContent());
        return toResponse(scriptService.create(script, user.getUser()));
    }

    @PutMapping("/{id}")
    public ScriptResponse update(
            @PathVariable Long id,
            @RequestBody ScriptRequest request,
            @AuthenticationPrincipal UserPrincipal user) {
        ScriptLibrary script = new ScriptLibrary();
        script.setScriptName(request.getScriptName());
        script.setDescription(request.getDescription());
        script.setContent(request.getContent());
        return toResponse(scriptService.update(id, script, user.getUser()));
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal user) {
        scriptService.delete(id, user.getUser());
    }

    private ScriptResponse toResponse(ScriptLibrary script) {
        ScriptResponse response = new ScriptResponse();
        response.setId(script.getId());
        response.setFunctionName(script.getFunctionName());
        response.setScriptName(script.getScriptName());
        response.setDescription(script.getDescription());
        response.setContent(script.getContent());
        response.setProjectId(null);
        response.setCreatedAt(script.getCreatedAt());
        response.setUpdatedAt(script.getUpdatedAt());
        if (script.getCreator() != null) {
            CreatorSummary creator = new CreatorSummary();
            creator.setId(script.getCreator().getId());
            creator.setUsername(script.getCreator().getUsername());
            response.setCreator(creator);
        }
        return response;
    }

    public static class ScriptRequest {
        private String scriptName;
        private String description;
        private String content;

        public String getScriptName() { return scriptName; }
        public void setScriptName(String scriptName) { this.scriptName = scriptName; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
    }

    public static class ScriptResponse {
        private Long id;
        private String functionName;
        private String scriptName;
        private String description;
        private String content;
        private Long projectId;
        private Instant createdAt;
        private Instant updatedAt;
        private CreatorSummary creator;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getFunctionName() { return functionName; }
        public void setFunctionName(String functionName) { this.functionName = functionName; }
        public String getScriptName() { return scriptName; }
        public void setScriptName(String scriptName) { this.scriptName = scriptName; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public Long getProjectId() { return projectId; }
        public void setProjectId(Long projectId) { this.projectId = projectId; }
        public Instant getCreatedAt() { return createdAt; }
        public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
        public Instant getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
        public CreatorSummary getCreator() { return creator; }
        public void setCreator(CreatorSummary creator) { this.creator = creator; }
    }

    public static class CreatorSummary {
        private Long id;
        private String username;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
    }
}
