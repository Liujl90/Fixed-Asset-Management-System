package com.fixedasset.security;

import java.util.Set;

/**
 * JWT 解析后的当前登录用户。
 *
 * <p>该对象作为 Spring Security principal 放入 SecurityContext，业务层通过
 * {@link SecurityUtils} 获取，不直接依赖 Controller 或 HttpServletRequest。</p>
 */
public record AuthenticatedUser(
        Long userId,
        String username,
        String realName,
        Long employeeId,
        Set<String> roles,
        Set<String> permissions
) {
}
