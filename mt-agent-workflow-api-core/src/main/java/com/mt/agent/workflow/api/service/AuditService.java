package com.mt.agent.workflow.api.service;

public interface AuditService {
    void log(String action, String objectType, Long objectId, String detailJson, String ip);
}


