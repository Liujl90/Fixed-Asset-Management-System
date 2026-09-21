package com.fixedasset.security;

import com.fixedasset.common.exception.BusinessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static AuthenticatedUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            throw new BusinessException(4010, "登录状态无效");
        }
        return user;
    }

    public static Long currentUserId() {
        return currentUser().userId();
    }

    public static String currentUsername() {
        return currentUser().username();
    }
}
