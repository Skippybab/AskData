package com.mt.agent.router.util;

import com.mt.agent.model.workflow.JavaExecutable;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Python代码解析工具类
 * 
 * <p>
 * 采用策略模式设计，通过解析器链处理不同类型的Python语句
 * </p>
 * 
 * <h3>支持的Python语法：</h3>
 * <ul>
 * <li>函数调用：result = getData("param")</li>
 * <li>带索引函数调用：result = getData("param")[-1]</li>
 * <li>直接函数调用：processData("param")</li>
 * <li>数组访问：latest = data[-1]</li>
 * <li>变量赋值：name = "value"</li>
 * <li>四则运算：result = a + b</li>
 * <li>列表创建：mylist = [1, 2, 3]</li>
 * <li>字典创建：mydict = {"key": "value"}</li>
 * </ul>
 * 
 * @author lfz
 * @date 2025/1/27
 */
@Slf4j
public class PythonCodeParserUtil {

    // ======================== 常量定义 ========================

    /**
     * 正则表达式常量
     */
    private static class RegexPatterns {
        /** 函数调用带索引：result = getData("param")[-1] */
        static final Pattern FUNCTION_WITH_INDEX = Pattern.compile("(\\w+)\\s*=\\s*(\\w+)\\(([^)]+)\\)\\[(-?\\d+)\\]");

        /** 基本函数调用：result = getData("param") */
        static final Pattern FUNCTION_CALL = Pattern.compile("(\\w+)\\s*=\\s*(\\w+)\\(([^)]+)\\)");

        /** 直接函数调用：processData("param") */
        static final Pattern DIRECT_FUNCTION_CALL = Pattern.compile("^\\s*(\\w+)\\(([^)]+)\\)\\s*$");

        /** 数组访问：latest = data[-1] */
        static final Pattern ARRAY_ACCESS = Pattern.compile("(\\w+)\\s*=\\s*(\\w+)\\[(-?\\d+)\\]");
    }

    /**
     * 代码块标记常量
     */
    private static class CodeBlockMarkers {
        static final String PYTHON_START = "```python";
        static final String CODE_END = "```";
    }

    // ======================== 公共接口 ========================

    /**
     * 解析Python代码为Java执行指令列表
     * 
     * @param pythonCode Python代码字符串
     * @return Java执行指令列表
     * @throws IllegalArgumentException 当代码无法解析时抛出
     */
    public static List<JavaExecutable> parseJavaOrders(String pythonCode) {
        if (pythonCode == null || pythonCode.trim().isEmpty()) {
            return new ArrayList<>();
        }

        log.info("开始解析Python代码，总长度: {} 字符", pythonCode.length());

        // 1. 清理代码格式
        String cleanedCode = CodeCleaner.cleanPythonCode(pythonCode);

        // 2. 按行解析
        String[] lines = cleanedCode.split("\n");
        List<JavaExecutable> executables = new ArrayList<>();
        int stepNum = 0;

        for (String line : lines) {
            String trimmedLine = line.trim();

            // 跳过空行和注释
            if (LineFilter.shouldSkipLine(trimmedLine)) {
                continue;
            }

            try {
                JavaExecutable executable = ParsersChain.parse(trimmedLine, ++stepNum);
                if (executable != null) {
                    executables.add(executable);
                    log.info("✓ 步骤{}: {} -> {}", stepNum, trimmedLine, executable.getFunctionName());
                } else {
                    log.warn("✗ 无法解析的代码行: {}", trimmedLine);
                }
            } catch (Exception e) {
                log.error("✗ 解析失败: {}, 错误: {}", trimmedLine, e.getMessage());
                throw new IllegalArgumentException("无法解析Python指令：" + trimmedLine, e);
            }
        }

        log.info("Python代码解析完成，共生成 {} 个执行指令", executables.size());
        return executables;
    }

    /**
     * 解析复杂参数字符串
     * 
     * @param params 参数字符串，如：getData("param"), [1,2,3], {"key": "value"}
     * @return 参数列表
     */
    public static List<String> parseComplexParams(String params) {
        if (params == null || params.trim().isEmpty()) {
            return new ArrayList<>();
        }

        return ParameterTokenizer.tokenize(params);
    }

