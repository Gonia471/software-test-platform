package com.testplatform.controller.apitest;

import com.testplatform.config.UserPrincipal;
import com.testplatform.entity.apitest.ScriptLibrary;
import com.testplatform.service.apitest.PythonScriptService;
import com.testplatform.service.apitest.ScriptLibraryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/api-test/scripts")
@RequiredArgsConstructor
public class ScriptLibraryController {

    private final ScriptLibraryService scriptService;

    @GetMapping
    public List<ScriptLibrary> list() {
        return scriptService.getAll();
    }

    @GetMapping("/{id}")
    public ScriptLibrary get(@PathVariable Long id) {
        return scriptService.getById(id).orElse(null);
    }

    @GetMapping("/function/{functionName}")
    public ScriptLibrary getByFunctionName(@PathVariable String functionName) {
        return scriptService.getByFunctionName(functionName).orElse(null);
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
    public ScriptLibrary create(
            @RequestBody ScriptRequest request,
            @AuthenticationPrincipal UserPrincipal user) {
        ScriptLibrary script = new ScriptLibrary();
        script.setScriptName(request.getScriptName());
        script.setDescription(request.getDescription());
        script.setContent(request.getContent());
        return scriptService.create(script, user.getUser());
    }

    @PutMapping("/{id}")
    public ScriptLibrary update(
            @PathVariable Long id,
            @RequestBody ScriptRequest request,
            @AuthenticationPrincipal UserPrincipal user) {
        ScriptLibrary script = new ScriptLibrary();
        script.setScriptName(request.getScriptName());
        script.setDescription(request.getDescription());
        script.setContent(request.getContent());
        return scriptService.update(id, script, user.getUser());
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal user) {
        scriptService.delete(id, user.getUser());
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
}