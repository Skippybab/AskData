package com.mt.agent.workflow.api.util;

public interface ExePrompt {


String
        GEN_SQL = """
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
            - SQL生成的“别名”要跟Python代码中使用的“别名”保持一致
            - 请直接返回由“```SQL”和“```”包裹的SQL代码，不要返回任何解释文本
            ```
            """;


}
