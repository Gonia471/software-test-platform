package com.testplatform.service.uitest;

import com.testplatform.service.uitest.model.PageElementCandidate;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class PageElementSnapshotService {

    private static final Logger log = LoggerFactory.getLogger(PageElementSnapshotService.class);

    public List<PageElementCandidate> snapshotInteractiveElements(WebDriver driver) {
        if (!(driver instanceof JavascriptExecutor js)) {
            return List.of();
        }

        try {
            Object result = js.executeScript(BROWSER_SIDE_SNAPSHOT_SCRIPT);
            if (!(result instanceof List<?> rows)) {
                return List.of();
            }
            List<PageElementCandidate> candidates = new ArrayList<>();
            for (Object row : rows) {
                if (!(row instanceof Map<?, ?> map)) {
                    continue;
                }
                PageElementCandidate candidate = new PageElementCandidate();
                candidate.setIndex(readInteger(map.get("index")));
                candidate.setTag(readText(map.get("tag")));
                candidate.setText(readText(map.get("text")));
                candidate.setPlaceholder(readText(map.get("placeholder")));
                candidate.setAriaLabel(readText(map.get("ariaLabel")));
                candidate.setRole(readText(map.get("role")));
                candidate.setType(readText(map.get("type")));
                candidate.setId(readText(map.get("id")));
                candidate.setName(readText(map.get("name")));
                candidate.setLocatorType(readText(map.get("locatorType")));
                candidate.setLocatorValue(readText(map.get("locatorValue")));
                candidates.add(candidate);
            }
            return candidates;
        } catch (Exception e) {
            log.debug("[AI] 页面候选元素提取失败: {}", e.getMessage());
            return List.of();
        }
    }

    private Integer readInteger(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            return null;
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String readText(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isBlank() ? null : text;
    }

    private static final String BROWSER_SIDE_SNAPSHOT_SCRIPT = """
            const MAX_COUNT = 40;
            const selector = 'button, a, input, textarea, select, [role="button"], [onclick], [tabindex]';
            const all = Array.from(document.querySelectorAll(selector));
            const candidates = [];
            const seen = new Set();

            function truncate(value, max = 60) {
              if (!value) return '';
              const text = String(value).replace(/\\s+/g, ' ').trim();
              return text.length > max ? text.slice(0, max) : text;
            }

            function isVisible(el) {
              if (!el) return false;
              const rect = el.getBoundingClientRect();
              const style = window.getComputedStyle(el);
              return rect.width > 0
                && rect.height > 0
                && style.display !== 'none'
                && style.visibility !== 'hidden'
                && style.pointerEvents !== 'none';
            }

            function cssEscape(value) {
              if (window.CSS && typeof window.CSS.escape === 'function') {
                return window.CSS.escape(value);
              }
              return String(value).replace(/(["\\\\#.:\\[\\]>+~*^$|=])/g, '\\\\$1');
            }

            function buildXPath(el) {
              if (!el || el.nodeType !== Node.ELEMENT_NODE) return '';
              if (el.id) {
                return `//*[@id="${el.id.replace(/"/g, '\\"')}"]`;
              }
              const parts = [];
              let current = el;
              while (current && current.nodeType === Node.ELEMENT_NODE) {
                let index = 1;
                let sibling = current.previousElementSibling;
                while (sibling) {
                  if (sibling.tagName === current.tagName) index++;
                  sibling = sibling.previousElementSibling;
                }
                parts.unshift(`${current.tagName.toLowerCase()}[${index}]`);
                current = current.parentElement;
              }
              return '/' + parts.join('/');
            }

            function buildLocator(el) {
              if (el.id) {
                return { locatorType: 'id', locatorValue: el.id };
              }
              if (el.name) {
                return { locatorType: 'name', locatorValue: el.name };
              }
              if (el.getAttribute('data-testid')) {
                return { locatorType: 'css', locatorValue: `[data-testid="${cssEscape(el.getAttribute('data-testid'))}"]` };
              }
              return { locatorType: 'xpath', locatorValue: buildXPath(el) };
            }

            for (const el of all) {
              if (!isVisible(el)) continue;
              const tag = (el.tagName || '').toLowerCase();
              const text = truncate(el.innerText || el.textContent || el.value || '');
              const placeholder = truncate(el.getAttribute('placeholder') || '');
              const ariaLabel = truncate(el.getAttribute('aria-label') || '');
              const role = truncate(el.getAttribute('role') || '');
              const type = truncate(el.getAttribute('type') || '');
              const id = truncate(el.id || '');
              const name = truncate(el.getAttribute('name') || '');
              const key = [tag, text, placeholder, ariaLabel, role, type, id, name].join('|');
              if (seen.has(key)) continue;
              seen.add(key);
              const locator = buildLocator(el);
              candidates.push({
                index: candidates.length + 1,
                tag,
                text,
                placeholder,
                ariaLabel,
                role,
                type,
                id,
                name,
                locatorType: locator.locatorType,
                locatorValue: locator.locatorValue
              });
              if (candidates.length >= MAX_COUNT) break;
            }

            return candidates;
            """;
}
