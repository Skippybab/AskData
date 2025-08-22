package com.mt.agent.workflow.api.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mt.agent.workflow.api.entity.ApiConfig;
import org.apache.ibatis.annotations.Mapper;

/**
 * API配置Mapper接口
 */
@Mapper
public interface ApiConfigMapper extends BaseMapper<ApiConfig> {
}