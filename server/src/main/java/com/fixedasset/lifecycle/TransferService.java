package com.fixedasset.lifecycle;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fixedasset.asset.AssetService;
import com.fixedasset.asset.entity.Asset;
import com.fixedasset.common.aop.OperationLog;
import com.fixedasset.common.exception.BusinessException;
import com.fixedasset.common.model.PageResult;
import com.fixedasset.lifecycle.dto.TransferCreateRequest;
import com.fixedasset.lifecycle.entity.TransferRecord;
import com.fixedasset.lifecycle.mapper.TransferRecordMapper;
import com.fixedasset.organization.entity.Employee;
import com.fixedasset.organization.mapper.EmployeeMapper;
import com.fixedasset.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.CacheEvict;

import java.time.LocalDateTime;

/**
 * 资产调拨服务。
 *
 * <p>调拨不会改变资产“在用”状态，只改变部门和负责人，并生成可追溯的调拨记录。
 * 目标负责人必须属于目标部门，避免产生非法归属。</p>
 */
@Service
public class TransferService {

    private final TransferRecordMapper transferRecordMapper;
    private final AssetService assetService;
    private final EmployeeMapper employeeMapper;

    public TransferService(
            TransferRecordMapper transferRecordMapper,
            AssetService assetService,
            EmployeeMapper employeeMapper
    ) {
        this.transferRecordMapper = transferRecordMapper;
        this.assetService = assetService;
        this.employeeMapper = employeeMapper;
    }

    public PageResult<TransferRecord> page(long page, long size, Long assetId) {
        Page<TransferRecord> result = transferRecordMapper.selectPage(new Page<>(page, size),
                Wrappers.<TransferRecord>lambdaQuery()
                        .eq(assetId != null, TransferRecord::getAssetId, assetId)
                        .orderByDesc(TransferRecord::getTransferredAt));
        return PageResult.from(result);
    }

    @Transactional
    @OperationLog(module = "资产调拨", action = "发起资产调拨")
    @CacheEvict(cacheNames = "dashboardSummary", allEntries = true)
    public TransferRecord create(TransferCreateRequest request) {
        // 校验目标归属后再写调拨记录，最后同步资产当前快照。
        Asset asset = assetService.require(request.assetId());
        if (!"IN_USE".equals(asset.getStatus())) {
            throw new BusinessException("只有在用资产可以调拨");
        }
        Employee targetOwner = employeeMapper.selectById(request.toOwnerId());
        if (targetOwner == null || !"ACTIVE".equals(targetOwner.getStatus())) {
            throw new BusinessException("目标负责人不存在或已离职");
        }
        if (!request.toDepartmentId().equals(targetOwner.getDepartmentId())) {
            throw new BusinessException("目标负责人不属于目标部门");
        }
        if (asset.getDepartmentId().equals(request.toDepartmentId())
                && asset.getOwnerId().equals(request.toOwnerId())) {
            throw new BusinessException("目标归属与当前归属相同");
        }

        TransferRecord record = new TransferRecord();
        record.setAssetId(asset.getId());
        record.setFromDepartmentId(asset.getDepartmentId());
        record.setToDepartmentId(request.toDepartmentId());
        record.setFromOwnerId(asset.getOwnerId());
        record.setToOwnerId(request.toOwnerId());
        record.setReason(request.reason());
        record.setTransferredAt(LocalDateTime.now());
        record.setStatus("COMPLETED");
        transferRecordMapper.insert(record);

        asset.setDepartmentId(request.toDepartmentId());
        asset.setOwnerId(request.toOwnerId());
        asset.setUpdatedAt(LocalDateTime.now());
        assetService.update(asset.getId(), asset);
        assetService.addChange(asset, "资产调拨",
                asset.getName() + "完成部门/负责人调拨",
                SecurityUtils.currentUser().realName());
        return record;
    }
}
