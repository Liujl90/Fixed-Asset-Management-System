package com.fixedasset.organization.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fixedasset.organization.entity.Employee;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface EmployeeMapper extends BaseMapper<Employee> {
}
