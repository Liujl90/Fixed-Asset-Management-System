package com.fixedasset.operation.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fixedasset.asset.AssetService;
import com.fixedasset.asset.entity.Asset;
import com.fixedasset.asset.mapper.AssetMapper;
import com.fixedasset.common.aop.OperationLog;
import com.fixedasset.common.exception.BusinessException;
import com.fixedasset.common.model.PageResult;
import com.fixedasset.operation.dto.OperationRequests.InventoryCreateRequest;
import com.fixedasset.operation.dto.OperationRequests.InventoryItemUpdateRequest;
import com.fixedasset.operation.entity.InventoryCheck;
import com.fixedasset.operation.entity.InventoryCheckItem;
import com.fixedasset.operation.mapper.InventoryCheckItemMapper;
import com.fixedasset.operation.mapper.InventoryCheckMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class InventoryService {

    private final InventoryCheckMapper inventoryCheckMapper;
    private final InventoryCheckItemMapper inventoryItemMapper;
    private final AssetMapper assetMapper;
    private final AssetService assetService;

    public InventoryService(
            InventoryCheckMapper inventoryCheckMapper,
            InventoryCheckItemMapper inventoryItemMapper,
            AssetMapper assetMapper,
            AssetService assetService
    ) {
        this.inventoryCheckMapper = inventoryCheckMapper;
        this.inventoryItemMapper = inventoryItemMapper;
        this.assetMapper = assetMapper;
        this.assetService = assetService;
    }

    public PageResult<InventoryCheck> page(long page, long size, String status) {
        Page<InventoryCheck> result = inventoryCheckMapper.selectPage(new Page<>(page, size),
                Wrappers.<InventoryCheck>lambdaQuery()
                        .eq(status != null && !status.isBlank(), InventoryCheck::getStatus, status)
                        .orderByDesc(InventoryCheck::getCheckDate));
        return PageResult.from(result);
    }

    public Map<String, Object> detail(Long id) {
        InventoryCheck check = require(id);
        List<InventoryCheckItem> items = inventoryItemMapper.selectList(
                Wrappers.<InventoryCheckItem>lambdaQuery()
                        .eq(InventoryCheckItem::getInventoryCheckId, id));
        return Map.of("check", check, "items", items);
    }

    @Transactional
    @OperationLog(module = "资产盘点", action = "创建盘点任务")
    public InventoryCheck create(InventoryCreateRequest request) {
        if (inventoryCheckMapper.selectCount(Wrappers.<InventoryCheck>lambdaQuery()
                .eq(InventoryCheck::getCheckNo, request.checkNo())) > 0) {
            throw new BusinessException("盘点单号已存在");
        }
        List<Asset> assets = assetMapper.selectList(Wrappers.<Asset>lambdaQuery()
                .ne(Asset::getStatus, "SCRAPPED")
                .eq(request.departmentId() != null, Asset::getDepartmentId, request.departmentId())
                .orderByAsc(Asset::getId));
        if (assets.isEmpty()) {
            throw new BusinessException("盘点范围内没有资产");
        }
        InventoryCheck check = new InventoryCheck();
        check.setCheckNo(request.checkNo());
        check.setCheckName(request.checkName());
        check.setDepartmentId(request.departmentId());
        check.setCheckDate(request.checkDate());
        check.setStatus("IN_PROGRESS");
        check.setOperatorId(request.operatorId());
        check.setTotalCount(assets.size());
        check.setNormalCount(0);
        check.setAbnormalCount(0);
        check.setRemark(request.remark());
        check.setCreatedAt(LocalDateTime.now());
        check.setUpdatedAt(LocalDateTime.now());
        inventoryCheckMapper.insert(check);

        for (Asset asset : assets) {
            InventoryCheckItem item = new InventoryCheckItem();
            item.setInventoryCheckId(check.getId());
            item.setAssetId(asset.getId());
            item.setExpectedDepartmentId(asset.getDepartmentId());
            item.setExpectedStatus(asset.getStatus());
            item.setResult("PENDING");
            inventoryItemMapper.insert(item);
        }
        return check;
    }

    @OperationLog(module = "资产盘点", action = "录入盘点结果")
    public InventoryCheckItem updateItem(Long id, InventoryItemUpdateRequest request) {
        InventoryCheckItem item = inventoryItemMapper.selectById(id);
        if (item == null) {
            throw new BusinessException(404, "盘点明细不存在");
        }
        InventoryCheck check = require(item.getInventoryCheckId());
        if (!"IN_PROGRESS".equals(check.getStatus())) {
            throw new BusinessException("盘点任务已结束");
        }
        item.setActualDepartmentId(request.actualDepartmentId());
        item.setActualStatus(request.actualStatus());
        item.setResult(request.result());
        item.setRemark(request.remark());
        inventoryItemMapper.updateById(item);
        return item;
    }

    @Transactional
    @OperationLog(module = "资产盘点", action = "完成盘点任务")
    public InventoryCheck complete(Long id) {
        InventoryCheck check = require(id);
        if (!"IN_PROGRESS".equals(check.getStatus())) {
            throw new BusinessException("盘点任务当前不可完成");
        }
        List<InventoryCheckItem> items = inventoryItemMapper.selectList(
                Wrappers.<InventoryCheckItem>lambdaQuery()
                        .eq(InventoryCheckItem::getInventoryCheckId, id));
        int normal = 0;
        int abnormal = 0;
        for (InventoryCheckItem item : items) {
            if (item.getActualStatus() == null) {
                item.setActualStatus(item.getExpectedStatus());
            }
            if (item.getActualDepartmentId() == null) {
                item.setActualDepartmentId(item.getExpectedDepartmentId());
            }
            String result = item.getResult();
            if (result == null || "PENDING".equals(result)) {
                boolean sameStatus = Objects.equals(item.getExpectedStatus(), item.getActualStatus());
                boolean sameDepartment =
                        Objects.equals(item.getExpectedDepartmentId(), item.getActualDepartmentId());
                result = sameStatus && sameDepartment ? "NORMAL" : "ABNORMAL";
                item.setResult(result);
            }
            inventoryItemMapper.updateById(item);
            if ("NORMAL".equals(result)) {
                normal++;
            } else {
                abnormal++;
            }
        }
        check.setNormalCount(normal);
        check.setAbnormalCount(abnormal);
        check.setStatus("COMPLETED");
        check.setUpdatedAt(LocalDateTime.now());
        inventoryCheckMapper.updateById(check);
        return check;
    }

    public Map<String, Object> summary() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", inventoryCheckMapper.selectCount(null));
        result.put("inProgress", inventoryCheckMapper.selectCount(Wrappers.<InventoryCheck>lambdaQuery()
                .eq(InventoryCheck::getStatus, "IN_PROGRESS")));
        result.put("completed", inventoryCheckMapper.selectCount(Wrappers.<InventoryCheck>lambdaQuery()
                .eq(InventoryCheck::getStatus, "COMPLETED")));
        return result;
    }

    private InventoryCheck require(Long id) {
        InventoryCheck check = inventoryCheckMapper.selectById(id);
        if (check == null) {
            throw new BusinessException(404, "盘点任务不存在");
        }
        return check;
    }
}
