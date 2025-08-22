package com.mt.agent.workflow.api.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mt.agent.workflow.api.entity.TableInfo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 表信息Mapper接口
 */
@Mapper
public interface TableInfoMapper extends BaseMapper<TableInfo> {
}
