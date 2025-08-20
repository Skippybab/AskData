package com.mt.agent.router.constant;

public interface Prompt {

    // 路由匹配
    String PY_TO_JAVA = """
           已知以下python代码：
           ```
           ${py_orders}
           ```
           
           已知以下函数都在 com.mt.agent.sysUtil.FunctionUtil 类中实现：
           ```
           String sysQueryAnswer(String text);
           String sqlGenSingleInd(String queryText);
           String sqlGenSingleEnt(String queryText);
           String sqlGenFieldRange(String fieldName);
           List<Map<String, Object>> executeSQL(String sql);
           List<Map<String, Object>> cpDualInd(String firstInd, String secondInd,String queryText)
           List<Map<String, Object>> cpDualEnt(String firstInd, String secondInd,String queryText)
           void visTextBox(String text, SubEventReporter reporter);
           void visTextBlock(String nameField, Double valueField, SubEventReporter reporter);
           void visSingleBar(String title, List<String> xData, List<Double> yData, SubEventReporter reporter);
           void visClusteredBar(String title, List<String> xData, String barALabel,String barBLabel,List<Double> barAYData, List<Double> barBYData, SubEventReporter reporter);
           void visPieChart(String title, List<String> pieTags, List<Double> pieData, SubEventReporter reporter);
           void visTable(String title, List<Map<String, Object>> data, SubEventReporter reporter);
           ```
           
           已知以下函数都在 com.mt.agent.sysUtil.DataUtil 类中实现：
           ```
           public static List<String> extractStringListFromDataList(List<Map<String, Object>> dataList, String propertyName) // 从List<Map<String, Object>>中提取指定属性名的所有值，返回List<String>
           public static List<Double> extractDoubleListFromDataList(List<Map<String, Object>> dataList, String propertyName) // 从List<Map<String, Object>>中提取指定属性名的所有值，返回List<Double>
           ```
           
           请参考python代码，将代码中的main函数实现翻译成可执行的java文件，要求：
           - 输出结果只包含直接可执行的java代码，不要有额外的说明,使用```java```包裹符号
           - java类名以ExcuseSteps命名,需要生成package和包导入
           - 执行逻辑的函数定义为 String steps(FunctionUtil functionUtil, SubEventReporter reporter)
           - SubEventReporter的类目录是 com.mt.agent.reporter.SubEventReporter,调用FunctionUtil中的vis开头的函数直接使用传入 steps 的 reporter
           - StepResultData 的类目录是 com.mt.agent.reporter.StepResultData
           - 检查sqlGen开头函数的返回结果变量,如果后续步骤使用到sql执行的结果，请在函数入参的queryText拼接限制,例如加上"查询字段包含..."
           - 检查方法入参来自executeSQL执行结果的地方，通过Map get()的方式获取参数要检查key值是否上面代码有明确提到
           - 如果涉及数据类型转化，请使用包装类的方法进行，不能直接进行强转，默认值也要注意类型
           - 在原本的实现逻辑中，结合步骤的注释说明，用以下形式写入步骤执行信息如“开始执行步骤1 查询数据”
           ```
           reporter.reportStep("开始执行步骤1 生成SQL");
           // 步骤1执行
           reporter.reportStepResult(new StepResultData());//步骤1执行完成
           reporter.reportStep("开始执行步骤2 执行SQL");
           // 步骤2执行
           reporter.reportStepResult(new StepResultData());//步骤2执行完成
           ```
           """;

    String PY_TO_JAVA_ORDER = """
           已知以下【python代码】：
           ```
           ${py_orders}
           ```
           
           已知现有的【java工具函数】如下：
           String sysQueryAnswer(String text);
           String sqlGenSingleInd(String queryText);
           String sqlGenSingleEnt(String queryText);
           String sqlGenFieldRange(String fieldName);
           List<Map<String, Object>> executeSQL(String sql);
           List<Map<String, Object>> cpDualInd(String firstInd, String secondInd,String queryText)
           List<Map<String, Object>> cpDualEnt(String firstInd, String secondInd,String queryText)
           void visTextBox(String text);
           void visTextBlock(String nameField, Double valueField);
           void visSingleBar(String title, List<String> xData, List<Double> yData);
           void visClusteredBar(String title, List<String> xData, String barALabel,String barBLabel,List<Double> barAYData, List<Double> barBYData);
           void visPieChart(String title, List<String> pieTags, List<Double> pieData);
           void visTable(String title, List<Map<String, Object>> data);
           
           已知现有的计算与类型转化函数如下：
           double sum(List<Double> list);//对list数组进行求和
           double avg(List<Double> list);//对list数组求平均数
           double changeRate(Double lastData, Double nextData);//求变化率
           double percentage(Double part, Double total);//求占比
           List<String> extractStringListFromDataList(List<Map<String, Object>> dataList, String propertyName); // 从List<Map<String, Object>>中提取指定属性名的所有值，返回List<String>
           List<Double> extractDoubleListFromDataList(List<Map<String, Object>> dataList, String propertyName); // 从List<Map<String, Object>>中提取指定属性名的所有值，返回List<Double>
           
           请参考【python代码】，将代码中的main函数实现翻译成可执行的【java工具函数】调用步骤列表，要求：
           - 直接输出步骤列表，不要有额外的说明
           - 检查sqlGen开头函数的返回结果变量,如果后续步骤使用到sql执行的结果，请在函数入参的queryText拼接限制,例如加上"查询字段包含..."
           - 步骤列表的格式如下：
           返回结果 = 函数名称（参数1,参数2,...）
           sql1 = sqlGenSingleInd("...")
           data1 = executeSQL(sql1);
           - 步骤中返回结果的命名要唯一，以保证步骤可执行
           """;


}
