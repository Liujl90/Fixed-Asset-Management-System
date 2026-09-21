package com.fixedasset.lifecycle.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("loan_record")
public class LoanRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long assetId;
    private Long applicantId;
    private Long departmentId;
    private Long ownerId;
    private LocalDateTime requestedAt;
    private LocalDateTime approvedAt;
    private LocalDateTime loanDate;
    private LocalDateTime returnRequestedAt;
    private LocalDateTime returnDate;
    private String status;
    private String remark;
}
