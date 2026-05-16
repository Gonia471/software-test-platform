package com.testplatform.service.uitest;

import com.testplatform.service.uitest.model.ExecutionOptions;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.Point;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Component
public class WebDriverFactory {

    private static final Logger log = LoggerFactory.getLogger(WebDriverFactory.class);

    /**
     * 可在 application.yml 中配置 app.ui-test.chrome-driver-path 指向本地 chromedriver.exe，
     * 国内网络无法自动下载时使用此选项。留空则由 Selenium Manager 自动管理。
     */
    @Value("${app.ui-test.chrome-driver-path:}")
    private String chromeDriverPath;

    /** Windows 常见 Chrome 安装路径 */
    private static final List<Path> WINDOWS_CHROME_PATHS = List.of(
            Paths.get(System.getenv().getOrDefault("PROGRAMFILES", "C:\\Program Files"),
                    "Google", "Chrome", "Application", "chrome.exe"),
            Paths.get(System.getenv().getOrDefault("PROGRAMFILES(X86)", "C:\\Program Files (x86)"),
                    "Google", "Chrome", "Application", "chrome.exe"),
            Paths.get(System.getenv().getOrDefault("LOCALAPPDATA",
                    System.getProperty("user.home") + "\\AppData\\Local"),
                    "Google", "Chrome", "Application", "chrome.exe")
    );

    public RemoteWebDriver createLocalChrome(ExecutionOptions options) {
        checkChromeInstalled();
        setupChromeDriver();

        ChromeOptions chromeOptions = new ChromeOptions();
        chromeOptions.setPageLoadStrategy(PageLoadStrategy.NORMAL);
        Path chromeBinary = resolveChromeBinary();
        if (chromeBinary != null) {
            chromeOptions.setBinary(chromeBinary.toString());
            log.info("[ChromeDriver] 浏览器可执行文件: {}", chromeBinary);
        }

        String osName = System.getProperty("os.name", "").toLowerCase();
        boolean isWindows = osName.contains("win");

        boolean headlessMode = options.isHeadless();
        log.info("[ChromeDriver] headless={}, 显示浏览器={}", headlessMode, !headlessMode);

        if (headlessMode) {
            log.info("[ChromeDriver] 启用无头模式");
            chromeOptions.addArguments("--headless=new");
            if (isWindows) {
                chromeOptions.addArguments("--headless");
            }
        } else {
            log.info("[ChromeDriver] 启用可视化模式（显示浏览器窗口）");
            chromeOptions.addArguments("--start-maximized");
            chromeOptions.addArguments("--disable-blink-features=AutomationControlled");
            if (isWindows) {
                chromeOptions.addArguments("--disable-extensions");
                chromeOptions.addArguments("--disable-popup-blocking");
                chromeOptions.addArguments("--no-first-run");
                chromeOptions.addArguments("--no-default-browser-check");
            }
        }

        if (isWindows) {
            chromeOptions.addArguments("--disable-gpu");
            chromeOptions.addArguments("--disable-software-rasterizer");
            chromeOptions.addArguments("--disable-gpu-sandbox");
            chromeOptions.addArguments("--disable-background-networking");
            chromeOptions.addArguments("--disable-default-apps");
            chromeOptions.addArguments("--disable-sync");
            chromeOptions.addArguments("--disable-translate");
            chromeOptions.addArguments("--metrics-recording-only");
            chromeOptions.addArguments("--mute-audio");
            chromeOptions.addArguments("--no-sandbox");
            chromeOptions.addArguments("--ignore-certificate-errors");
            chromeOptions.addArguments("--ignore-ssl-errors");
            chromeOptions.addArguments("--ignore-certificate-errors-spki-list");
            chromeOptions.addArguments("--ignore-user-profile-probing");
            chromeOptions.addArguments("--disable-client-side-phishing-detection");
            chromeOptions.addArguments("--disable-hang-monitor");
            chromeOptions.addArguments("--disable-ipc-flooding-protection");
            chromeOptions.addArguments("--disable-renderer-backgrounding");
            chromeOptions.addArguments("--disable-background-timer-throttling");
            chromeOptions.addArguments("--disable-backgrounding-occluded-windows");
            chromeOptions.addArguments("--disable-web-security");
            chromeOptions.addArguments("--force-color-profile=srgb");
            chromeOptions.addArguments("--disable-features=TranslateUI");
            chromeOptions.addArguments("--allow-running-insecure-content");
            chromeOptions.addArguments("--enable-features=NetworkService,NetworkServiceInProcess");
            chromeOptions.setExperimentalOption("excludeSwitches", List.of("enable-automation"));
            chromeOptions.setExperimentalOption("useAutomationExtension", false);

            String userDataDir = System.getProperty("java.io.tmpdir") + "\\selenium-chrome-data-" + System.currentTimeMillis();
            chromeOptions.addArguments("--user-data-dir=" + userDataDir);
            log.info("[ChromeDriver] Windows 环境，使用临时用户数据目录: {}", userDataDir);
        } else {
            chromeOptions.addArguments("--disable-gpu");
            chromeOptions.addArguments("--no-sandbox");
            chromeOptions.addArguments("--disable-dev-shm-usage");
        }

        chromeOptions.addArguments("--remote-allow-origins=*");
        chromeOptions.addArguments("--window-size=1280,800");
        if (!headlessMode) {
            chromeOptions.addArguments("--window-position=0,0");
        }

        log.info("[ChromeDriver] 正在启动 Chrome 浏览器");
        log.info("[ChromeDriver] ChromeOptions: {}", chromeOptions.asMap());

        try {
            ChromeDriver driver = new ChromeDriver(chromeOptions);
            if (!headlessMode) {
                try {
                    driver.manage().window().setPosition(new Point(0, 0));
                    driver.manage().window().maximize();
                } catch (Exception e) {
                    log.debug("[ChromeDriver] 浏览器窗口前置/最大化失败: {}", e.getMessage());
                }
            }
            return driver;
        } catch (Exception e) {
            log.error("[ChromeDriver] 启动失败，切换为无头模式: {}", e.getMessage());
            chromeOptions.addArguments("--headless=new");
            return new ChromeDriver(chromeOptions);
        }
    }