    /**
     * 格式化打印解析结果
     * 
     * @param executables 解析结果列表
     */
    public static void printParseResults(List<JavaExecutable> executables) {
        if (executables == null || executables.isEmpty()) {
            log.info("解析结果为空");
            return;
        }

        log.info("=== Python代码解析结果 ===");
        for (JavaExecutable executable : executables) {
            printSingleResult(executable);
        }
        log.info("=== 解析完成，共{}个步骤 ===", executables.size());
    }

    /**
     * 格式化打印单个解析结果
     * 
     * @param executable 单个执行指令
     */
    public static void printSingleResult(JavaExecutable executable) {
        if (executable == null) {
            log.info("执行指令为null");
            return;
        }

        log.info("步骤：{}", executable.getNum() != null ? executable.getNum() : "null");
        log.info("调用函数：{}", executable.getFunctionName() != null ? executable.getFunctionName() : "null");

        // 格式化输入参数
        if (executable.getInputs() != null && !executable.getInputs().isEmpty()) {
            log.info("输入参数：[{}]", String.join(", ", executable.getInputs()));
        } else {
            log.info("输入参数：null");
        }

        log.info("输出变量：{}", executable.getOutputName() != null ? executable.getOutputName() : "null");
        log.info("数据下标：{}", executable.getResultDataIndex() != null ? executable.getResultDataIndex() : "null");
        log.info("执行类型：{}", executable.getJavaExecutableType() != null ? executable.getJavaExecutableType() : "null");
        log.info("---");
    }

    // ======================== 内部工具类 ========================

    /**
     * 代码清理器
     * 负责移除Python代码块标记和格式化代码
     */
    private static class CodeCleaner {

        /**
         * 清理Python代码格式
         */
        static String cleanPythonCode(String pythonCode) {
            if (pythonCode == null) {
                return "";
            }

            String cleaned = pythonCode;

            // 移除开始标记
            int startIndex = cleaned.indexOf(CodeBlockMarkers.PYTHON_START);
            if (startIndex != -1) {
                cleaned = cleaned.substring(startIndex + CodeBlockMarkers.PYTHON_START.length());
            }

            // 移除结束标记
            int endIndex = cleaned.indexOf(CodeBlockMarkers.CODE_END);
            if (endIndex != -1) {
                cleaned = cleaned.substring(0, endIndex);
            }

            return cleaned;
        }
    }

    /**
     * 行过滤器
     * 判断是否应该跳过某一行代码
     */
    private static class LineFilter {

        /**
         * 判断是否应该跳过该行
         */
        static boolean shouldSkipLine(String line) {
            return line.isEmpty() || line.startsWith("#");
        }
    }

    /**
     * 解析器链
     * 按优先级顺序尝试不同的解析器
     */
    private static class ParsersChain {

        // 解析器列表，按优先级排序
        private static final List<StatementParser> PARSERS = Arrays.asList(
                new ArithmeticExpressionParser(),
                new FunctionWithIndexParser(),
                new FunctionCallParser(),
                new DirectFunctionCallParser(),
                new ArrayAccessParser(),
                new AssignmentParser());

        /**
         * 使用解析器链解析单行代码
         */
        static JavaExecutable parse(String line, int stepNum) {
            for (StatementParser parser : PARSERS) {
                if (parser.canParse(line)) {
                    log.debug("使用 {} 解析: {}", parser.getParserName(), line);
                    return parser.parse(line, stepNum);
                }
            }
            return null;
        }
    }

    // ======================== 解析器接口和实现 ========================

    /**
     * 语句解析器接口
     * 定义了解析器的基本契约
     */
    private interface StatementParser {

        /**
         * 判断是否能够解析该语句
         */
        boolean canParse(String line);

        /**
         * 解析语句为Java执行指令
         */
        JavaExecutable parse(String line, int stepNum);

        /**
         * 获取解析器名称（用于日志）
         */
        default String getParserName() {
            return this.getClass().getSimpleName();
        }
    }

    /**
     * 四则运算表达式解析器
     * 处理：result = a + b, result = func(x) + func(y)
     */
    private static class ArithmeticExpressionParser implements StatementParser {

        @Override
        public boolean canParse(String line) {
            return line.contains("=") && ArithmeticDetector.containsArithmeticOperators(
                    line.substring(line.indexOf("=") + 1).trim());
        }

