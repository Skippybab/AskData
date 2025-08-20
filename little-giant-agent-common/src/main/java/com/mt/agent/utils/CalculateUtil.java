package com.mt.agent.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据计算工具类
 *
 * @author lfz
 * @date 2025/3/25 17:00
 */
public class CalculateUtil {

    /**
     * 格式化double值为两位小数
     *
     * @author lfz
     * @date 2025/3/25 17:01
     * @param value
     * @return
     */
    public static double formatDouble(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    /**
     * 单位转换，根据数值大小自动转换为对应单位表示
     * 支持单位：万、亿
     * 保留两位小数
     *
     * @param value 原始数值
     * @return Map<String, Object> 包含 value（保留两位小数的数值）和 unit（单位）
     */
    public static Map<String, Object> formatWithUnit(double value) {
        Map<String, Object> result = new HashMap<>();

        if (value == 0) {
            result.put("value", 0.0);
            result.put("unit", "");
            return result;
        }

        double absValue = Math.abs(value);
        double convertedValue;
        String unit;

        if (absValue < 10000) {
            // 小于1万，不添加单位
            convertedValue = absValue;
            unit = "";
        } else if (absValue < 100000000) {
            // 万级别 (10,000-99,999,999)
            convertedValue = absValue / 10000;
            unit = "万";
        } else {
            // 亿级别 (100,000,000以上)
            convertedValue = absValue / 100000000;
            unit = "亿";
        }

        // 设置符号
        if (value < 0) {
            convertedValue = -convertedValue;
        }

        result.put("value", formatDouble(convertedValue));
        result.put("unit", unit);
        return result;
    }

    public static String formatWithUnit(double value, String unit) {
        if (value == 0) {
            return "0" + unit;
        }

        double absValue = Math.abs(value);
        double convertedValue;

        if (absValue < 10000) {
            // 小于1万，不添加单位
            convertedValue = absValue;

        } else if (absValue < 100000000) {
            // 万级别 (10,000-99,999,999)
            convertedValue = absValue / 10000;
            unit = "万" + unit;
        } else if (absValue < 10000000000000.0) {
            // 亿级别 (100,000,000-999,999,999,999)
            convertedValue = absValue / 100000000;
            unit = "亿" + unit;
        } else {
            // 兆级别
            convertedValue = value / 10000000000000.0;
            unit = "兆" + unit;
        }
        return formatDouble(convertedValue) + unit;
    }

    /**
     * 对列表中的数值进行单位转换，并返回转换后的值列表和统一单位
     * 根据列表中的最大值确定统一单位（万或亿）
     *
     * @param values 原始数值列表（支持Number类型及其子类）
     * @return Map<String, Object> 包含 values（转换后的值列表）和 unit（统一单位）
     */
    public static Map<String, Object> formatListWithUnit(List<Object> values) {
        Map<String, Object> result = new HashMap<>();

        if (values == null || values.isEmpty()) {
            result.put("values", new ArrayList<>());
            result.put("unit", "");
            return result;
        }

        // 找出列表中的最大值
        double maxValue = 0.0;
        List<Double> doubleValues = new ArrayList<>();

        for (Object obj : values) {
            double value = 0.0;
            if (obj != null) {
                if (obj instanceof Number) {
                    value = ((Number) obj).doubleValue();
                } else {
                    try {
                        value = Double.parseDouble(obj.toString());
                    } catch (NumberFormatException e) {
                        // 忽略无法转换为数字的值
                        continue;
                    }
                }
            }
            doubleValues.add(value);
            maxValue = Math.max(maxValue, Math.abs(value));
        }

        if (doubleValues.isEmpty()) {
            result.put("values", new ArrayList<>());
            result.put("unit", "");
            return result;
        }

        // 根据最大值确定统一单位
        String unit;
        double divisor;

        if (maxValue < 10000) {
            // 小于1万，不添加单位
            unit = "";
            divisor = 1.0;
        } else if (maxValue < 100000000) {
            // 万级别 (10,000-99,999,999)
            unit = "万";
            divisor = 10000.0;
        } else {
            // 亿级别 (100,000,000以上)
            unit = "亿";
            divisor = 100000000.0;
        }

        // 转换所有值
        List<Double> convertedValues = new ArrayList<>();
        for (double value : doubleValues) {
            convertedValues.add(formatDouble(value / divisor));
        }

        result.put("values", convertedValues);
        result.put("unit", unit);
        return result;
    }

    /**
     * 将原始值转换为目标类型
     *
     * @param rawValue   原始值
     * @param targetType 目标类型
     * @return 转换后的值
     */
    public static Object convertValueType(Object rawValue, String targetType) {
        if (rawValue == null) {
            return null;
        }

        return switch (targetType.toLowerCase()) {
            case "string" -> String.valueOf(rawValue);
            case "integer", "int" -> Integer.valueOf(String.valueOf(rawValue));
            case "long" -> Long.valueOf(String.valueOf(rawValue));
            case "double" -> Double.valueOf(String.valueOf(rawValue));
            case "float" -> Float.valueOf(String.valueOf(rawValue));
            case "boolean" -> Boolean.valueOf(String.valueOf(rawValue));
            default -> rawValue;
        };
    }

    /**
     * 单位转换工具方法一：基于列表最小值的单位转换
     * 传入一个List<Double>数据列表，返回包含统一单位和格式化后数据的Map
     * 以列表中的最小值（绝对值）作为单位提取依据
     *
     * @param data 原始数值列表
     * @return Map 包含 util（单位）和 formatData（转换后的数据列表）
     */
    public static Map<String, Object> formatListWithMinUnit(List<Double> data) {
        Map<String, Object> result = new HashMap<>();

        if (data == null || data.isEmpty()) {
            result.put("unit", "");
            result.put("formatData", new ArrayList<>());
            return result;
        }

        // 找出列表中的最小非零绝对值
        double minValue = Double.MAX_VALUE;
        for (Double value : data) {
            if (value != null && value != 0) {
                double absValue = Math.abs(value);
                if (absValue < minValue) {
                    minValue = absValue;
                }
            }
        }

        // 如果所有值都是0或null
        if (minValue == Double.MAX_VALUE) {
            minValue = 0;
        }

        // 根据最小值确定统一单位
        String unit;
        double divisor;

        if (minValue < 10000) {
            // 小于1万，不添加单位
            unit = "";
            divisor = 1.0;
        } else if (minValue < 100000000) {
            // 万级别 (10,000-99,999,999)
            unit = "万";
            divisor = 10000.0;
        } else if (minValue < 10000000000000.0) {
            // 亿级别 (100,000,000-9,999,999,999,999)
            unit = "亿";
            divisor = 100000000.0;
        } else {
            // 兆级别 (10^13及以上)
            unit = "兆";
            divisor = 10000000000000.0;
        }

        // 转换所有值
        List<Double> formattedData = new ArrayList<>();
        for (Double value : data) {
            if (value == null) {
                formattedData.add(null);
            } else {
                formattedData.add(formatDouble(value / divisor));
            }
        }

        result.put("unit", unit);
        result.put("formatData", formattedData);
        return result;
    }

    /**
     * 单位转换工具方法二：单值单位转换
     * 传入一个Double值，返回包含单位和格式化后数据的Map
     *
     * @param value 原始数值
     * @return Map 包含 util（单位）和 formatData（转换后的数值）
     */
    public static Map<String, Object> formatSingleWithUnit(Double value) {
        Map<String, Object> result = new HashMap<>();

        if (value == null || value == 0) {
            result.put("unit", "");
            result.put("formatData", 0.0);
            return result;
        }

        double absValue = Math.abs(value);
        double formattedValue;
        String unit;

        if (absValue < 10000) {
            //
            formattedValue = value;
            unit = "";
        } else if (absValue < 100000000) {
            // 万级别 (10,000-99,999,999)
            formattedValue = value / 10000;
            unit = "万";
        } else if (absValue < 10000000000000.0) {
            // 亿级别 (100,000,000-9,999,999,999,999)
            formattedValue = value / 100000000;
            unit = "亿";
        } else {
            // 兆级别 (10^13及以上)
            formattedValue = value / 10000000000000.0;
            unit = "兆";
        }

        result.put("unit", unit);
        result.put("formatData", formatDouble(formattedValue));
        return result;
    }

}
