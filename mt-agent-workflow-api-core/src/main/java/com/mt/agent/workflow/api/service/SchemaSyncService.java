package com.mt.agent.workflow.api.service;

import com.mt.agent.workflow.api.entity.SchemaVersion;

public interface SchemaSyncService {
    SchemaVersion startSync(Long dbConfigId);
}


