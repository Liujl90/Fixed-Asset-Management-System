package com.fixedasset.lifecycle.dto;

import jakarta.validation.constraints.NotNull;

public record LoanApplyRequest(
        @NotNull Long assetId,
        Long applicantId,
        Long departmentId,
        String remark
) {
}
