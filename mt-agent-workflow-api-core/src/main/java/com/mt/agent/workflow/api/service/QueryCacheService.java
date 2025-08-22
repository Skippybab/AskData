package com.mt.agent.workflow.api.service;

import java.util.Optional;

public interface QueryCacheService {
    Optional<String> findPreview(Long tenantId, Long dbConfigId, Long schemaVersionId, String sqlHash);
    void savePreview(Long tenantId, Long dbConfigId, Long schemaVersionId, String sqlHash, String resultPreview, long ttlMs);
}


