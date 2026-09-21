package com.fixedasset.operation.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fixedasset.asset.entity.Asset;
import com.fixedasset.asset.mapper.AssetMapper;
import com.fixedasset.common.aop.OperationLog;
import com.fixedasset.common.model.PageResult;
import com.fixedasset.operation.entity.DepreciationRecord;
import com.fixedasset.operation.mapper.DepreciationRecordMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

/**
 * 月度折旧计算服务。
 *
 * <p>采用直线法：月折旧 = 原值 / 使用年限 / 12；累计折旧不超过原值。
 * 报废资产不参与新增折旧，同一资产同一月份通过唯一约束保证幂等。</p>
 */
@Service
public class DepreciationService {

    private final DepreciationRecordMapper depreciationRecordMapper;
    private final AssetMapper assetMapper;

    public DepreciationService(
            DepreciationRecordMapper depreciationRecordMapper,
            AssetMapper assetMapper
    ) {
        this.depreciationRecordMapper = depreciationRecordMapper;
        this.assetMapper = assetMapper;
    }

    public PageResult<DepreciationRecord> page(long page, long size, String month) {
        Page<DepreciationRecord> result = depreciationRecordMapper.selectPage(new Page<>(page, size),
                Wrappers.<DepreciationRecord>lambdaQuery()
                        .eq(month != null && !month.isBlank(), DepreciationRecord::getDepreciationMonth, month)
                        .orderByDesc(DepreciationRecord::getDepreciationMonth)
                        .orderByAsc(DepreciationRecord::getAssetId));
        return PageResult.from(result);
    }

    @Transactional
    @OperationLog(module = "资产折旧", action = "执行月度折旧")
    public int runMonthlyDepreciation(String requestedMonth) {
        // requestedMonth 为空时按当前月份执行，也支持补算历史月份。
        YearMonth month = requestedMonth == null || requestedMonth.isBlank()
                ? YearMonth.now()
                : YearMonth.parse(requestedMonth);
        LocalDate monthEnd = month.atEndOfMonth();
        String monthValue = month.toString();
        List<Asset> assets = assetMapper.selectList(Wrappers.<Asset>lambdaQuery()
                .ne(Asset::getStatus, "SCRAPPED")
                .isNotNull(Asset::getPurchaseDate)
                .le(Asset::getPurchaseDate, monthEnd)
                .gt(Asset::getUsefulLife, 0)
                .gt(Asset::getOriginalValue, BigDecimal.ZERO));
        int created = 0;
        for (Asset asset : assets) {
            // 先检查该资产当月是否已有记录，重复执行不会重复扣减。
            long existing = depreciationRecordMapper.selectCount(Wrappers.<DepreciationRecord>lambdaQuery()
                    .eq(DepreciationRecord::getAssetId, asset.getId())
                    .eq(DepreciationRecord::getDepreciationMonth, monthValue));
            if (existing > 0) {
                continue;
            }
            BigDecimal months = BigDecimal.valueOf(asset.getUsefulLife()).multiply(BigDecimal.valueOf(12));
            BigDecimal monthly = asset.getOriginalValue().divide(months, 2, RoundingMode.HALF_UP);
            BigDecimal accumulated = depreciationRecordMapper.selectList(
                            Wrappers.<DepreciationRecord>lambdaQuery()
                                    .eq(DepreciationRecord::getAssetId, asset.getId()))
                    .stream()
                    .map(DepreciationRecord::getMonthlyAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .add(monthly)
                    .min(asset.getOriginalValue());

            DepreciationRecord record = new DepreciationRecord();
            record.setAssetId(asset.getId());
            record.setDepreciationMonth(monthValue);
            record.setOriginalValue(asset.getOriginalValue());
            record.setMonthlyAmount(monthly);
            record.setAccumulatedAmount(accumulated);
            record.setCreatedAt(LocalDateTime.now());
            depreciationRecordMapper.insert(record);
            created++;
        }
        return created;
    }
}
