package com.mt.agent.test;

import com.mt.agent.sysUtil.AISQLQueryUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

@Slf4j
@SpringBootTest
public class TestSQLExecute {
    @Autowired
    private AISQLQueryUtil aiSQLQueryUtil;

    @Test
    public void testExecuteSQL1() {
        String sql = """
                SELECT AVG(revenue_per_capita) AS `人均营业收入_元_avg`, year FROM data_little_giant_business_info WHERE year = 2022 GROUP BY year
                UNION ALL
                SELECT AVG(revenue_per_capita) AS `人均营业收入_元_avg`, year FROM data_little_giant_business_info WHERE year = 2023 GROUP BY year;
                """;

        List<Map<String, Object>> maps = aiSQLQueryUtil.executeSQL(sql);
        log.info("maps: {}", maps);

    }
    @Test
    public void testExecuteSQL2() {
        String sql = """
                SELECT
                    AVG(employee_num) AS avg_employee_num_2023,
                    (SELECT AVG(employee_num) FROM data_little_giant_business_info WHERE industry_code LIKE '%医药制造业%' AND year = 2022) AS avg_employee_num_2022
                FROM
                    data_little_giant_business_info
                WHERE
                    industry_code LIKE '%医药制造业%' AND year = 2023
                """;

        List<Map<String, Object>> maps = aiSQLQueryUtil.executeSQL(sql);
        log.info("maps: {}", maps);

    }
}
