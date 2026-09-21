package com.fixedasset.asset.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("asset")
public class Asset {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String assetNo;
    private String name;
    private Long categoryId;
    private String brandModel;
    private LocalDate purchaseDate;
    private BigDecimal originalValue;
    private Integer usefulLife;
    private Long departmentId;
    private Long ownerId;
    private String status;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
