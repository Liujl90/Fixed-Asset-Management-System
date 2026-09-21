package com.fixedasset.asset.excel;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AssetExcelRow {
    @ExcelProperty("资产编号")
    private String assetNo;
    @ExcelProperty("资产名称")
    private String name;
    @ExcelProperty("分类ID")
    private Long categoryId;
    @ExcelProperty("品牌型号")
    private String brandModel;
    @ExcelProperty("购买日期")
    private String purchaseDate;
    @ExcelProperty("资产原值")
    private BigDecimal originalValue;
    @ExcelProperty("使用年限")
    private Integer usefulLife;
    @ExcelProperty("部门ID")
    private Long departmentId;
    @ExcelProperty("负责人ID")
    private Long ownerId;
    @ExcelProperty("资产状态")
    private String status;
    @ExcelProperty("备注")
    private String remark;
}
