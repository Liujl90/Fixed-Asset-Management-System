package com.fixedasset.asset;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fixedasset.asset.entity.Asset;
import com.fixedasset.asset.entity.AssetCategory;
import com.fixedasset.asset.mapper.AssetCategoryMapper;
import com.fixedasset.asset.mapper.AssetMapper;
import com.fixedasset.common.aop.OperationLog;
import com.fixedasset.common.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 资产分类服务。
 *
 * <p>分类树允许一个父节点拥有多个子节点。删除前必须确认没有子分类和关联资产，
 * 分类查询通过缓存减少频繁的基础资料读取。</p>
 */
@Service
public class CategoryService {

    private final AssetCategoryMapper categoryMapper;
    private final AssetMapper assetMapper;

    public CategoryService(AssetCategoryMapper categoryMapper, AssetMapper assetMapper) {
        this.categoryMapper = categoryMapper;
        this.assetMapper = assetMapper;
    }

    @Cacheable(cacheNames = "assetCategoryList", key = "#keyword == null ? 'all' : #keyword")
    public List<AssetCategory> list(String keyword) {
        // 分类为低频变更、高频读取数据，适合 Cache Aside。
        return categoryMapper.selectList(Wrappers.<AssetCategory>lambdaQuery()
                .and(keyword != null && !keyword.isBlank(), query -> query
                        .like(AssetCategory::getName, keyword)
                        .or()
                        .like(AssetCategory::getCode, keyword))
                .orderByAsc(AssetCategory::getId));
    }

    @OperationLog(module = "资产分类", action = "新增分类")
    @CacheEvict(cacheNames = "assetCategoryList", allEntries = true)
    public AssetCategory create(AssetCategory category) {
        validate(category, null);
        category.setId(null);
        category.setStatus(category.getStatus() == null ? "ACTIVE" : category.getStatus());
        category.setCreatedAt(LocalDateTime.now());
        category.setUpdatedAt(LocalDateTime.now());
        categoryMapper.insert(category);
        return category;
    }

    @OperationLog(module = "资产分类", action = "编辑分类")
    @CacheEvict(cacheNames = "assetCategoryList", allEntries = true)
    public AssetCategory update(Long id, AssetCategory category) {
        AssetCategory existing = require(id);
        validate(category, id);
        existing.setName(category.getName());
        existing.setCode(category.getCode());
        existing.setParentId(category.getParentId());
        existing.setStatus(category.getStatus());
        existing.setUpdatedAt(LocalDateTime.now());
        categoryMapper.updateById(existing);
        return existing;
    }

    @OperationLog(module = "资产分类", action = "删除分类")
    @CacheEvict(cacheNames = "assetCategoryList", allEntries = true)
    public void delete(Long id) {
        require(id);
        long children = categoryMapper.selectCount(Wrappers.<AssetCategory>lambdaQuery()
                .eq(AssetCategory::getParentId, id));
        long assets = assetMapper.selectCount(Wrappers.<Asset>lambdaQuery()
                .eq(Asset::getCategoryId, id));
        if (children > 0 || assets > 0) {
            throw new BusinessException("该分类仍有子分类或关联资产，不能删除");
        }
        categoryMapper.deleteById(id);
    }

    private void validate(AssetCategory category, Long excludeId) {
        // 当前只允许一级分类作为父分类，避免出现循环或多层无限扩展。
        if (category.getParentId() != null) {
            if (category.getParentId().equals(excludeId) || categoryMapper.selectById(category.getParentId()) == null) {
                throw new BusinessException("上级分类不存在");
            }
        }
        long count = categoryMapper.selectCount(Wrappers.<AssetCategory>lambdaQuery()
                .eq(AssetCategory::getCode, category.getCode())
                .ne(excludeId != null, AssetCategory::getId, excludeId));
        if (count > 0) {
            throw new BusinessException("分类编码已存在");
        }
    }

    private AssetCategory require(Long id) {
        AssetCategory category = categoryMapper.selectById(id);
        if (category == null) {
            throw new BusinessException(404, "资产分类不存在");
        }
        return category;
    }
}