    public RemoteWebDriver createRemote(URL url, ExecutionOptions options) {
        ChromeOptions chromeOptions = new ChromeOptions();
        if (options.isHeadless()) {
            chromeOptions.addArguments("--headless=new");
        }
        chromeOptions.addArguments("--disable-gpu");
        chromeOptions.addArguments("--window-size=1280,800");
        return new RemoteWebDriver(url, chromeOptions);
    }

    /**
     * 决定使用哪个 ChromeDriver：
     * 1. application.yml 中手动配置了路径 → 直接使用（完全离线）
     * 2. 系统 PATH 中存在 chromedriver → 直接使用
     * 3. 以上都没有 → 交给 Selenium Manager 自动处理（首次需联网）
     */
    private void setupChromeDriver() {
        sanitizeDriverSystemProperty();

        // 优先：配置文件中指定了本地路径
        if (chromeDriverPath != null && !chromeDriverPath.isBlank()) {
            String normalized = normalizePath(chromeDriverPath);
            try {
                Path path = Paths.get(normalized);
                if (Files.exists(path)) {
                    System.setProperty("webdriver.chrome.driver", path.toString());
                    log.info("[ChromeDriver] 使用配置的本地 ChromeDriver: {}", path);
                    return;
                }
                log.warn("[ChromeDriver] 配置的路径不存在: {}，回退到自动模式", normalized);
            } catch (InvalidPathException ex) {
                log.warn("[ChromeDriver] 配置的路径格式非法: {}，回退到自动模式", chromeDriverPath);
            }
        }

        // 次优：检查系统 PATH 中是否已有 chromedriver
        String pathFromSystem = findChromeDriverInPath();
        if (pathFromSystem != null) {
            log.info("[ChromeDriver] 在系统 PATH 中找到 chromedriver: {}", pathFromSystem);
            return; // Selenium 会自动从 PATH 中找到
        }

        // 兜底：Selenium Manager 自动下载（需联网，首次可能较慢）
        log.info("[ChromeDriver] 未找到本地 chromedriver，将由 Selenium Manager 自动管理（首次需联网）");
        log.info("[ChromeDriver] 如网络受限，请手动下载 chromedriver.exe 并在 application.yml 中配置:");
        log.info("[ChromeDriver]   app.ui-test.chrome-driver-path: C:/path/to/chromedriver.exe");
        log.info("[ChromeDriver] 国内下载地址: https://registry.npmmirror.com/binary.html?path=chromedriver/");
    }

