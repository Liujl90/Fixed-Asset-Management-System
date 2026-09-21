package com.fixedasset.asset.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("asset_category")
public class AssetCategory {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String code;
    private Long parentId;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
