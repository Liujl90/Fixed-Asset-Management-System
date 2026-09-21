package com.fixedasset.operation.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fixedasset.asset.AssetService;
import com.fixedasset.common.aop.OperationLog;
import com.fixedasset.common.exception.BusinessException;
import com.fixedasset.common.model.PageResult;
import com.fixedasset.operation.entity.MaintenancePlan;
import com.fixedasset.operation.mapper.MaintenancePlanMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class MaintenanceService {

    private final MaintenancePlanMapper maintenancePlanMapper;
    private final AssetService assetService;

    public MaintenanceService(
            MaintenancePlanMapper maintenancePlanMapper,
            AssetService assetService
    ) {
        this.maintenancePlanMapper = maintenancePlanMapper;
        this.assetService = assetService;
    }

    public PageResult<MaintenancePlan> page(long page, long size, String status) {
        Page<MaintenancePlan> result = maintenancePlanMapper.selectPage(new Page<>(page, size),
                Wrappers.<MaintenancePlan>lambdaQuery()
                        .eq(status != null && !status.isBlank(), MaintenancePlan::getStatus, status)
                        .orderByAsc(MaintenancePlan::getPlanDate));
        return PageResult.from(result);
    }

    @OperationLog(module = "维修保养", action = "新增保养计划")
    public MaintenancePlan create(MaintenancePlan plan) {
        assetService.require(plan.getAssetId());
        plan.setId(null);
        plan.setStatus("PENDING");
        plan.setCreatedAt(LocalDateTime.now());
        plan.setUpdatedAt(LocalDateTime.now());
        maintenancePlanMapper.insert(plan);
        return plan;
    }

    @OperationLog(module = "维修保养", action = "编辑保养计划")
    public MaintenancePlan update(Long id, MaintenancePlan payload) {
        MaintenancePlan plan = require(id);
        plan.setPlanName(payload.getPlanName());
        plan.setMaintenanceType(payload.getMaintenanceType());
        plan.setPlanDate(payload.getPlanDate());
        plan.setCycleMonths(payload.getCycleMonths());
        plan.setRemark(payload.getRemark());
        plan.setUpdatedAt(LocalDateTime.now());
        maintenancePlanMapper.updateById(plan);
        return plan;
    }

    @Transactional
    @OperationLog(module = "维修保养", action = "完成保养计划")
    public MaintenancePlan complete(Long id) {
        MaintenancePlan plan = require(id);
        plan.setStatus("COMPLETED");
        plan.setCompletedAt(LocalDateTime.now());
        plan.setUpdatedAt(LocalDateTime.now());
        maintenancePlanMapper.updateById(plan);
        return plan;
    }

    @Transactional
    public int checkDuePlans() {
        LocalDate today = LocalDate.now();
        List<MaintenancePlan> plans = maintenancePlanMapper.selectList(
                Wrappers.<MaintenancePlan>lambdaQuery()
                        .in(MaintenancePlan::getStatus, "PENDING", "DUE", "OVERDUE")
                        .le(MaintenancePlan::getPlanDate, today.plusDays(7)));
        int updated = 0;
        for (MaintenancePlan plan : plans) {
            String targetStatus = plan.getPlanDate().isBefore(today) ? "OVERDUE" : "DUE";
            if (!targetStatus.equals(plan.getStatus())) {
                plan.setStatus(targetStatus);
                plan.setUpdatedAt(LocalDateTime.now());
                maintenancePlanMapper.updateById(plan);
                updated++;
            }
        }
        return updated;
    }

    private MaintenancePlan require(Long id) {
        MaintenancePlan plan = maintenancePlanMapper.selectById(id);
        if (plan == null) {
            throw new BusinessException(404, "保养计划不存在");
        }
        return plan;
    }
}
