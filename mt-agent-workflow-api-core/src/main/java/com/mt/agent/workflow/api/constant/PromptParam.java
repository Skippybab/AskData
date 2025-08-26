package com.mt.agent.workflow.api.constant;

import jakarta.annotation.PostConstruct;

import java.util.HashMap;
import java.util.Map;

public interface PromptParam {

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

    String SYS_FUN_PARAMS_CULIVATE = """
            def gen_sql(query_text: str, table_name: str) -> str:
                '''基于文本描述的查询条件，生成sql代码'''
                return bridge.call_java_function('gen_sql', query_text, table_name)
        
            def exec_sql(sql_code: str) -> list[dict[str, Any]]:
                '''通过java的orm框架执行sql代码，固定返回List<Map>这种格式的查询结果'''
                return bridge.call_java_function('exec_sql', sql_code)
            
            def output_result(res: dict[str, object]) -> None:
               '''将待展示的结果进行输出,其中key值是数据说明'''
               return bridge.call_java_function('output_result', res)
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
    String SYS_FUN_SHORT = """
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
//            5、通过网页将查询结果和分析结果进行可视化
            ```
            """;


    String CAICT_TABLE_1 = """
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

    String CAICT_TABLE_2 = """
            SQL数据库的表名是```declaration_item_change_stats```，表描述为```申报项变化统计表```
            并且字段按照逗号分隔的格式“字段名,数据类型,字段描述”列举如下：
            enterprise_name,varchar,企业名称,
            enterprise_industry,varchar,行业名称,
            declaration_item,varchar,申报项名称,
            change_count,int,该申报项被修改的次数,
            consultation_count,int,该申报项被咨询的次数
            """;

    String BG1 = """
    主要用于对企业培育进度进行分析，支持用户按行业、区域筛选企业，分析培育情况，统计合格企业数量等
    """;

    String BG2 = """
    主要用于对企业梯度培育的目标达成情况进行分析，支持用户查询培育目标、分析目标完成情况数据等
    """;

    String BG3 = """
    主要用于对重点企业进行筛选分析，支持用户筛选重点企业、了解重点企业情况、掌握重点企业变化等
    """;

    String BG4 = """
    主要用于对重点企业进行成长潜力分析，支持按行业筛选企业，分析发展好的行业与企业、识别值得重点培养的企业等
    """;

    String BG5 = """
    主要用于对重点企业进行风险预警，支持用户分析企业经营数据、评估风险等级、识别高风险企业等
    """;

    String BG6 = """
    主要用于对重点企业进行数据分析，支持按行业筛选企业，分析各行业企业的营收数据、统计企业数量等
    """;

    String BG7 = """
    主要用于对企业政策进行实施效果分析，支持用户分析申报项变化、研究企业关注的政策、明晰实施效果等
    """;

    String BG8 = """
    主要用于对企业政策咨询的辅助效果进行分析，支持用户分析企业申报填写情况
    """;

    String BG9 = """
    主要用于对企业政策进行理解误区分析，支持用户分析申报项变化，提炼企业疑问集中、理解困难的政策并掌握情况等
    """;

    String BG10 = """
    主要用于分析区域竞争力的竞争优势，支持用户分析各区域申报企业或行业在发展、创新、人才等方面的差异及优势
    """;

    String BG11 = """
    主要用于分析不同区域竞争力的发展差距，支持用户对比不同区域的发展差距与核心短板，剖析各区域人才结构存在的问题等
    """;



}
