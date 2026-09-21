package com.fixedasset.organization.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("employee")
public class Employee {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String employeeNo;
    private String name;
    private Long departmentId;
    private String phone;
    private String email;
    private LocalDate joinDate;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
