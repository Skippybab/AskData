package com.mt.agent.workflow.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mt.agent.workflow.api.entity.QueryResultCache;
import com.mt.agent.workflow.api.mapper.QueryResultCacheMapper;
import com.mt.agent.workflow.api.service.QueryCacheService;
import com.mt.agent.workflow.api.util.MD5Util;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
public class QueryCacheServiceImpl implements QueryCacheService {

    @Autowired
    private QueryResultCacheMapper cacheMapper;

    @Override
    public Optional<String> findPreview(Long tenantId, Long dbConfigId, Long schemaVersionId, String sqlHash) {
        try {
            QueryResultCache cache = cacheMapper.selectOne(
                new LambdaQueryWrapper<QueryResultCache>()
                    .eq(QueryResultCache::getTenantId, tenantId)
                    .eq(QueryResultCache::getDbConfigId, dbConfigId)
                    .eq(QueryResultCache::getSchemaVersionId, schemaVersionId)
                    .eq(QueryResultCache::getSqlHash, sqlHash)
                    .gt(QueryResultCache::getExpiredAtMs, System.currentTimeMillis()) // 未过期
                    .orderByDesc(QueryResultCache::getCreatedAtMs)
                    .last("limit 1")
            );

            if (cache != null && cache.getPreviewJson() != null) {
                log.debug("找到缓存的查询预览，sqlHash: {}", sqlHash);
                return Optional.of(cache.getPreviewJson());
            }

            return Optional.empty();
        } catch (Exception e) {
            log.error("查询缓存预览失败: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }

    @Override
    public void savePreview(Long tenantId, Long dbConfigId, Long schemaVersionId, String sqlHash, 
                           String resultPreview, long ttlMs) {
        try {
            // 删除旧的缓存记录
            cacheMapper.delete(
                new LambdaQueryWrapper<QueryResultCache>()
                    .eq(QueryResultCache::getTenantId, tenantId)
                    .eq(QueryResultCache::getDbConfigId, dbConfigId)
                    .eq(QueryResultCache::getSqlHash, sqlHash)
            );

            // 创建新的缓存记录
            QueryResultCache cache = new QueryResultCache();
            cache.setTenantId(tenantId);
            cache.setDbConfigId(dbConfigId);
            cache.setSchemaVersionId(schemaVersionId);
            cache.setSqlHash(sqlHash);
            cache.setPreviewJson(resultPreview);
            cache.setCreatedAtMs(System.currentTimeMillis());
            cache.setExpiredAtMs(System.currentTimeMillis() + ttlMs);

            cacheMapper.insert(cache);
            log.debug("保存查询预览缓存成功，sqlHash: {}", sqlHash);

        } catch (Exception e) {
            log.error("保存查询预览缓存失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 计算SQL的hash值
     */
    public String calculateSqlHash(String sql) {
        // 标准化SQL（移除多余空格、转小写）
        String normalizedSql = sql.trim().toLowerCase().replaceAll("\\s+", " ");
        return MD5Util.encrypt(normalizedSql);
    }
}
