package com.fixedasset.security;

import java.util.Set;

public record AuthenticatedUser(
        Long userId,
        String username,
        String realName,
        Long employeeId,
        Set<String> roles,
        Set<String> permissions
) {
}
