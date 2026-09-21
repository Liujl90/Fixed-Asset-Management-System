package com.fixedasset.operation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("maintenance_plan")
public class MaintenancePlan {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long assetId;
    private String planName;
    private String maintenanceType;
    private LocalDate planDate;
    private Integer cycleMonths;
    private String status;
    private LocalDateTime completedAt;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
