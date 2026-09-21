package com.fixedasset.operation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("inventory_check")
public class InventoryCheck {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String checkNo;
    private String checkName;
    private Long departmentId;
    private LocalDate checkDate;
    private String status;
    private Long operatorId;
    private Integer totalCount;
    private Integer normalCount;
    private Integer abnormalCount;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
