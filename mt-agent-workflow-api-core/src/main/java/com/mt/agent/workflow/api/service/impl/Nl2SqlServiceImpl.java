package com.mt.agent.workflow.api.service.impl;

import com.mt.agent.workflow.api.service.Nl2SqlService;
import com.mt.agent.workflow.api.service.SchemaContextService;
import com.mt.agent.workflow.api.util.DifyWorkflowCaller;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class Nl2SqlServiceImpl implements Nl2SqlService {

    @Autowired
    private SchemaContextService schemaContextService;
    @Autowired
    private DifyWorkflowCaller difyWorkflowCaller;

    @Value("${dify.nl2sql.base-url:http://113.45.193.155:8888/v1}")
    private String nl2sqlBaseUrl;
    
    @Value("${dify.nl2sql.api-key:app-E96pDaNHzay95rKx49Vppnwy}")
    private String nl2sqlApiKey;

    @Override
    public String generateSql(Long dbConfigId, String instruction) {
        try {
            log.info("开始NL2SQL转换，dbConfigId: {}, instruction: {}", dbConfigId, instruction);

            // 1. 获取数据库schema上下文
            String schemaContext = schemaContextService.buildPromptContext(dbConfigId);
            
            // 2. 构建Dify调用参数
            Map<String, Object> inputs = new HashMap<>();
            inputs.put("schema_context", schemaContext);
            inputs.put("user_question", instruction);
            inputs.put("db_type", "mysql"); // 目前固定为mysql
            
            // 3. 调用Dify工作流
            String userId = "nl2sql_user_" + System.currentTimeMillis();
            DifyWorkflowCaller.DifyWorkflowResponse response = difyWorkflowCaller.executeWorkflow(
                nl2sqlBaseUrl, nl2sqlApiKey, inputs, userId
            );

            if (!response.isSuccess()) {
                log.error("Dify NL2SQL调用失败: {}", response.getErrorMessage());
                throw new RuntimeException("NL2SQL转换失败: " + response.getErrorMessage());
            }

            // 4. 提取生成的SQL
            String generatedSql = response.getOutput("sql");
            if (generatedSql == null || generatedSql.trim().isEmpty()) {
                // 尝试从text字段获取
                generatedSql = response.getText();
                if (generatedSql == null || generatedSql.trim().isEmpty()) {
                    throw new RuntimeException("Dify未返回有效的SQL语句");
                }
            }

            log.info("NL2SQL转换成功，生成SQL: {}", generatedSql);
            return generatedSql.trim();

        } catch (Exception e) {
            log.error("NL2SQL转换异常: {}", e.getMessage(), e);
            throw new RuntimeException("自然语言转SQL失败: " + e.getMessage(), e);
        }
    }
}
