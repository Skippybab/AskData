package com.cultivate.executor;

import com.cultivate.buffer.util.BufferUtil;
import com.cultivate.common.exception.DataAccessException;
import com.cultivate.common.exception.ExecutionFailureException;
import com.cultivate.constant.PromptParam;
import com.cultivate.executor.sysUtils.FunctionUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Python直接执行服务
 * 通过外部Python进程执行大模型生成的Python代码
 *
 * @author lfz
 * @date 2025/1/7
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PythonExecutor {

    private final FunctionUtil functionUtil;
    private final ObjectMapper objectMapper;
    private final BufferUtil bufferUtil;

    // 测试数据收集现在通过静态回调机制实现

    // 存储Python执行过程中的错误信息，用于异常分析
    private String pythonErrorOutput = "";

    // 配置ObjectMapper以正确处理UTF-8编码
    @PostConstruct
    private void configureObjectMapper() {
        // 确保ObjectMapper正确处理UTF-8编码，不转义非ASCII字符
        this.objectMapper.getFactory().disable(
                com.fasterxml.jackson.core.JsonGenerator.Feature.ESCAPE_NON_ASCII);
    }

    /**
     * 执行Python代码
     *
     * @param pythonCode Python代码字符串
     * @param paramMap   参数映射
     * @param userId     用户ID
     */
    public void executePythonCode(String pythonCode, HashMap<String, Object> paramMap, String userId) {
        Path tempDir = null;
        Process pythonProcess = null;
        pythonErrorOutput = ""; // 重置错误输出

        try {
//            reporter.reportStep("开始准备Python执行环境\n");
            log.info("准备Python执行环境...");

            // 1. 创建临时执行环境
            tempDir = createPythonEnvironment(paramMap);

            // 2. 生成完整的main.py文件
            createMainPythonFile(tempDir, pythonCode);

//            reporter.reportStep("启动Python进程执行代码\n");
            log.info("启动Python进程执行代码...");

            // 3. 启动Python进程
            pythonProcess = startPythonProcess(tempDir);

            // 4. 处理Python进程的输入输出
            handlePythonExecution(pythonProcess, userId);

            // 5. 等待执行完成
            boolean finished = pythonProcess.waitFor(300, TimeUnit.SECONDS);
            if (!finished) {
                throw ExecutionFailureException.timeoutError("Python代码执行超时（300秒）");
            }

            int exitCode = pythonProcess.exitValue();
            if (exitCode != 0) {
                // 根据错误输出和退出码分析异常类型
                throw analyzeAndCreateException(exitCode, pythonErrorOutput, pythonCode);
            }

//            reporter.reportStep("Python代码执行完成\n");
            log.info("Python代码执行完成");

        } catch (DataAccessException | ExecutionFailureException e) {
            // 已经是分类异常，直接重新抛出
            log.error("Python代码执行失败: {}", e.getMessage(), e);
//            reporter.reportStep("执行失败: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            // 未分类的异常，进行分析
            log.error("Python代码执行失败", e);
//            reporter.reportStep("执行失败: " + e.getMessage());
            throw analyzeGenericException(e);
        } finally {
            // 清理资源
            if (pythonProcess != null && pythonProcess.isAlive()) {
                pythonProcess.destroyForcibly();
            }
            cleanupTempDirectory(tempDir);
        }
    }

    /**
     * 分析Python执行异常并创建相应的异常类型
     */
    private RuntimeException analyzeAndCreateException(int exitCode, String errorOutput, String pythonCode) {
        log.info("分析Python执行异常 - 退出码: {}, 错误输出: {}", exitCode, errorOutput);

        String lowerErrorOutput = errorOutput.toLowerCase();

        // 检测数组越界和数据访问相关异常
        if (containsDataAccessError(lowerErrorOutput)) {
            return createDataAccessException(errorOutput);
        }

        // 检测语法错误
        if (containsSyntaxError(lowerErrorOutput)) {
            return ExecutionFailureException.syntaxError("代码语法错误 - " + extractErrorDetails(errorOutput));
        }

        // 检测运行时错误
        if (containsRuntimeError(lowerErrorOutput)) {
            return ExecutionFailureException.runtimeError("代码运行时错误 - " + extractErrorDetails(errorOutput));
        }

        // 检测进程相关错误
        if (containsProcessError(lowerErrorOutput, exitCode)) {
            return ExecutionFailureException
                    .processError("进程执行异常，退出码: " + exitCode + " - " + extractErrorDetails(errorOutput));
        }

        // 默认为未知错误
        return ExecutionFailureException.unknownError("退出码: " + exitCode + " - " + extractErrorDetails(errorOutput));
    }

    /**
     * 分析通用异常
     */
    private RuntimeException analyzeGenericException(Exception e) {
        String errorMessage = e.getMessage() != null ? e.getMessage().toLowerCase() : "";

        // 检测超时相关异常
        if (e.getClass().getSimpleName().contains("Timeout") || errorMessage.contains("timeout")) {
            return ExecutionFailureException.timeoutError(e.getMessage());
        }

        // 检测IO相关异常
        if (e instanceof IOException) {
            return ExecutionFailureException.processError("IO异常 - " + e.getMessage());
        }

        // 检测中断异常
        if (e instanceof InterruptedException) {
            return ExecutionFailureException.processError("执行被中断 - " + e.getMessage());
        }

        // 默认为未知错误
        return ExecutionFailureException.unknownError(e.getMessage());
    }

    /**
     * 检测是否为数据访问相关错误
     */
    private boolean containsDataAccessError(String errorOutput) {
        return errorOutput.contains("indexerror") ||
                errorOutput.contains("index out of range") ||
                errorOutput.contains("list index out of range") ||
                errorOutput.contains("list assignment index out of range") ||
                errorOutput.contains("keyerror") ||
                errorOutput.contains("empty") ||
                errorOutput.contains("no data") ||
                errorOutput.contains("查无数据") ||
                errorOutput.contains("数据为空") ||
                errorOutput.contains("结果集为空");
    }

    /**
     * 创建数据访问异常
     */
    private DataAccessException createDataAccessException(String errorOutput) {
        String lowerErrorOutput = errorOutput.toLowerCase();

        if (lowerErrorOutput.contains("indexerror") ||
                lowerErrorOutput.contains("index out of range") ||
                lowerErrorOutput.contains("list index out of range")) {
            return DataAccessException.arrayIndexOutOfBounds("访问数组索引超出范围 - " + extractErrorDetails(errorOutput));
        }

        if (lowerErrorOutput.contains("empty") ||
                lowerErrorOutput.contains("no data") ||
                lowerErrorOutput.contains("查无数据") ||
                lowerErrorOutput.contains("数据为空")) {
            return DataAccessException.emptyQueryResult("查询结果为空 - " + extractErrorDetails(errorOutput));
        }

        return DataAccessException.noDataAvailable("数据不可用 - " + extractErrorDetails(errorOutput));
    }

    /**
     * 检测是否为语法错误
     */
    private boolean containsSyntaxError(String errorOutput) {
        return errorOutput.contains("syntaxerror") ||
                errorOutput.contains("invalid syntax") ||
                errorOutput.contains("indentationerror") ||
                errorOutput.contains("tabserror");
    }

    /**
     * 检测是否为运行时错误
     */
    private boolean containsRuntimeError(String errorOutput) {
        return errorOutput.contains("nameerror") ||
                errorOutput.contains("typeerror") ||
                errorOutput.contains("valueerror") ||
                errorOutput.contains("attributeerror") ||
                errorOutput.contains("zerodivisionerror") ||
                errorOutput.contains("runtimeerror");
    }

    /**
     * 检测是否为进程相关错误
     */
    private boolean containsProcessError(String errorOutput, int exitCode) {
        return exitCode == 1 ||
                errorOutput.contains("permission denied") ||
                errorOutput.contains("access denied") ||
                errorOutput.contains("command not found") ||
                errorOutput.contains("no such file");
    }

    /**
     * 提取错误详细信息
     */
    private String extractErrorDetails(String errorOutput) {
        if (errorOutput == null || errorOutput.trim().isEmpty()) {
            return "无详细错误信息";
        }

        // 提取最后几行错误信息，限制长度
        String[] lines = errorOutput.split("\n");
        StringBuilder details = new StringBuilder();
        int startIndex = Math.max(0, lines.length - 3);

        for (int i = startIndex; i < lines.length; i++) {
            String line = lines[i].trim();
            if (!line.isEmpty()) {
                details.append(line).append(" ");
            }
        }

        String result = details.toString().trim();
        return result.length() > 200 ? result.substring(0, 200) + "..." : result;
    }

    /**
     * 创建Python执行环境
     */
    private Path createPythonEnvironment(HashMap<String, Object> paramMap) throws IOException {
        Path tempDir = Files.createTempDirectory("python_exec_");

        // 创建Java Bridge模块
        createJavaBridge(tempDir);

        // 创建系统函数模块
        createSystemFunctions(tempDir);

        // 创建参数文件
        String paramsJson = objectMapper.writeValueAsString(paramMap);
        Files.writeString(tempDir.resolve("params.json"), paramsJson, StandardCharsets.UTF_8);

        log.info("Python执行环境创建完成: {}", tempDir);
        return tempDir;
    }

    /**
     * 创建Java Bridge通信模块
     */
    private void createJavaBridge(Path tempDir) throws IOException {
        String bridgeCode = """
                import json
                import sys
                import threading
                import queue
                from typing import Any, Dict, List
                
                class JavaBridge:
                    def __init__(self):
                        self.request_id = 0
                        self.response_queue = queue.Queue()
                
                    def call_java_function(self, function_name: str, *args) -> Any:
                        '''调用Java函数并返回结果'''
                        self.request_id += 1
                        request = {
                            'id': self.request_id,
                            'type': 'function_call',
                            'function': function_name,
                            'args': list(args)
                        }
                
                        # 发送请求到Java端，确保使用UTF-8编码
                        print(f"JAVA_REQUEST:{json.dumps(request, ensure_ascii=False)}", flush=True)
                
                        # 等待Java响应
                        try:
                            response_line = input()
                            if response_line.startswith("JAVA_RESPONSE:"):
                                response_data = response_line[14:]  # 移除前缀
                                response = json.loads(response_data)
                
                                if response.get('error'):
                                    raise Exception(f"Java函数调用失败: {response['error']}")
                
                                return response.get('result')
                            else:
                                raise Exception(f"无效的Java响应格式: {response_line}")
                        except EOFError:
                            raise Exception("Java进程通信中断")
                        except json.JSONDecodeError as e:
                            raise Exception(f"Java响应JSON解析失败: {e}")
                
                    def report_step(self, message: str):
                        '''报告执行步骤'''
                        self.call_java_function('report_step', message)
                
                    def report_progress(self, message: str):
                        '''报告执行进度'''
                        self.call_java_function('report_progress', message)
                
                # 全局Bridge实例
                bridge = JavaBridge()
                
                # 报告函数 - 供用户代码调用
                def report(message: str):
                    '''流式输出报告函数，供大模型在Python代码中调用'''
                    bridge.report_progress(message)
                """;

        Files.writeString(tempDir.resolve("java_bridge.py"), bridgeCode, StandardCharsets.UTF_8);
    }

    /**
     * 创建系统函数模块
     */
    private void createSystemFunctions(Path tempDir) throws IOException {
        String systemFunctionsCode = """
                from java_bridge import bridge
                from typing import List, Dict, Any
                
                """;

        systemFunctionsCode = systemFunctionsCode + PromptParam.SYS_FUN_PARAMS_CULIVATE;
        Files.writeString(tempDir.resolve("system_functions.py"), systemFunctionsCode, StandardCharsets.UTF_8);
    }

    /**
     * 生成主执行文件（新的灵活执行方式）
     */
    private void createMainPythonFile(Path tempDir, String userPythonCode) throws IOException {
        // 分析代码结构，决定使用哪种执行策略
        CodeStructure structure = analyzeCodeStructure(userPythonCode);

        if (structure.requiresFlexibleExecution()) {
            // 使用新的灵活执行方式
            createFlexiblePythonFile(tempDir, userPythonCode, structure);
        } else {
            // 使用原有的函数调用方式（向后兼容）
            createLegacyPythonFile(tempDir, userPythonCode);
        }
    }

    /**
     * 创建灵活的Python执行文件（方案三实现）
     */
    private void createFlexiblePythonFile(Path tempDir, String userPythonCode, CodeStructure structure) throws IOException {
        // 转义用户代码中的三引号
        String escapedUserCode = escapeUserCode(userPythonCode);

        String flexibleTemplate = """
                # -*- coding: utf-8 -*-
                import json
                import sys
                import traceback
                import types
                from java_bridge import bridge, report
                from system_functions import *
                
                def execute_dynamic_code():
                    try:
                        bridge.report_step("开始动态执行Python代码\\n")
                
                        # 加载参数到全局命名空间
                        with open('params.json', 'r', encoding='utf-8') as f:
                            params = json.load(f)
                
                        # 创建安全的执行命名空间
                        exec_namespace = create_execution_namespace()
        
                        # 安全更新参数，确保params不为None
                        if params is not None:
                            exec_namespace.update(params)
                        else:
                            print("警告: params.json中的参数为None，跳过参数更新")
                
                        # 用户代码
                        user_code = '''%s'''
                
                        bridge.report_step("正在执行用户定义的代码\\n")
                
                        # 动态执行用户代码
                        exec(user_code, exec_namespace)
                
                        # 执行后处理逻辑
                        post_execution_handler(exec_namespace)
                
                        bridge.report_step("Python代码执行完成\\n")
                
                    except Exception as e:
                        bridge.report_step(f"执行失败: {str(e)}\\n")
                        traceback.print_exc()
                        sys.exit(1)
                
                def create_execution_namespace():
                    '''创建安全的执行命名空间'''
                    return {
                        '__name__': '__main__',
                        '__builtins__': __builtins__,
                        'bridge': bridge,
                        'report': report,
                        'gen_sql': gen_sql,
                        'exec_sql': exec_sql,
                        # 添加常用的Python内置模块
                        'json': json,
                        'sys': sys,
                        'traceback': traceback,
                        'types': types
                    }
                
                def post_execution_handler(namespace):
                    '''执行后处理逻辑'''
                    try:
                        # 1. 检查是否有main函数并调用
                        if 'main' in namespace and callable(namespace['main']):
                            bridge.report_step("检测到main函数，正在调用\\n")
                            namespace['main']()
                            return
                
                        # 2. 检查是否有其他需要调用的函数
                        function_calls = detect_uncalled_functions(namespace)
                        if function_calls:
                            bridge.report_step(f"检测到未调用的函数，正在执行: {function_calls}\\n")
                            for func_name in function_calls:
                                if func_name in namespace and callable(namespace[func_name]):
                                    try:
                                        namespace[func_name]()
                                    except Exception as e:
                                        bridge.report_step(f"调用函数{func_name}时出错: {str(e)}\\n")
                
                    except Exception as e:
                        bridge.report_step(f"后处理阶段出错: {str(e)}\\n")
                        traceback.print_exc()
                
                def detect_uncalled_functions(namespace):
                    '''检测命名空间中未被调用的用户定义函数'''
                    user_functions = []
                    for name, obj in namespace.items():
                        if (callable(obj) and 
                            hasattr(obj, '__module__') and 
                            obj.__module__ == '__main__' and
                            not name.startswith('_') and
                            name not in ['main']):  # 排除main函数，它已经被特殊处理
                            user_functions.append(name)
                    return user_functions
                
                if __name__ == "__main__":
                    execute_dynamic_code()
                """;

        String finalCode = String.format(flexibleTemplate, escapedUserCode);
        log.info("灵活执行代码：" + finalCode);

        Files.writeString(tempDir.resolve("main.py"), finalCode, StandardCharsets.UTF_8);
    }

    /**
     * 创建传统的Python执行文件（向后兼容）
     */
    private void createLegacyPythonFile(Path tempDir, String userPythonCode) throws IOException {
        // 预处理用户代码，确保正确的缩进
        String processedUserCode = preprocessUserCode(userPythonCode);

        // 检测是否需要自动调用函数
        String functionCall = detectAndGenerateFunctionCall(userPythonCode);

        String mainCode1 = """
                # -*- coding: utf-8 -*-
                import json
                import sys
                import traceback
                from java_bridge import bridge, report
                from system_functions import *
                
                def main():
                    try:
                        bridge.report_step("开始执行Python代码\\n")
                        # 加载参数到全局命名空间
                        with open('params.json', 'r', encoding='utf-8') as f:
                            params = json.load(f)
                        globals().update(params)
                
                        # 执行用户代码
                """;
        String mainCode2 = """
                
                        # 自动调用检测到的函数
                """ + functionCall + """
                        bridge.report_step("Python代码执行完成\\n")
                    except Exception as e:
                        traceback.print_exc()
                        sys.exit(1)
                
                if __name__ == "__main__":
                    main();
                """;
        String mainCode = mainCode1 + processedUserCode + mainCode2;
        log.info("传统执行代码：" + mainCode);

        Files.writeString(tempDir.resolve("main.py"), mainCode, StandardCharsets.UTF_8);
    }

    /**
     * 检测用户代码中的函数定义并生成相应的函数调用
     * 改进版本：避免重复调用已经被调用的函数
     */
    private String detectAndGenerateFunctionCall(String userPythonCode) {
        if (userPythonCode == null || userPythonCode.trim().isEmpty()) {
            return "";
        }

        // 检测函数定义和调用
        Set<String> definedFunctions = new HashSet<>();
        Set<String> calledFunctions = new HashSet<>();

        String[] lines = userPythonCode.split("\n");

        // 第一遍扫描：收集所有函数定义
        for (String line : lines) {
            String trimmedLine = line.trim();

            // 检测函数定义 (def 函数名():)
            if (trimmedLine.startsWith("def ") && trimmedLine.contains("(") && trimmedLine.endsWith(":")) {
                String functionName = extractFunctionName(trimmedLine);
                if (functionName != null && !functionName.isEmpty()) {
                    definedFunctions.add(functionName);
                    log.info("🔍 [函数检测] 检测到函数定义: {}", functionName);
                }
            }
        }

        // 第二遍扫描：收集所有函数调用
        for (String line : lines) {
            String trimmedLine = line.trim();

            // 跳过函数定义行和注释行
            if (trimmedLine.startsWith("def ") || trimmedLine.startsWith("#")) {
                continue;
            }

            // 检测函数调用
            for (String functionName : definedFunctions) {
                if (isFunctionCalled(trimmedLine, functionName)) {
                    calledFunctions.add(functionName);
                    log.info("🔍 [函数检测] 检测到函数调用: {}", functionName);
                }
            }
        }

        // 生成自动调用代码：只为未被调用的函数生成调用
        StringBuilder functionCalls = new StringBuilder();

        // 统计信息
        int totalFunctions = definedFunctions.size();
        int calledFunctionsCount = calledFunctions.size();
        int uncalledFunctions = totalFunctions - calledFunctionsCount;

        log.info("📊 [函数统计] 总函数数: {}, 已调用: {}, 未调用: {}", totalFunctions, calledFunctionsCount, uncalledFunctions);

        for (String functionName : definedFunctions) {
            if (!calledFunctions.contains(functionName)) {
                // 添加函数调用，使用正确的缩进（8个空格，匹配main() -> try内的缩进级别）
                functionCalls.append("        ").append(functionName).append("()\n");
                log.info("🚀 [函数调用] 为未调用的函数生成自动调用: {}()", functionName);
            } else {
                log.info("⚠️ [函数调用] 函数已被调用，跳过自动调用: {}", functionName);
            }
        }

        String result = functionCalls.toString();
        if (!result.isEmpty()) {
            log.info("🚀 [函数调用] 生成的自动调用代码:\n{}", result);
            log.info("✅ [改进效果] 避免了 {} 个函数的重复调用", calledFunctionsCount);
        } else {
            log.info("⚠️ [函数检测] 所有函数都已被调用或无函数定义，无需添加自动调用");
        }

        return result;
    }

    /**
     * 检测指定行是否调用了指定函数
     *
     * @param line         代码行
     * @param functionName 函数名
     * @return 是否调用了该函数
     */
    private boolean isFunctionCalled(String line, String functionName) {
        if (line == null || functionName == null) {
            return false;
        }

        // 精确匹配函数调用模式：函数名后跟(
        String functionCallPattern = functionName + "(";

        // 检查行中是否包含函数调用模式
        if (line.contains(functionCallPattern)) {
            // 进一步验证，确保不是字符串中的内容或其他误判

            // 1. 检查是否在字符串中（简单检查）
            if (isInString(line, functionCallPattern)) {
                return false;
            }

            // 2. 检查前面是否有有效的调用上下文
            int index = line.indexOf(functionCallPattern);
            if (index > 0) {
                char prevChar = line.charAt(index - 1);
                // 函数调用前应该是空格、制表符、=、(、,、[、{等
                if (!Character.isWhitespace(prevChar) &&
                        prevChar != '=' && prevChar != '(' && prevChar != ',' &&
                        prevChar != '[' && prevChar != '{' && prevChar != '\n' &&
                        prevChar != '\t') {
                    return false;
                }
            }

            return true;
        }

        return false;
    }

    /**
     * 简单检查字符串是否在引号中（基础版本）
     *
     * @param line   代码行
     * @param target 目标字符串
     * @return 是否在字符串中
     */
    private boolean isInString(String line, String target) {
        int targetIndex = line.indexOf(target);
        if (targetIndex == -1) {
            return false;
        }

        // 计算目标字符串前面的引号数量
        int singleQuoteCount = 0;
        int doubleQuoteCount = 0;

        for (int i = 0; i < targetIndex; i++) {
            char c = line.charAt(i);
            if (c == '\'' && (i == 0 || line.charAt(i - 1) != '\\')) {
                singleQuoteCount++;
            } else if (c == '"' && (i == 0 || line.charAt(i - 1) != '\\')) {
                doubleQuoteCount++;
            }
        }

        // 如果引号数量为奇数，说明在字符串中
        return (singleQuoteCount % 2 == 1) || (doubleQuoteCount % 2 == 1);
    }

    /**
     * 从函数定义行中提取函数名
     */
    private String extractFunctionName(String functionDefLine) {
        try {
            // 移除 "def " 前缀
            String withoutDef = functionDefLine.substring(4).trim();

            // 找到第一个左括号的位置
            int parenIndex = withoutDef.indexOf('(');
            if (parenIndex > 0) {
                return withoutDef.substring(0, parenIndex).trim();
            }
        } catch (Exception e) {
            log.warn("⚠️ [函数名提取] 提取函数名失败: {}", functionDefLine, e);
        }
        return null;
    }

    /**
     * 预处理用户Python代码，确保正确的缩进和格式
     */
    private String preprocessUserCode(String userPythonCode) {
        if (userPythonCode == null || userPythonCode.trim().isEmpty()) {
            return "";
        }

        // 分割成行
        String[] lines = userPythonCode.split("\n");
        StringBuilder processedCode = new StringBuilder();

        // 找到第一个非空行的缩进作为基准
        int baseIndent = findBaseIndent(lines);

        for (String line : lines) {
            if (line.trim().isEmpty()) {
                // 保留空行
                processedCode.append("\n");
            } else {
                // 计算相对缩进
                int currentIndent = getIndentLevel(line);
                int relativeIndent = Math.max(0, currentIndent - baseIndent);

                // 添加基础缩进（8个空格，因为在main() -> try -> 用户代码）加上相对缩进
                String indent = "        " + " ".repeat(relativeIndent);
                processedCode.append(indent).append(line.trim()).append("\n");
            }
        }

        return processedCode.toString();
    }

    /**
     * 找到代码的基础缩进级别
     */
    private int findBaseIndent(String[] lines) {
        for (String line : lines) {
            if (!line.trim().isEmpty()) {
                return getIndentLevel(line);
            }
        }
        return 0;
    }

    /**
     * 获取行的缩进级别
     */
    private int getIndentLevel(String line) {
        int indent = 0;
        for (char c : line.toCharArray()) {
            if (c == ' ') {
                indent++;
            } else if (c == '\t') {
                indent += 4; // 假设tab等于4个空格
            } else {
                break;
            }
        }
        return indent;
    }

    /**
     * 启动Python进程
     */
    private Process startPythonProcess(Path tempDir) throws IOException {
        ProcessBuilder processBuilder = new ProcessBuilder("python", "main.py");
        processBuilder.directory(tempDir.toFile());
        processBuilder.redirectErrorStream(true);

        // 设置环境变量确保Python使用UTF-8编码
        Map<String, String> env = processBuilder.environment();
        env.put("PYTHONIOENCODING", "utf-8");

        return processBuilder.start();
    }

    /**
     * 处理Python进程执行
     */
    private void handlePythonExecution(Process pythonProcess, String userId) {
        // 获取Python进程的输入输出流，明确指定UTF-8编码
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(pythonProcess.getInputStream(), StandardCharsets.UTF_8));
        PrintWriter writer = new PrintWriter(
                new OutputStreamWriter(pythonProcess.getOutputStream(), StandardCharsets.UTF_8), true);

        // 启动输出处理线程
        CompletableFuture<Void> outputHandler = CompletableFuture.runAsync(() -> {
            try {
                String line;
                while ((line = reader.readLine()) != null) {
                    handlePythonOutput(line, writer, userId);
                }
            } catch (IOException e) {
                log.error("处理Python输出时发生错误", e);
            } finally {
                try {
                    reader.close();
                    writer.close();
                } catch (IOException e) {
                    log.error("关闭Python进程流时发生错误", e);
                }
            }
        });

        // 等待输出处理完成
        try {
            outputHandler.get(300, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("Python输出处理超时或失败", e);
        }
    }

    /**
     * 处理Python输出
     */
    private void handlePythonOutput(String line, PrintWriter writer, String userId) {
        try {
            if (line.startsWith("JAVA_REQUEST:")) {
                // 处理Java函数调用请求
                handleJavaFunctionCall(line.substring(13), writer, userId);
            } else {
                // 检测是否为错误输出
                if (isErrorOutput(line)) {
                    pythonErrorOutput += line + "\n";
                    log.error("Python错误输出: {}", line);
                } else {
                    // 普通输出
                    log.info("Python输出: {}", line);
                }
            }
        } catch (Exception e) {
            log.error("处理Python输出失败: {}", line, e);
            pythonErrorOutput += "处理输出异常: " + line + "\n";
        }
    }

    /**
     * 检测是否为错误输出
     */
    private boolean isErrorOutput(String line) {
        if (line == null)
            return false;
        String lowerLine = line.toLowerCase();
        return lowerLine.contains("error") ||
                lowerLine.contains("exception") ||
                lowerLine.contains("traceback") ||
                lowerLine.contains("failed") ||
                lowerLine.contains("invalid") ||
                line.trim().startsWith("File \"") ||
                line.trim().matches("\\s*\\^.*") || // 指向错误位置的箭头
                lowerLine.contains("syntax") ||
                lowerLine.contains("indent");
    }

    /**
     * 处理Java函数调用
     */
    private void handleJavaFunctionCall(String requestJson, PrintWriter writer,
                                        String userId) {
        try {
            Map<String, Object> request = objectMapper.readValue(requestJson, Map.class);
            String functionName = (String) request.get("function");
            List<Object> args = (List<Object>) request.get("args");
            Integer requestId = (Integer) request.get("id");

            // 调用对应的Java函数
            Object result = callJavaFunction(functionName, args, userId);

            // 返回结果给Python
            Map<String, Object> response = Map.of(
                    "id", requestId,
                    "result", result != null ? result : "");

            // 配置ObjectMapper以正确处理非ASCII字符
            String responseJson = objectMapper.writeValueAsString(response);

            // 发送响应到Python进程
            writer.println("JAVA_RESPONSE:" + responseJson);
            writer.flush();

        } catch (DataAccessException e) {
            log.error("Java函数调用发生数据访问异常: {}", e.getMessage(), e);
            // 直接在Python端抛出异常，让Python代码处理
            Integer requestId = extractRequestIdSafely(requestJson);
            sendErrorResponse(writer, e.getMessage(), requestId);
            // 记录错误信息供后续异常分析使用
            pythonErrorOutput += "Java函数调用异常: " + e.getMessage() + "\n";
        } catch (Exception e) {
            log.error("处理Java函数调用失败", e);
            // 发送错误响应
            Integer requestId = extractRequestIdSafely(requestJson);
            sendErrorResponse(writer, e.getMessage(), requestId);
            pythonErrorOutput += "Java函数调用异常: " + e.getMessage() + "\n";
        }
    }

    /**
     * 安全地提取请求ID，避免在异常处理中再次抛出异常
     */
    private Integer extractRequestIdSafely(String requestJson) {
        try {
            Map<String, Object> request = objectMapper.readValue(requestJson, Map.class);
            return (Integer) request.get("id");
        } catch (Exception e) {
            log.warn("提取请求ID失败: {}", e.getMessage());
            return -1; // 返回默认值
        }
    }

    /**
     * 发送错误响应到Python
     */
    private void sendErrorResponse(PrintWriter writer, String errorMessage, Integer requestId) {
        try {
            Map<String, Object> errorResponse = Map.of(
                    "id", requestId != null ? requestId : -1,
                    "error", errorMessage);
            String responseJson = objectMapper.writeValueAsString(errorResponse);
            writer.println("JAVA_RESPONSE:" + responseJson);
            writer.flush();
        } catch (Exception ex) {
            log.error("发送错误响应失败", ex);
        }
    }

    /**
     * 调用对应的Java函数
     *
     * @@functionCase
     */
    private Object callJavaFunction(String functionName, List<Object> args, String userId) {
        switch (functionName) {
            case "report_step":
                log.info("报告步骤: {}", args.get(0));
                return null;

            case "report_progress":
                // 这里可以实现更细粒度的进度报告
                log.info("报告进度: {}", args.get(0));
                return null;

            case "gen_sql":
                return genSQL(args, userId);

            case "exec_sql":
                Object execResult = functionUtil.executeSQL((String) args.get(0));
                // 检测SQL执行结果是否为空
                if (execResult == null) {
                    throw DataAccessException.emptyQueryResult("SQL查询返回null结果");
                }
                if (execResult instanceof List) {
                    List<?> resultList = (List<?>) execResult;
                }
                return execResult;

            default:
                throw new IllegalArgumentException("未知的函数: " + functionName);
        }
    }

    @Nullable
    private String genSQL(List<Object> args, String userId) {
        String pythonCode = bufferUtil.getPythonCode(userId);
        String historyStr = bufferUtil.getField(userId, "historyStr");
        String question = bufferUtil.getField(userId, "question");
        String tables = bufferUtil.getField(userId, "tables");
        String sqlResult = functionUtil.genSQLCAICT((String) args.get(0), (String) args.get(1), pythonCode,
                historyStr, question, tables);

        // 收集SQL结果用于测试
        log.info("🚀 [gen_sql] 即将调用collectSQLForTest, sqlResult: {}",
                sqlResult != null ? sqlResult.substring(0, Math.min(sqlResult.length(), 50)) + "..." : "null");

        return sqlResult;
    }


    /**
     * 清理临时目录
     */
    private void cleanupTempDirectory(Path tempDir) {
        if (tempDir != null) {
            try {
                Files.walk(tempDir)
                        .sorted((a, b) -> -a.compareTo(b))
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (IOException e) {
                                log.warn("删除临时文件失败: {}", path, e);
                            }
                        });
            } catch (IOException e) {
                log.warn("清理临时目录失败: {}", tempDir, e);
            }
        }
    }

    /**
     * 代码结构分析类
     */
    private static class CodeStructure {
        public boolean hasMainFunction = false;
        public boolean hasTopLevelCode = false;
        public boolean hasClassDefinitions = false;
        public boolean hasComplexStructure = false;
        public List<String> functionNames = new ArrayList<>();
        public List<String> imports = new ArrayList<>();

        /**
         * 判断是否需要使用灵活执行模式
         */
        public boolean requiresFlexibleExecution() {
            // 如果有以下特征之一，使用灵活执行：
            // 1. 有main函数
            // 2. 有顶级执行代码
            // 3. 有类定义
            // 4. 有复杂结构（如装饰器、异步函数等）
            return hasMainFunction || hasTopLevelCode || hasClassDefinitions || hasComplexStructure;
        }

        @Override
        public String toString() {
            return String.format("CodeStructure{main=%s, topLevel=%s, classes=%s, complex=%s, functions=%s}",
                    hasMainFunction, hasTopLevelCode, hasClassDefinitions, hasComplexStructure, functionNames);
        }
    }

    /**
     * 分析代码结构
     */
    private CodeStructure analyzeCodeStructure(String userPythonCode) {
        CodeStructure structure = new CodeStructure();

        if (userPythonCode == null || userPythonCode.trim().isEmpty()) {
            return structure;
        }

        String[] lines = userPythonCode.split("\n");
        boolean inMultilineString = false;
        String multilineStringDelimiter = null;

        for (String line : lines) {
            String trimmed = line.trim();

            // 跳过空行
            if (trimmed.isEmpty()) {
                continue;
            }

            // 处理多行字符串
            if (inMultilineString) {
                if (trimmed.contains(multilineStringDelimiter)) {
                    inMultilineString = false;
                    multilineStringDelimiter = null;
                }
                continue;
            }

            // 检测多行字符串开始
            if (trimmed.contains("\"\"\"") || trimmed.contains("'''")) {
                if (trimmed.contains("\"\"\"")) {
                    multilineStringDelimiter = "\"\"\"";
                } else {
                    multilineStringDelimiter = "'''";
                }
                // 检查是否在同一行结束
                if (trimmed.indexOf(multilineStringDelimiter) != trimmed.lastIndexOf(multilineStringDelimiter)) {
                    // 在同一行开始和结束
                    continue;
                } else {
                    inMultilineString = true;
                    continue;
                }
            }

            // 跳过注释行
            if (trimmed.startsWith("#")) {
                continue;
            }

            // 检测导入语句
            if (trimmed.startsWith("import ") || trimmed.startsWith("from ")) {
                structure.imports.add(trimmed);
                continue;
            }

            // 检测函数定义
            if (trimmed.startsWith("def ")) {
                String functionName = extractFunctionName(trimmed);
                if (functionName != null) {
                    structure.functionNames.add(functionName);
                    if ("main".equals(functionName)) {
                        structure.hasMainFunction = true;
                        log.info("🔍 [代码分析] 检测到main函数");
                    }
                }

                // 检测复杂结构
                if (trimmed.contains("@") || trimmed.contains("async ") || trimmed.contains("yield")) {
                    structure.hasComplexStructure = true;
                }
                continue;
            }

            // 检测类定义
            if (trimmed.startsWith("class ")) {
                structure.hasClassDefinitions = true;
                log.info("🔍 [代码分析] 检测到类定义");
                continue;
            }

            // 检测装饰器
            if (trimmed.startsWith("@")) {
                structure.hasComplexStructure = true;
                continue;
            }

            // 检测其他复杂结构
            if (trimmed.contains("async ") || trimmed.contains("await ") ||
                    trimmed.contains("yield ") || trimmed.contains("lambda ")) {
                structure.hasComplexStructure = true;
            }

            // 检测顶级执行代码（不是函数或类定义的代码）
            if (!trimmed.startsWith("def ") && !trimmed.startsWith("class ") &&
                    !trimmed.startsWith("@") && !isVariableAssignment(trimmed)) {
                structure.hasTopLevelCode = true;
                log.info("🔍 [代码分析] 检测到顶级执行代码: {}", trimmed.length() > 50 ? trimmed.substring(0, 50) + "..." : trimmed);
            }
        }

        log.info("📊 [代码分析] 分析结果: {}", structure);
        return structure;
    }

    /**
     * 判断是否为简单的变量赋值
     */
    private boolean isVariableAssignment(String line) {
        // 简单检测变量赋值：变量名 = 值
        return line.matches("^[a-zA-Z_][a-zA-Z0-9_]*\\s*=\\s*.+$") &&
                !line.contains("(") && !line.contains("[") && !line.contains("{");
    }

    /**
     * 转义用户代码中的特殊字符
     */
    private String escapeUserCode(String userCode) {
        if (userCode == null) {
            return "";
        }

        // 转义三引号以避免字符串模板冲突
        return userCode.replace("'''", "\\'\\'\\'");
    }
}