        @Override
        public JavaExecutable parse(String line, int stepNum) {
            String[] parts = line.split("=", 2);
            String varName = parts[0].trim();
            String expression = parts[1].trim();

            JavaExecutable executable = ExecutableBuilder.createFunctionExecutable()
                    .withStepNum(stepNum)
                    .withFunctionName("arithmetic_expression")
                    .withOutputName(varName)
                    .withInputs(List.of(expression))
                    .build();

            log.debug("四则运算: {} = {}", varName, expression);
            return executable;
        }
    }

    /**
     * 带索引的函数调用解析器
     * 处理：result = getData("param")[-1]
     */
    private static class FunctionWithIndexParser implements StatementParser {

        @Override
        public boolean canParse(String line) {
            // 改进检测逻辑，避免使用有问题的正则表达式
            return FunctionCallDetector.isFunctionCallWithIndex(line);
        }

        @Override
        public JavaExecutable parse(String line, int stepNum) {
            // 使用改进的解析逻辑
            FunctionCallWithIndexInfo indexInfo = FunctionCallDetector.parseFunctionCallWithIndex(line);
            if (indexInfo != null) {
                JavaExecutable executable = ExecutableBuilder.createFunctionExecutable()
                        .withStepNum(stepNum)
                        .withFunctionName(indexInfo.functionName)
                        .withOutputName(indexInfo.varName)
                        .withInputs(ParameterTokenizer.tokenize(indexInfo.parameters))
                        .withIndex(indexInfo.index)
                        .build();

                log.debug("带索引函数调用: {} = {}({})[ {} ]", indexInfo.varName, indexInfo.functionName, indexInfo.parameters,
                        indexInfo.index);
                return executable;
            }
            return null;
        }
    }

    /**
     * 基本函数调用解析器
     * 处理：result = getData("param")
     */
    private static class FunctionCallParser implements StatementParser {

        @Override
        public boolean canParse(String line) {
            // 改进检测逻辑，避免使用有问题的正则表达式
            return FunctionCallDetector.isFunctionCallAssignment(line);
        }

        @Override
        public JavaExecutable parse(String line, int stepNum) {
            // 使用改进的解析逻辑，避免正则表达式的括号匹配问题
            FunctionCallAssignmentInfo assignmentInfo = FunctionCallDetector.parseFunctionCallAssignment(line);
            if (assignmentInfo != null) {
                JavaExecutable executable = ExecutableBuilder.createFunctionExecutable()
                        .withStepNum(stepNum)
                        .withFunctionName(assignmentInfo.functionName)
                        .withOutputName(assignmentInfo.varName)
                        .withInputs(ParameterTokenizer.tokenize(assignmentInfo.parameters))
                        .build();

                log.debug("函数调用: {} = {}({})", assignmentInfo.varName, assignmentInfo.functionName,
                        assignmentInfo.parameters);
                return executable;
            }
            return null;
        }
    }

    /**
     * 直接函数调用解析器
     * 处理：processData("param"), vis_textblock("text", func()[0])
     */
    private static class DirectFunctionCallParser implements StatementParser {

        @Override
        public boolean canParse(String line) {
            // 改进的检测逻辑：检查是否为函数调用且不包含赋值操作
            line = line.trim();
            if (line.contains("=")) {
                return false; // 包含赋值操作，不是直接函数调用
            }

            // 检查是否匹配函数调用模式：functionName(...)
            return FunctionCallDetector.isDirectFunctionCall(line);
        }

        @Override
        public JavaExecutable parse(String line, int stepNum) {
            line = line.trim();

            // 使用改进的解析逻辑
            FunctionCallInfo callInfo = FunctionCallDetector.parseDirectFunctionCall(line);
            if (callInfo != null) {
                JavaExecutable executable = ExecutableBuilder.createFunctionExecutable()
                        .withStepNum(stepNum)
                        .withFunctionName(callInfo.functionName)
                        .withInputs(ParameterTokenizer.tokenize(callInfo.parameters))
                        .build();

                log.debug("直接函数调用: {}({})", callInfo.functionName, callInfo.parameters);
                return executable;
            }
            return null;
        }
    }

    /**
     * 数组访问解析器
     * 处理：latest = data[-1]
     */
    private static class ArrayAccessParser implements StatementParser {

        @Override
        public boolean canParse(String line) {
            return RegexPatterns.ARRAY_ACCESS.matcher(line).find();
        }

