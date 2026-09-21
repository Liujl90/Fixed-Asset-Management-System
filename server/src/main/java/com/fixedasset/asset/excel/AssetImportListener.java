package com.fixedasset.asset.excel;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;

import java.util.ArrayList;
import java.util.List;

public class AssetImportListener implements ReadListener<AssetExcelRow> {

    private final List<AssetExcelRow> rows = new ArrayList<>();

    @Override
    public void invoke(AssetExcelRow row, AnalysisContext context) {
        rows.add(row);
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        // Parsing is complete. Validation and persistence happen after read.
    }

    public List<AssetExcelRow> getRows() {
        return rows;
    }
}
