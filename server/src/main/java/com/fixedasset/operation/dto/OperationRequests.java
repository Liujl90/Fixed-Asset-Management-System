package com.fixedasset.operation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class OperationRequests {

    private OperationRequests() {
    }

    public record MaintenanceCreateRequest(
            @NotBlank String maintenanceNo,
            @NotNull Long assetId,
            @NotBlank String maintenanceType,
            @NotBlank String description,
            BigDecimal cost,
            LocalDate startDate,
            @NotNull Long operatorId
    ) {
    }

    public record MaintenanceCompleteRequest(
            String result,
            BigDecimal cost,
            LocalDate endDate
    ) {
    }

    public record InventoryCreateRequest(
            @NotBlank String checkNo,
            @NotBlank String checkName,
            Long departmentId,
            @NotNull LocalDate checkDate,
            @NotNull Long operatorId,
            String remark
    ) {
    }

    public record InventoryItemUpdateRequest(
            Long actualDepartmentId,
            String actualStatus,
            String result,
            String remark
    ) {
    }

    public record ScrapCreateRequest(
            @NotBlank String scrapNo,
            @NotNull Long assetId,
            @NotBlank String reason,
            @NotNull Long applicantId,
            String remark
    ) {
    }

    public record ScrapCompleteRequest(
            @NotBlank String disposalMethod,
            BigDecimal disposalAmount,
            String remark
    ) {
    }
}