        @Override
        public JavaExecutable parse(String line, int stepNum) {
            Matcher matcher = RegexPatterns.ARRAY_ACCESS.matcher(line);
            if (matcher.find()) {
                String varName = matcher.group(1);
                String arrayName = matcher.group(2);
                int index = Integer.parseInt(matcher.group(3));

                JavaExecutable executable = ExecutableBuilder.createDataExtractionExecutable()
                        .withStepNum(stepNum)
                        .withFunctionName(arrayName)
                        .withOutputName(varName)
                        .withIndex(index)
                        .build();

                log.debug("数组访问: {} = {}[{}]", varName, arrayName, index);
                return executable;
            }
            return null;
        }
    }

    /**
     * 赋值表达式解析器
     * 处理：name = "value", list = [1,2,3], dict = {"key": "value"}
     */
    private static class AssignmentParser implements StatementParser {

        @Override
        public boolean canParse(String line) {
            return line.contains("=");
        }

        @Override
        public JavaExecutable parse(String line, int stepNum) {
            String[] parts = line.split("=", 2);
            String varName = parts[0].trim();
            String value = parts[1].trim();

            ValueType valueType = ValueTypeDetector.detectType(value);

            switch (valueType) {
                case LIST:
                    return createListAssignment(varName, value, stepNum);
                case DICT:
                    return createDictAssignment(varName, value, stepNum);
                case STRING:
                    return createStringAssignment(varName, value, stepNum);
                default:
                    return createDefaultAssignment(varName, value, stepNum);
            }
        }

        private JavaExecutable createListAssignment(String varName, String value, int stepNum) {
            List<String> elements = ValueParser.parseListValue(value);

            JavaExecutable executable = ExecutableBuilder.createFunctionExecutable()
                    .withStepNum(stepNum)
                    .withFunctionName("create_list")
                    .withOutputName(varName)
                    .withInputs(elements)
                    .build();

            log.debug("列表赋值: {} = {}", varName, elements);
            return executable;
        }

        private JavaExecutable createDictAssignment(String varName, String value, int stepNum) {
            List<String> keyValuePairs = ValueParser.parseDictValue(value);

            JavaExecutable executable = ExecutableBuilder.createFunctionExecutable()
                    .withStepNum(stepNum)
                    .withFunctionName("create_dict")
                    .withOutputName(varName)
                    .withInputs(keyValuePairs)
                    .build();

            log.debug("字典赋值: {} = {}", varName, keyValuePairs);
            return executable;
        }

        private JavaExecutable createStringAssignment(String varName, String value, int stepNum) {
            String parsedValue = ValueParser.parseStringValue(value);

            JavaExecutable executable = ExecutableBuilder.createDefaultValueExecutable()
                    .withStepNum(stepNum)
                    .withFunctionName(parsedValue)
                    .withOutputName(varName)
                    .build();

            log.debug("字符串赋值: {} = {}", varName, parsedValue);
            return executable;
        }

        private JavaExecutable createDefaultAssignment(String varName, String value, int stepNum) {
            JavaExecutable executable = ExecutableBuilder.createDefaultValueExecutable()
                    .withStepNum(stepNum)
                    .withFunctionName(value)
                    .withOutputName(varName)
                    .build();

            log.debug("默认值赋值: {} = {}", varName, value);
            return executable;
        }
    }

    // ======================== 辅助工具类 ========================

    /**
     * 算术运算检测器
     */
    private static class ArithmeticDetector {

        /**
         * 检查字符串是否包含算术运算符（排除引号内的和括号内的）
         */
        static boolean containsArithmeticOperators(String expr) {
            boolean inQuotes = false;
            char quoteChar = '\0';
            int parenthesesLevel = 0;
            int bracketLevel = 0;
            int braceLevel = 0;

            for (int i = 0; i < expr.length(); i++) {
                char c = expr.charAt(i);

                if (!inQuotes) {
                    if (c == '"' || c == '\'') {
                        inQuotes = true;
                        quoteChar = c;
                    } else if (c == '(') {
                        parenthesesLevel++;
                    } else if (c == ')') {
                        parenthesesLevel--;
                    } else if (c == '[') {
                        bracketLevel++;
                    } else if (c == ']') {
                        bracketLevel--;
                    } else if (c == '{') {
                        braceLevel++;
                    } else if (c == '}') {
                        braceLevel--;
                    } else if (isTopLevelContext(parenthesesLevel, bracketLevel, braceLevel) &&
                            (c == '+' || c == '-' || c == '*' || c == '/')) {
                        // 只有在顶层（不在任何括号内）才认为是算术运算符
                        return true;
                    }
                } else {
                    if (c == quoteChar && (i == 0 || expr.charAt(i - 1) != '\\')) {
                        inQuotes = false;
                        quoteChar = '\0';
                    }
                }
            }
            return false;
        }

