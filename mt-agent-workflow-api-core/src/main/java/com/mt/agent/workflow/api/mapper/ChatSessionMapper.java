package com.mt.agent.workflow.api.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mt.agent.workflow.api.entity.ChatSession;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChatSessionMapper extends BaseMapper<ChatSession> {
}
