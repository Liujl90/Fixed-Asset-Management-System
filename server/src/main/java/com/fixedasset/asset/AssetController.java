package com.fixedasset.asset;

import com.fixedasset.asset.entity.Asset;
import com.fixedasset.asset.entity.AssetChangeRecord;
import com.fixedasset.common.model.ApiResponse;
import com.fixedasset.common.model.PageResult;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/assets")
public class AssetController {

    private final AssetService assetService;

    public AssetController(AssetService assetService) {
        this.assetService = assetService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('asset:read')")
    public ApiResponse<PageResult<Asset>> page(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long departmentId
    ) {
        return ApiResponse.ok(assetService.page(page, size, keyword, categoryId, status, departmentId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('asset:read')")
    public ApiResponse<Asset> detail(@PathVariable Long id) {
        return ApiResponse.ok(assetService.detail(id));
    }

    @GetMapping("/{id}/changes")
    @PreAuthorize("hasAuthority('asset:read')")
    public ApiResponse<List<AssetChangeRecord>> changes(@PathVariable Long id) {
        return ApiResponse.ok(assetService.changes(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('asset:write')")
    public ApiResponse<Asset> create(@RequestBody Asset asset) {
        return ApiResponse.ok(assetService.create(asset));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('asset:write')")
    public ApiResponse<Asset> update(@PathVariable Long id, @RequestBody Asset asset) {
        return ApiResponse.ok(assetService.update(id, asset));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('asset:write')")
    public ApiResponse<Asset> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        return ApiResponse.ok(assetService.updateStatus(id, payload.get("status")));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('asset:write')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        assetService.delete(id);
        return ApiResponse.ok();
    }
}
