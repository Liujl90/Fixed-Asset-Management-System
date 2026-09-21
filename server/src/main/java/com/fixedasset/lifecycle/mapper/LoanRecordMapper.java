package com.fixedasset.lifecycle.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fixedasset.lifecycle.entity.LoanRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LoanRecordMapper extends BaseMapper<LoanRecord> {
}
