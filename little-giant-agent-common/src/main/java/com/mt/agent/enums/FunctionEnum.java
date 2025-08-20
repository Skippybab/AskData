package com.mt.agent.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 系统功能枚举类
 * 整合了基础功能和可视化工具
 */
@Getter
@AllArgsConstructor
public enum FunctionEnum {

    /**
     * SQL查询功能
     * 根据用户需求生成sql，对指定数据表进行查询及使用聚合函数进行运算
     */
    SQL_QUERY("SQL查询",
            FunctionType.BASIC,
            new String[] { "年份", "行业名*", "企业名*", "城市", "区县*", "查询字段" }),

    /**
     * 系统功能解答
     * 对系统现有的功能进行讲解，帮助用户理解系统边界
     */
    SYSTEM_FUNCTION_ANSWER("系统功能解答",
            FunctionType.BASIC,
            new String[] { "问询文本" }),

    /**
     * 任务总结
     * 将当前任务的执行情况总结为文字
     */
    TASK_SUMMARY("任务总结",
            FunctionType.BASIC,
            new String[] { "任务步骤1的执行结果", "任务步骤2的执行结果", "..." }),

    /**
     * 文本框
     * 以文本形式进行输出
     */
    TEXT_BOX("文本框",
            FunctionType.VISUALIZATION,
            new String[] { "文本内容" }),

    /**
     * 单柱状图
     * 以柱状图的形式呈现一组数据，每个数据类别对应单个数据系列
     */
    SINGLE_BAR_CHART("单柱状图",
            FunctionType.VISUALIZATION,
            new String[] { "图表标题", "X轴数据", "Y轴数据" }),

    /**
     * 二分组柱状图
     * 以柱状图的形式呈现两组数据，每个数据类别对应两个数据系列
     */
    DOUBLE_BAR_CHART("二分组柱状图",
            FunctionType.VISUALIZATION,
            new String[] { "图表标题", "X轴数据", "A柱的标签", "B柱的标签", "A柱对应的数据", "B柱对应的数据" }),

    /**
     * 饼图
     * 以饼图的形式呈现数据
     */
    PIE_CHART("饼图",
            FunctionType.VISUALIZATION,
            new String[] { "图表标题", "饼图标签列表", "饼图数据" }),

    /**
     * 指标信息块
     * 以信息块的形式呈现指标内容
     */
    INDICATOR_BLOCK("指标信息块",
            FunctionType.VISUALIZATION,
            new String[] { "指标名称", "指标值" }),


    /**
     * 数组求和
     */
    SUM("数组求和",
            FunctionType.BASIC,
            new String[]{"数组数据"}),

    /**
     * 数组求平均值
     */
    AVERAGE("数组求平均",
            FunctionType.BASIC,
            new String[]{"数组数据"}),

    /**
     * 数值计算增长率
     */
    GROWTH_RATE("数值计算增长率",
            FunctionType.BASIC,
            new String[]{"数据旧值","数据新值"}),

    /**
     * 数值计算占比
     */
    PERCENTAGE("数值计算占比",
            FunctionType.BASIC,
            new String[]{"部分数值","整体数值"});

    /**
     * 功能名称
     */
    private final String name;

    /**
     * 功能类型
     */
    private final FunctionType type;

    /**
     * 输入参数列表
     */
    private final String[] inputs;

    /**
     * 根据功能名称获取枚举值
     *
     * @param name 功能名称
     * @return 对应的枚举，如果不存在则返回null
     */
    public static FunctionEnum getByName(String name) {
        for (FunctionEnum type : FunctionEnum.values()) {
            if (type.name.equals(name)) {
                return type;
            }
        }
        return null;
    }

    /**
     * 判断参数是否为必填
     * 非必填参数以*结尾
     *
     * @param paramName    参数名称
     * @return 是否为必填参数
     */
    public static boolean isParamRequired(String paramName) {
        return !paramName.endsWith("*");
    }

    /**
     * 获取参数的实际名称（去除*标记）
     *
     * @param paramName 参数名称
     * @return 实际参数名称
     */
    public static String getActualParamName(String paramName) {
        return paramName.endsWith("*") ? paramName.substring(0, paramName.length() - 1) : paramName;
    }

}