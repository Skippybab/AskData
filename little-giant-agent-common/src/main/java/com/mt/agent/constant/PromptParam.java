package com.mt.agent.constant;

public interface PromptParam {

    //支持自然语言调度的系统功能列表
    String CHAT_SYS_FUN= """
            - SQL查询：根据用户需求生成sql，对指定数据表进行查询及使用聚合函数进行运算
            - 系统功能解答：对系统现有的功能进行讲解，帮助用户理解系统边界，包括系统能做什么、可选择的数据表有哪些、数据字段有哪些
            - 任务总结：基于某个目的，对多个总结要点进行概况性总结
            - 数据计算：系统支持"数组求和"、"数组求平均"、"数值计算增长率"、"数值计算占比"这个几种类型的基础计算
            """;

    //需要键盘鼠标操作系统功能列表
    String OPERATE_FUN= """
            需要键盘鼠标操作的系统功能列表
            - 组件拖拽：问题回复内容支持拖拽到右边白板中，成为可视化组件
            - 组件调整：白板的组件支持调整大小、位置，可删除
            - 报告导出：当前白板上的组件布局和呈现结果可形成报告，保存为pdf/png文件
            - 用户数据上传：支持用户自行上传数据表文件作为新的数据源
            - 更改分析对象：可通过鼠标操作，选择要分析的数据
            """;

    //系统功能参数说明
    String SYS_FUN_PARAMS = """
            # sql代码生成
            def generate_sql(query_text: str) -> str:
                '''输入数据库查询相关步骤和条件，返回可执行的SQL代码'''
            
            # sql代码执行
            def execute_sql(sql_code: str) -> List[dict[str, object]]:
                '''输入可执行的SQL代码，返回SQL查询结果'''
            
            # 系统功能解释
            def explain_system_function(question: str) -> str:
                '''输入用户关于系统功能的提问，返回问题的回答文本'''
            
            # 文本框可视化
            def vis_textbox(content: str) -> None:
                '''输入文本内容，在前端对话界面渲染1个文本框'''
            
            # 信息块可视化
            def vis_textblock(title: str, value: float) -> None:
                '''输入单个指标的标题和数值，在前端对话界面渲染1个指标信息块'''
            
            # 单柱状图可视化
            def vis_single_bar(title: str, x_labels: List[str], y_data: List[float]) -> None:
                '''输入标题、X轴标签列表和Y轴数据列表，在前端对话界面渲染1个单柱状图'''
            
            # 二分组柱状图可视化
            def vis_clustered_bar(title: str, x_labels: List[str], group_a: List[float], group_b: List[float]) -> None:
                '''输入标题、X轴标签列表和a、b两组数据，在前端对话界面渲染1个二分组柱状图'''
            
            # 饼状图可视化
            def vis_pie_chart(title: str, labels: List[str], data: List[float]) -> None:
                '''输入标题、标签列表和数据列表，在前端对话界面渲染1个饼状图'''
            
            # 二维表格可视化
            def vis_table(title: str, data:List[dict[str, object]]) ->  None:
                '''输入表格名称和表格数据，在前端对话界面渲染1个二维表格'''
            """;

    // todo 数据信息${dataInfos}
    String DATA_INFOS = """
            2张数据表：
            1 广州2021年到2023年小巨人企业年度经营数据
            记录数量：370条
            字段描述：'企业所属区编码','所属地市','所属区县','注册时间','注册资本','所属行业','从事细分市场年限','年份','全职员工数量','营业收入','主营业务收入','主营业务收入占比','主持国际/国家标准','主持行业标准','参与制修订标准总数','有效发明专利数量','销售费用','管理费用','主营业务成本','毛利率百分比','人均营业收入','出口额','研发费用总额','研发费用占比','营业收入增长率','净利润总额','净利润增长率'
            
            2 广州天河2019年到2022年小巨人企业月度经营数据
            记录数量：89条
            字段描述：'注册时间','注册资本','所属行业','数据统计月份','全职员工数量','营业收入','有效发明专利数量','毛利率百分比','人均营业收入','出口额','研发费用总额','研发费用占比','营业收入增长率','净利润总额','净利润增长率'
            """;


