package com.fixedasset.operation;

import com.fixedasset.common.model.ApiResponse;
import com.fixedasset.common.model.PageResult;
import com.fixedasset.operation.dto.OperationRequests.InventoryCreateRequest;
import com.fixedasset.operation.dto.OperationRequests.InventoryItemUpdateRequest;
import com.fixedasset.operation.entity.InventoryCheck;
import com.fixedasset.operation.entity.InventoryCheckItem;
import com.fixedasset.operation.service.InventoryService;
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

import java.util.Map;

@RestController
@RequestMapping("/api/operations/inventory-checks")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('inventory:read')")
    public ApiResponse<PageResult<InventoryCheck>> page(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String status
    ) {
        return ApiResponse.ok(inventoryService.page(page, size, status));
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAuthority('inventory:read')")
    public ApiResponse<Map<String, Object>> summary() {
        return ApiResponse.ok(inventoryService.summary());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('inventory:read')")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long id) {
        return ApiResponse.ok(inventoryService.detail(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('inventory:write')")
    public ApiResponse<InventoryCheck> create(
            @Valid @RequestBody InventoryCreateRequest request
    ) {
        return ApiResponse.ok(inventoryService.create(request));
    }

    @PutMapping("/items/{itemId}")
    @PreAuthorize("hasAuthority('inventory:write')")
    public ApiResponse<InventoryCheckItem> updateItem(
            @PathVariable Long itemId,
            @RequestBody InventoryItemUpdateRequest request
    ) {
        return ApiResponse.ok(inventoryService.updateItem(itemId, request));
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAuthority('inventory:write')")
    public ApiResponse<InventoryCheck> complete(@PathVariable Long id) {
        return ApiResponse.ok(inventoryService.complete(id));
    }
}
