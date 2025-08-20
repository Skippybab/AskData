package com.mt.agent.utils;

import java.util.ArrayList;
import java.util.List;

/**
 * 字符串格式化工具类
 *
 * @author lfz
 * @date 2025/5/12 17:13
 */
public class StringFormatUtil {

    /**
     * 清理字符串，移除方括号和引号，样例：["xxx"]，移除后保留xxx
     * 
     * @param input 输入字符串
     * @return 清理后的字符串
     */
    public static String cleanBracketsAndQuotes(String input) {
        if (input == null) {
            return null;
        }
        // 移除开头和结尾的[]
        String result = input.replaceAll("^\\[|\\]$", "");
        // 移除引号
        result = result.replaceAll("\"", "");
        return result;
    }



    /**
     *
     * 该方法能够正确处理双引号内包含逗号的情况，如 ["param1", "param2, param3"] 会被解析为两个参数：
     * "param1" 和 "param2, param3"，而不会错误地将 "param2, param3" 分割为两个参数。
     * 
     *
     * 处理流程：
     * 1. 移除外层的方括号 []
     * 2. 使用状态机解析参数，处理引号内的逗号，确保双引号内的内容被视为一个整体
     * 3. 对每个参数进行trim处理，移除前后空白
     * 4. 移除参数前后的引号，保留参数内容
     * 5. 处理null值，将字符串"null"转换为null对象
     * 
     * 示例：
     * 输入: ["2023", "通用设备制造业", null, "广州", null, "SUM(营收), AVG(营收)"]
     * 输出: ["2023", "通用设备制造业", null, "广州", null, "SUM(营收), AVG(营收)"]
     * 
     * @param paramListStr 参数列表字符串，如 ["param1", "param2", ...]
     * @return 解析后的参数列表，如 ["param1", "param2", ...]
     */
    public static List<String> parseParameterList(String paramListStr) {
        List<String> params = new ArrayList<>();

        if (paramListStr == null || paramListStr.trim().isEmpty()) {
            return params;
        }

        // 移除首尾的 [ 和 ]
        String cleanedStr = paramListStr.trim();
        if (cleanedStr.startsWith("[")) {
            cleanedStr = cleanedStr.substring(1);
        }
        if (cleanedStr.endsWith("]")) {
            cleanedStr = cleanedStr.substring(0, cleanedStr.length() - 1);
        }

        // 如果清理后的字符串为空，返回空列表
        if (cleanedStr.isEmpty()) {
            return params;
        }

        // 使用状态机解析参数，处理引号内的逗号
        StringBuilder currentParam = new StringBuilder();
        boolean inQuotes = false;

        for (char c : cleanedStr.toCharArray()) {
            if (c == '"') {
                inQuotes = !inQuotes; // 切换引号状态
                // 不添加引号到结果中
            } else if ((c == ',' || c == '，') && !inQuotes) {
                // 只在不在引号内时分割，同时支持中英文逗号
                String param = currentParam.toString().trim();
                // 移除参数前后的引号
                if (param.startsWith("\"") && param.endsWith("\"")) {
                    param = param.substring(1, param.length() - 1);
                }
                    params.add(param);
                currentParam = new StringBuilder();
            } else {
                currentParam.append(c);
            }
        }

        // 添加最后一个参数
        if (currentParam.length() > 0) {
            String param = currentParam.toString().trim();
            // 移除参数前后的引号
            if (param.startsWith("\"") && param.endsWith("\"")) {
                param = param.substring(1, param.length() - 1);
            }
//            if (!param.isEmpty() && !param.equals("null")) {
                params.add(param);
//            } else if (param.equals("null")) {
//                params.add(null); // 保留null值
//            }
        }

        return params;
    }

    /**
     * 将参数列表列表转为字符串
     * 
     * @param params 参数列表
     **/
    public static String parseListToString(List<String> params) {

        StringBuilder sb = new StringBuilder();
        for (String param : params) {
            sb.append(param).append(",");
        }
        return sb.toString().substring(0, sb.length() - 1);
    }


    /**
     * 去除字符串的引号
     *
     * @param s                     需要处理的字符串
     * @param wasElementOfOuterList 是否为外部列表的元素
     * @return 去除引号后的字符串
     */
    private static String unquoteString(String s, boolean wasElementOfOuterList) {
        if (s == null)
            return null;
        String trimmed = s.trim();

        // 首先处理外部引号（如果是外部列表元素）
        if (wasElementOfOuterList) {
            // 如果它是外部列表的元素，如 ["elem1", "elem2"]，
            // 那么元素应该是字符串字面量，如 "foo" 或 "[bar,baz]"
            // 在调用此方法前，s 应该是 "\"foo\"" 或 "\"\[bar,baz]\""
            if ((trimmed.startsWith("\"") && trimmed.endsWith("\"")) ||
                    (trimmed.startsWith("'") && trimmed.endsWith("'"))) {
                if (trimmed.length() >= 2) {
                    trimmed = trimmed.substring(1, trimmed.length() - 1);
                } else {
                    return ""; // 处理 "" 或 '' 的情况
                }
            }
        }

        // 然后处理内部转义字符
        return trimmed
                // 处理常见的转义序列
                .replace("\\\"", "\"") // 双引号
                .replace("\\'", "'") // 单引号
                .replace("\\\\", "\\") // 反斜杠
                .replace("\\n", "\n") // 换行符
                .replace("\\r", "\r") // 回车符
                .replace("\\t", "\t") // 制表符
                .replace("\\b", "\b") // 退格符
                .replace("\\f", "\f"); // 换页符
    }

