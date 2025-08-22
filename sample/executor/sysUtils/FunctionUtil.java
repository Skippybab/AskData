package com.cultivate.executor.sysUtils;

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
    private AISQLQueryUtil aisqlQueryUtil;

    public String genSQLCAICT(String queryText, String tableName, String pythonCode, String diagHistory, String question,String  tables) {
        pythonCode = pythonCode.replace("```python", "").replace("```Python", "").replace("```", "");
        return aisqlQueryUtil.genSQLCAICT(queryText,tableName, pythonCode, diagHistory, question, tables);
    }

    public List<Map<String, Object>> executeSQL(String sql) {
        return aisqlQueryUtil.executeSQL(sql);
    }


}
