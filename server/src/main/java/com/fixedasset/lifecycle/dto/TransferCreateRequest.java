package com.fixedasset.lifecycle.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TransferCreateRequest(
        @NotNull Long assetId,
        @NotNull Long toDepartmentId,
        @NotNull Long toOwnerId,
        @NotBlank String reason
) {
}
