package com.testplatform.service.uitest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.testplatform.entity.uitest.UiExecutionStep;
import com.testplatform.entity.uitest.UiTestCase;
import com.testplatform.entity.uitest.UiTestExecution;
import com.testplatform.repository.uitest.UiExecutionStepRepository;
import com.testplatform.repository.uitest.UiTestCaseRepository;
import com.testplatform.repository.uitest.UiTestExecutionRepository;
import com.testplatform.service.uitest.handler.StepDispatcher;
import com.testplatform.service.uitest.model.ExecutionContext;
import com.testplatform.service.uitest.model.ExecutionOptions;
import com.testplatform.service.uitest.model.StepDefinition;
import com.testplatform.service.uitest.model.StepResult;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class UiTestExecutionEngine {

    private static final Logger log = LoggerFactory.getLogger(UiTestExecutionEngine.class);

    private final UiTestCaseRepository testCaseRepository;
    private final UiTestExecutionRepository executionRepository;
    private final UiExecutionStepRepository stepRepository;
    private final WebDriverFactory webDriverFactory;
    private final StepDispatcher stepDispatcher;
    private final ObjectMapper objectMapper;

    public UiTestExecutionEngine(UiTestCaseRepository testCaseRepository,
                                 UiTestExecutionRepository executionRepository,
                                 UiExecutionStepRepository stepRepository,
                                 WebDriverFactory webDriverFactory,
                                 StepDispatcher stepDispatcher,
                                 ObjectMapper objectMapper) {
        this.testCaseRepository = testCaseRepository;
        this.executionRepository = executionRepository;
        this.stepRepository = stepRepository;
        this.webDriverFactory = webDriverFactory;
        this.stepDispatcher = stepDispatcher;
        this.objectMapper = objectMapper;
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void runExecution(Long executionId, ExecutionOptions options) {
        log.info("[UI执行] 开始 executionId={} headless={} 线程={}",
                executionId, options.isHeadless(), Thread.currentThread().getName());

        UiTestExecution execution = executionRepository.findById(executionId)
                .orElseThrow(() -> new IllegalArgumentException("执行记录不存在: " + executionId));

        execution.setStatus("RUNNING");
        execution.setStartTime(Instant.now());
        executionRepository.save(execution);
        log.info("[UI执行] 状态已更新为 RUNNING, executionId={}", executionId);

        WebDriver driver = null;
        AtomicBoolean stopRequested = new AtomicBoolean(false);
        Path screenshotRoot = Paths.get("screenshots", "ui", String.valueOf(executionId));

        try {
            UiTestCase testCase = testCaseRepository.findById(execution.getTestCaseId())
                    .orElseThrow(() -> new IllegalArgumentException("测试用例不存在: " + execution.getTestCaseId()));
            List<StepDefinition> steps = parseSteps(testCase.getStepsJson());
            log.info("[UI执行] 解析到 {} 个步骤, executionId={}", steps.size(), executionId);

            log.info("[UI执行] 正在初始化 ChromeDriver, headless={}", options.isHeadless());
            driver = webDriverFactory.createLocalChrome(options);
            log.info("[UI执行] Chrome 启动成功, executionId={}", executionId);
            Files.createDirectories(screenshotRoot);
            ExecutionContext ctx = new ExecutionContext(execution, driver, options, stopRequested, screenshotRoot);

            for (int i = 0; i < steps.size(); i++) {
                // 每步前刷新一次停止标记
                UiTestExecution latest = executionRepository.findById(executionId).orElseThrow();
                if (Boolean.TRUE.equals(latest.getStopRequested())) {
                    execution.setStatus("STOPPED");
                    execution.setErrorMessage("用户主动停止执行");
                    break;
                }

                StepDefinition step = steps.get(i);
                UiExecutionStep stepRecord = initStepRecord(execution, i, step);
                ctx.getVariables().put("currentStepIndex", i + 1);
                try {
                    StepResult result = stepDispatcher.dispatch(step, ctx);
                    fillStepSuccess(stepRecord, result);
                    // 每步快照：步骤执行完成后的页面状态
                    if (result.getScreenshotPath() == null && driver instanceof TakesScreenshot ts && options.isScreenshotEveryStep()) {
                        try {
                            String path = saveScreenshot(ts, screenshotRoot, i + 1);
                            stepRecord.setScreenshotPath(path);
                        } catch (Exception shotEx) {
                            log.warn("[UI执行] 步骤 {} 快照失败: {}", i + 1, shotEx.getMessage());
                        }
                    }
                    // 确保截图路径被立即保存到数据库
                    stepRepository.save(stepRecord);
                } catch (Exception e) {
                    String msg = e.getMessage() != null ? e.getMessage() : e.toString();
                    stepRecord.setStatus("FAILED");
                    stepRecord.setErrorMessage(msg);
                    // 失败时截图
                    if (options.isScreenshotOnFailure() && driver instanceof TakesScreenshot ts) {
                        try {
                            String path = saveScreenshot(ts, screenshotRoot, i + 1);
                            stepRecord.setScreenshotPath(path);
                        } catch (Exception shotEx) {
                            log.warn("[UI执行] 失败快照失败: {}", shotEx.getMessage());
                        }
                    }
                    stepRecord.setEndTime(Instant.now());
                    stepRepository.save(stepRecord);

                    execution.setStatus("FAILED");
                    execution.setErrorMessage(msg);
                    if (options.isStopOnFailure()) {
                        break;
                    } else {
                        continue;
                    }
                }
                // 再次确保保存（虽然在 try/catch 内部已有保存，此处作为双重保险）
                stepRepository.save(stepRecord);
            }

            if (!"FAILED".equals(execution.getStatus()) && !"STOPPED".equals(execution.getStatus())) {
                execution.setStatus("PASSED");
            }
        } catch (Exception e) {
            log.error("[UI执行] 执行失败 executionId={}: {}", executionId, e.getMessage(), e);
            execution.setStatus("FAILED");
            execution.setErrorMessage(e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
        } finally {
            execution.setEndTime(Instant.now());
            executionRepository.save(execution);
            log.info("[UI执行] 执行结束 executionId={} status={}", executionId, execution.getStatus());
            if (driver != null) {
                try { driver.quit(); } catch (Exception ignored) {}
            }
        }
    }

    private List<StepDefinition> parseSteps(String stepsJson) {
        try {
            // 约定 stepsJson 存的是 steps 数组
            return objectMapper.readValue(stepsJson, new TypeReference<List<StepDefinition>>() {
            });
        } catch (Exception e) {
            throw new IllegalArgumentException("解析测试步骤失败", e);
        }
    }

    private UiExecutionStep initStepRecord(UiTestExecution execution, int index, StepDefinition step) {
        UiExecutionStep s = new UiExecutionStep();
        s.setExecutionId(execution.getId());
        s.setStepIndex(index + 1);
        s.setStepType(step.getType());
        s.setAction(step.getAction());
        s.setStatus("RUNNING");
        s.setStartTime(Instant.now());
        try {
            s.setRawStepJson(objectMapper.writeValueAsString(step));
        } catch (Exception ignored) {
        }
        return s;
    }

    private void fillStepSuccess(UiExecutionStep stepRecord, StepResult result) {
        stepRecord.setStatus("PASSED");
        stepRecord.setErrorMessage(null);
        stepRecord.setLogText(result.getLogText());
        stepRecord.setEndTime(Instant.now());
        // handler 内生成的截图（如 AI 步骤）；若后续引擎按步截全页会覆盖
        if (result.getScreenshotPath() != null) {
            stepRecord.setScreenshotPath(result.getScreenshotPath());
        }
    }

    private String saveScreenshot(TakesScreenshot ts, Path root, int index) throws Exception {
        File file = ts.getScreenshotAs(OutputType.FILE);
        String filename = "step-" + index + ".png";
        Path target = root.resolve(filename);
        Files.createDirectories(root);
        Files.copy(file.toPath(), target);
        return target.toString();
    }
}

