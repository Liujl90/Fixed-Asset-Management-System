package com.fixedasset.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UserSaveRequest(
        @NotBlank String username,
        String password,
        @NotBlank String realName,
        @NotNull Long roleId,
        Long employeeId,
        String phone,
        String email,
        String status
) {
}
