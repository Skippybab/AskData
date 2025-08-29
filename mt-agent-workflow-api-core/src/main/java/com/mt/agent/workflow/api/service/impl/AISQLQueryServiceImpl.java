package com.mt.agent.workflow.api.service.impl;

import com.mt.agent.workflow.api.service.AISQLQueryService;
import com.mt.agent.workflow.api.service.SchemaContextService;
import com.mt.agent.workflow.api.util.AISQLQueryUtil;
import com.mt.agent.workflow.api.util.DifyWorkflowCaller;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * AI SQL查询服务实现
 */
@Slf4j
@Service
public class AISQLQueryServiceImpl implements AISQLQueryService {
    
    @Autowired
    private SchemaContextService schemaContextService;
    
    @Autowired
    private DifyWorkflowCaller difyWorkflowCaller;

    @Autowired
    private AISQLQueryUtil aisqlQueryUtil;
    
    @Value("${dify.nl2sql.base-url:http://113.45.193.155:8888/v1}")
    private String nl2sqlBaseUrl;
    
    @Value("${dify.nl2sql.api-key:app-E96pDaNHzay95rKx49Vppnwy}")
    private String nl2sqlApiKey;
    
    @Override
    public String generateSQL(String queryText, String tableName, String pythonCode, 
                            String historyStr, String question, String tables) {
        try {
//            log.info("开始生成SQL，queryText: {}, tableName: {}", queryText, tableName);
            return aisqlQueryUtil.genSQLCAICT(queryText,tableName, pythonCode, historyStr, question, tables);

        } catch (Exception e) {
            log.error("生成SQL失败: {}", e.getMessage(), e);
            throw new RuntimeException("生成SQL失败: " + e.getMessage());
        }
    }

}
