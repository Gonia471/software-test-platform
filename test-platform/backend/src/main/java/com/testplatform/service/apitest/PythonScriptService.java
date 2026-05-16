package com.testplatform.service.apitest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.testplatform.entity.apitest.ScriptLibrary;
import com.testplatform.repository.apitest.ScriptLibraryRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class PythonScriptService {

    private final ScriptLibraryRepository scriptRepository;
    private final ObjectMapper objectMapper;

    private Path tempDir;

    @Value("${spring.python.command:python3}")
    private String pythonCommand;

    @PostConstruct
    public void init() {
        try {
            tempDir = Files.createTempDirectory("python-scripts");
            log.info("Python脚本临时目录: {}, 使用命令: {}", tempDir, pythonCommand);
        } catch (IOException e) {
            log.error("创建临时目录失败", e);
        }
    }

    @Transactional(readOnly = true)
    public List<ScriptLibrary> getAllScripts() {
        return scriptRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public Optional<ScriptLibrary> getScriptByFunctionName(String functionName) {
        return scriptRepository.findByFunctionName(functionName);
    }

    public CompileResult compileScript(String content) {
        CompileResult result = new CompileResult();
        result.setSuccess(true);

        Path scriptFile = null;
        try {
            String functionName = extractFunctionName(content);
            result.setFunctionName(functionName);

            if (functionName == null) {
                result.setSuccess(false);
                result.setErrorMessage("未找到可调用的函数，请确保代码中包含 def 函数名(参数): 的定义");
                return result;
            }

            scriptFile = tempDir.resolve("compile_" + System.currentTimeMillis() + ".py");
            Files.writeString(scriptFile, content, StandardCharsets.UTF_8);

            ProcessBuilder pb = new ProcessBuilder(pythonCommand, "-m", "py_compile", scriptFile.toString());
            pb.environment().put("PYTHONIOENCODING", "utf-8");
            pb.redirectErrorStream(true);

            Process process = pb.start();
            String output = readStream(process.getInputStream());
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                result.setSuccess(false);
                result.setErrorMessage("语法错误: " + output);
            } else {
                result.setSuccess(true);
                log.info("脚本编译成功: {}", functionName);
            }
        } catch (Exception e) {
            result.setSuccess(false);
            result.setErrorMessage("编译异常: " + e.getMessage());
            log.error("Python脚本编译异常", e);
        } finally {
            if (scriptFile != null) {
                try { Files.deleteIfExists(scriptFile); } catch (IOException ignored) {}
            }
        }

        return result;
    }

    public TestResult testScript(String content, List<String> params) {
        TestResult result = new TestResult();

        Path scriptFile = null;
        try {
            String functionName = extractFunctionName(content);
            if (functionName == null) {
                result.setSuccess(false);
                result.setOutput("未找到可调用的函数");
                return result;
            }

            String fullScript = generateTestScript(content, functionName, params);
            scriptFile = tempDir.resolve("test_" + System.currentTimeMillis() + ".py");
            Files.writeString(scriptFile, fullScript, StandardCharsets.UTF_8);

            ProcessBuilder pb = new ProcessBuilder(pythonCommand, scriptFile.toString());
            pb.environment().put("PYTHONIOENCODING", "utf-8");
            pb.redirectErrorStream(true);

            Process process = pb.start();
            boolean finished = process.waitFor(30, TimeUnit.SECONDS);

            if (!finished) {
                process.destroy();
                result.setSuccess(false);
                result.setOutput("执行超时（30秒）");
                return result;
            }

            int exitCode = process.exitValue();
            String output = readStream(process.getInputStream());
            String error = readStream(process.getErrorStream());

            result.setExitCode(exitCode);

            if (exitCode != 0) {
                result.setSuccess(false);
                result.setOutput(error.isEmpty() ? output : error);
            } else {
                result.setSuccess(true);
                result.setFunctionName(functionName);
                result.setOutput(output);

                Map<String, String> outputParams = parseFunctionOutput(output);
                result.setOutputParams(outputParams);
            }
        } catch (Exception e) {
            result.setSuccess(false);
            result.setOutput("执行失败: " + e.getMessage());
            log.error("Python脚本测试失败", e);
        } finally {
            if (scriptFile != null) {
                try { Files.deleteIfExists(scriptFile); } catch (IOException ignored) {}
            }
        }

        return result;
    }

    public FunctionCallResult executeFunction(
            ScriptLibrary script,
            List<String> params,
            Map<String, String> variables) {

        FunctionCallResult result = new FunctionCallResult();
        result.setFunctionName(script.getFunctionName());

        Path scriptFile = null;
        try {
            String fullScript = generateCallScript(script.getContent(), script.getFunctionName(), params);
            scriptFile = tempDir.resolve("exec_" + System.currentTimeMillis() + ".py");
            Files.writeString(scriptFile, fullScript, StandardCharsets.UTF_8);

            ProcessBuilder pb = new ProcessBuilder(pythonCommand, scriptFile.toString());
            pb.environment().put("PYTHONIOENCODING", "utf-8");
            pb.redirectErrorStream(true);

            Process process = pb.start();
            boolean finished = process.waitFor(30, TimeUnit.SECONDS);

            if (!finished) {
                process.destroy();
                result.setSuccess(false);
                result.setErrorMessage("执行超时（30秒）");
                return result;
            }

            int exitCode = process.exitValue();
            String output = readStream(process.getInputStream());
            String error = readStream(process.getErrorStream());

            if (exitCode != 0) {
                result.setSuccess(false);
                result.setErrorMessage(error.isEmpty() ? output : error);
                log.warn("函数执行失败: {}, error={}", script.getFunctionName(), error);
            } else {
                result.setSuccess(true);
                Map<String, String> outputParams = parseFunctionOutput(output);
                result.setOutputParams(outputParams);
                result.setOutput(output);
                log.info("函数执行成功: {}, output={}", script.getFunctionName(), outputParams);
            }
        } catch (Exception e) {
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
            log.error("函数执行异常: {}", script.getFunctionName(), e);
        } finally {
            if (scriptFile != null) {
                try { Files.deleteIfExists(scriptFile); } catch (IOException ignored) {}
            }
        }

        return result;
    }

    private String extractFunctionName(String content) {
        Pattern pattern = Pattern.compile("def\\s+(\\w+)\\s*\\(");
        Matcher matcher = pattern.matcher(content);

        List<String> functions = new ArrayList<>();
        while (matcher.find()) {
            functions.add(matcher.group(1));
        }

        if (functions.isEmpty()) {
            return null;
        }
        return functions.get(functions.size() - 1);
    }

    private String generateTestScript(String content, String functionName, List<String> params) {
        StringBuilder sb = new StringBuilder();
        sb.append("# -*- coding: utf-8 -*-\n");
        sb.append(content);
        sb.append("\n\n");
        sb.append("import sys\n");
        sb.append("import json\n");

        if (params != null && !params.isEmpty()) {
            String paramList = String.join(", ", params);
            sb.append(String.format("output = %s(%s)\n", functionName, paramList));
        } else {
            sb.append(String.format("output = %s()\n", functionName));
        }

        sb.append("if isinstance(output, dict):\n");
        sb.append("    for k, v in output.items():\n");
        sb.append("        print('__PARAM__' + str(k) + '=' + str(v) + '__PARAM__')\n");
        sb.append("elif output is not None:\n");
        sb.append("    print('__PARAM__result=' + str(output) + '__PARAM__')\n");

        return sb.toString();
    }

    private String generateCallScript(String content, String functionName, List<String> params) {
        return generateTestScript(content, functionName, params);
    }

    private String readStream(InputStream stream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
        }
        return sb.toString().trim();
    }

    private Map<String, String> parseFunctionOutput(String output) {
        Map<String, String> params = new HashMap<>();
        if (output == null || output.isEmpty()) {
            return params;
        }

        Pattern pattern = Pattern.compile("__PARAM__(.+?)=(.+?)__PARAM__");
        Matcher matcher = pattern.matcher(output);

        while (matcher.find()) {
            String key = matcher.group(1).trim();
            String value = matcher.group(2).trim();
            params.put(key, value);
        }

        return params;
    }

    public static class CompileResult {
        private boolean success;
        private String functionName;
        private String errorMessage;

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getFunctionName() { return functionName; }
        public void setFunctionName(String functionName) { this.functionName = functionName; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    }

    public static class TestResult {
        private boolean success;
        private String functionName;
        private String output;
        private String errorMessage;
        private int exitCode;
        private Map<String, String> outputParams;

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getFunctionName() { return functionName; }
        public void setFunctionName(String functionName) { this.functionName = functionName; }
        public String getOutput() { return output; }
        public void setOutput(String output) { this.output = output; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        public int getExitCode() { return exitCode; }
        public void setExitCode(int exitCode) { this.exitCode = exitCode; }
        public Map<String, String> getOutputParams() { return outputParams; }
        public void setOutputParams(Map<String, String> outputParams) { this.outputParams = outputParams; }
    }

    public static class FunctionCallResult {
        private String functionName;
        private boolean success;
        private String output;
        private String errorMessage;
        private Map<String, String> outputParams;

        public String getFunctionName() { return functionName; }
        public void setFunctionName(String functionName) { this.functionName = functionName; }
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getOutput() { return output; }
        public void setOutput(String output) { this.output = output; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        public Map<String, String> getOutputParams() { return outputParams; }
        public void setOutputParams(Map<String, String> outputParams) { this.outputParams = outputParams; }
    }
}
