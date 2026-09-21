package com.fixedasset.auth.dto;

import java.util.Set;

public record UserProfileResponse(
        Long id,
        String username,
        String realName,
        String phone,
        String email,
        Long employeeId,
        String status,
        Set<String> roles,
        Set<String> permissions
) {
}
