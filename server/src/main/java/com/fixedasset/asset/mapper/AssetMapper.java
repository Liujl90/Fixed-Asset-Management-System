package com.fixedasset.asset.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fixedasset.asset.entity.Asset;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AssetMapper extends BaseMapper<Asset> {
}
