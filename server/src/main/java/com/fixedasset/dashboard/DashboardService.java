package com.fixedasset.dashboard;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fixedasset.asset.entity.Asset;
import com.fixedasset.asset.entity.AssetChangeRecord;
import com.fixedasset.asset.mapper.AssetChangeRecordMapper;
import com.fixedasset.asset.mapper.AssetMapper;
import com.fixedasset.organization.entity.Department;
import com.fixedasset.organization.mapper.DepartmentMapper;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.Cacheable;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardService {

    private final AssetMapper assetMapper;
    private final AssetChangeRecordMapper changeRecordMapper;
    private final DepartmentMapper departmentMapper;

    public DashboardService(
            AssetMapper assetMapper,
            AssetChangeRecordMapper changeRecordMapper,
            DepartmentMapper departmentMapper
    ) {
        this.assetMapper = assetMapper;
        this.changeRecordMapper = changeRecordMapper;
        this.departmentMapper = departmentMapper;
    }

    @Cacheable(cacheNames = "dashboardSummary", key = "'summary'")
    public Map<String, Object> summary() {
        List<Asset> assets = assetMapper.selectList(Wrappers.<Asset>lambdaQuery()
                .orderByAsc(Asset::getId));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", assets.size());
        result.put("idle", count(assets, "IDLE"));
        result.put("inUse", count(assets, "IN_USE"));
        result.put("maintenance", count(assets, "MAINTENANCE"));
        result.put("scrapped", count(assets, "SCRAPPED"));
        result.put("originalValue", assets.stream()
                .map(Asset::getOriginalValue)
                .filter(value -> value != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        result.put("departmentCounts", departmentCounts(assets));
        result.put("statusCounts", statusCounts(assets));
        result.put("recentChanges", changeRecordMapper.selectList(Wrappers.<AssetChangeRecord>lambdaQuery()
                .orderByDesc(AssetChangeRecord::getCreatedAt)
                .last("LIMIT 10")));
        return result;
    }

    private long count(List<Asset> assets, String status) {
        return assets.stream().filter(asset -> status.equals(asset.getStatus())).count();
    }

    private List<Map<String, Object>> departmentCounts(List<Asset> assets) {
        return departmentMapper.selectList(Wrappers.<Department>lambdaQuery()
                        .orderByAsc(Department::getId))
                .stream()
                .map(department -> Map.<String, Object>of(
                        "departmentId", department.getId(),
                        "name", department.getName(),
                        "value", assets.stream()
                                .filter(asset -> department.getId().equals(asset.getDepartmentId()))
                                .count()))
                .toList();
    }

    private List<Map<String, Object>> statusCounts(List<Asset> assets) {
        return List.of(
                Map.of("status", "IDLE", "name", "闲置", "value", count(assets, "IDLE")),
                Map.of("status", "IN_USE", "name", "在用", "value", count(assets, "IN_USE")),
                Map.of("status", "MAINTENANCE", "name", "维修中", "value", count(assets, "MAINTENANCE")),
                Map.of("status", "SCRAPPED", "name", "报废", "value", count(assets, "SCRAPPED")));
    }
}
