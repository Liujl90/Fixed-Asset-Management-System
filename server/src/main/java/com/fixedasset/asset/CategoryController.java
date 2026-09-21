package com.fixedasset.asset;

import com.fixedasset.asset.entity.AssetCategory;
import com.fixedasset.common.model.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('category:read')")
    public ApiResponse<List<AssetCategory>> list(@RequestParam(required = false) String keyword) {
        return ApiResponse.ok(categoryService.list(keyword));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('category:write')")
    public ApiResponse<AssetCategory> create(@RequestBody AssetCategory category) {
        return ApiResponse.ok(categoryService.create(category));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('category:write')")
    public ApiResponse<AssetCategory> update(@PathVariable Long id, @RequestBody AssetCategory category) {
        return ApiResponse.ok(categoryService.update(id, category));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('category:write')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return ApiResponse.ok();
    }
}
