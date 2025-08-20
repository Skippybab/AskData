package com.mt.agent.enums;

import lombok.Getter;
import lombok.AllArgsConstructor;

import java.util.Arrays;
import java.util.Optional;

/**
 * 可视化工具枚举类
 * 对应PromptParam.VISUAL_TOOL_PARAM中定义的可视化工具
 */
@Getter
@AllArgsConstructor
public enum VisualizationToolEnum {

    /**
     * 文本框
     * 以文本形式进行输出
     */
    TEXT_BOX("文本框",
            new String[]{"文本内容"}),

    /**
     * 单柱状图
     * 以柱状图的形式呈现一组数据，每个数据类别对应单个数据系列
     */
    SINGLE_BAR_CHART("单柱状图",
            new String[]{"图表标题", "X轴数据", "Y轴数据"}),

    /**
     * 二分组柱状图
     * 以柱状图的形式呈现两组数据，每个数据类别对应两个数据系列
     */
    DOUBLE_BAR_CHART("二分组柱状图",
            new String[]{"图表标题", "X轴数据", "A柱的标签", "B柱的标签", "A柱对应的数据", "B柱对应的数据"}),

    /**
     * 饼图
     * 以饼图的形式呈现数据
     */
    PIE_CHART("饼图",
            new String[]{"图表标题", "饼图标签列表", "饼图数据"}),

    /**
     * 指标信息块
     * 以信息块的形式呈现指标内容
     */
    INDICATOR_BLOCK("指标信息块",
            new String[]{"指标名称", "指标值"});

    /**
     * 工具名称
     */
    private final String name;

    /**
     * 输入参数列表
     */
    private final String[] inputs;

    /**
     * 根据工具名称获取枚举值
     *
     * @param name 工具名称
     * @return 对应的枚举值，如果不存在则返回空
     */
    public static Optional<VisualizationToolEnum> getByName(String name) {
        return Arrays.stream(VisualizationToolEnum.values())
                .filter(tool -> tool.getName().equals(name))
                .findFirst();
    }

    /**
     * 获取工具所需的输入参数数量
     *
     * @return 输入参数数量
     */
    public int getInputCount() {
        return inputs.length;
    }

    /**
     * 验证输入参数数量是否符合要求
     *
     * @param inputParams 实际输入的参数列表
     * @return 是否符合要求
     */
    public boolean validateInputCount(String[] inputParams) {
        return inputParams != null && inputParams.length >= inputs.length;
    }
}