package com.testplatform.service.apitest;

import com.testplatform.entity.apitest.ScriptLibrary;
import com.testplatform.entity.User;
import com.testplatform.repository.apitest.ScriptLibraryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScriptLibraryService {

    private final ScriptLibraryRepository scriptRepository;
    private final PythonScriptService pythonScriptService;

    @Transactional(readOnly = true)
    public List<ScriptLibrary> getAll() {
        return scriptRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly =true)
    public Optional<ScriptLibrary> getById(Long id) {
        return scriptRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<ScriptLibrary> getByFunctionName(String functionName) {
        return scriptRepository.findByFunctionName(functionName);
    }

    @Transactional(readOnly = true)
    public boolean existsByFunctionName(String functionName) {
        return scriptRepository.existsByFunctionName(functionName);
    }

    @Transactional
    public ScriptLibrary create(ScriptLibrary script, User creator) {
        PythonScriptService.CompileResult compileResult = pythonScriptService.compileScript(script.getContent());
        if (!compileResult.isSuccess()) {
            throw new IllegalArgumentException("脚本编译失败: " + compileResult.getErrorMessage());
        }

        String functionName = compileResult.getFunctionName();
        if (scriptRepository.existsByFunctionName(functionName)) {
            throw new IllegalArgumentException("函数名已存在: " + functionName);
        }

        script.setFunctionName(functionName);
        script.setCreator(creator);

        ScriptLibrary saved = scriptRepository.save(script);
        log.info("创建脚本: functionName={}, creator={}", functionName, creator.getUsername());
        return saved;
    }

    @Transactional
    public ScriptLibrary update(Long id, ScriptLibrary scriptData, User user) {
        ScriptLibrary script = scriptRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("脚本不存在: " + id));

        if (!script.getCreator().getId().equals(user.getId())) {
            throw new IllegalArgumentException("无权限修改此脚本");
        }

        PythonScriptService.CompileResult compileResult = pythonScriptService.compileScript(scriptData.getContent());
        if (!compileResult.isSuccess()) {
            throw new IllegalArgumentException("脚本编译失败: " + compileResult.getErrorMessage());
        }

        String newFunctionName = compileResult.getFunctionName();

        if (!newFunctionName.equals(script.getFunctionName())) {
            if (scriptRepository.existsByFunctionName(newFunctionName)) {
                throw new IllegalArgumentException("函数名已存在: " + newFunctionName);
            }
        }

        script.setContent(scriptData.getContent());
        script.setScriptName(scriptData.getScriptName());
        script.setDescription(scriptData.getDescription());
        script.setFunctionName(newFunctionName);

        ScriptLibrary saved = scriptRepository.save(script);
        log.info("更新脚本: id={}, functionName={}", id, newFunctionName);
        return saved;
    }

    @Transactional
    public void delete(Long id, User user) {
        ScriptLibrary script = scriptRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("脚本不存在: " + id));

        if (!script.getCreator().getId().equals(user.getId())) {
            throw new IllegalArgumentException("无权限删除此脚本");
        }

        scriptRepository.delete(script);
        log.info("删除脚本: id={}, functionName={}", id, script.getFunctionName());
    }

    public PythonScriptService.CompileResult compile(String content) {
        return pythonScriptService.compileScript(content);
    }

    public PythonScriptService.TestResult test(String content, List<String> params) {
        return pythonScriptService.testScript(content, params);
    }
}