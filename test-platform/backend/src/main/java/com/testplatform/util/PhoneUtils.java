package com.testplatform.util;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public final class PhoneUtils {

    private PhoneUtils() {
    }

    public static String normalizeAndValidate(String rawPhone) {
        String normalized = rawPhone == null ? "" : rawPhone.replaceAll("\\D", "");
        if (!normalized.matches("\\d{11}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "手机号必须为11位数字");
        }
        return normalized;
    }
}