        /**
         * 检查是否处于顶层上下文（不在任何括号内）
         */
        private static boolean isTopLevelContext(int parenthesesLevel, int bracketLevel, int braceLevel) {
            return parenthesesLevel == 0 && bracketLevel == 0 && braceLevel == 0;
        }
    }

    /**
     * 值类型枚举
     */
    private enum ValueType {
        STRING, LIST, DICT, DEFAULT
    }

    /**
     * 值类型检测器
     */
    private static class ValueTypeDetector {

        /**
         * 检测值的类型
         */
        static ValueType detectType(String value) {
            value = value.trim();

            if (isStringValue(value)) {
                return ValueType.STRING;
            } else if (isListValue(value)) {
                return ValueType.LIST;
            } else if (isDictValue(value)) {
                return ValueType.DICT;
            } else {
                return ValueType.DEFAULT;
            }
        }

        private static boolean isStringValue(String value) {
            return (value.startsWith("\"") && value.endsWith("\"")) ||
                    (value.startsWith("'") && value.endsWith("'")) ||
                    value.startsWith("f\"") || value.startsWith("f'");
        }

        private static boolean isListValue(String value) {
            return value.startsWith("[") && value.endsWith("]");
        }

        private static boolean isDictValue(String value) {
            return value.startsWith("{") && value.endsWith("}");
        }
    }

    /**
     * 值解析器
     */
    private static class ValueParser {

        /**
         * 解析字符串值
         */
        static String parseStringValue(String value) {
            value = value.trim();

            // 处理f-string
            if (value.startsWith("f\"") || value.startsWith("f'")) {
                return value; // 保留原始格式
            }

            // 移除引号
            if ((value.startsWith("\"") && value.endsWith("\"")) ||
                    (value.startsWith("'") && value.endsWith("'"))) {
                return value.substring(1, value.length() - 1);
            }

            return value;
        }

        /**
         * 解析列表值
         */
        static List<String> parseListValue(String value) {
            value = value.trim();

            if (!value.startsWith("[") || !value.endsWith("]")) {
                return new ArrayList<>();
            }

            String content = value.substring(1, value.length() - 1).trim();
            if (content.isEmpty()) {
                return new ArrayList<>();
            }

            return ParameterTokenizer.tokenize(content);
        }

        /**
         * 解析字典值
         */
        static List<String> parseDictValue(String value) {
            value = value.trim();

            if (!value.startsWith("{") || !value.endsWith("}")) {
                return new ArrayList<>();
            }

            String content = value.substring(1, value.length() - 1).trim();
            if (content.isEmpty()) {
                return new ArrayList<>();
            }

            List<String> pairs = ParameterTokenizer.tokenize(content);
            List<String> result = new ArrayList<>();

            for (String pair : pairs) {
                if (pair.contains(":")) {
                    String[] keyValue = pair.split(":", 2);
                    result.add(keyValue[0].trim());
                    result.add(keyValue[1].trim());
                }
            }

            return result;
        }
    }

    /**
     * 函数调用信息
     */
    private static class FunctionCallInfo {
        final String functionName;
        final String parameters;

        FunctionCallInfo(String functionName, String parameters) {
            this.functionName = functionName;
            this.parameters = parameters;
        }
    }

    /**
     * 函数调用赋值信息
     */
    private static class FunctionCallAssignmentInfo {
        final String varName;
        final String functionName;
        final String parameters;

        FunctionCallAssignmentInfo(String varName, String functionName, String parameters) {
            this.varName = varName;
            this.functionName = functionName;
            this.parameters = parameters;
        }
    }

    /**
     * 带索引的函数调用信息
     */
    private static class FunctionCallWithIndexInfo {
        final String varName;
        final String functionName;
        final String parameters;
        final int index;

        FunctionCallWithIndexInfo(String varName, String functionName, String parameters, int index) {
            this.varName = varName;
            this.functionName = functionName;
            this.parameters = parameters;
            this.index = index;
        }
    }

    /**
     * 函数调用检测器
     * 负责检测和解析复杂的函数调用结构
     */
    private static class FunctionCallDetector {

