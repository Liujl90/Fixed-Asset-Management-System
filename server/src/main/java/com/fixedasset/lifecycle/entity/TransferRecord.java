package com.fixedasset.lifecycle.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("transfer_record")
public class TransferRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long assetId;
    private Long fromDepartmentId;
    private Long toDepartmentId;
    private Long fromOwnerId;
    private Long toOwnerId;
    private String reason;
    private LocalDateTime transferredAt;
    private String status;
}
