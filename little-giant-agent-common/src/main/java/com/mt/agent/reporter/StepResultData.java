package com.mt.agent.reporter;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 步骤结果数据，用于报告步骤执行结果
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StepResultData {

    /**
     * 显示类型
     */
    public enum DisplayType {
        TEXT, // 纯文本显示
        TABLE, // 表格显示
        COMPLETE // 步骤完成，无显示内容
    }

    /**
     * 纯文本结果
     *
     * @param text 文本内容
     */
    public StepResultData(String text) {
        this.displayType = DisplayType.TEXT;
        this.data = Map.of("text", text);
    }

    /**
     * 表格结果
     *
     * @param data 表格数据
     */
    public StepResultData(Map<String, Object> data) {
        this.displayType = DisplayType.TABLE;
        this.data = data;
    }

    /**
     * 显示类型
     */
    private DisplayType displayType = DisplayType.COMPLETE;

    /**
     * 数据内容
     *
     * 如果为纯文本，key为text，值为文本内容
     *
     * 如果为表格:
     * key1为columns，值为表格列名数组，值的格式为List<String>
     * key2为data，值为表格行数据数组，值的格式为List<List<String>>
     */
    private Map<String, Object> data;
}