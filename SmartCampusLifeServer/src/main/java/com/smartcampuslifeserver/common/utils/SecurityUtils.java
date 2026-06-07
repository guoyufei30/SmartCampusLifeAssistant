package com.smartcampuslifeserver.common.utils;

import com.smartcampuslifeserver.common.exception.BusinessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static String getUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException(401, "未授权或Token无效");
        }
        Object principal = authentication.getPrincipal();
        if (principal == null || "anonymousUser".equals(principal)) {
            throw new BusinessException(401, "未授权或Token无效");
        }
        return principal.toString();
    }
}
