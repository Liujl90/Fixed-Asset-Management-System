package com.fixedasset.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record ProfileUpdateRequest(
        @NotBlank String realName,
        String phone,
        String email
) {
}
