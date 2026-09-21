package com.fixedasset.organization;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fixedasset.asset.mapper.AssetMapper;
import com.fixedasset.asset.entity.Asset;
import com.fixedasset.common.aop.OperationLog;
import com.fixedasset.common.exception.BusinessException;
import com.fixedasset.organization.entity.Department;
import com.fixedasset.organization.entity.Employee;
import com.fixedasset.organization.mapper.DepartmentMapper;
import com.fixedasset.organization.mapper.EmployeeMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DepartmentService {

    private final DepartmentMapper departmentMapper;
    private final EmployeeMapper employeeMapper;
    private final AssetMapper assetMapper;

    public DepartmentService(
            DepartmentMapper departmentMapper,
            EmployeeMapper employeeMapper,
            AssetMapper assetMapper
    ) {
        this.departmentMapper = departmentMapper;
        this.employeeMapper = employeeMapper;
        this.assetMapper = assetMapper;
    }

    public List<Department> list(String keyword) {
        return departmentMapper.selectList(Wrappers.<Department>lambdaQuery()
                .and(keyword != null && !keyword.isBlank(), query -> query
                        .like(Department::getName, keyword)
                        .or()
                        .like(Department::getCode, keyword))
                .orderByAsc(Department::getId));
    }

    @OperationLog(module = "部门管理", action = "新增部门")
    public Department create(Department department) {
        requireUniqueCode(department.getCode(), null);
        department.setId(null);
        department.setStatus(defaultStatus(department.getStatus()));
        department.setCreatedAt(LocalDateTime.now());
        department.setUpdatedAt(LocalDateTime.now());
        departmentMapper.insert(department);
        return department;
    }

    @OperationLog(module = "部门管理", action = "编辑部门")
    public Department update(Long id, Department department) {
        Department existing = require(id);
        requireUniqueCode(department.getCode(), id);
        existing.setName(department.getName());
        existing.setCode(department.getCode());
        existing.setManagerId(department.getManagerId());
        existing.setStatus(defaultStatus(department.getStatus()));
        existing.setUpdatedAt(LocalDateTime.now());
        departmentMapper.updateById(existing);
        return existing;
    }

    @OperationLog(module = "部门管理", action = "删除部门")
    public void delete(Long id) {
        require(id);
        long employeeCount = employeeMapper.selectCount(Wrappers.<Employee>lambdaQuery()
                .eq(Employee::getDepartmentId, id));
        long assetCount = assetMapper.selectCount(Wrappers.<Asset>lambdaQuery()
                .eq(Asset::getDepartmentId, id));
        if (employeeCount > 0 || assetCount > 0) {
            throw new BusinessException("该部门仍被员工或资产引用，不能删除");
        }
        departmentMapper.deleteById(id);
    }

    private Department require(Long id) {
        Department department = departmentMapper.selectById(id);
        if (department == null) {
            throw new BusinessException(404, "部门不存在");
        }
        return department;
    }

    private void requireUniqueCode(String code, Long excludeId) {
        long count = departmentMapper.selectCount(Wrappers.<Department>lambdaQuery()
                .eq(Department::getCode, code)
                .ne(excludeId != null, Department::getId, excludeId));
        if (count > 0) {
            throw new BusinessException("部门编码已存在");
        }
    }

    private String defaultStatus(String status) {
        return status == null || status.isBlank() ? "ACTIVE" : status;
    }
}
