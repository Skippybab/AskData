package com.mt.agent.workflow.api.service;

import com.mt.agent.workflow.api.entity.SqlGuardPolicy;

public interface SqlGuardService {
    SqlGuardPolicy getEffectivePolicy(Long tenantId, Long dbConfigId);
    void validate(String sql, SqlGuardPolicy policy);
}


