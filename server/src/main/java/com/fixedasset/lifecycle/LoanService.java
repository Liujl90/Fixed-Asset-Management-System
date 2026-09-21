package com.fixedasset.lifecycle;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fixedasset.asset.AssetService;
import com.fixedasset.asset.entity.Asset;
import com.fixedasset.common.aop.OperationLog;
import com.fixedasset.common.exception.BusinessException;
import com.fixedasset.common.model.PageResult;
import com.fixedasset.lifecycle.dto.LoanApplyRequest;
import com.fixedasset.lifecycle.entity.LoanRecord;
import com.fixedasset.lifecycle.mapper.LoanRecordMapper;
import com.fixedasset.organization.entity.Employee;
import com.fixedasset.organization.mapper.EmployeeMapper;
import com.fixedasset.security.AuthenticatedUser;
import com.fixedasset.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.CacheEvict;

import java.time.LocalDateTime;

/**
 * 领用与归还状态机。
 *
 * <pre>
 * PENDING --approve--> ACTIVE --requestReturn--> RETURN_PENDING --confirmReturn--> RETURNED
 * PENDING --reject--> REJECTED
 * </pre>
 *
 * <p>每次状态推进都同步更新资产主表和资产变动记录，避免只改业务单而资产仍是旧状态。</p>
 */
@Service
public class LoanService {

    private final LoanRecordMapper loanRecordMapper;
    private final AssetService assetService;
    private final EmployeeMapper employeeMapper;

    public LoanService(
            LoanRecordMapper loanRecordMapper,
            AssetService assetService,
            EmployeeMapper employeeMapper
    ) {
        this.loanRecordMapper = loanRecordMapper;
        this.assetService = assetService;
        this.employeeMapper = employeeMapper;
    }

    public PageResult<LoanRecord> page(long page, long size, Long applicantId, String status, Long assetId) {
        AuthenticatedUser user = SecurityUtils.currentUser();
        Long effectiveApplicantId = user.roles().contains("EMPLOYEE")
                ? user.employeeId()
                : applicantId;
        Page<LoanRecord> result = loanRecordMapper.selectPage(new Page<>(page, size),
                Wrappers.<LoanRecord>lambdaQuery()
                        .eq(effectiveApplicantId != null, LoanRecord::getApplicantId, effectiveApplicantId)
                        .eq(status != null && !status.isBlank(), LoanRecord::getStatus, status)
                        .eq(assetId != null, LoanRecord::getAssetId, assetId)
                        .orderByDesc(LoanRecord::getRequestedAt));
        return PageResult.from(result);
    }

    @Transactional
    @OperationLog(module = "领用归还", action = "提交领用申请")
    public LoanRecord apply(LoanApplyRequest request) {
        // 领用申请只锁业务记录，不提前改变资产状态，避免申请未通过时资产被占用。
        Asset asset = assetService.require(request.assetId());
        if (!"IDLE".equals(asset.getStatus())) {
            throw new BusinessException("只有闲置资产可以申请领用");
        }
        long activeCount = loanRecordMapper.selectCount(Wrappers.<LoanRecord>lambdaQuery()
                .eq(LoanRecord::getAssetId, request.assetId())
                .in(LoanRecord::getStatus, "PENDING", "ACTIVE", "RETURN_PENDING"));
        if (activeCount > 0) {
            throw new BusinessException("该资产已存在进行中的领用记录");
        }

        AuthenticatedUser user = SecurityUtils.currentUser();
        Long applicantId = request.applicantId() != null ? request.applicantId() : user.employeeId();
        Employee applicant = requireActiveEmployee(applicantId);
        Long departmentId = request.departmentId() != null
                ? request.departmentId()
                : applicant.getDepartmentId();

        LoanRecord record = new LoanRecord();
        record.setAssetId(request.assetId());
        record.setApplicantId(applicantId);
        record.setDepartmentId(departmentId);
        record.setRequestedAt(LocalDateTime.now());
        record.setStatus("PENDING");
        record.setRemark(request.remark());
        loanRecordMapper.insert(record);
        assetService.addChange(asset, "领用申请",
                applicant.getName() + "提交了" + asset.getName() + "的领用申请",
                applicant.getName());
        return record;
    }

