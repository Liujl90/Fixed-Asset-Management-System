package com.fixedasset.operation.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fixedasset.asset.AssetService;
import com.fixedasset.asset.entity.Asset;
import com.fixedasset.common.aop.OperationLog;
import com.fixedasset.common.exception.BusinessException;
import com.fixedasset.common.model.PageResult;
import com.fixedasset.operation.dto.OperationRequests.ScrapCompleteRequest;
import com.fixedasset.operation.dto.OperationRequests.ScrapCreateRequest;
import com.fixedasset.operation.entity.ScrapRecord;
import com.fixedasset.operation.mapper.ScrapRecordMapper;
import com.fixedasset.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 资产报废状态机。
 *
 * <pre>
 * PENDING -> APPROVED -> COMPLETED
 * PENDING -> REJECTED
 * </pre>
 *
 * <p>报废完成是资产状态的唯一终点：只有审核通过后，完成处置才会把资产改为 SCRAPPED。</p>
 */
@Service
public class ScrapService {

    private final ScrapRecordMapper scrapRecordMapper;
    private final AssetService assetService;

    public ScrapService(ScrapRecordMapper scrapRecordMapper, AssetService assetService) {
        this.scrapRecordMapper = scrapRecordMapper;
        this.assetService = assetService;
    }

    public PageResult<ScrapRecord> page(long page, long size, String status) {
        Page<ScrapRecord> result = scrapRecordMapper.selectPage(new Page<>(page, size),
                Wrappers.<ScrapRecord>lambdaQuery()
                        .eq(status != null && !status.isBlank(), ScrapRecord::getStatus, status)
                        .orderByDesc(ScrapRecord::getCreatedAt));
        return PageResult.from(result);
    }

    @OperationLog(module = "资产报废", action = "提交报废申请")
    public ScrapRecord create(ScrapCreateRequest request) {
        // 在用资产必须先归还，避免资产仍在使用时被报废。
        Asset asset = assetService.require(request.assetId());
        if ("SCRAPPED".equals(asset.getStatus())) {
            throw new BusinessException("资产已经报废");
        }
        if ("IN_USE".equals(asset.getStatus())) {
            throw new BusinessException("在用资产请先归还后再申请报废");
        }
        long active = scrapRecordMapper.selectCount(Wrappers.<ScrapRecord>lambdaQuery()
                .eq(ScrapRecord::getAssetId, request.assetId())
                .in(ScrapRecord::getStatus, "PENDING", "APPROVED"));
        if (active > 0) {
            throw new BusinessException("该资产已有进行中的报废申请");
        }
        if (scrapRecordMapper.selectCount(Wrappers.<ScrapRecord>lambdaQuery()
                .eq(ScrapRecord::getScrapNo, request.scrapNo())) > 0) {
            throw new BusinessException("报废单号已存在");
        }
        ScrapRecord record = new ScrapRecord();
        record.setScrapNo(request.scrapNo());
        record.setAssetId(request.assetId());
        record.setReason(request.reason());
        record.setApplicantId(request.applicantId());
        record.setStatus("PENDING");
        record.setRemark(request.remark());
        record.setCreatedAt(LocalDateTime.now());
        record.setUpdatedAt(LocalDateTime.now());
        scrapRecordMapper.insert(record);
        return record;
    }

    @OperationLog(module = "资产报废", action = "审核报废申请")
    public ScrapRecord approve(Long id) {
        ScrapRecord record = require(id);
        if (!"PENDING".equals(record.getStatus())) {
            throw new BusinessException("报废申请当前不可审核");
        }
        record.setStatus("APPROVED");
        record.setApprovedBy(SecurityUtils.currentUserId());
        record.setApprovedAt(LocalDateTime.now());
        record.setUpdatedAt(LocalDateTime.now());
        scrapRecordMapper.updateById(record);
        return record;
    }

    @OperationLog(module = "资产报废", action = "驳回报废申请")
    public ScrapRecord reject(Long id, String reason) {
        ScrapRecord record = require(id);
        if (!"PENDING".equals(record.getStatus())) {
            throw new BusinessException("报废申请当前不可驳回");
        }
        record.setStatus("REJECTED");
        record.setRemark(reason);
        record.setUpdatedAt(LocalDateTime.now());
        scrapRecordMapper.updateById(record);
        return record;
    }

    @Transactional
    @OperationLog(module = "资产报废", action = "完成资产报废")
    public ScrapRecord complete(Long id, ScrapCompleteRequest request) {
        // 保存处置方式和金额后再将资产置为 SCRAPPED。
        ScrapRecord record = require(id);
        if (!"APPROVED".equals(record.getStatus())) {
            throw new BusinessException("只有审核通过后才能完成报废");
        }
        record.setStatus("COMPLETED");
        record.setDisposalMethod(request.disposalMethod());
        record.setDisposalAmount(request.disposalAmount() == null
                ? BigDecimal.ZERO
                : request.disposalAmount());
        record.setRemark(request.remark() == null ? record.getRemark() : request.remark());
        record.setCompletedAt(LocalDateTime.now());
        record.setUpdatedAt(LocalDateTime.now());
        scrapRecordMapper.updateById(record);
        assetService.updateStatus(record.getAssetId(), "SCRAPPED");
        return record;
    }

    private ScrapRecord require(Long id) {
        ScrapRecord record = scrapRecordMapper.selectById(id);
        if (record == null) {
            throw new BusinessException(404, "报废申请不存在");
        }
        return record;
    }
}
