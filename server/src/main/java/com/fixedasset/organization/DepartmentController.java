package com.fixedasset.organization;

import com.fixedasset.common.model.ApiResponse;
import com.fixedasset.organization.entity.Department;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/departments")
public class DepartmentController {

    private final DepartmentService departmentService;

    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('department:read')")
    public ApiResponse<List<Department>> list(@RequestParam(required = false) String keyword) {
        return ApiResponse.ok(departmentService.list(keyword));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('department:write')")
    public ApiResponse<Department> create(@RequestBody Department department) {
        return ApiResponse.ok(departmentService.create(department));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('department:write')")
    public ApiResponse<Department> update(@PathVariable Long id, @RequestBody Department department) {
        return ApiResponse.ok(departmentService.update(id, department));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('department:write')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        departmentService.delete(id);
        return ApiResponse.ok();
    }
}