    /**
     * 分割字符串为列表，忽略嵌套括号内的逗号
     *
     * 核心处理逻辑：
     * 1. 判断输入是否为列表格式（以 [ 开始，以 ] 结束）
     * 2. 使用状态追踪（引号内部、括号层级）进行字符遍历
     * 3. 只有在非引号内部且括号层级为0时，才将逗号视为分隔符
     * 4. 使用 unquoteString 方法处理每个分割后的元素，去除引号
     *
     * 示例：
     * 输入："[\"xxx\",\"[xxx,xxx]\"]"
     * 输出：[xxx, [xxx,xxx]]
     *
     * @param input 需要分割的字符串
     * @return 分割后的字符串列表
     */
    public static List<String> splitIgnoringBrackets(String input) {
        List<String> result = new ArrayList<>();
        if (input == null)
            return result;

        String trimmedInput = input.trim();
        if (trimmedInput.isEmpty())
            return result;

        String contentToParse;
        boolean wasOuterList = false;

        // 启发式检查整个输入是否是类似 "[.....]" 的列表表示，
        // 以决定是否解析其内容
        if (trimmedInput.startsWith("[") && trimmedInput.endsWith("]")) {
            // 假设如果以 '[' 开始并以 ']' 结束，
            // 这些是要解析的元素列表的分隔符
            contentToParse = trimmedInput.substring(1, trimmedInput.length() - 1);
            wasOuterList = true;
        } else {
            contentToParse = trimmedInput;
        }

        StringBuilder current = new StringBuilder();
        int bracketLevel = 0; // 用于跟踪未引用的 contentToParse 段中的括号
        boolean inQuotedString = false; // 用于标识定义字符串字面量段的引号
        char currentQuoteChar = 0; // 存储开始当前引用字符串的字符（例如 " 或 '）
        boolean escapeNext = false;

        for (char c : contentToParse.toCharArray()) {
            if (escapeNext) {
                current.append(c);
                escapeNext = false;
                continue;
            }

            if (c == '\\') {
                escapeNext = true;
                current.append(c); // 暂时保留转义字符
                continue;
            }

            if (inQuotedString) {
                if (c == currentQuoteChar) {
                    inQuotedString = false; // 引用字符串结束
                    // currentQuoteChar = 0; // 在这里重置并非严格必要
                }
                // current.append(c); // 如果在引用字符串中，总是附加 - 移至通用附加处理
            } else { // 当前不在引用字符串内
                if (c == '"' || c == '\'') {
                    inQuotedString = true;
                    currentQuoteChar = c;
                } else if (c == '[') {
                    bracketLevel++;
                } else if (c == ']') {
                    bracketLevel = Math.max(0, bracketLevel - 1); // 避免负层级
                }
            }

            // 分割逻辑：
            // 逗号在不在多字符引用字符串内部，且在基本括号层级（对于未引用的括号结构）时分割
            if ((c == ',' || c == '，') && bracketLevel == 0 && !inQuotedString) {
                result.add(unquoteString(current.toString(), wasOuterList));
                current.setLength(0); // 重置以准备下一个元素
            } else {
                current.append(c); // 将字符附加到当前元素字符串
            }
        }

        // 处理循环中有内容时直接添加
        // 如果 contentToParse 为空（例如来自 "[]"），循环不运行，current 为空
        if (current.length() > 0) {
            result.add(unquoteString(current.toString(), wasOuterList));
        } else if (wasOuterList && contentToParse.trim().isEmpty() && result.isEmpty()) {
            // 这个特定条件处理像 "[]" 或 "[ ]" 这样的空列表的边缘情况
            // 如果 contentToParse 为空（源自 "[]" 或 "[ ]"）且没有解析到元素，
            // 结果列表应该为空，而不是包含一个空字符串
            // 但是，如果输入是像 "[""]"（包含一个空字符串的列表），
            // contentToParse 将是 """，current 会变成 """，然后它会被添加并去引号为 ""
            // 所以，如果主逻辑为 "[]" 生成空列表，这个特殊分支可能不是严格必要的
            // 让我们测试：输入 "[]" -> contentToParse=""，循环不运行，current 为空。结果为空。正确。
            // 所以这里的 `else if` 分支可能是多余的
        }

        return result;
    }

    /**
     * 文本列表转换行输出的纯文本
     *
     * @author lfz
     * @date 2025/5/22 14:36
     * @param list 待转换的文本列表
     * @return 可换行输出的纯文本列表
     */
    public static String ListToString(List<String> list) {
        StringBuilder stringBuilder = new StringBuilder();
        for (String s : list) {
            stringBuilder.append(s).append("\n");
        }
        return stringBuilder.toString();
    }
}
