package com.fixedasset.organization;

import com.fixedasset.common.model.ApiResponse;
import com.fixedasset.organization.entity.Employee;
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
@RequestMapping("/api/employees")
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('employee:read')")
    public ApiResponse<List<Employee>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) String status
    ) {
        return ApiResponse.ok(employeeService.list(keyword, departmentId, status));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('employee:write')")
    public ApiResponse<Employee> create(@RequestBody Employee employee) {
        return ApiResponse.ok(employeeService.create(employee));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('employee:write')")
    public ApiResponse<Employee> update(@PathVariable Long id, @RequestBody Employee employee) {
        return ApiResponse.ok(employeeService.update(id, employee));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('employee:write')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        employeeService.delete(id);
        return ApiResponse.ok();
    }
}
