package com.fixedasset.operation;

import com.fixedasset.common.model.ApiResponse;
import com.fixedasset.common.model.PageResult;
import com.fixedasset.operation.entity.DepreciationRecord;
import com.fixedasset.operation.entity.MaintenancePlan;
import com.fixedasset.operation.service.DepreciationService;
import com.fixedasset.operation.service.MaintenanceService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/operations")
/**
 * 运维接口：保养计划、折旧记录以及 Quartz 任务手动触发入口。
 */
public class OperationController {

    private final MaintenanceService maintenanceService;
    private final DepreciationService depreciationService;

    public OperationController(
            MaintenanceService maintenanceService,
            DepreciationService depreciationService
    ) {
        this.maintenanceService = maintenanceService;
        this.depreciationService = depreciationService;
    }

    @GetMapping("/maintenance-plans")
    @PreAuthorize("hasAuthority('maintenance:read')")
    public ApiResponse<PageResult<MaintenancePlan>> maintenancePlans(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String status
    ) {
        return ApiResponse.ok(maintenanceService.page(page, size, status));
    }

    @PostMapping("/maintenance-plans")
    @PreAuthorize("hasAuthority('maintenance:write')")
    public ApiResponse<MaintenancePlan> createMaintenancePlan(@RequestBody MaintenancePlan plan) {
        return ApiResponse.ok(maintenanceService.create(plan));
    }

    @PutMapping("/maintenance-plans/{id}")
    @PreAuthorize("hasAuthority('maintenance:write')")
    public ApiResponse<MaintenancePlan> updateMaintenancePlan(
            @PathVariable Long id,
            @RequestBody MaintenancePlan plan
    ) {
        return ApiResponse.ok(maintenanceService.update(id, plan));
    }

    @PatchMapping("/maintenance-plans/{id}/complete")
    @PreAuthorize("hasAuthority('maintenance:write')")
    public ApiResponse<MaintenancePlan> completeMaintenancePlan(@PathVariable Long id) {
        return ApiResponse.ok(maintenanceService.complete(id));
    }

    @GetMapping("/depreciations")
    @PreAuthorize("hasAuthority('depreciation:read')")
    public ApiResponse<PageResult<DepreciationRecord>> depreciations(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String month
    ) {
        return ApiResponse.ok(depreciationService.page(page, size, month));
    }

    @PostMapping("/jobs/depreciation")
    @PreAuthorize("hasAuthority('depreciation:run')")
    public ApiResponse<Map<String, Object>> runDepreciation(
            @RequestBody(required = false) Map<String, String> payload
    ) {
        String month = payload == null ? null : payload.get("month");
        return ApiResponse.ok(Map.of("created", depreciationService.runMonthlyDepreciation(month)));
    }

    @PostMapping("/jobs/maintenance-check")
    @PreAuthorize("hasAuthority('maintenance:write')")
    public ApiResponse<Map<String, Object>> runMaintenanceCheck() {
        return ApiResponse.ok(Map.of("updated", maintenanceService.checkDuePlans()));
    }
}
