package com.fixedasset.supply.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class SupplyRequests {

    private SupplyRequests() {
    }

    public record PurchaseItemRequest(
            @NotBlank String assetName,
            @NotNull Long categoryId,
            @NotNull Integer quantity,
            @NotNull BigDecimal unitPrice,
            String remark
    ) {
    }

    public record PurchaseOrderRequest(
            @NotBlank String orderNo,
            @NotNull Long supplierId,
            @NotNull Long applicantId,
            @NotNull LocalDate orderDate,
            LocalDate expectedDate,
            String remark,
            @NotEmpty List<PurchaseItemRequest> items
    ) {
    }

    public record InboundItemRequest(
            @NotBlank String assetName,
            @NotNull Long categoryId,
            String brandModel,
            @NotNull Integer quantity,
            @NotNull BigDecimal unitPrice,
            @NotNull Long departmentId,
            String remark
    ) {
    }

    public record InboundOrderRequest(
            @NotBlank String inboundNo,
            Long purchaseOrderId,
            @NotNull Long supplierId,
            @NotBlank String warehouseName,
            @NotNull LocalDate inboundDate,
            @NotNull Long operatorId,
            String remark,
            @NotEmpty List<InboundItemRequest> items
    ) {
    }
}
