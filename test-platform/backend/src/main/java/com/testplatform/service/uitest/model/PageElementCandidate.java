package com.testplatform.service.uitest.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PageElementCandidate {

    private Integer index;
    private String tag;
    private String text;
    private String placeholder;
    private String ariaLabel;
    private String role;
    private String type;
    private String id;
    private String name;
    private String locatorType;
    private String locatorValue;

    public String searchableText() {
        return join(text, placeholder, ariaLabel, role, type, id, name, tag);
    }

    private String join(String... values) {
        StringBuilder builder = new StringBuilder();
        for (String value : values) {
            if (value == null || value.isBlank()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(value.trim());
        }
        return builder.toString();
    }
}
