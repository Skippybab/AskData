package com.mt.agent.workflow.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mt.agent.workflow.api.entity.SqlGuardPolicy;
import com.mt.agent.workflow.api.infra.SqlParserUtil;
import com.mt.agent.workflow.api.mapper.SqlGuardPolicyMapper;
import com.mt.agent.workflow.api.service.SqlGuardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SqlGuardServiceImpl implements SqlGuardService {

    @Autowired
    private SqlGuardPolicyMapper policyMapper;

    @Override
    public SqlGuardPolicy getEffectivePolicy(Long tenantId, Long dbConfigId) {
        LambdaQueryWrapper<SqlGuardPolicy> qw = new LambdaQueryWrapper<SqlGuardPolicy>()
                .eq(SqlGuardPolicy::getTenantId, tenantId)
                .eq(SqlGuardPolicy::getDbConfigId, dbConfigId)
                .eq(SqlGuardPolicy::getStatus, 1)
                .last("limit 1");
        SqlGuardPolicy p = policyMapper.selectOne(qw);
        if (p == null) {
            p = new SqlGuardPolicy();
            p.setReadOnly(1);
            p.setMaxScanRows(1000L);
            p.setMaxTimeoutMs(30_000);
            p.setStatus(1);
        }
        return p;
    }

    @Override
    public void validate(String sql, SqlGuardPolicy policy) {
        if (policy.getReadOnly() == null || policy.getReadOnly() == 1) {
            SqlParserUtil.assertReadOnly(sql);
        }
        // deny keywords 简化处理
        if (policy.getDenyKeywords() != null && !policy.getDenyKeywords().isEmpty()) {
            String upper = sql.toUpperCase();
            for (String kw : policy.getDenyKeywords().replace("[", "").replace("]", "").split(",")) {
                kw = kw.replace("\"", "").trim().toUpperCase();
                if (!kw.isEmpty() && upper.contains(kw)) {
                    throw new IllegalArgumentException("包含禁止关键字: " + kw);
                }
            }
        }
        // TODO: 表级白名单/黑名单校验（需要解析表名或通过 EXPLAIN 粗判）
    }
}