    private String findChromeDriverInPath() {
        String pathEnv = System.getenv("PATH");
        if (pathEnv == null) return null;
        String exeName = System.getProperty("os.name", "").toLowerCase().contains("win")
                ? "chromedriver.exe" : "chromedriver";
        for (String dir : pathEnv.split(java.io.File.pathSeparator)) {
            String normalizedDir = normalizePath(dir);
            if (normalizedDir.isBlank()) {
                continue;
            }
            try {
                Path rawPath = Paths.get(normalizedDir);
                // 兼容 PATH 中直接放了 chromedriver 可执行文件路径的情况
                if (Files.isRegularFile(rawPath)
                        && rawPath.getFileName() != null
                        && rawPath.getFileName().toString().equalsIgnoreCase(exeName)) {
                    return rawPath.toString();
                }

                Path candidate = rawPath.resolve(exeName);
                if (Files.exists(candidate)) {
                    return candidate.toString();
                }
            } catch (InvalidPathException ex) {
                log.debug("[ChromeDriver] 跳过非法 PATH 目录: {}", dir);
            }
        }
        return null;
    }

    /**
     * 一些运行环境会把 webdriver.chrome.driver 配成带引号的路径（如 "E:\xx\chromedriver.exe），
     * Selenium 内部解析该值时会抛 Illegal char <"> at index 0，这里先统一清洗。
     */
    private void sanitizeDriverSystemProperty() {
        String current = System.getProperty("webdriver.chrome.driver");
        if (current == null || current.isBlank()) {
            return;
        }
        String normalized = normalizePath(current);
        if (!current.equals(normalized)) {
            System.setProperty("webdriver.chrome.driver", normalized);
            log.warn("[ChromeDriver] 已清洗 webdriver.chrome.driver 中的异常引号: {}", normalized);
        }
        try {
            Path path = Paths.get(normalized);
            if (!Files.exists(path)) {
                System.clearProperty("webdriver.chrome.driver");
                log.warn("[ChromeDriver] webdriver.chrome.driver 指向文件不存在，已清空并回退自动模式: {}", normalized);
            }
        } catch (InvalidPathException ex) {
            System.clearProperty("webdriver.chrome.driver");
            log.warn("[ChromeDriver] 检测到非法 webdriver.chrome.driver，已清空并回退自动模式: {}", current);
        }
    }

    private String normalizePath(String rawPath) {
        if (rawPath == null) {
            return "";
        }
        String trimmed = rawPath.trim();
        if (trimmed.length() >= 2 && trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
            return trimmed.substring(1, trimmed.length() - 1).trim();
        }
        if (trimmed.startsWith("\"")) {
            return trimmed.substring(1).trim();
        }
        if (trimmed.endsWith("\"")) {
            return trimmed.substring(0, trimmed.length() - 1).trim();
        }
        return trimmed;
    }

    private void checkChromeInstalled() {
        if (resolveChromeBinary() != null) {
            log.info("[ChromeDriver] Chrome/Chromium 检测通过");
            return;
        }
        String os = System.getProperty("os.name", "").toLowerCase();
        if (!os.contains("win")) {
            throw new IllegalStateException(
                    "当前环境未找到 Chrome/Chromium。Docker/Linux 请在镜像内安装浏览器，或设置环境变量 CHROME_BIN / GOOGLE_CHROME_BIN。");
        }
        boolean found = WINDOWS_CHROME_PATHS.stream().anyMatch(Files::exists);
        if (!found) {
            throw new IllegalStateException(
                    "未在本机找到 Chrome 浏览器，请安装 Google Chrome 后再运行本地 UI 测试。" +
                    "下载地址: https://www.google.com/chrome/");
        }
        log.info("[ChromeDriver] Chrome 浏览器检测通过（Windows）");
    }

    /**
     * CHROME_BIN / GOOGLE_CHROME_BIN 优先，其次常见路径（Docker 中为 /usr/bin/google-chrome-stable）。
     */
    private Path resolveChromeBinary() {
        String fromEnv = firstNonBlank(System.getenv("CHROME_BIN"), System.getenv("GOOGLE_CHROME_BIN"));
        if (fromEnv != null) {
            try {
                Path p = Paths.get(normalizePath(fromEnv));
                if (Files.exists(p) && Files.isRegularFile(p)) {
                    return p;
                }
            } catch (InvalidPathException ignored) {
            }
        }
        List<Path> candidates = new ArrayList<>();
        candidates.addAll(WINDOWS_CHROME_PATHS);
        candidates.add(Paths.get("/usr/bin/google-chrome-stable"));
        candidates.add(Paths.get("/usr/bin/google-chrome"));
        candidates.add(Paths.get("/usr/bin/chromium"));
        candidates.add(Paths.get("/usr/bin/chromium-browser"));
        for (Path p : candidates) {
            if (Files.exists(p) && Files.isRegularFile(p)) {
                return p;
            }
        }
        return null;
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a.trim();
        }
        if (b != null && !b.isBlank()) {
            return b.trim();
        }
        return null;
    }
}
