package com.testplatform.service.uitest;

import com.testplatform.service.uitest.handler.LocatorSupport;
import com.testplatform.service.uitest.model.ExecutionOptions;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class XPathPreviewService {

    private final WebDriverFactory webDriverFactory;

    public XPathPreviewService(WebDriverFactory webDriverFactory) {
        this.webDriverFactory = webDriverFactory;
    }

    public Map<String, String> preview(String xpath, String pageUrl) {
        Map<String, String> fallback = new LinkedHashMap<>(LocatorSupport.analyzeXpath(xpath));
        fallback.put("mode", "string");

        if (xpath == null || xpath.isBlank()) {
            return fallback;
        }

        if (pageUrl == null || pageUrl.isBlank()) {
            fallback.put("message", "未找到前置打开网页步骤，暂时只能做字符串级优化");
            return fallback;
        }

        ExecutionOptions options = new ExecutionOptions();
        options.setHeadless(true);

        RemoteWebDriver driver = null;
        try {
            driver = webDriverFactory.createLocalChrome(options);
            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(20));
            driver.manage().timeouts().scriptTimeout(Duration.ofSeconds(10));
            driver.get(pageUrl);

            new WebDriverWait(driver, Duration.ofSeconds(10)).until(webDriver ->
                    "complete".equals(((JavascriptExecutor) webDriver).executeScript("return document.readyState"))
            );

            WebElement element = driver.findElement(By.xpath(xpath));
            Object result = ((JavascriptExecutor) driver).executeScript(BROWSER_SIDE_OPTIMIZER, element);
            if (result instanceof Map<?, ?> map) {
                Map<String, String> response = new LinkedHashMap<>();
                response.put("original", xpath);
                response.put("isAbsolute", String.valueOf(xpath.startsWith("/html") || xpath.startsWith("/body")));
                response.put("optimized", stringify(map.get("optimized"), xpath));
                response.put("elementName", stringify(map.get("elementName"), fallback.getOrDefault("elementName", "")));
                response.put("strategy", stringify(map.get("strategy"), "dom"));
                response.put("mode", "dom");
                return response;
            }
            return fallback;
        } catch (NoSuchElementException ex) {
            fallback.put("message", "未在页面中找到该 XPath 对应元素，已回退为字符串优化");
            return fallback;
        } catch (TimeoutException ex) {
            fallback.put("message", "页面加载超时，已回退为字符串优化");
            return fallback;
        } catch (Exception ex) {
            fallback.put("message", "DOM 优化失败，已回退为字符串优化");
            return fallback;
        } finally {
            if (driver != null) {
                driver.quit();
            }
        }
    }

    private String stringify(Object value, String defaultValue) {
        return value == null ? defaultValue : String.valueOf(value);
    }

    private static final String BROWSER_SIDE_OPTIMIZER = """
            const target = arguments[0];
            if (!target || !target.tagName) {
              return { optimized: '', elementName: '', strategy: 'invalid' };
            }

            const randomPatterns = [
              /^[a-f0-9]{6,}$/i,
              /^[0-9a-f]{8}-[0-9a-f-]{27}$/i,
              /^[a-z0-9]+-[a-f0-9]{5,}$/i,
              /^_[a-f0-9]{5,}$/i,
              /^css-[a-z0-9]+$/i,
              /^sc-[a-z0-9]+$/i,
              /^jsx-[a-z0-9]+$/i,
              /^react-[a-z0-9-]+$/i,
              /^vue-[a-z0-9-]+$/i,
              /^ng-[a-z0-9-]+$/i,
              /^__[a-z0-9-]+$/i,
              /^[a-z][0-9a-f]{7}$/i,
              /^[a-z]{2}[0-9a-f]{6}$/i,
              /^[a-z0-9]{8,12}$/i
            ];

            function escapeXPath(text) {
              if (!text) return "''";
              if (text.includes("'") && !text.includes('"')) return `"${text}"`;
              if (text.includes('"') && !text.includes("'")) return `'${text}'`;
              if (text.includes("'") && text.includes('"')) {
                const parts = text.split("'").map(part => `'${part}'`);
                return `concat(${parts.join(", \"'\" , ")})`;
              }
              return `'${text}'`;
            }

            function getVisibleText(element) {
              if (!element) return '';
              const style = window.getComputedStyle(element);
              if (style.display === 'none' || style.visibility === 'hidden' || style.opacity === '0') {
                return '';
              }
              const textNodes = [];
              for (const node of element.childNodes) {
                if (node.nodeType === Node.TEXT_NODE) {
                  const value = (node.textContent || '').trim();
                  if (value) textNodes.push(value);
                }
              }
              return textNodes.join(' ').trim();
            }

            function isRandomValue(value) {
              if (!value || typeof value !== 'string') return false;
              const trimmed = value.trim();
              return randomPatterns.some(pattern => pattern.test(trimmed));
            }

            function getFilteredClasses(className) {
              if (!className || typeof className !== 'string') return [];
              return className
                .split(/\\s+/)
                .map(item => item.trim())
                .filter(Boolean)
                .filter(item => !isRandomValue(item));
            }

            function getValidId(id) {
              if (!id || isRandomValue(id)) return null;
              return id;
            }

            function evaluateSnapshot(xpath) {
              try {
                return document.evaluate(xpath, document, null, XPathResult.ORDERED_NODE_SNAPSHOT_TYPE, null);
              } catch (e) {
                return null;
              }
            }

            function isUnique(xpath, element) {
              const result = evaluateSnapshot(xpath);
              return !!result && result.snapshotLength === 1 && result.snapshotItem(0) === element;
            }

            function pushCandidate(candidates, xpath, strategy, score) {
              if (!xpath) return;
              candidates.push({ xpath, strategy, score });
            }

            function getStableAttributes(element) {
              const attrs = [];
              const preferred = ['name', 'title', 'alt', 'role', 'type', 'placeholder', 'aria-label', 'data-testid', 'data-test', 'data-cy', 'data-qa'];
              for (const attr of element.attributes) {
                const name = attr.name;
                const value = attr.value;
                if (!value || ['style', 'class'].includes(name)) continue;
                if (/^data-v-[a-f0-9]{8}$/i.test(name)) continue;
                if (/^_ng(content|host)-/i.test(name)) continue;
                if (/^data-react/i.test(name)) continue;
                if (isRandomValue(value)) continue;
                if (preferred.includes(name) || (!name.startsWith('data-') && value.length <= 80)) {
                  attrs.push({ name, value, priority: preferred.indexOf(name) >= 0 ? preferred.indexOf(name) : 99 });
                }
              }
              return attrs.sort((a, b) => a.priority - b.priority);
            }

            function getTagIndexWithinParent(element) {
              if (!element.parentElement) return 1;
              const siblings = Array.from(element.parentElement.children).filter(item => item.tagName === element.tagName);
              return siblings.indexOf(element) + 1;
            }

            function getUniqueAnchorXPath(element) {
              let current = element.parentElement;
              while (current && current !== document.body) {
                const tag = current.tagName.toLowerCase();
                const validId = getValidId(current.id);
                if (validId) {
                  const xpath = `//*[@id=${escapeXPath(validId)}]`;
                  if (isUnique(xpath, current)) return xpath;
                }
                const attrs = getStableAttributes(current);
                for (const attr of attrs.slice(0, 2)) {
                  const xpath = `//${tag}[@${attr.name}=${escapeXPath(attr.value)}]`;
                  if (isUnique(xpath, current)) return xpath;
                }
                const classes = getFilteredClasses(current.className);
                for (const cls of classes.slice(0, 2)) {
                  const xpath = `//${tag}[contains(@class, ${escapeXPath(cls)})]`;
                  if (isUnique(xpath, current)) return xpath;
                }
                current = current.parentElement;
              }
              return null;
            }

            function buildRelativeFromAnchor(anchorXPath, element) {
              const tag = element.tagName.toLowerCase();
              const text = getVisibleText(element);
              const attrs = getStableAttributes(element);
              const candidates = [];

              if (text && text.length <= 50) {
                const escapedText = escapeXPath(text.replace(/\\s+/g, ' ').trim());
                candidates.push(`${anchorXPath}//${tag}[normalize-space(.)=${escapedText}]`);
                candidates.push(`${anchorXPath}//*[normalize-space(.)=${escapedText}]`);
              }

              for (const attr of attrs.slice(0, 2)) {
                candidates.push(`${anchorXPath}//${tag}[@${attr.name}=${escapeXPath(attr.value)}]`);
              }

              const index = getTagIndexWithinParent(element);
              candidates.push(index > 1 ? `${anchorXPath}//${tag}[${index}]` : `${anchorXPath}//${tag}`);

              for (const xpath of candidates) {
                if (isUnique(xpath, element)) return xpath;
              }
              return null;
            }

            function generateOptimizedXPath(element) {
              const tag = element.tagName.toLowerCase();
              const text = getVisibleText(element).trim();
              const candidates = [];

              if (text && text.length <= 50) {
                const escapedText = escapeXPath(text.replace(/\\s+/g, ' ').trim());
                pushCandidate(candidates, `//${tag}[normalize-space(.)=${escapedText}]`, 'text', 110);
                pushCandidate(candidates, `//*[normalize-space(.)=${escapedText}]`, 'text', 105);
                if (tag === 'span' && element.closest('button')) {
                  pushCandidate(candidates, `//button[normalize-space(.)=${escapedText}]`, 'text', 112);
                }
              }

              const validId = getValidId(element.id);
              if (validId) {
                pushCandidate(candidates, `//*[@id=${escapeXPath(validId)}]`, 'attribute', 100);
                pushCandidate(candidates, `//${tag}[@id=${escapeXPath(validId)}]`, 'attribute', 99);
              }

              const attrs = getStableAttributes(element);
              for (const attr of attrs.slice(0, 4)) {
                pushCandidate(candidates, `//${tag}[@${attr.name}=${escapeXPath(attr.value)}]`, 'attribute', 95 - attr.priority);
                pushCandidate(candidates, `//*[@${attr.name}=${escapeXPath(attr.value)}]`, 'attribute', 90 - attr.priority);
              }

              const classes = getFilteredClasses(element.className);
              for (const cls of classes.slice(0, 2)) {
                pushCandidate(candidates, `//${tag}[contains(@class, ${escapeXPath(cls)})]`, 'attribute', 85);
              }

              const anchorXPath = getUniqueAnchorXPath(element);
              if (anchorXPath) {
                const relative = buildRelativeFromAnchor(anchorXPath, element);
                pushCandidate(candidates, relative, 'container-context', 88);
              }

              const index = getTagIndexWithinParent(element);
              if (element.parentElement) {
                pushCandidate(candidates, index > 1 ? `//${tag}[${index}]` : `//${tag}`, 'relative', 60);
              }

              candidates.sort((a, b) => b.score - a.score);
              for (const candidate of candidates) {
                if (isUnique(candidate.xpath, element)) {
                  return {
                    optimized: candidate.xpath,
                    elementName: tag,
                    strategy: candidate.strategy
                  };
                }
              }

              return {
                optimized: `//${tag}`,
                elementName: tag,
                strategy: 'fallback'
              };
            }

            return generateOptimizedXPath(target);
            """;
}
