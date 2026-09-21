package com.fixedasset.supply;

import com.fixedasset.common.model.ApiResponse;
import com.fixedasset.common.model.PageResult;
import com.fixedasset.supply.dto.SupplyRequests.InboundOrderRequest;
import com.fixedasset.supply.dto.SupplyRequests.PurchaseOrderRequest;
import com.fixedasset.supply.entity.InboundOrder;
import com.fixedasset.supply.entity.PurchaseOrder;
import com.fixedasset.supply.entity.Supplier;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 供应商、采购单和入库单接口。
 */
@RestController
@RequestMapping("/api")
public class SupplyController {

    private final SupplyService supplyService;

    public SupplyController(SupplyService supplyService) {
        this.supplyService = supplyService;
    }

    @GetMapping("/suppliers")
    @PreAuthorize("hasAuthority('supplier:read')")
    public ApiResponse<List<Supplier>> suppliers(@RequestParam(required = false) String keyword) {
        return ApiResponse.ok(supplyService.suppliers(keyword));
    }

    @PostMapping("/suppliers")
    @PreAuthorize("hasAuthority('supplier:write')")
    public ApiResponse<Supplier> createSupplier(@RequestBody Supplier supplier) {
        return ApiResponse.ok(supplyService.createSupplier(supplier));
    }

    @PutMapping("/suppliers/{id}")
    @PreAuthorize("hasAuthority('supplier:write')")
    public ApiResponse<Supplier> updateSupplier(@PathVariable Long id, @RequestBody Supplier supplier) {
        return ApiResponse.ok(supplyService.updateSupplier(id, supplier));
    }

    @GetMapping("/purchases")
    @PreAuthorize("hasAuthority('purchase:read')")
    public ApiResponse<PageResult<PurchaseOrder>> purchases(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword
    ) {
        return ApiResponse.ok(supplyService.purchasePage(page, size, status, keyword));
    }

    @GetMapping("/purchases/{id}")
    @PreAuthorize("hasAuthority('purchase:read')")
    public ApiResponse<Map<String, Object>> purchaseDetail(@PathVariable Long id) {
        return ApiResponse.ok(supplyService.purchaseDetail(id));
    }

    @PostMapping("/purchases")
    @PreAuthorize("hasAuthority('purchase:write')")
    public ApiResponse<PurchaseOrder> createPurchase(@Valid @RequestBody PurchaseOrderRequest request) {
        return ApiResponse.ok(supplyService.createPurchase(request));
    }

    @PutMapping("/purchases/{id}")
    @PreAuthorize("hasAuthority('purchase:write')")
    public ApiResponse<PurchaseOrder> updatePurchase(
            @PathVariable Long id,
            @Valid @RequestBody PurchaseOrderRequest request
    ) {
        return ApiResponse.ok(supplyService.updatePurchase(id, request));
    }

    @PostMapping("/purchases/{id}/submit")
    @PreAuthorize("hasAuthority('purchase:write')")
    public ApiResponse<PurchaseOrder> submitPurchase(@PathVariable Long id) {
        return ApiResponse.ok(supplyService.submitPurchase(id));
    }

    @PostMapping("/purchases/{id}/approve")
    @PreAuthorize("hasAuthority('purchase:approve')")
    public ApiResponse<PurchaseOrder> approvePurchase(@PathVariable Long id) {
        return ApiResponse.ok(supplyService.approvePurchase(id));
    }

    @PostMapping("/purchases/{id}/reject")
    @PreAuthorize("hasAuthority('purchase:approve')")
    public ApiResponse<PurchaseOrder> rejectPurchase(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> payload
    ) {
        return ApiResponse.ok(supplyService.rejectPurchase(id, payload == null ? null : payload.get("reason")));
    }

    @GetMapping("/inbounds")
    @PreAuthorize("hasAuthority('inbound:read')")
    public ApiResponse<PageResult<InboundOrder>> inbounds(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String status
    ) {
        return ApiResponse.ok(supplyService.inboundPage(page, size, status));
    }

    @GetMapping("/inbounds/{id}")
    @PreAuthorize("hasAuthority('inbound:read')")
    public ApiResponse<Map<String, Object>> inboundDetail(@PathVariable Long id) {
        return ApiResponse.ok(supplyService.inboundDetail(id));
    }

    @PostMapping("/inbounds")
    @PreAuthorize("hasAuthority('inbound:write')")
    public ApiResponse<InboundOrder> createInbound(@Valid @RequestBody InboundOrderRequest request) {
        return ApiResponse.ok(supplyService.createInbound(request));
    }

    @PostMapping("/inbounds/{id}/confirm")
    @PreAuthorize("hasAuthority('inbound:write')")
    public ApiResponse<Map<String, Object>> confirmInbound(@PathVariable Long id) {
        return ApiResponse.ok(supplyService.confirmInbound(id));
    }

    @PostMapping("/inbounds/{id}/cancel")
    @PreAuthorize("hasAuthority('inbound:write')")
    public ApiResponse<InboundOrder> cancelInbound(@PathVariable Long id) {
        return ApiResponse.ok(supplyService.cancelInbound(id));
    }
}