        /**
         * 检测是否为直接函数调用
         */
        static boolean isDirectFunctionCall(String line) {
            line = line.trim();

            // 基本检查：必须包含函数名和括号
            if (!line.matches("^\\w+\\s*\\(.*\\)\\s*$")) {
                return false;
            }

            // 查找函数名和参数的边界
            int firstParen = line.indexOf('(');
            if (firstParen == -1) {
                return false;
            }

            // 检查括号是否匹配
            return hasMatchingParentheses(line, firstParen);
        }

        /**
         * 解析直接函数调用
         */
        static FunctionCallInfo parseDirectFunctionCall(String line) {
            line = line.trim();

            int firstParen = line.indexOf('(');
            if (firstParen == -1) {
                return null;
            }

            String functionName = line.substring(0, firstParen).trim();

            // 提取参数部分（处理嵌套括号）
            String parametersWithParens = line.substring(firstParen);
            if (!hasMatchingParentheses(parametersWithParens, 0)) {
                return null;
            }

            // 移除最外层的括号
            String parameters = parametersWithParens.substring(1, parametersWithParens.length() - 1).trim();

            return new FunctionCallInfo(functionName, parameters);
        }

        /**
         * 检测是否为函数调用赋值（带变量赋值的函数调用）
         */
        static boolean isFunctionCallAssignment(String line) {
            line = line.trim();

            // 必须包含等号
            if (!line.contains("=")) {
                return false;
            }

            // 分割等号，检查右侧是否为函数调用
            String[] parts = line.split("=", 2);
            if (parts.length != 2) {
                return false;
            }

            String leftPart = parts[0].trim();
            String rightPart = parts[1].trim();

            // 左侧应该是一个变量名
            if (!leftPart.matches("^\\w+$")) {
                return false;
            }

            // 右侧应该是函数调用
            return isDirectFunctionCall(rightPart);
        }

        /**
         * 解析函数调用赋值
         */
        static FunctionCallAssignmentInfo parseFunctionCallAssignment(String line) {
            line = line.trim();

            if (!line.contains("=")) {
                return null;
            }

            String[] parts = line.split("=", 2);
            if (parts.length != 2) {
                return null;
            }

            String varName = parts[0].trim();
            String functionCallPart = parts[1].trim();

            // 解析右侧的函数调用
            FunctionCallInfo callInfo = parseDirectFunctionCall(functionCallPart);
            if (callInfo != null) {
                return new FunctionCallAssignmentInfo(varName, callInfo.functionName, callInfo.parameters);
            }

            return null;
        }

        /**
         * 检测是否为带索引的函数调用赋值
         */
        static boolean isFunctionCallWithIndex(String line) {
            line = line.trim();

            // 必须包含等号
            if (!line.contains("=")) {
                return false;
            }

            // 分割等号
            String[] parts = line.split("=", 2);
            if (parts.length != 2) {
                return false;
            }

            String leftPart = parts[0].trim();
            String rightPart = parts[1].trim();

            // 左侧应该是一个变量名
            if (!leftPart.matches("^\\w+$")) {
                return false;
            }

            // 右侧应该是函数调用加索引：func()[index] 或 func()[-1]
            return rightPart.matches("^\\w+\\s*\\(.*\\)\\s*\\[\\s*-?\\d+\\s*\\]\\s*$") &&
                    hasMatchingParenthesesAndBrackets(rightPart);
        }

        /**
         * 解析带索引的函数调用赋值
         */
        static FunctionCallWithIndexInfo parseFunctionCallWithIndex(String line) {
            line = line.trim();

            if (!line.contains("=")) {
                return null;
            }

            String[] parts = line.split("=", 2);
            if (parts.length != 2) {
                return null;
            }

            String varName = parts[0].trim();
            String functionCallPart = parts[1].trim();

            // 查找索引部分 [...]
            int lastBracketStart = functionCallPart.lastIndexOf('[');
            int lastBracketEnd = functionCallPart.lastIndexOf(']');

            if (lastBracketStart == -1 || lastBracketEnd == -1 || lastBracketStart >= lastBracketEnd) {
                return null;
            }

            // 提取索引
            String indexStr = functionCallPart.substring(lastBracketStart + 1, lastBracketEnd).trim();
            int index;
            try {
                index = Integer.parseInt(indexStr);
            } catch (NumberFormatException e) {
                return null;
            }

            // 提取函数调用部分
            String functionCallOnly = functionCallPart.substring(0, lastBracketStart).trim();
            FunctionCallInfo callInfo = parseDirectFunctionCall(functionCallOnly);

            if (callInfo != null) {
                return new FunctionCallWithIndexInfo(varName, callInfo.functionName, callInfo.parameters, index);
            }

            return null;
        }

