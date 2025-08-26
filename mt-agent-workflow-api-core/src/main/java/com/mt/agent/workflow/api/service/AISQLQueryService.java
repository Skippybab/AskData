package com.mt.agent.workflow.api.service;

/**
 * AI SQL查询服务接口
 * 用于将自然语言转换为SQL
 */
public interface AISQLQueryService {
    
    /**
     * 生成SQL语句
     * @param queryText 查询文本
     * @param tableName 表名
     * @param pythonCode Python代码上下文
     * @param historyStr 历史对话
     * @param question 用户问题
     * @param tables 表结构信息
     * @return 生成的SQL语句
     */
    String generateSQL(String queryText, String tableName, String pythonCode, 
                      String historyStr, String question, String tables);

}
