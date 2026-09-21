package com.fixedasset.operation.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fixedasset.asset.AssetService;
import com.fixedasset.asset.entity.Asset;
import com.fixedasset.common.aop.OperationLog;
import com.fixedasset.common.exception.BusinessException;
import com.fixedasset.common.model.PageResult;
import com.fixedasset.operation.dto.OperationRequests.MaintenanceCompleteRequest;
import com.fixedasset.operation.dto.OperationRequests.MaintenanceCreateRequest;
import com.fixedasset.operation.entity.MaintenanceRecord;
import com.fixedasset.operation.mapper.MaintenanceRecordMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 维修记录状态机。
 *
 * <pre>
 * PENDING --start--> PROCESSING --complete--> COMPLETED
 * </pre>
 *
 * <p>开始维修时将资产置为 MAINTENANCE，完成时恢复原状态，原状态记录在
 * {@code asset_status_before} 中，避免维修完成后无法判断资产应回到闲置还是在用。</p>
 */
@Service
public class MaintenanceRecordService {

    private final MaintenanceRecordMapper maintenanceRecordMapper;
    private final AssetService assetService;

    public MaintenanceRecordService(
            MaintenanceRecordMapper maintenanceRecordMapper,
            AssetService assetService
    ) {
        this.maintenanceRecordMapper = maintenanceRecordMapper;
        this.assetService = assetService;
    }

    public PageResult<MaintenanceRecord> page(long page, long size, String status) {
        Page<MaintenanceRecord> result = maintenanceRecordMapper.selectPage(new Page<>(page, size),
                Wrappers.<MaintenanceRecord>lambdaQuery()
                        .eq(status != null && !status.isBlank(), MaintenanceRecord::getStatus, status)
                        .orderByDesc(MaintenanceRecord::getCreatedAt));
        return PageResult.from(result);
    }

    @Transactional
    @OperationLog(module = "维修管理", action = "创建维修记录")
    public MaintenanceRecord create(MaintenanceCreateRequest request) {
        if (maintenanceRecordMapper.selectCount(Wrappers.<MaintenanceRecord>lambdaQuery()
                .eq(MaintenanceRecord::getMaintenanceNo, request.maintenanceNo())) > 0) {
            throw new BusinessException("维修单号已存在");
        }
        Asset asset = assetService.require(request.assetId());
        if ("SCRAPPED".equals(asset.getStatus())) {
            throw new BusinessException("报废资产不能创建维修记录");
        }
        MaintenanceRecord record = new MaintenanceRecord();
        record.setMaintenanceNo(request.maintenanceNo());
        record.setAssetId(asset.getId());
        record.setAssetStatusBefore(asset.getStatus());
        record.setMaintenanceType(request.maintenanceType());
        record.setDescription(request.description());
        record.setCost(request.cost() == null ? BigDecimal.ZERO : request.cost());
        record.setStartDate(request.startDate());
        record.setStatus("PENDING");
        record.setOperatorId(request.operatorId());
        record.setCreatedAt(LocalDateTime.now());
        record.setUpdatedAt(LocalDateTime.now());
        maintenanceRecordMapper.insert(record);
        return record;
    }

    @Transactional
    @OperationLog(module = "维修管理", action = "开始维修")
    public MaintenanceRecord start(Long id) {
        // 维修中资产不可被领用或报废，因此开始维修需要同步资产状态。
        MaintenanceRecord record = require(id);
        if (!"PENDING".equals(record.getStatus())) {
            throw new BusinessException("只有待处理维修单可以开始");
        }
        assetService.updateStatus(record.getAssetId(), "MAINTENANCE");
        record.setStatus("PROCESSING");
        record.setStartDate(record.getStartDate() == null ? LocalDate.now() : record.getStartDate());
        record.setUpdatedAt(LocalDateTime.now());
        maintenanceRecordMapper.updateById(record);
        return record;
    }

    @Transactional
    @OperationLog(module = "维修管理", action = "完成维修")
    public MaintenanceRecord complete(Long id, MaintenanceCompleteRequest request) {
        // 先保存维修结果，再将资产恢复到维修前状态。
        MaintenanceRecord record = require(id);
        if (!"PROCESSING".equals(record.getStatus())) {
            throw new BusinessException("只有处理中维修单可以完成");
        }
        record.setStatus("COMPLETED");
        record.setResult(request.result());
        record.setCost(request.cost() == null ? record.getCost() : request.cost());
        record.setEndDate(request.endDate() == null ? LocalDate.now() : request.endDate());
        record.setUpdatedAt(LocalDateTime.now());
        maintenanceRecordMapper.updateById(record);
        assetService.updateStatus(record.getAssetId(), record.getAssetStatusBefore());
        return record;
    }

    private MaintenanceRecord require(Long id) {
        MaintenanceRecord record = maintenanceRecordMapper.selectById(id);
        if (record == null) {
            throw new BusinessException(404, "维修记录不存在");
        }
        return record;
    }
}
