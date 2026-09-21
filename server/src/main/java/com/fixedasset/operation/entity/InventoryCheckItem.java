package com.fixedasset.operation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("inventory_check_item")
public class InventoryCheckItem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long inventoryCheckId;
    private Long assetId;
    private Long expectedDepartmentId;
    private Long actualDepartmentId;
    private String expectedStatus;
    private String actualStatus;
    private String result;
    private String remark;
}
