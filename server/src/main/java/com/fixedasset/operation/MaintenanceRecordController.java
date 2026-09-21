package com.fixedasset.operation;

import com.fixedasset.common.model.ApiResponse;
import com.fixedasset.common.model.PageResult;
import com.fixedasset.operation.dto.OperationRequests.MaintenanceCompleteRequest;
import com.fixedasset.operation.dto.OperationRequests.MaintenanceCreateRequest;
import com.fixedasset.operation.entity.MaintenanceRecord;
import com.fixedasset.operation.service.MaintenanceRecordService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/operations/maintenance-records")
public class MaintenanceRecordController {

    private final MaintenanceRecordService maintenanceRecordService;

    public MaintenanceRecordController(MaintenanceRecordService maintenanceRecordService) {
        this.maintenanceRecordService = maintenanceRecordService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('maintenance:read')")
    public ApiResponse<PageResult<MaintenanceRecord>> page(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String status
    ) {
        return ApiResponse.ok(maintenanceRecordService.page(page, size, status));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('maintenance:write')")
    public ApiResponse<MaintenanceRecord> create(
            @Valid @RequestBody MaintenanceCreateRequest request
    ) {
        return ApiResponse.ok(maintenanceRecordService.create(request));
    }

    @PostMapping("/{id}/start")
    @PreAuthorize("hasAuthority('maintenance:write')")
    public ApiResponse<MaintenanceRecord> start(@PathVariable Long id) {
        return ApiResponse.ok(maintenanceRecordService.start(id));
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAuthority('maintenance:write')")
    public ApiResponse<MaintenanceRecord> complete(
            @PathVariable Long id,
            @Valid @RequestBody MaintenanceCompleteRequest request
    ) {
        return ApiResponse.ok(maintenanceRecordService.complete(id, request));
    }
}