    @Transactional
    @OperationLog(module = "领用归还", action = "审批领用申请")
    @CacheEvict(cacheNames = "dashboardSummary", allEntries = true)
    public LoanRecord approve(Long id) {
        // 审批是资产状态和领用记录同时变化的临界点，必须处于同一事务。
        LoanRecord record = require(id);
        if (!"PENDING".equals(record.getStatus())) {
            throw new BusinessException("领用申请状态已变化");
        }
        Asset asset = assetService.require(record.getAssetId());
        if (!"IDLE".equals(asset.getStatus())) {
            throw new BusinessException("资产当前不可领用");
        }
        Employee applicant = requireActiveEmployee(record.getApplicantId());
        LocalDateTime now = LocalDateTime.now();
        record.setStatus("ACTIVE");
        record.setApprovedAt(now);
        record.setLoanDate(now);
        record.setOwnerId(record.getApplicantId());
        record.setDepartmentId(applicant.getDepartmentId());
        loanRecordMapper.updateById(record);

        asset.setStatus("IN_USE");
        asset.setOwnerId(record.getApplicantId());
        asset.setDepartmentId(record.getDepartmentId());
        asset.setUpdatedAt(now);
        assetService.update(asset.getId(), asset);
        assetService.addChange(asset, "资产领用",
                applicant.getName() + "领用" + asset.getName(),
                SecurityUtils.currentUser().realName());
        return record;
    }

    @Transactional
    @OperationLog(module = "领用归还", action = "驳回领用申请")
    public LoanRecord reject(Long id, String reason) {
        LoanRecord record = require(id);
        if (!"PENDING".equals(record.getStatus())) {
            throw new BusinessException("领用申请状态已变化");
        }
        record.setStatus("REJECTED");
        record.setRemark(reason != null && !reason.isBlank() ? reason : record.getRemark());
        loanRecordMapper.updateById(record);
        Asset asset = assetService.require(record.getAssetId());
        assetService.addChange(asset, "领用驳回",
                "领用申请被驳回：" + record.getRemark(),
                SecurityUtils.currentUser().realName());
        return record;
    }

    @Transactional
    @OperationLog(module = "领用归还", action = "提交归还申请")
    public LoanRecord requestReturn(Long id) {
        // 归还申请先进入待确认状态，管理员确认后才释放资产。
        LoanRecord record = require(id);
        if (!"ACTIVE".equals(record.getStatus())) {
            throw new BusinessException("当前领用记录不可归还");
        }
        record.setStatus("RETURN_PENDING");
        record.setReturnRequestedAt(LocalDateTime.now());
        loanRecordMapper.updateById(record);
        Asset asset = assetService.require(record.getAssetId());
        assetService.addChange(asset, "归还申请",
                "资产归还申请已提交", SecurityUtils.currentUser().realName());
        return record;
    }

    @Transactional
    @OperationLog(module = "领用归还", action = "确认资产归还")
    @CacheEvict(cacheNames = "dashboardSummary", allEntries = true)
    public LoanRecord confirmReturn(Long id) {
        // 确认归还后清空当前负责人，资产重新进入 IDLE。
        LoanRecord record = require(id);
        if (!"RETURN_PENDING".equals(record.getStatus())) {
            throw new BusinessException("归还申请状态已变化");
        }
        Asset asset = assetService.require(record.getAssetId());
        record.setStatus("RETURNED");
        record.setReturnDate(LocalDateTime.now());
        loanRecordMapper.updateById(record);

        asset.setStatus("IDLE");
        asset.setOwnerId(null);
        asset.setUpdatedAt(LocalDateTime.now());
        assetService.update(asset.getId(), asset);
        assetService.addChange(asset, "资产归还",
                asset.getName() + "已归还", SecurityUtils.currentUser().realName());
        return record;
    }

    private LoanRecord require(Long id) {
        LoanRecord record = loanRecordMapper.selectById(id);
        if (record == null) {
            throw new BusinessException(404, "领用记录不存在");
        }
        return record;
    }

    private Employee requireActiveEmployee(Long employeeId) {
        Employee employee = employeeId == null ? null : employeeMapper.selectById(employeeId);
        if (employee == null || !"ACTIVE".equals(employee.getStatus())) {
            throw new BusinessException("申请人不存在或已离职");
        }
        return employee;
    }
}
