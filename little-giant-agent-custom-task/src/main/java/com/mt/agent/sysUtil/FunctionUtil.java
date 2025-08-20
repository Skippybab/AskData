package com.mt.agent.sysUtil;

import com.mt.agent.reporter.SubEventReporter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 所有工具的汇总调度
 */
@Slf4j
@Component
public class FunctionUtil {

    @Autowired
    private AIQueryAnswerUtil aiQueryAnswerUtil;
    @Autowired
    private AISQLQueryUtil aisqlQueryUtil;
    @Autowired
    private AISummaryUtil aiSummaryUtil;
    @Autowired
    private VisualUtil visualUtil;

    public String sysQueryAnswer(String text) {
        return aiQueryAnswerUtil.sysQueryAnswer(text);
    }

    public String genSQLOld(String queryText,String tableName, String pythonCode) {
        pythonCode = pythonCode.replace("```python", "").replace("```Python", "").replace("```", "");
        return aisqlQueryUtil.genSQLOld(queryText,tableName, pythonCode);
    }

    // @@修改提示词
    public String genSQLV14(String queryText, String tableName, String pythonCode, String diagHistory, String question) {
        pythonCode = pythonCode.replace("```python", "").replace("```Python", "").replace("```", "");
        return aisqlQueryUtil.genSQL(queryText,tableName, pythonCode, diagHistory, question);
    }

    public String genSQLCAICT(String queryText, String tableName, String pythonCode, String diagHistory, String question,String  tables) {
        pythonCode = pythonCode.replace("```python", "").replace("```Python", "").replace("```", "");
        return aisqlQueryUtil.genSQLCAICT(queryText,tableName, pythonCode, diagHistory, question, tables);
    }
    public String genSQLCAICTSilicon(String queryText, String tableName, String pythonCode, String diagHistory, String question,String  tables) {
        pythonCode = pythonCode.replace("```python", "").replace("```Python", "").replace("```", "");
        return aisqlQueryUtil.genSQLCAICTSilicon(queryText,tableName, pythonCode, diagHistory, question, tables);
    }


    public List<Map<String, Object>> executeSQL(String sql) {
        return aisqlQueryUtil.executeSQL(sql);
    }

    // todo summary待修改
    public String stepSummary(String summaryTitle){
        return aiSummaryUtil.stepSummary(summaryTitle);
    }

    public void visTextBox(String text, SubEventReporter reporter) {
        reporter.reportNodeResult(visualUtil.visTextBox(text));
    }

    public void visTextBlock(String nameField, Double valueField, SubEventReporter reporter) {
        reporter.reportNodeResult(visualUtil.visTextBlock(nameField, valueField));
    }

    public void visSingleBar(String title, List<String> xData, List<Double> yData, SubEventReporter reporter) {
        reporter.reportNodeResult(visualUtil.visBarChart(title, xData, yData));
    }

    public void visClusteredBar(
            String title, List<String> xData, String barALabel,
            String barBLabel,List<Double> barAYData, List<Double> barBYData,
            SubEventReporter reporter) {
        reporter.reportNodeResult(visualUtil.visDoubleBarChart(title,xData,barALabel,barBLabel ,barAYData,barBYData));
    }

    public void visPieChart(String title, List<String> pieTags, List<Double> pieData, SubEventReporter reporter) {
        reporter.reportNodeResult(visualUtil.visPieChart(title, pieTags, pieData));
    }

    public void visTable(String title, List<Map<String,Object>> data, SubEventReporter reporter) {
        reporter.reportNodeResult(visualUtil.toTwoDTable(title, data));
    }


}
