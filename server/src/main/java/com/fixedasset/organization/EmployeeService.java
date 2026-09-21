package com.fixedasset.organization;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fixedasset.asset.entity.Asset;
import com.fixedasset.asset.mapper.AssetMapper;
import com.fixedasset.common.aop.OperationLog;
import com.fixedasset.common.exception.BusinessException;
import com.fixedasset.lifecycle.entity.LoanRecord;
import com.fixedasset.lifecycle.mapper.LoanRecordMapper;
import com.fixedasset.organization.entity.Department;
import com.fixedasset.organization.entity.Employee;
import com.fixedasset.organization.mapper.DepartmentMapper;
import com.fixedasset.organization.mapper.EmployeeMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class EmployeeService {

    private final EmployeeMapper employeeMapper;
    private final DepartmentMapper departmentMapper;
    private final AssetMapper assetMapper;
    private final LoanRecordMapper loanRecordMapper;

    public EmployeeService(
            EmployeeMapper employeeMapper,
            DepartmentMapper departmentMapper,
            AssetMapper assetMapper,
            LoanRecordMapper loanRecordMapper
    ) {
        this.employeeMapper = employeeMapper;
        this.departmentMapper = departmentMapper;
        this.assetMapper = assetMapper;
        this.loanRecordMapper = loanRecordMapper;
    }

    public List<Employee> list(String keyword, Long departmentId, String status) {
        return employeeMapper.selectList(Wrappers.<Employee>lambdaQuery()
                .and(keyword != null && !keyword.isBlank(), query -> query
                        .like(Employee::getName, keyword)
                        .or()
                        .like(Employee::getEmployeeNo, keyword)
                        .or()
                        .like(Employee::getPhone, keyword))
                .eq(departmentId != null, Employee::getDepartmentId, departmentId)
                .eq(status != null && !status.isBlank(), Employee::getStatus, status)
                .orderByAsc(Employee::getId));
    }

    @OperationLog(module = "员工管理", action = "新增员工")
    public Employee create(Employee employee) {
        validate(employee, null);
        employee.setId(null);
        employee.setStatus(employee.getStatus() == null ? "ACTIVE" : employee.getStatus());
        employee.setCreatedAt(LocalDateTime.now());
        employee.setUpdatedAt(LocalDateTime.now());
        employeeMapper.insert(employee);
        return employee;
    }

    @OperationLog(module = "员工管理", action = "编辑员工")
    public Employee update(Long id, Employee employee) {
        Employee existing = require(id);
        validate(employee, id);
        existing.setEmployeeNo(employee.getEmployeeNo());
        existing.setName(employee.getName());
        existing.setDepartmentId(employee.getDepartmentId());
        existing.setPhone(employee.getPhone());
        existing.setEmail(employee.getEmail());
        existing.setJoinDate(employee.getJoinDate());
        existing.setStatus(employee.getStatus());
        existing.setUpdatedAt(LocalDateTime.now());
        employeeMapper.updateById(existing);
        return existing;
    }

    @OperationLog(module = "员工管理", action = "删除员工")
    public void delete(Long id) {
        require(id);
        long assetCount = assetMapper.selectCount(Wrappers.<Asset>lambdaQuery()
                .eq(Asset::getOwnerId, id));
        long activeLoanCount = loanRecordMapper.selectCount(Wrappers.<LoanRecord>lambdaQuery()
                .eq(LoanRecord::getOwnerId, id)
                .in(LoanRecord::getStatus, "ACTIVE", "RETURN_PENDING"));
        long managerCount = departmentMapper.selectCount(Wrappers.<Department>lambdaQuery()
                .eq(Department::getManagerId, id));
        if (assetCount > 0 || activeLoanCount > 0 || managerCount > 0) {
            throw new BusinessException("该员工仍关联资产、在用记录或部门负责人，不能删除");
        }
        employeeMapper.deleteById(id);
    }

    private Employee require(Long id) {
        Employee employee = employeeMapper.selectById(id);
        if (employee == null) {
            throw new BusinessException(404, "员工不存在");
        }
        return employee;
    }

    private void validate(Employee employee, Long excludeId) {
        if (employee.getDepartmentId() == null || departmentMapper.selectById(employee.getDepartmentId()) == null) {
            throw new BusinessException("所属部门不存在");
        }
        long count = employeeMapper.selectCount(Wrappers.<Employee>lambdaQuery()
                .eq(Employee::getEmployeeNo, employee.getEmployeeNo())
                .ne(excludeId != null, Employee::getId, excludeId));
        if (count > 0) {
            throw new BusinessException("员工工号已存在");
        }
    }
}
