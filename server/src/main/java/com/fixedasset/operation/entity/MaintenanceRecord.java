package com.fixedasset.operation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("maintenance_record")
public class MaintenanceRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String maintenanceNo;
    private Long assetId;
    private String assetStatusBefore;
    private String maintenanceType;
    private String description;
    private BigDecimal cost;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
    private Long operatorId;
    private String result;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