        /**
         * 检查括号和方括号是否匹配
         */
        private static boolean hasMatchingParenthesesAndBrackets(String str) {
            int parenLevel = 0;
            int bracketLevel = 0;
            boolean inQuotes = false;
            char quoteChar = '\0';

            for (int i = 0; i < str.length(); i++) {
                char c = str.charAt(i);

                if (!inQuotes) {
                    if (c == '"' || c == '\'') {
                        inQuotes = true;
                        quoteChar = c;
                    } else if (c == '(') {
                        parenLevel++;
                    } else if (c == ')') {
                        parenLevel--;
                        if (parenLevel < 0)
                            return false;
                    } else if (c == '[') {
                        bracketLevel++;
                    } else if (c == ']') {
                        bracketLevel--;
                        if (bracketLevel < 0)
                            return false;
                    }
                } else {
                    if (c == quoteChar && (i == 0 || str.charAt(i - 1) != '\\')) {
                        inQuotes = false;
                        quoteChar = '\0';
                    }
                }
            }

            return parenLevel == 0 && bracketLevel == 0;
        }

        /**
         * 检查括号是否匹配
         */
        private static boolean hasMatchingParentheses(String str, int startIndex) {
            int level = 0;
            boolean inQuotes = false;
            char quoteChar = '\0';

            for (int i = startIndex; i < str.length(); i++) {
                char c = str.charAt(i);

                if (!inQuotes) {
                    if (c == '"' || c == '\'') {
                        inQuotes = true;
                        quoteChar = c;
                    } else if (c == '(') {
                        level++;
                    } else if (c == ')') {
                        level--;
                        if (level == 0) {
                            // 检查是否还有非空白字符（除了可能的索引访问）
                            String remaining = str.substring(i + 1).trim();
                            return remaining.isEmpty() || remaining.matches("^\\s*\\[.*\\]\\s*$");
                        } else if (level < 0) {
                            return false;
                        }
                    }
                } else {
                    if (c == quoteChar && (i == 0 || str.charAt(i - 1) != '\\')) {
                        inQuotes = false;
                        quoteChar = '\0';
                    }
                }
            }

            return level == 0;
        }
    }

    /**
     * 参数分词器
     * 负责将复杂的参数字符串分解为独立的参数
     */
    private static class ParameterTokenizer {

        /**
         * 将参数字符串分解为token列表
         */
        static List<String> tokenize(String params) {
            if (params == null || params.trim().isEmpty()) {
                return new ArrayList<>();
            }

            List<String> tokens = new ArrayList<>();
            StringBuilder currentToken = new StringBuilder();

            TokenizerState state = new TokenizerState();

            for (int i = 0; i < params.length(); i++) {
                char c = params.charAt(i);

                if (state.processCharacter(c, i, params)) {
                    // 遇到顶层逗号，分割参数
                    if (currentToken.length() > 0) {
                        tokens.add(currentToken.toString().trim());
                        currentToken.setLength(0);
                    }
                } else {
                    currentToken.append(c);
                }
            }

            // 添加最后一个token
            if (currentToken.length() > 0) {
                tokens.add(currentToken.toString().trim());
            }

            return tokens;
        }

        /**
         * 分词器状态管理
         * 修复了引号内括号处理的bug
         */
        private static class TokenizerState {
            private int parenthesesLevel = 0;
            private int bracketLevel = 0;
            private int braceLevel = 0;
            private boolean inQuotes = false;
            private char quoteChar = '\0';

            /**
             * 处理字符，返回true表示遇到分隔符
             */
            boolean processCharacter(char c, int index, String params) {
                if (!inQuotes) {
                    return processNonQuotedCharacter(c);
                } else {
                    return processQuotedCharacter(c, index, params);
                }
            }

            private boolean processNonQuotedCharacter(char c) {
                switch (c) {
                    case '"':
                    case '\'':
                        inQuotes = true;
                        quoteChar = c;
                        return false;
                    case '(':
                        parenthesesLevel++;
                        return false;
                    case ')':
                        parenthesesLevel--;
                        return false;
                    case '[':
                        bracketLevel++;
                        return false;
                    case ']':
                        bracketLevel--;
                        return false;
                    case '{':
                        braceLevel++;
                        return false;
                    case '}':
                        braceLevel--;
                        return false;
                    case ',':
                        return isTopLevel();
                    default:
                        return false;
                }
            }

