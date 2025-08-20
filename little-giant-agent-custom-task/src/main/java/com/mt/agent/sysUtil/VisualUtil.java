package com.mt.agent.sysUtil;

import cn.hutool.json.JSONException;
import com.mt.agent.enums.ViewComponentType;
import com.mt.agent.utils.CalculateUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

import static com.mt.agent.utils.CalculateUtil.formatWithUnit;

/**
 * 数据可视化工具类
 * 用于将各种数据格式转换为前端可视化所需的标准化格式
 */
@Slf4j
@Component
public class VisualUtil {

    /**
     * 可视化数据类型枚举
     */
    public enum VisualType {
        INDICATOR_BLOCK("指标信息块"),
        BAR_CHART("单柱状图"),
        DOUBLE_BAR_CHART("二分组柱状图"),
        PIE_CHART("饼图"),
        MD_TEXT("文本框");

        private final String desc;

        VisualType(String desc) {
            this.desc = desc;
        }
    }

    /**
     * 转换为指标信息块格式
     *
     * @param nameField  指标名称字段
     * @param valueField 指标值字段
     * @return 指标信息块JSON
     */
    public Map<String,Object> visTextBlock(String nameField, Double valueField) {
        if (nameField==null) {
            return createEmptyIndicatorBlock();
        }


        Map<String,Object> result = new HashMap<>();
        result.put("type", ViewComponentType.INDICATOR_BLOCK.getCode());
        Map<String, Object> stringObjectMap = CalculateUtil.formatSingleWithUnit(valueField);
        result.put("unit", stringObjectMap.get("unit"));
        result.put("value", stringObjectMap.get("formatData"));

        result.put("label", nameField);

        return result;
    }

    /**
     * 转换为单柱状图格式
     *
     * @param title      图表标题
     * @param xData   横坐标字段
     * @param yData 数值字段
     * @return 单柱状图JSON
     */
    public Map<String,Object> visBarChart(String title,
            List<String> xData, List<Double> yData) {
        Map<String,Object> result;
        try {
            result = new HashMap<>();
            result.put("type", ViewComponentType.BARCHART.getCode());
            result.put("title", title);

            result.put("tags", xData);
            Map<String, Object> stringObjectMap = CalculateUtil.formatListWithMinUnit(yData);
            result.put("unit", stringObjectMap.get("unit"));
            result.put("value", stringObjectMap.get("formatData"));
        } catch (Exception e) {
            return createEmptyBarChart(title);
        }

        return result;
    }

    /**
     * 转换为二分组柱状图格式
     *
     * @param title       图表标题
     * @param xData   X轴数据
     * @param barALabel A柱的标签
     * @param barBLabel B柱的标签
     * @param barAYData      A柱对应的数据
     * @param barBYData      B柱对应的数据
     * @return 二分组柱状图JSON
     */
    public Map<String,Object> visDoubleBarChart(String title,
            List<String> xData, String barALabel, String barBLabel,List<Double> barAYData, List<Double> barBYData) {


        try {
            Map<String,Object> result = new HashMap<>();
            result.put("type", ViewComponentType.DOUBLE_BARCHART.getCode());
            result.put("title", title);
            result.put("unit", "");
            result.put("barLabel1", barALabel);
            result.put("barLabel2", barBLabel);


            result.put("tags", xData);
            result.put("barValue1", barAYData);
            result.put("barValue2", barBYData);

            return result;
        } catch (Exception e) {
            return createEmptyDoubleBarChart(title, barALabel, barBLabel);
        }
    }

    /**
     * 转换为饼图格式
     *
     * @param title      图表标题
     * @param pieTags  饼图标签列表
     * @param pieData 饼图数据
     * @return 饼图JSON
     */
    public Map<String,Object> visPieChart(String title,
            List<String> pieTags, List<Double> pieData) {


        try {
            Map<String,Object> result = new HashMap<>();
            result.put("type", ViewComponentType.PIE_CHART.getCode());
            result.put("title", title);
            Map<String, Object> stringObjectMap = CalculateUtil.formatListWithMinUnit(pieData);
            List<Double> formatData = (List<Double>) stringObjectMap.get("formatData");
            String unit = (String) stringObjectMap.get("unit");
            result.put("unit", unit);
            result.put("itemName", "");

            List<Object> dataList = new ArrayList<>();
            for (int i = 0; i < pieTags.size(); i++) {
                HashMap<String, Object> objectObjectHashMap = new HashMap<>();
                objectObjectHashMap.put("name", pieTags.get(i));
                if(pieData.get(i) != null){
                    objectObjectHashMap.put("value", formatData.get(i));
                }else {
                    objectObjectHashMap.put("value", 0);
                }
                dataList.add(objectObjectHashMap);
            }
            result.put("value", dataList);

            return result;
        } catch (JSONException e) {
            return createEmptyPieChart(title);
        }
    }

    /**
     * 转换为文本框格式
     *
     * @param text 文本内容
     * @return 文本框JSON
     */
    public Map<String,Object> visTextBox(String text) {
        Map<String,Object> result = new HashMap<>();
        result.put("type", ViewComponentType.MD_TEXT.getCode());

        result.put("text", text != null ? text : "");
        return result;
    }

    /**
     * 创建空的指标信息块
     */
    private Map<String,Object> createEmptyIndicatorBlock() {
        Map<String,Object> result = new HashMap<>();
        result.put("type", ViewComponentType.INDICATOR_BLOCK.getCode());
        result.put("value", 0);

        result.put("unit", "");
        result.put("label", "");
        return result;
    }

    /**
     * 创建空的单柱状图
     */
    private Map<String,Object> createEmptyBarChart(Object title) {
        Map<String,Object> result = new HashMap<>();
        result.put("type", ViewComponentType.BARCHART.getCode());
        result.put("title", title);
        result.put("unit", "");
        result.put("tags", new ArrayList<>());
        result.put("value", new ArrayList<>());
        return result;
    }

    /**
     * 创建空的二分组柱状图
     */
    private Map<String,Object> createEmptyDoubleBarChart(Object title, Object label1, Object label2) {
        Map<String,Object> result = new HashMap<>();
        result.put("type", ViewComponentType.DOUBLE_BARCHART.getCode());
        result.put("title", title);
        result.put("barUnit", " ");
        result.put("barLabel1", label1);
        result.put("barLabel2", label2);
        result.put("tags", new ArrayList<>());
        result.put("barValue1", new ArrayList<>());
        result.put("barValue2", new ArrayList<>());
        return result;
    }

    /**
     * 创建空的饼图
     */
    private Map<String,Object> createEmptyPieChart(Object title) {
        Map<String,Object> result = new HashMap<>();
        result.put("type", ViewComponentType.PIE_CHART.getCode());
        result.put("title", title);
        result.put("unit", "");
        result.put("value", new ArrayList<>());
        return result;
    }

    public Map<String,Object> toTwoDTable(String title, List<Map<String,Object>> data){
        Map<String,Object> result = new HashMap<>();
        result.put("type", ViewComponentType.TWO_D_TABLE.getCode());
        result.put("title", title);
        result.put("data", data);
        return result;
    }
}
