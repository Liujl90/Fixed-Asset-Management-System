package com.fixedasset.asset.excel;

import com.alibaba.excel.EasyExcel;
import com.fixedasset.asset.AssetService;
import com.fixedasset.asset.entity.Asset;
import com.fixedasset.common.exception.BusinessException;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

/**
 * 固定资产 Excel 导入导出服务。
 *
 * <p>导出读取数据库后写入响应流；导入先完整解析到内存，再在事务内逐条校验并保存。
 * 任意一行失败都会抛出业务异常并回滚整批数据，避免导入结果半成功。</p>
 */
@Service
public class AssetExcelService {

    private final AssetService assetService;

    public AssetExcelService(AssetService assetService) {
        this.assetService = assetService;
    }

    public void exportAssets(HttpServletResponse response, Long departmentId, String status) {
        List<Asset> assets = assetService.listForExport(departmentId, status);
        List<AssetExcelRow> rows = assets.stream().map(this::toRow).toList();
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        String filename = URLEncoder.encode("assets", StandardCharsets.UTF_8);
        response.setHeader("Content-Disposition", "attachment;filename*=UTF-8''" + filename + ".xlsx");
        try {
            EasyExcel.write(response.getOutputStream(), AssetExcelRow.class)
                    .sheet("固定资产")
                    .doWrite(rows);
        } catch (IOException exception) {
            throw new BusinessException("资产导出失败");
        }
    }

    @Transactional
    public int importAssets(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择 Excel 文件");
        }
        AssetImportListener listener = new AssetImportListener();
        try {
            EasyExcel.read(file.getInputStream(), AssetExcelRow.class, listener)
                    .sheet()
                    .doRead();
        } catch (IOException exception) {
            throw new BusinessException("Excel 读取失败");
        }
        List<AssetExcelRow> rows = listener.getRows();
        if (rows.isEmpty()) {
            throw new BusinessException("Excel 中没有资产数据");
        }
        for (int index = 0; index < rows.size(); index++) {
            AssetExcelRow row = rows.get(index);
            try {
                assetService.create(fromRow(row));
            } catch (RuntimeException exception) {
                throw new BusinessException(
                        "第 " + (index + 2) + " 行导入失败：" + exception.getMessage());
            }
        }
        return rows.size();
    }

    private AssetExcelRow toRow(Asset asset) {
        AssetExcelRow row = new AssetExcelRow();
        row.setAssetNo(asset.getAssetNo());
        row.setName(asset.getName());
        row.setCategoryId(asset.getCategoryId());
        row.setBrandModel(asset.getBrandModel());
        row.setPurchaseDate(asset.getPurchaseDate() == null ? null : asset.getPurchaseDate().toString());
        row.setOriginalValue(asset.getOriginalValue());
        row.setUsefulLife(asset.getUsefulLife());
        row.setDepartmentId(asset.getDepartmentId());
        row.setOwnerId(asset.getOwnerId());
        row.setStatus(asset.getStatus());
        row.setRemark(asset.getRemark());
        return row;
    }

    private Asset fromRow(AssetExcelRow row) {
        Asset asset = new Asset();
        asset.setAssetNo(row.getAssetNo());
        asset.setName(row.getName());
        asset.setCategoryId(row.getCategoryId());
        asset.setBrandModel(row.getBrandModel());
        asset.setPurchaseDate(parseDate(row.getPurchaseDate()));
        asset.setOriginalValue(row.getOriginalValue());
        asset.setUsefulLife(row.getUsefulLife());
        asset.setDepartmentId(row.getDepartmentId());
        asset.setOwnerId(row.getOwnerId());
        asset.setStatus(row.getStatus() == null || row.getStatus().isBlank()
                ? "IDLE"
                : row.getStatus().trim().toUpperCase());
        asset.setRemark(row.getRemark());
        return asset;
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (RuntimeException exception) {
            throw new BusinessException("购买日期格式应为 yyyy-MM-dd");
        }
    }
}
