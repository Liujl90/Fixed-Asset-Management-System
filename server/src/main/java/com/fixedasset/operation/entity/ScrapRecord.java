package com.fixedasset.operation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("scrap_record")
public class ScrapRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String scrapNo;
    private Long assetId;
    private String reason;
    private Long applicantId;
    private String status;
    private Long approvedBy;
    private LocalDateTime approvedAt;
    private String disposalMethod;
    private BigDecimal disposalAmount;
    private LocalDateTime completedAt;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
