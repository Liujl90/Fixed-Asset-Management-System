package com.fixedasset.asset.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("asset_change_record")
public class AssetChangeRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long assetId;
    private String type;
    private String description;
    private String operator;
    private LocalDateTime createdAt;
}
