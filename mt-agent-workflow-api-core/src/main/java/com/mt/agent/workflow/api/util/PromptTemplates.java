package com.mt.agent.workflow.api.util;

/**
 * 提示词模板常量类
 */
public class PromptTemplates {
    
    /**
     * SQL生成提示词模板
     */
    public static final String SQL_GENERATION_TEMPLATE = """
        已知【本业务系统】支持用户通过文本对话的方式对数据进行分析，并将分析结果进行可视化。
        用户的【历史请求】包括
        ```
        {{diag_history}}
        {{question}}
        ```
        其中，当前【用户的请求】是最后一句
        根据当前【用户的请求】，【本业务系统】以Python代码的方式制定了以下【行动计划】
        {{py_codes}}
        
        已知gen_sql是通过调用LLM实现的，
        ```
        {{tableSchema}}
        ```
        当前，gen_sql的输入文本是```{{query_text}}```，指定的数据表的描述是```{{table_name}}```。请帮我生成一个可以直接执行的SQL代码，使得gen_sql执行之后，【行动计划】的Python代码可以正确响应用户请求。
        具体要求如下：
        - 只生成SELECT语句，绝不包含任何数据修改操作（INSERT、UPDATE、DELETE）
        - 生成的SQL代码符合Mysql语法，关键词之间要有空格隔开
        - 所有SQL中涉及的**表名、字段名必须使用英文**（与上方提供的表结构完全一致），不允许出现任何中文字段名或表名
        - 若需使用别名（AS后的名称），可以用中文（例如：SELECT enterprise_name AS `企业名称` FROM enterprise_operation 是允许的，但 SELECT 企业名称 FROM 企业营收数据表 是禁止的）
        - 禁止生成嵌套select
        - 禁止在WHERE条件中使用LAG窗函数
        - 不做单位转换处理，不要做任何假设
        - 字符串匹配的时候要允许模糊查询，并考虑语义相似性
        - SQL生成的"别名"要跟Python代码中使用的"别名"保持一致
        - 请直接返回由"```SQL"和"```"包裹的SQL代码，不要返回任何解释文本
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
}
