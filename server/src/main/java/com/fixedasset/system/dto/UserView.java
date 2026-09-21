package com.fixedasset.system.dto;

public record UserView(
        Long id,
        String username,
        String realName,
        Long roleId,
        String roleName,
        Long employeeId,
        String phone,
        String email,
        String status
) {
}
