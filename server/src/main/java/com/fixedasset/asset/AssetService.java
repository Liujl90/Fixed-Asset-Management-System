package com.fixedasset.asset;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fixedasset.asset.entity.Asset;
import com.fixedasset.asset.entity.AssetCategory;
import com.fixedasset.asset.entity.AssetChangeRecord;
import com.fixedasset.asset.mapper.AssetCategoryMapper;
import com.fixedasset.asset.mapper.AssetChangeRecordMapper;
import com.fixedasset.asset.mapper.AssetMapper;
import com.fixedasset.common.aop.OperationLog;
import com.fixedasset.common.exception.BusinessException;
import com.fixedasset.common.model.PageResult;
import com.fixedasset.lifecycle.entity.LoanRecord;
import com.fixedasset.lifecycle.entity.TransferRecord;
import com.fixedasset.lifecycle.mapper.LoanRecordMapper;
import com.fixedasset.lifecycle.mapper.TransferRecordMapper;
import com.fixedasset.organization.entity.Employee;
import com.fixedasset.organization.mapper.EmployeeMapper;
import com.fixedasset.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.CacheEvict;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
public class AssetService {

    private static final Set<String> ALLOWED_STATUSES =
            Set.of("IDLE", "IN_USE", "MAINTENANCE", "SCRAPPED");

    private final AssetMapper assetMapper;
    private final AssetCategoryMapper categoryMapper;
    private final AssetChangeRecordMapper changeRecordMapper;
    private final EmployeeMapper employeeMapper;
    private final LoanRecordMapper loanRecordMapper;
    private final TransferRecordMapper transferRecordMapper;

    public AssetService(
            AssetMapper assetMapper,
            AssetCategoryMapper categoryMapper,
            AssetChangeRecordMapper changeRecordMapper,
            EmployeeMapper employeeMapper,
            LoanRecordMapper loanRecordMapper,
            TransferRecordMapper transferRecordMapper
    ) {
        this.assetMapper = assetMapper;
        this.categoryMapper = categoryMapper;
        this.changeRecordMapper = changeRecordMapper;
        this.employeeMapper = employeeMapper;
        this.loanRecordMapper = loanRecordMapper;
        this.transferRecordMapper = transferRecordMapper;
    }

    public PageResult<Asset> page(
            long page,
            long size,
            String keyword,
            Long categoryId,
            String status,
            Long departmentId
    ) {
        Page<Asset> result = assetMapper.selectPage(new Page<>(page, size),
                Wrappers.<Asset>lambdaQuery()
                        .and(keyword != null && !keyword.isBlank(), query -> query
                                .like(Asset::getAssetNo, keyword)
                                .or()
                                .like(Asset::getName, keyword)
                                .or()
                                .like(Asset::getBrandModel, keyword))
                        .eq(categoryId != null, Asset::getCategoryId, categoryId)
                        .eq(status != null && !status.isBlank(), Asset::getStatus, status)
                        .eq(departmentId != null, Asset::getDepartmentId, departmentId)
                        .orderByDesc(Asset::getId));
        return PageResult.from(result);
    }

    public Asset detail(Long id) {
        return require(id);
    }

    public List<AssetChangeRecord> changes(Long assetId) {
        require(assetId);
        return changeRecordMapper.selectList(Wrappers.<AssetChangeRecord>lambdaQuery()
                .eq(AssetChangeRecord::getAssetId, assetId)
                .orderByDesc(AssetChangeRecord::getCreatedAt));
    }

    public List<Asset> listForExport(Long departmentId, String status) {
        return assetMapper.selectList(Wrappers.<Asset>lambdaQuery()
                .eq(departmentId != null, Asset::getDepartmentId, departmentId)
                .eq(status != null && !status.isBlank(), Asset::getStatus, status)
                .orderByAsc(Asset::getId));
    }

    @Transactional
    @OperationLog(module = "固定资产", action = "登记资产")
    @CacheEvict(cacheNames = "dashboardSummary", allEntries = true)
    public Asset create(Asset asset) {
        validate(asset, null);
        asset.setId(null);
        asset.setStatus(asset.getStatus() == null ? "IDLE" : asset.getStatus());
        asset.setCreatedAt(LocalDateTime.now());
        asset.setUpdatedAt(LocalDateTime.now());
        assetMapper.insert(asset);
        addChange(asset, "资产登记", asset.getName() + "完成资产登记", currentOperator());
        return asset;
    }

