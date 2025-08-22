package com.mt.agent.workflow.api.service.impl;

import com.mt.agent.workflow.api.entity.AuditLog;
import com.mt.agent.workflow.api.mapper.AuditLogMapper;
import com.mt.agent.workflow.api.service.AuditService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AuditServiceImpl implements AuditService {

    @Autowired
    private AuditLogMapper auditLogMapper;

    @Override
    public void log(String action, String objectType, Long objectId, String detailJson, String ip) {
        AuditLog log = new AuditLog();
        log.setTenantId(0L); // 默认租户
        log.setUserId(1L); // 默认用户，实际应该从上下文获取
        log.setAction(action);
        log.setObjectType(objectType);
        log.setObjectId(objectId);
        log.setDetailJson(detailJson);
        log.setIp(ip);
        log.setCreatedAtMs(System.currentTimeMillis());
        auditLogMapper.insert(log);
    }
}


