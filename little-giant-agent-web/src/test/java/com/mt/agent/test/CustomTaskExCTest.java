package com.mt.agent.test;

import com.mt.agent.enums.FunctionEnum;
import com.mt.agent.model.dto.CustomTaskResultDTO;
import com.mt.agent.model.workflow.TaskStep;
import com.mt.agent.service.CustomTaskService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.List;

@Slf4j
@SpringBootTest
public class CustomTaskExCTest {
    @Autowired
    private CustomTaskService customTaskService;

    /**
     * 测试文本回复
     */
    @Test
    public void testSysQA() {
        TaskStep step = new TaskStep("步骤1", "查询系统支持什么数据表的分析", "系统功能解答", List.of("问询文本=查询系统支持什么数据表的分析"), List.of("数据源信息列表"));

//        //构建一个步骤与方法的映射map
//        HashMap<String, String> stepFunMap = new HashMap<>();
//        stepFunMap.put(step.getNum(), step.getFunName());
//
//        HashMap<String, Object> paramMap = new HashMap<>();
//        CustomTaskResultDTO customTaskResultDTO = customTaskService.executeCustomTask("4", step, paramMap, stepFunMap);
//        log.info("customTaskResultDTO:{}", customTaskResultDTO);

    }

    @Test
    public void testSQLGen() {
        TaskStep step = new TaskStep("步骤1", "筛选广州市通用设备制造业2023年营收数据", FunctionEnum.SQL_QUERY.getName(), List.of("2023", "通用设备制造业", "null", "广州", "null", "区县, 营收"), List.of("区县营收数据"));

        //构建一个步骤与方法的映射map
//        HashMap<String, String> stepFunMap = new HashMap<>();
//        stepFunMap.put(step.getNum(), step.getFunName());
//
//        HashMap<String, Object> paramMap = new HashMap<>();
//        CustomTaskResultDTO customTaskResultDTO = customTaskService.executeCustomTask("4", step, paramMap, stepFunMap);
//        log.info("customTaskResultDTO:{}", customTaskResultDTO);
    }

}