            private boolean processQuotedCharacter(char c, int index, String params) {
                // 在引号内，只需要检查是否遇到结束引号
                // 引号内的括号不应该影响外层的括号计数
                if (c == quoteChar && (index == 0 || params.charAt(index - 1) != '\\')) {
                    inQuotes = false;
                    quoteChar = '\0';
                }
                // 引号内永远不分割参数
                return false;
            }

            private boolean isTopLevel() {
                return parenthesesLevel == 0 && bracketLevel == 0 && braceLevel == 0 && !inQuotes;
            }
        }
    }

    /**
     * JavaExecutable构建器
     * 提供流畅的API来构建JavaExecutable对象
     */
    private static class ExecutableBuilder {

        /**
         * 创建函数执行类型的构建器
         */
        static FunctionExecutableBuilder createFunctionExecutable() {
            return new FunctionExecutableBuilder();
        }

        /**
         * 创建数据提取类型的构建器
         */
        static DataExtractionExecutableBuilder createDataExtractionExecutable() {
            return new DataExtractionExecutableBuilder();
        }

        /**
         * 创建默认值类型的构建器
         */
        static DefaultValueExecutableBuilder createDefaultValueExecutable() {
            return new DefaultValueExecutableBuilder();
        }

        /**
         * 函数执行类型构建器
         */
        static class FunctionExecutableBuilder {
            private final JavaExecutable executable = new JavaExecutable();

            FunctionExecutableBuilder() {
                executable.setJavaExecutableType(JavaExecutable.JavaExecutableType.FUNCTIOM_EXCUTION.getType());
                executable.setInputs(new ArrayList<>());
            }

            FunctionExecutableBuilder withStepNum(int stepNum) {
                executable.setNum(stepNum);
                return this;
            }

            FunctionExecutableBuilder withFunctionName(String functionName) {
                executable.setFunctionName(functionName);
                return this;
            }

            FunctionExecutableBuilder withOutputName(String outputName) {
                executable.setOutputName(outputName);
                return this;
            }

            FunctionExecutableBuilder withInputs(List<String> inputs) {
                executable.setInputs(inputs);
                return this;
            }

            FunctionExecutableBuilder withIndex(Integer index) {
                executable.setResultDataIndex(index);
                return this;
            }

            JavaExecutable build() {
                return executable;
            }
        }

        /**
         * 数据提取类型构建器
         */
        static class DataExtractionExecutableBuilder {
            private final JavaExecutable executable = new JavaExecutable();

            DataExtractionExecutableBuilder() {
                executable.setJavaExecutableType(JavaExecutable.JavaExecutableType.DATA_EXTRACTION.getType());
                executable.setInputs(new ArrayList<>());
            }

            DataExtractionExecutableBuilder withStepNum(int stepNum) {
                executable.setNum(stepNum);
                return this;
            }

            DataExtractionExecutableBuilder withFunctionName(String functionName) {
                executable.setFunctionName(functionName);
                return this;
            }

            DataExtractionExecutableBuilder withOutputName(String outputName) {
                executable.setOutputName(outputName);
                return this;
            }

            DataExtractionExecutableBuilder withIndex(Integer index) {
                executable.setResultDataIndex(index);
                return this;
            }

            JavaExecutable build() {
                return executable;
            }
        }

        /**
         * 默认值类型构建器
         */
        static class DefaultValueExecutableBuilder {
            private final JavaExecutable executable = new JavaExecutable();

            DefaultValueExecutableBuilder() {
                executable.setJavaExecutableType(JavaExecutable.JavaExecutableType.DEFAULT_VALUE.getType());
                executable.setInputs(new ArrayList<>());
            }

            DefaultValueExecutableBuilder withStepNum(int stepNum) {
                executable.setNum(stepNum);
                return this;
            }

            DefaultValueExecutableBuilder withFunctionName(String functionName) {
                executable.setFunctionName(functionName);
                return this;
            }

            DefaultValueExecutableBuilder withOutputName(String outputName) {
                executable.setOutputName(outputName);
                return this;
            }

            JavaExecutable build() {
                return executable;
            }
        }
    }
}