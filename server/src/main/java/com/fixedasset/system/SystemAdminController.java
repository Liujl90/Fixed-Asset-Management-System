package com.fixedasset.system;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fixedasset.common.model.ApiResponse;
import com.fixedasset.common.model.PageResult;
import com.fixedasset.system.dto.RoleUpdateRequest;
import com.fixedasset.system.dto.UserSaveRequest;
import com.fixedasset.system.dto.UserView;
import com.fixedasset.system.entity.SysOperationLog;
import com.fixedasset.system.entity.SysPermission;
import com.fixedasset.system.entity.SysRole;
import com.fixedasset.system.mapper.SysOperationLogMapper;
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

@RestController
@RequestMapping("/api/system")
public class SystemAdminController {

    private final SystemAdminService systemAdminService;
    private final SysOperationLogMapper operationLogMapper;

    public SystemAdminController(
            SystemAdminService systemAdminService,
            SysOperationLogMapper operationLogMapper
    ) {
        this.systemAdminService = systemAdminService;
        this.operationLogMapper = operationLogMapper;
    }

    @GetMapping("/users")
    @PreAuthorize("hasAuthority('user:read')")
    public ApiResponse<List<UserView>> users() {
        return ApiResponse.ok(systemAdminService.users());
    }

    @PostMapping("/users")
    @PreAuthorize("hasAuthority('user:write')")
    public ApiResponse<UserView> createUser(@Valid @RequestBody UserSaveRequest request) {
        return ApiResponse.ok(systemAdminService.createUser(request));
    }

    @PutMapping("/users/{id}")
    @PreAuthorize("hasAuthority('user:write')")
    public ApiResponse<UserView> updateUser(@PathVariable Long id, @Valid @RequestBody UserSaveRequest request) {
        return ApiResponse.ok(systemAdminService.updateUser(id, request));
    }

    @GetMapping("/roles")
    @PreAuthorize("hasAuthority('role:read')")
    public ApiResponse<List<SysRole>> roles() {
        return ApiResponse.ok(systemAdminService.roles());
    }

    @GetMapping("/roles/{id}/permissions")
    @PreAuthorize("hasAuthority('role:read')")
    public ApiResponse<List<Long>> rolePermissions(@PathVariable Long id) {
        return ApiResponse.ok(systemAdminService.rolePermissionIds(id));
    }

    @PutMapping("/roles/{id}")
    @PreAuthorize("hasAuthority('role:write')")
    public ApiResponse<SysRole> updateRole(
            @PathVariable Long id,
            @Valid @RequestBody RoleUpdateRequest request
    ) {
        return ApiResponse.ok(systemAdminService.updateRole(id, request));
    }

    @GetMapping("/permissions")
    @PreAuthorize("hasAuthority('role:read')")
    public ApiResponse<List<SysPermission>> permissions() {
        return ApiResponse.ok(systemAdminService.permissions());
    }

    @GetMapping("/logs")
    @PreAuthorize("hasAuthority('log:read')")
    public ApiResponse<PageResult<SysOperationLog>> logs(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size
    ) {
        var result = operationLogMapper.selectPage(
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, size),
                Wrappers.<SysOperationLog>lambdaQuery().orderByDesc(SysOperationLog::getCreatedAt));
        return ApiResponse.ok(PageResult.from(result));
    }
}