    // todo 数据信息${dataInfo}
    String DATA_INFO = """
            数据信息描述，按照逗号分隔的格式“数据类型，字段描述”：
            bigint，'数据编号',
            varchar(16)，'注册时间',
            double，'注册资本（元）',
            varchar，'所属行业',
            varchar(6),'数据统计月份',
            int，'全职员工数量(人)',
            double，'营业收入（元）',
            int，'有效发明专利数量',
            double，'毛利率百分比',
            double，'人均营业收入（元）',
            double，'研发费用总额（元）',
            double，'研发费用占比',
            double，'营业收入增长率',
            double，'净利润总额（元）',
            double，'净利润增长率'
            """;

    // todo 数据源说明 ${tableSchema}
    String TABLE_SCHEMA_OLD = """
            并且字段按照逗号分隔的格式“字段名,数据类型,字段描述”列举如下：
            id,bigint,'数据编号',
            district_code,varchar,'企业代号',
            city,varchar,'所属地市',
            region,varchar,'所属区县',
            registered_time,varchar,'注册时间',
            registered_capital,double,'注册资本（元）',
            industry_code,varchar,'所属行业',
            market_experience,varchar,'从事细分市场年限',
            year,int,'年份',
            employee_num,int,'全职员工数量(人)',
            total_revenue,double,'营业收入（元）',
            main_revenue,double,'主营业务收入（元）',
            main_revenue_ratio,double,'主营业务收入占比',
            preside_international_std,int ,'主持国际/国家标准',
            preside_industry_std,int,'主持行业标准',
            participate_std,int ,'参与制修订标准总数',
            valid_patents,int,'有效发明专利数量',
            sales_expenses,double,'销售费用（元）',
            management_expenses,double,'管理费用（元）',
            main_business_cost,double,'主营业务成本（元）',
            gross_margin_pct,double,'毛利率百分比',
            revenue_per_capita,double,'人均营业收入（元）',
            export_amount,double,'出口额（元）',
            rd_expense_total, double,'研发费用总额（元）',
            rd_revenue_ratio,double,'研发费用占比',
            revenue_growth_rate,double,'营业收入增长率',
            net_profit,double,'净利润总额（元）',
            profit_growth_rate,double,'净利润增长率
            """;

    String TABLE_SCHEMA = """
            并且字段按照逗号分隔的格式“字段名,数据类型,字段描述”列举如下：
            id,bigint,数据编号,
            district_code,varchar,企业代号,
            city,varchar,所属地市,
            region,varchar,所属区县,
            registered_time,varchar,注册时间,
            registered_capital,double,注册资本_元,
            industry_code,varchar,所属行业数字编号及名称,
            market_experience,int,从事细分市场年限_年,
            year,int,年份,
            employee_num,int,全职员工数量_人,
            total_revenue,double,营业收入_元,
            main_revenue,double,主营业务收入_元,
            main_revenue_ratio,double,主营业务收入占比_%,
            preside_international_std,int ,主持国际/国家标准_个,
            preside_industry_std,int,主持行业标准_个,
            participate_std,int,参与制修订标准总数_个,
            valid_patents,int,有效发明专利数量_个,
            sales_expenses,double,销售费用_元,
            management_expenses,double,管理费用_元,
            main_business_cost,double,主营业务成本_元,
            gross_margin_pct,double,毛利率百分比_%,
            revenue_per_capita,double,人均营业收入_元,
            export_amount,double,出口额_元,
            rd_expense_total,double,研发费用总额_元,
            rd_revenue_ratio,double,研发费用占比_%,
            revenue_growth_rate,double,营业收入增长率_%,
            net_profit,double,净利润总额_元,
            profit_growth_rate,double,净利润增长率_%,
            employee_num_growth_rate,double,全职员工数量增长率_%,
            main_revenue_growth_rate,double,主营业务收入增长率_%,
            net_profit_margin,double,净利率_%,
            gross_margin_pct_growth_rate,double,毛利率增长率_%,
            net_profit_margin_growth_rate,double,净利率增长率_%
            """;

    //系统支持的可视化样式
    String SYS_VISUAL_STYLE = """
            - 文本框：以文本形式进行输出一段文本
            - 单柱状图：以柱状图的形式呈现单一指标在多个同类别下的数值，如展示2021-2023年营收规模，X轴为年份，Y轴为营收规模的数值
            - 二分组柱状图：以柱状图的形式一起呈现两种指标的多个数值，如展示从2021到2023年的营收规模（A柱）与利润规模（B柱），X轴为指标共性部分（例如数据都是2021年），Y轴为双指标相同维度的数值（如金额，如增长率）
            - 饼图：以饼图的形式呈现数据
            - 指标信息块：以信息块的形式呈现指标数据，包括指标的标签、指标的数值
            """;

