package com.fixedasset.system.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record RoleUpdateRequest(
        @NotBlank String name,
        @NotBlank String code,
        String description,
        List<Long> permissionIds
) {
}
