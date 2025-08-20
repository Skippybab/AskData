package com.mt.agent.workflow.api.util;

/**
 * 提示词模板常量类
 */
public class PromptTemplates {
    
    /**
     * SQL生成提示词模板
     */
    public static final String SQL_GENERATION_TEMPLATE = """
        你是一个专业的数据库查询专家。请根据用户的自然语言描述生成对应的SQL查询语句。
        
        ## 表结构信息：
        {{tableSchema}}
        
        ## 用户问题：
        {{question}}
        
        ## 查询描述：
        {{query_text}}
        
        ## 目标表：
        {{table_name}}
        
        ## 历史对话上下文：
        {{diag_history}}
        
        ## Python代码上下文（如果有）：
        {{py_codes}}
        
        ## 要求：
        1. 只生成SELECT查询语句
        2. 确保SQL语法正确
        3. 根据表结构使用正确的字段名
        4. 如果需要聚合，使用适当的GROUP BY
        5. 添加必要的WHERE条件
        6. 考虑性能，添加适当的LIMIT
        7. 只返回SQL语句，不要有其他解释
        
        请生成SQL查询语句：
        """;
    
    /**
     * Python代码生成模板
     */
    public static final String PYTHON_CODE_GENERATION_TEMPLATE = """
        你是一个Python数据分析专家。请根据用户的需求生成Python代码来查询和分析数据。
        
        ## 可用的系统函数：
        - gen_sql(query_text, table_name): 生成SQL查询语句
        - exec_sql(sql_code): 执行SQL并返回结果
        - vis_textbox(content): 显示文本框
        - vis_textblock(title, value): 显示指标块
        - vis_single_bar(title, x_labels, y_data): 显示柱状图
        - vis_pie_chart(title, labels, data): 显示饼图
        - vis_table(title, data): 显示表格
        
        ## 用户需求：
        {{user_question}}
        
        ## 可用的数据表：
        {{available_tables}}
        
        ## 要求：
        1. 使用提供的系统函数查询数据
        2. 对查询结果进行必要的处理和分析
        3. 使用可视化函数展示结果
        4. 代码要有适当的错误处理
        5. 添加必要的注释
        
        请生成Python代码：
        """;
    
    /**
     * 数据解释模板
     */
    public static final String DATA_EXPLANATION_TEMPLATE = """
        请对以下查询结果进行分析和解释：
        
        ## 用户问题：
        {{question}}
        
        ## 执行的SQL：
        {{sql}}
        
        ## 查询结果：
        {{result}}
        
        ## 要求：
        1. 用通俗易懂的语言解释数据
        2. 突出关键发现和洞察
        3. 如果数据有异常或特殊情况，请指出
        4. 给出可能的建议或后续分析方向
        
        请提供数据分析：
        """;
}