    //可视化工具参数说明
    String VISUAL_TOOL_PARAM = """
            # 文本框可视化
            def vis_textbox(content: str) -> None:
                '''输入文本内容，在前端对话界面渲染1个文本框'''
            
            # 信息块可视化
            def vis_textblock(title: str, value: float) -> None:
                '''输入单个指标的标题和数值，在前端对话界面渲染1个指标信息块'''
            
            # 单柱状图可视化
            def vis_single_bar(title: str, x_labels: List[str], y_data: List[float]) -> None:
                '''输入标题、X轴标签列表和Y轴数据列表，生成在前端对话界面渲染1个单柱状图'''
            
            # 二分组柱状图可视化
            def vis_clustered_bar(title: str, x_labels: List[str], group_a: List[float], group_b: List[float]) -> None:
                '''输入标题、X轴标签列表和a、b两组数据，在前端对话界面渲染1个二分组柱状图'''
            
            # 饼状图可视化
            def vis_pie_chart(title: str, labels: List[str], data: List[float]) -> None:
                '''输入标题、标签列表和数据列表，在前端对话界面渲染1个饼状图'''
            
            # 二维表格可视化
            def vis_table(title: str, data: List[dict[str, object]]) ->  None:
                '''输入表格名称和表格数据，在前端对话界面渲染1个二维表格'''
            """;


    //支持自然语言调度的系统功能列表
    String SYS_FUN_SHORT= """
            【本业务系统】支持用户通过文本对话的方式对数据进行分析，并将分析结果进行可视化，其中
            - 【数据范围】仅限于以下内容
            ```
            {{all_table_names}}
            ```
            - 【分析能力】仅限于以下功能
            ```
            1、通过文本对话设定“数据查询条件”和结果的“可视化参数”
            2、根据“数据查询条件”生成SQL查询语句
            3、执行SQL语句，获得“数据查询结果”
            4、对若干“数据查询结果”进行二次分析（包括统计、排序、四则运算等）
            5、通过网页将查询结果和分析结果进行可视化
            ```
            """;


    String CAICT_TABLE_1="""
            SQL数据库的表名是```enterprise_operation```，表描述为```2023年到2025年广州市各企业年度营收数据表```
            并且字段按照逗号分隔的格式“字段名,数据类型,字段描述”列举如下：
            enterprise_name,varchar,企业名称,
            enterprise_industry,varchar,企业行业,
            year,int,年份,
            full_time_employees,int,全职员工数量_人,
            r_d_employees,int,研发人员数量_人,
            operating_income,double,营业收入_元,
            main_business_income,double,主营业务收入_元,
            main_business_income_growth_rate,double,主营业务收入增长率_%,
            total_profit,double,利润总额_元,
            net_profit,double,净利润总额_元,
            net_profit_growth_rate,double,净利润增长率_%,
            selling_expenses,double,销售费用_元,
            management_expenses,double,管理费用_元,
            operating_cost,double,营业成本_元,
            main_business_cost,double,主营业务成本_元,
            product_sales_cost,double,产品销售成本_元,
            total_assets,double,资产总额_元,
            end_period_net_assets,double,期末净资产_元,
            total_liabilities,double,负债总额_元,
            asset_liability_ratio,double,资产负债率_%,
            tax_paid,double,上缴税金_元,
            equity_financing_amount,double,股权融资总额_元,
            corresponding_valuation,double,对应估值_元,
            bank_loans,double,银行贷款_元,
            domestic_bonds,double,境内债券_元,
            overseas_bonds,double,境外债券_元,
            total_r_d_expenses,double,研发费用总额_元,
            r_d_expenses_ratio,double,研发费用总额占营业收入总额比重,
            r_d_employees_ratio,double,研发人员占全部职工比重_%
            """;

    String CAICT_TABLE_2="""
            SQL数据库的表名是```declaration_item_change_stats```，表描述为```申报项变化统计表```
            并且字段按照逗号分隔的格式“字段名,数据类型,字段描述”列举如下：
            enterprise_name,varchar,企业名称,
            enterprise_industry,varchar,行业名称,
            declaration_item,varchar,申报项名称,
            change_count,int,该申报项被修改的次数,
            consultation_count,int,该申报项被咨询的次数
            """;

}
