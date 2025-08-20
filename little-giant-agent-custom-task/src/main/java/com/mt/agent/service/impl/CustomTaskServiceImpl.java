package com.mt.agent.service.impl;

import com.mt.agent.model.workflow.JavaExecutable;
import com.mt.agent.reporter.StepResultData;
import com.mt.agent.reporter.SubEventReporter;
import com.mt.agent.service.CustomTaskService;
import com.mt.agent.sysUtil.ComputingUtil;
import com.mt.agent.sysUtil.DataUtil;
import com.mt.agent.sysUtil.FunctionUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 执行定制任务服务接口实现类
 *
 * @author lfz
 * @date 2025/4/23 16:33
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomTaskServiceImpl implements CustomTaskService {

    private final FunctionUtil functionUtil;
    private final PythonDirectExecutorService pythonDirectExecutorService;

    @Override
    public void executeJavaOrders(JavaExecutable javaExecutable, HashMap<String, Object> paramMap,
            SubEventReporter reporter) {

        // 返回前端执行步骤
        reporter.reportStep("开始执行步骤" + javaExecutable.getNum());

        // 执行函数
        executeStep(javaExecutable, paramMap, reporter);

        // 返回步骤执行完成
        reporter.reportStepResult(new StepResultData());
    }

    @Override
    public void executePythonCode(String pythonCode, HashMap<String, Object> paramMap, SubEventReporter reporter, String userId) {
        pythonCode = pythonCode.replace("```python", "").replace("```Python", "").replace("```", "");
        pythonDirectExecutorService.executePythonCode(pythonCode, paramMap, reporter, userId);
    }

    private void executeStep(JavaExecutable javaExecutable, HashMap<String, Object> paramMap,
            SubEventReporter reporter) {
        List<String> inputs = javaExecutable.getInputs();
        String javaExecutableType = javaExecutable.getJavaExecutableType();
        switch (javaExecutableType) {
            case "function": {
                switch (javaExecutable.getFunctionName()) {
                    case "sum": {
                        double sum = ComputingUtil.sum(getObjectFromMap(paramMap, inputs.get(0), List.class));
                        saveResult(javaExecutable, sum, paramMap);
                        break;
                    }
                    case "avg": {
                        double avg = ComputingUtil.avg(getObjectFromMap(paramMap, inputs.get(0), List.class));
                        saveResult(javaExecutable, avg, paramMap);
                        break;
                    }
                    case "change_rate": {
                        double changeRate = ComputingUtil.changeRate(
                                getObjectFromMap(paramMap, inputs.get(0), Double.class),
                                getObjectFromMap(paramMap, inputs.get(1), Double.class));
                        saveResult(javaExecutable, changeRate, paramMap);
                        break;
                    }
                    case "percentage": {
                        double percentage = ComputingUtil.percentage(
                                getObjectFromMap(paramMap, inputs.get(0), Double.class),
                                getObjectFromMap(paramMap, inputs.get(1), Double.class));
                        saveResult(javaExecutable, percentage, paramMap);
                        break;
                    }
                    case "ext_str_list_from_dataList": {
                        List list = DataUtil.extractStringListFromDataList(
                                getObjectFromMap(paramMap, inputs.get(0), List.class),
                                getObjectFromMap(paramMap, inputs.get(1), String.class));
                        saveResult(javaExecutable, list, paramMap);
                        break;
                    }
                    case "ext_double_list_from_dataList": {
                        List list = DataUtil.extractDoubleListFromDataList(
                                getObjectFromMap(paramMap, inputs.get(0), List.class),
                                getObjectFromMap(paramMap, inputs.get(1), String.class));
                        saveResult(javaExecutable, list, paramMap);
                        break;
                    }
                    case "create_new_array": {
                        List<Object> list = new ArrayList<>();
                        for (String input : inputs) {
                            list.add(getObjectFromMap(paramMap, input, Object.class));
                        }
                        saveResult(javaExecutable, list, paramMap);
                        break;
                    }
                    case "explain_system_function": {
                        String answer = functionUtil.sysQueryAnswer(inputs.get(0));
                        saveResult(javaExecutable, answer, paramMap);
                        break;
                    }
                    case "sql_exc": {
                        String key = inputs.get(0);
                        List<Map<String, Object>> maps = functionUtil
                                .executeSQL(getObjectFromMap(paramMap, key, String.class));
                        saveResult(javaExecutable, maps, paramMap);
                        break;
                    }
                    case "vis_textbox": {
                        functionUtil.visTextBox(
                                getObjectFromMap(paramMap, inputs.get(0), String.class),
                                reporter);
                        break;
                    }
                    case "vis_textblock": {
                        functionUtil.visTextBlock(
                                getObjectFromMap(paramMap, inputs.get(0), String.class),
                                getObjectFromMap(paramMap, inputs.get(1), Double.class),
                                reporter);
                        break;
                    }
                    case "vis_single_bar": {
                        functionUtil.visSingleBar(
                                getObjectFromMap(paramMap, inputs.get(0), String.class),
                                getObjectFromMap(paramMap, inputs.get(1), List.class),
                                getObjectFromMap(paramMap, inputs.get(2), List.class),
                                reporter);
                        break;
                    }
                    case "vis_clustered_bar": {
                        functionUtil.visClusteredBar(
                                getObjectFromMap(paramMap, inputs.get(0), String.class),
                                getObjectFromMap(paramMap, inputs.get(1), List.class),
                                getObjectFromMap(paramMap, inputs.get(2), String.class),
                                getObjectFromMap(paramMap, inputs.get(3), String.class),
                                getObjectFromMap(paramMap, inputs.get(4), List.class),
                                getObjectFromMap(paramMap, inputs.get(5), List.class),
                                reporter);
                        break;
                    }
                    case "vis_pie_chart": {
                        functionUtil.visPieChart(
                                getObjectFromMap(paramMap, inputs.get(0), String.class),
                                getObjectFromMap(paramMap, inputs.get(1), List.class),
                                getObjectFromMap(paramMap, inputs.get(2), List.class),
                                reporter);
                        break;
                    }
                    case "vis_table": {
                        functionUtil.visTable(
                                getObjectFromMap(paramMap, inputs.get(0), String.class),
                                getObjectFromMap(paramMap, inputs.get(1), List.class),
                                reporter);
                        break;
                    }
                    case "arithmetic_expression": {
                        Object result = ArithmeticUtil.evaluateArithmeticExpression(inputs.get(0), paramMap);
                        saveResult(javaExecutable, result, paramMap);
                        break;
                    }
                    case "create_list": {
                        List<Object> list = new ArrayList<>();
                        for (String input : inputs) {
                            Object value = getObjectFromMap(paramMap, input, Object.class);
                            if (value == null) {
                                // 如果从paramMap中获取不到，尝试解析为字面值
                                value = parseLiteralValue(input);
                            }
                            list.add(value);
                        }
                        saveResult(javaExecutable, list, paramMap);
                        break;
                    }
                    case "create_dict": {
                        Map<String, Object> dict = new HashMap<>();
                        // inputs应该是成对出现的：key1, value1, key2, value2, ...
                        for (int i = 0; i < inputs.size() - 1; i += 2) {
                            String key = inputs.get(i);
                            String valueStr = inputs.get(i + 1);

                            // 解析key
                            Object keyObj = getObjectFromMap(paramMap, key, Object.class);
                            if (keyObj == null) {
                                keyObj = parseLiteralValue(key);
                            }

                            // 解析value
                            Object valueObj = getObjectFromMap(paramMap, valueStr, Object.class);
                            if (valueObj == null) {
                                valueObj = parseLiteralValue(valueStr);
                            }

                            dict.put(String.valueOf(keyObj), valueObj);
                        }
                        saveResult(javaExecutable, dict, paramMap);
                        break;
                    }
                    default:
                        throw new IllegalArgumentException("未知的函数：" + javaExecutable.getFunctionName());
                }
                break;
            }
            case "data": {
                saveResult(javaExecutable, null, paramMap);
                break;
            }
            case "default": {
                saveResult(javaExecutable, null, paramMap);
                break;
            }
            default:
                throw new IllegalArgumentException("未知的JavaExecutable类型：" + javaExecutableType);
        }
    }

    /**
     * 从Map中获取指定类型的对象
     *
     * @param paramMap 参数映射
     * @param key      参数键
     * @param clazz    期望的类型
     * @param <T>      泛型类型
     * @return 转换后的对象
     * @throws IllegalArgumentException 当类型不匹配时抛出异常
     */
    public static <T> T getObjectFromMap(Map<String, Object> paramMap, String key, Class<T> clazz)
            throws IllegalArgumentException {
        // 第一步：从Map中获取原始对象
        Object obj = paramMap.get(key);

        if (obj == null) {
            // 第二步：处理null值的特殊情况

            // 检查是否是List类型
            if (List.class.isAssignableFrom(clazz)) {
                // 检查key是否符合 ["param1","param2",...] 这种格式
                if (isListFormat(key)) {
                    // 解析字符串为List
                    List<String> parsedList = parseStringToList(key);
                    return (T) parsedList;
                }
            }
            // 检查key是否符合 varName[-1]
            Pattern pattern = Pattern.compile("(\\w+)\\[(-?\\d+)\\]");
            Matcher matcher = pattern.matcher(key);
            if (matcher.find()) {
                // 提取匹配到的各个部分
                String varName = matcher.group(1); // 变量名
                String index = matcher.group(2); // 下标

                Object o = paramMap.get(varName);
                if (o instanceof List) {
                    List<Object> list = (List<Object>) o;
                    if (index != null && !index.isEmpty()) {
                        int idx = Integer.parseInt(index);
                        if (idx >= 0 && idx < list.size()) {
                            return (T) list.get(idx);
                        } else {
                            return (T) list.get(list.size() + idx);
                        }
                    }
                }
            }
            return (T) key.replaceAll("\"", "");
        }
        // 第四步：返回类型匹配的对象
        return (T) obj;
    }

    /**
     * 检查字符串是否符合List格式 ["param1","param2",...]
     *
     * @param str 要检查的字符串
     * @return 是否符合List格式
     */
    private static boolean isListFormat(String str) {
        if (str == null || str.trim().isEmpty()) {
            return false;
        }

        String trimmed = str.trim();
        return trimmed.startsWith("[") && trimmed.endsWith("]") && trimmed.length() > 2;
    }

    /**
     * 将符合格式的字符串解析为List
     * 格式：["param1","param2",...] 或 ['param1','param2',...]
     *
     * @param str 要解析的字符串
     * @return 解析后的List
     */
    private static List<String> parseStringToList(String str) {
        List<String> result = new ArrayList<>();

        if (!isListFormat(str)) {
            return result;
        }

        try {
            // 去除首尾的方括号
            String content = str.trim().substring(1, str.trim().length() - 1).trim();

            if (content.isEmpty()) {
                return result;
            }

            // 使用正则表达式匹配引号内的内容
            Pattern pattern = Pattern.compile("\"([^\"]*)\"|'([^']*)'");
            Matcher matcher = pattern.matcher(content);

            while (matcher.find()) {
                String value = matcher.group(1); // 双引号内容
                if (value == null) {
                    value = matcher.group(2); // 单引号内容
                }
                if (value != null) {
                    result.add(value);
                }
            }

            // 如果没有匹配到引号，尝试按逗号分割（处理没有引号的情况）
            if (result.isEmpty()) {
                String[] parts = content.split(",");
                for (String part : parts) {
                    String trimmed = part.trim();
                    if (!trimmed.isEmpty()) {
                        result.add(trimmed);
                    }
                }
            }

        } catch (Exception e) {
            log.warn("解析字符串为List时发生异常: {}, 字符串: {}", e.getMessage(), str);
        }

        return result;
    }

    // 保存结果
    private static void saveResult(JavaExecutable javaExecutable, Object result, Map<String, Object> results) {
        // 如果是函数执行，保存结果
        if (javaExecutable.getJavaExecutableType()
                .equals(JavaExecutable.JavaExecutableType.FUNCTIOM_EXCUTION.getType())) {
            if (javaExecutable.getResultDataIndex() == null) {
                results.put(javaExecutable.getOutputName(), result);
            } else {
                List<?> list = (List<?>) result;
                if (list != null && !list.isEmpty()) {
                    if (list.get(0) instanceof Double) {
                        if (javaExecutable.getResultDataIndex() >= 0) {
                            results.put(javaExecutable.getOutputName(),
                                    (Double) list.get(javaExecutable.getResultDataIndex()));
                        } else {
                            results.put(javaExecutable.getOutputName(),
                                    (Double) list.get(list.size() + javaExecutable.getResultDataIndex()));
                        }
                    } else if (list.get(0) instanceof String) {
                        if (javaExecutable.getResultDataIndex() >= 0) {
                            results.put(javaExecutable.getOutputName(),
                                    (String) list.get(javaExecutable.getResultDataIndex()));
                        } else {
                            results.put(javaExecutable.getOutputName(),
                                    (String) list.get(list.size() + javaExecutable.getResultDataIndex()));
                        }
                    }
                }
            }
        }
        // 如果是数据提取，保存结果
        else if (javaExecutable.getJavaExecutableType()
                .equals(JavaExecutable.JavaExecutableType.DATA_EXTRACTION.getType())) {
            String arrayName = javaExecutable.getFunctionName();
            if (javaExecutable.getResultDataIndex() != null) {
                List<?> list = (List<?>) results.get(arrayName);
                int size = list.size();
                Object o = null;
                if (javaExecutable.getResultDataIndex() < 0) {
                    o = list.get(size + javaExecutable.getResultDataIndex());
                } else {
                    o = list.get(javaExecutable.getResultDataIndex());
                }
                if (o instanceof Double) {
                    results.put(javaExecutable.getOutputName(), Double.parseDouble(o.toString()));
                } else if (o instanceof Integer) {
                    results.put(javaExecutable.getOutputName(), Integer.valueOf(o.toString()));
                } else {
                    results.put(javaExecutable.getOutputName(), o.toString());
                }
            }
        } // 默认值
        else if (javaExecutable.getJavaExecutableType()
                .equals(JavaExecutable.JavaExecutableType.DEFAULT_VALUE.getType())) {
            // todo 可能需要类型转化
            results.put(javaExecutable.getOutputName(), javaExecutable.getFunctionName());
        }
    }

    /**
     * 解析字面值（数字、字符串等）
     */
    private Object parseLiteralValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        value = value.trim();

        // 字符串类型
        if ((value.startsWith("\"") && value.endsWith("\"")) ||
                (value.startsWith("'") && value.endsWith("'"))) {
            return value.substring(1, value.length() - 1);
        }

        // 布尔类型
        if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
            return Boolean.parseBoolean(value);
        }

        // 数字类型
        try {
            if (value.contains(".")) {
                return Double.parseDouble(value);
            } else {
                return Long.parseLong(value);
            }
        } catch (NumberFormatException e) {
            // 如果不是数字，返回原字符串
            return value;
        }
    }

    /**
     * 算术表达式计算工具类
     */
    public static class ArithmeticUtil {

        /**
         * 计算算术表达式
         */
        public static Object evaluateArithmeticExpression(String expression, Map<String, Object> paramMap) {
            try {
                // 替换表达式中的变量
                String processedExpression = replaceVariables(expression, paramMap);

                // 评估表达式
                return evaluateExpression(processedExpression);
            } catch (Exception e) {
                log.error("计算算术表达式失败: {}", expression, e);
                throw new RuntimeException("计算算术表达式失败: " + expression, e);
            }
        }

        /**
         * 替换表达式中的变量和函数调用
         */
        private static String replaceVariables(String expression, Map<String, Object> paramMap) {
            String result = expression;

            // 处理函数调用 func(param)[index]
            Pattern funcPattern = Pattern.compile("(\\w+)\\(([^)]+)\\)\\[(-?\\d+)\\]");
            Matcher funcMatcher = funcPattern.matcher(result);
            while (funcMatcher.find()) {
                String funcName = funcMatcher.group(1);
                String params = funcMatcher.group(2);
                int index = Integer.parseInt(funcMatcher.group(3));

                // 这里应该调用相应的函数，简化处理直接从paramMap获取
                String key = funcName + "(" + params + ")[" + index + "]";
                Object value = paramMap.get(key);
                if (value != null) {
                    result = result.replace(funcMatcher.group(0), String.valueOf(value));
                }
            }

            // 处理简单变量
            for (Map.Entry<String, Object> entry : paramMap.entrySet()) {
                String varName = entry.getKey();
                Object varValue = entry.getValue();

                if (varValue instanceof Number) {
                    result = result.replaceAll("\\b" + Pattern.quote(varName) + "\\b",
                            String.valueOf(varValue));
                }
            }

            return result;
        }

        /**
         * 简单的表达式计算器
         */
        private static Object evaluateExpression(String expression) {
            // 移除空格
            expression = expression.replaceAll("\\s+", "");

            try {
                // 简单的加减乘除计算
                if (expression.contains("+")) {
                    String[] parts = expression.split("\\+");
                    double result = 0;
                    for (String part : parts) {
                        result += Double.parseDouble(part.trim());
                    }
                    return result;
                } else if (expression.contains("-") && !expression.startsWith("-")) {
                    String[] parts = expression.split("-");
                    double result = Double.parseDouble(parts[0].trim());
                    for (int i = 1; i < parts.length; i++) {
                        result -= Double.parseDouble(parts[i].trim());
                    }
                    return result;
                } else if (expression.contains("*")) {
                    String[] parts = expression.split("\\*");
                    double result = 1;
                    for (String part : parts) {
                        result *= Double.parseDouble(part.trim());
                    }
                    return result;
                } else if (expression.contains("/")) {
                    String[] parts = expression.split("/");
                    double result = Double.parseDouble(parts[0].trim());
                    for (int i = 1; i < parts.length; i++) {
                        result /= Double.parseDouble(parts[i].trim());
                    }
                    return result;
                } else {
                    // 单个数字
                    return Double.parseDouble(expression);
                }
            } catch (NumberFormatException e) {
                log.warn("无法解析表达式: {}", expression);
                return expression; // 返回原字符串
            }
        }
    }
}
