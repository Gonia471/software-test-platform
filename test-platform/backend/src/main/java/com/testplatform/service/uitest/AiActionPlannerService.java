package com.testplatform.service.uitest;

import com.testplatform.service.uitest.model.AiPlannedAction;
import com.testplatform.service.uitest.model.AiPlanningRequest;
import com.testplatform.service.uitest.model.PageElementCandidate;
import com.testplatform.service.uitest.provider.AiProviderClient;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AiActionPlannerService {

    private static final Logger log = LoggerFactory.getLogger(AiActionPlannerService.class);
    private static final Pattern WAIT_SECONDS_PATTERN = Pattern.compile("(\\d{1,3})\\s*秒");

    private final PageElementSnapshotService pageElementSnapshotService;
    private final List<AiProviderClient> providerClients;

    @Value("${app.ui-test.ai.enabled:false}")
    private boolean aiEnabled;

    @Value("${app.ui-test.ai.provider:dashscope}")
    private String aiProvider;

    @Value("${app.ui-test.ai.min-confidence:0.7}")
    private double minConfidence;

    public AiActionPlannerService(PageElementSnapshotService pageElementSnapshotService,
                                  List<AiProviderClient> providerClients) {
        this.pageElementSnapshotService = pageElementSnapshotService;
        this.providerClients = providerClients;
    }

    public AiPlannedAction planFromInstruction(String instruction, WebDriver driver) {
        if (instruction == null || instruction.isBlank()) {
            throw new IllegalArgumentException("自然语言指令为空");
        }

        List<PageElementCandidate> candidates = pageElementSnapshotService.snapshotInteractiveElements(driver);
        String screenshotBase64 = captureScreenshotBase64(driver);

        AiProviderClient providerClient = resolveProviderClient();
        if (shouldUseRemoteAi(providerClient)) {
            try {
                AiPlannedAction action = providerClient.plan(buildPlanningRequest(
                        "nl_command", instruction, null, screenshotBase64, candidates
                ));
                action = enrichAction(action, candidates);
                if (isUsable(action)) {
                    if (action.getReason() == null || action.getReason().isBlank()) {
                        action.setReason("远程 AI(" + providerClient.getProviderName() + ") 规划成功");
                    }
                    return action;
                }
                log.warn("[AI] 远程自然语言规划结果不可用, provider={}, actionType={}, confidence={}",
                        providerClient.getProviderName(),
                        action == null ? null : action.getActionType(),
                        action == null ? null : action.getConfidence());
            } catch (Exception e) {
                log.warn("[AI] 远程自然语言规划失败，provider={}，回退本地规则: {}",
                        providerClient.getProviderName(),
                        e.getMessage());
            }
        }

        return fallbackByInstruction(instruction, candidates);
    }

    public AiPlannedAction planImageClick(String imagePath, String instruction, WebDriver driver) {
        List<PageElementCandidate> candidates = pageElementSnapshotService.snapshotInteractiveElements(driver);
        String hint = (instruction != null && !instruction.isBlank()) ? instruction : imagePath;
        return fallbackByInstruction(hint, candidates);
    }

    private AiPlanningRequest buildPlanningRequest(String scene,
                                                   String instruction,
                                                   String imagePath,
                                                   String screenshotBase64,
                                                   List<PageElementCandidate> candidates) {
        AiPlanningRequest request = new AiPlanningRequest();
        request.setScene(scene);
        request.setInstruction(instruction);
        request.setImagePath(imagePath);
        request.setScreenshotBase64(screenshotBase64);
        request.setCandidateElements(candidates);
        return request;
    }

    private AiProviderClient resolveProviderClient() {
        return providerClients.stream()
                .filter(client -> client.getProviderName().equalsIgnoreCase(aiProvider))
                .findFirst()
                .orElse(null);
    }

    private boolean shouldUseRemoteAi(AiProviderClient providerClient) {
        return aiEnabled && providerClient != null && providerClient.isAvailable();
    }

    private AiPlannedAction enrichAction(AiPlannedAction action, List<PageElementCandidate> candidates) {
        if (action == null || action.getCandidateIndex() == null) {
            return action;
        }
        PageElementCandidate candidate = findByIndex(action.getCandidateIndex(), candidates);
        if (candidate == null) {
            return action;
        }
        if (isBlank(action.getLocatorType())) {
            action.setLocatorType(candidate.getLocatorType());
        }
        if (isBlank(action.getLocatorValue())) {
            action.setLocatorValue(candidate.getLocatorValue());
        }
        if (isBlank(action.getReason())) {
            action.setReason("AI 从候选元素中选择目标: " + candidate.searchableText());
        }
        return action;
    }

    private boolean isUsable(AiPlannedAction action) {
        if (action == null) {
            return false;
        }
        if (action.getConfidence() != null && action.getConfidence() < minConfidence) {
            return false;
        }
        if ("wait".equalsIgnoreCase(action.getActionType())) {
            return true;
        }
        if ("input".equalsIgnoreCase(action.getActionType()) && action.getText() == null) {
            return false;
        }
        return hasLocator(action) || hasPoint(action);
    }

    private AiPlannedAction fallbackByInstruction(String instruction, List<PageElementCandidate> candidates) {
        String normalized = instruction == null ? "" : instruction.trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("自然语言指令为空");
        }
        if (containsWaitIntent(normalized)) {
            return fallbackWait(normalized);
        }
        if (containsInputIntent(normalized)) {
            return fallbackInput(normalized, candidates);
        }
        return fallbackClick(normalized, candidates);
    }

    private AiPlannedAction fallbackWait(String instruction) {
        AiPlannedAction action = new AiPlannedAction();
        action.setActionType("wait");
        action.setWaitSeconds(extractWaitSeconds(instruction));
        action.setConfidence(0.6);
        action.setReason("本地规则回退，识别为等待动作");
        return action;
    }

    private AiPlannedAction fallbackInput(String instruction, List<PageElementCandidate> candidates) {
        String inputText = extractInputText(instruction);
        String targetHint = extractInputTargetHint(instruction);
        PageElementCandidate candidate = findBestCandidate(targetHint, candidates, true);

        AiPlannedAction action = new AiPlannedAction();
        action.setActionType("input");
        action.setText(inputText == null ? "" : inputText);
        action.setConfidence(candidate != null ? 0.7 : 0.55);
        action.setReason(candidate != null
                ? "本地规则回退，根据输入框候选元素匹配"
                : "本地规则回退，按通用输入框定位");
        applyCandidate(action, candidate);
        if (!hasLocator(action)) {
            action.setLocatorType("xpath");
            action.setLocatorValue("//input | //textarea");
        }
        return action;
    }

    private AiPlannedAction fallbackClick(String instruction, List<PageElementCandidate> candidates) {
        String hint = extractClickHint(instruction);
        PageElementCandidate candidate = isSearchSubmitIntent(instruction)
                ? findSearchSubmitCandidate(candidates)
                : findBestCandidate(hint, candidates, false);
        if (candidate == null) {
            candidate = findBestCandidate(hint, candidates, false);
        }

        AiPlannedAction action = new AiPlannedAction();
        action.setActionType("click");
        action.setConfidence(candidate != null ? 0.68 : 0.5);
        action.setReason(candidate != null
                ? (isSearchSubmitIntent(instruction)
                ? "本地规则回退，根据搜索提交元素匹配"
                : "本地规则回退，根据候选元素文本匹配")
                : "本地规则回退，按关键词点击");
        applyCandidate(action, candidate);
        if (!hasLocator(action)) {
            if (isSearchSubmitIntent(instruction)) {
                action.setLocatorType("css");
                action.setLocatorValue("#chat-submit-button, #su, button[type='submit'], input[type='submit']");
            } else {
                action.setLocatorType("xpath");
                action.setLocatorValue(String.format(
                        "//*[self::button or self::a or self::span or self::div][contains(normalize-space(.),'%s')]",
                        escapeXPathText(defaultIfBlank(hint, "确定"))
                ));
            }
        }
        return action;
    }

    private void applyCandidate(AiPlannedAction action, PageElementCandidate candidate) {
        if (candidate == null) {
            return;
        }
        action.setCandidateIndex(candidate.getIndex());
        action.setLocatorType(candidate.getLocatorType());
        action.setLocatorValue(candidate.getLocatorValue());
    }

    private PageElementCandidate findBestCandidate(String hint,
                                                   List<PageElementCandidate> candidates,
                                                   boolean preferInput) {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }

        String normalizedHint = normalizeForMatch(hint);
        PageElementCandidate best = null;
        int bestScore = Integer.MIN_VALUE;
        for (PageElementCandidate candidate : candidates) {
            int score = scoreCandidate(candidate, normalizedHint, preferInput);
            if (score > bestScore) {
                bestScore = score;
                best = candidate;
            }
        }
        return bestScore > 0 ? best : null;
    }

    private int scoreCandidate(PageElementCandidate candidate, String hint, boolean preferInput) {
        if (candidate == null) {
            return Integer.MIN_VALUE;
        }

        boolean inputCandidate = isInputCandidate(candidate);
        boolean clickCandidate = isClickCandidate(candidate);
        boolean searchSubmitIntent = isSearchSubmitIntent(hint);
        int score = 0;
        if (preferInput) {
            score += inputCandidate ? 20 : -10;
        } else {
            score += clickCandidate ? 20 : -5;
        }

        String haystack = normalizeForMatch(candidate.searchableText());
        if (hint.isBlank()) {
            return score + (preferInput && inputCandidate ? 10 : 0);
        }

        if (haystack.equals(hint)) {
            score += 40;
        }
        if (haystack.contains(hint)) {
            score += 24;
        }
        for (String token : splitTokens(hint)) {
            if (!token.isBlank() && haystack.contains(token)) {
                score += 8;
            }
        }

        if (!preferInput && hint.contains("按钮") && "button".equalsIgnoreCase(candidate.getTag())) {
            score += 8;
        }
        if (!preferInput && hint.contains("按钮") && "a".equalsIgnoreCase(candidate.getTag())) {
            score -= 12;
        }
        if (!preferInput && searchSubmitIntent) {
            if (isSearchSubmitCandidate(candidate)) {
                score += 40;
            }
            if ("button".equalsIgnoreCase(candidate.getTag())) {
                score += 8;
            }
            if ("a".equalsIgnoreCase(candidate.getTag()) && !isSearchSubmitCandidate(candidate)) {
                score -= 20;
            }
        }
        if (!preferInput && (candidate.getText() == null || candidate.getText().isBlank())
                && !isSearchSubmitCandidate(candidate)) {
            score -= 8;
        }
        if (preferInput && candidate.getPlaceholder() != null && normalizeForMatch(candidate.getPlaceholder()).contains(hint)) {
            score += 8;
        }
        return score;
    }

    private PageElementCandidate findSearchSubmitCandidate(List<PageElementCandidate> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }
        PageElementCandidate best = null;
        int bestScore = Integer.MIN_VALUE;
        for (PageElementCandidate candidate : candidates) {
            int score = isSearchSubmitCandidate(candidate) ? 100 : Integer.MIN_VALUE;
            if (score > bestScore) {
                bestScore = score;
                best = candidate;
            }
        }
        return bestScore > 0 ? best : null;
    }

    private boolean isSearchSubmitCandidate(PageElementCandidate candidate) {
        if (candidate == null) {
            return false;
        }
        String searchable = normalizeForMatch(candidate.searchableText());
        String tag = lower(candidate.getTag());
        String id = lower(candidate.getId());
        String name = lower(candidate.getName());
        String type = lower(candidate.getType());
        return "button".equals(tag)
                || "submit".equals(type)
                || "su".equals(id)
                || "chat-submit-button".equals(id)
                || name.contains("submit")
                || id.contains("submit")
                || id.contains("search")
                || searchable.contains("百度一下")
                || searchable.contains("搜索")
                || searchable.contains("提交");
    }

    private boolean isInputCandidate(PageElementCandidate candidate) {
        String tag = lower(candidate.getTag());
        return "input".equals(tag) || "textarea".equals(tag) || "select".equals(tag);
    }

    private boolean isClickCandidate(PageElementCandidate candidate) {
        String tag = lower(candidate.getTag());
        String role = lower(candidate.getRole());
        return "button".equals(tag)
                || "a".equals(tag)
                || "button".equals(role)
                || (candidate.getText() != null && !candidate.getText().isBlank());
    }

    private PageElementCandidate findByIndex(Integer index, List<PageElementCandidate> candidates) {
        if (index == null || candidates == null) {
            return null;
        }
        for (PageElementCandidate candidate : candidates) {
            if (index.equals(candidate.getIndex())) {
                return candidate;
            }
        }
        return null;
    }

    private boolean containsInputIntent(String text) {
        return text.contains("输入") || lower(text).contains("type");
    }

    private boolean containsWaitIntent(String text) {
        return text.contains("等待") || lower(text).contains("wait");
    }

    private int extractWaitSeconds(String instruction) {
        Matcher matcher = WAIT_SECONDS_PATTERN.matcher(instruction);
        if (matcher.find()) {
            return Math.max(1, Integer.parseInt(matcher.group(1)));
        }
        return 2;
    }

    private String extractInputText(String instruction) {
        int idx = instruction.lastIndexOf("输入");
        if (idx < 0) {
            return "";
        }
        String rest = instruction.substring(idx + 2).trim();
        return stripQuotesAndPunctuation(rest);
    }

    private String extractInputTargetHint(String instruction) {
        int idx = instruction.lastIndexOf("输入");
        if (idx <= 0) {
            return "";
        }
        String prefix = instruction.substring(0, idx).trim();
        prefix = prefix.replace("请", "")
                .replace("将", "")
                .replace("把", "")
                .replace("在", "")
                .replace("中", "")
                .replace("里", "")
                .trim();
        return stripQuotesAndPunctuation(prefix);
    }

    private String extractClickHint(String instruction) {
        int idx = instruction.indexOf("点击");
        String rest = idx >= 0 ? instruction.substring(idx + 2) : instruction;
        rest = rest.replace("按钮", "")
                .replace("链接", "")
                .replace("选项", "")
                .replace("一下", "")
                .trim();
        return stripQuotesAndPunctuation(rest);
    }

    private String stripQuotesAndPunctuation(String text) {
        if (text == null) {
            return "";
        }
        String cleaned = text.replace("“", "")
                .replace("”", "")
                .replace("\"", "")
                .replace("'", "")
                .replace("：", "")
                .replace(":", "")
                .trim();
        return cleaned;
    }

    private String normalizeForMatch(String text) {
        if (text == null) {
            return "";
        }
        return text.replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
    }

    private List<String> splitTokens(String hint) {
        if (hint == null || hint.isBlank()) {
            return List.of();
        }
        if (hint.length() <= 2) {
            return List.of(hint);
        }
        if (hint.contains("框")) {
            return List.of(hint, hint.replace("框", ""));
        }
        return List.of(hint);
    }

    private String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String escapeXPathText(String text) {
        return text.replace("'", "");
    }

    private boolean isSearchSubmitIntent(String instruction) {
        String normalized = normalizeForMatch(instruction);
        return normalized.contains("百度一下")
                || normalized.contains("搜索")
                || normalized.contains("查询")
                || normalized.contains("提交");
    }

    private boolean hasLocator(AiPlannedAction action) {
        return !isBlank(action.getLocatorType()) && !isBlank(action.getLocatorValue());
    }

    private boolean hasPoint(AiPlannedAction action) {
        return action.getX() != null && action.getY() != null;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String lower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private String captureScreenshotBase64(WebDriver driver) {
        if (!(driver instanceof TakesScreenshot ts)) {
            return "";
        }
        try {
            return ts.getScreenshotAs(OutputType.BASE64);
        } catch (Exception e) {
            log.debug("[AI] 截图失败: {}", e.getMessage());
            return "";
        }
    }
}
