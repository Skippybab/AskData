package com.mt.agent.workflow.api.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mt.agent.workflow.api.entity.AuditLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AuditLogMapper extends BaseMapper<AuditLog> {
}