    @Transactional
    @OperationLog(module = "固定资产", action = "编辑资产")
    @CacheEvict(cacheNames = "dashboardSummary", allEntries = true)
    public Asset update(Long id, Asset payload) {
        Asset existing = require(id);
        validate(payload, id);
        String oldStatus = existing.getStatus();
        existing.setAssetNo(payload.getAssetNo());
        existing.setName(payload.getName());
        existing.setCategoryId(payload.getCategoryId());
        existing.setBrandModel(payload.getBrandModel());
        existing.setPurchaseDate(payload.getPurchaseDate());
        existing.setOriginalValue(payload.getOriginalValue());
        existing.setUsefulLife(payload.getUsefulLife());
        existing.setDepartmentId(payload.getDepartmentId());
        existing.setOwnerId("IDLE".equals(payload.getStatus()) ? null : payload.getOwnerId());
        existing.setStatus(payload.getStatus());
        existing.setRemark(payload.getRemark());
        existing.setUpdatedAt(LocalDateTime.now());
        assetMapper.updateById(existing);
        if (!oldStatus.equals(payload.getStatus())) {
            addChange(existing, "状态调整",
                    existing.getName() + "状态调整为" + payload.getStatus(), currentOperator());
        }
        return existing;
    }

    @Transactional
    @OperationLog(module = "固定资产", action = "调整资产状态")
    @CacheEvict(cacheNames = "dashboardSummary", allEntries = true)
    public Asset updateStatus(Long id, String status) {
        Asset asset = require(id);
        if (!ALLOWED_STATUSES.contains(status)) {
            throw new BusinessException("资产状态不合法");
        }
        if ("IN_USE".equals(status) && (asset.getDepartmentId() == null || asset.getOwnerId() == null)) {
            throw new BusinessException("在用资产必须指定所属部门和负责人");
        }
        if ("IDLE".equals(status)) {
            asset.setOwnerId(null);
        }
        String oldStatus = asset.getStatus();
        asset.setStatus(status);
        asset.setUpdatedAt(LocalDateTime.now());
        assetMapper.updateById(asset);
        addChange(asset, "状态调整", asset.getName() + "状态由" + oldStatus + "调整为" + status, currentOperator());
        return asset;
    }

    @OperationLog(module = "固定资产", action = "删除资产")
    @CacheEvict(cacheNames = "dashboardSummary", allEntries = true)
    public void delete(Long id) {
        Asset asset = require(id);
        long loanCount = loanRecordMapper.selectCount(Wrappers.<LoanRecord>lambdaQuery()
                .eq(LoanRecord::getAssetId, id));
        long transferCount = transferRecordMapper.selectCount(Wrappers.<TransferRecord>lambdaQuery()
                .eq(TransferRecord::getAssetId, id));
        if (loanCount > 0 || transferCount > 0 || "IN_USE".equals(asset.getStatus())) {
            throw new BusinessException("该资产已有业务记录或在用，不能删除");
        }
        assetMapper.deleteById(id);
        changeRecordMapper.delete(Wrappers.<AssetChangeRecord>lambdaQuery()
                .eq(AssetChangeRecord::getAssetId, id));
    }

    public Asset require(Long id) {
        Asset asset = assetMapper.selectById(id);
        if (asset == null) {
            throw new BusinessException(404, "资产不存在");
        }
        return asset;
    }

    public void addChange(Asset asset, String type, String description, String operator) {
        AssetChangeRecord record = new AssetChangeRecord();
        record.setAssetId(asset.getId());
        record.setType(type);
        record.setDescription(description);
        record.setOperator(operator);
        record.setCreatedAt(LocalDateTime.now());
        changeRecordMapper.insert(record);
    }

    private void validate(Asset asset, Long excludeId) {
        if (asset.getAssetNo() == null || asset.getAssetNo().isBlank()) {
            throw new BusinessException("资产编号不能为空");
        }
        if (asset.getCategoryId() == null || categoryMapper.selectById(asset.getCategoryId()) == null) {
            throw new BusinessException("资产分类不存在");
        }
        if (asset.getStatus() == null || !ALLOWED_STATUSES.contains(asset.getStatus())) {
            throw new BusinessException("资产状态不合法");
        }
        if ("IN_USE".equals(asset.getStatus())
                && (asset.getDepartmentId() == null || asset.getOwnerId() == null)) {
            throw new BusinessException("在用资产必须指定所属部门和负责人");
        }
        if (asset.getOwnerId() != null) {
            Employee owner = employeeMapper.selectById(asset.getOwnerId());
            if (owner == null || !"ACTIVE".equals(owner.getStatus())) {
                throw new BusinessException("资产负责人不存在或已离职");
            }
            if (asset.getDepartmentId() != null && !asset.getDepartmentId().equals(owner.getDepartmentId())) {
                throw new BusinessException("资产负责人必须属于资产所属部门");
            }
        }
        long count = assetMapper.selectCount(Wrappers.<Asset>lambdaQuery()
                .eq(Asset::getAssetNo, asset.getAssetNo())
                .ne(excludeId != null, Asset::getId, excludeId));
        if (count > 0) {
            throw new BusinessException("资产编号已存在");
        }
        if (asset.getOriginalValue() == null) {
            asset.setOriginalValue(BigDecimal.ZERO);
        }
        if (asset.getUsefulLife() == null) {
            asset.setUsefulLife(0);
        }
    }

    private String currentOperator() {
        try {
            return SecurityUtils.currentUser().realName();
        } catch (RuntimeException exception) {
            return "系统";
        }
    }
}
