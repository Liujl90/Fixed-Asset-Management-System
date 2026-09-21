package com.fixedasset.supply.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("inbound_order_item")
public class InboundOrderItem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long inboundOrderId;
    private String assetName;
    private Long categoryId;
    private String brandModel;
    private Integer quantity;
    private BigDecimal unitPrice;
    private Long departmentId;
    private String remark;
}
