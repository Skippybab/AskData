package com.mt.agent.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mt.agent.buffer.util.BufferUtil;
import com.mt.agent.model.exception.DataAccessException;
import com.mt.agent.model.exception.ExecutionFailureException;
import com.mt.agent.reporter.SubEventReporter;
import com.mt.agent.service.TestDataCollectorCallback;
import com.mt.agent.sysUtil.FunctionUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.nio.charset.StandardCharsets;
import jakarta.annotation.PostConstruct;
import java.util.HashSet;
import java.util.Set;

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
public class PythonDirectExecutorService {

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
        this.objectMapper.getFactory().configure(
                com.fasterxml.jackson.core.JsonGenerator.Feature.ESCAPE_NON_ASCII, false);
    }

    /**
     * 执行Python代码
     *
     * @param pythonCode Python代码字符串
     * @param paramMap   参数映射
     * @param reporter   流式报告器
     */
    public void executePythonCode(String pythonCode, HashMap<String, Object> paramMap,
            SubEventReporter reporter, String userId) {
        Path tempDir = null;
        Process pythonProcess = null;
        pythonErrorOutput = ""; // 重置错误输出

        try {
            reporter.reportStep("开始准备Python执行环境\n");

            // 1. 创建临时执行环境
            tempDir = createPythonEnvironment(paramMap);

            // 2. 生成完整的main.py文件
            createMainPythonFile(tempDir, pythonCode);

            reporter.reportStep("启动Python进程执行代码\n");

            // 3. 启动Python进程
            pythonProcess = startPythonProcess(tempDir);

            // 4. 处理Python进程的输入输出
            handlePythonExecution(pythonProcess, reporter, userId);

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

            reporter.reportStep("Python代码执行完成\n");

        } catch (DataAccessException | ExecutionFailureException e) {
            // 已经是分类异常，直接重新抛出
            log.error("Python代码执行失败: {}", e.getMessage(), e);
            reporter.reportStep("执行失败: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            // 未分类的异常，进行分析
            log.error("Python代码执行失败", e);
            reporter.reportStep("执行失败: " + e.getMessage());
            throw analyzeGenericException(e, pythonCode);
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
    private RuntimeException analyzeGenericException(Exception e, String pythonCode) {
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

                # SQL生成函数
                def gen_sql(query_text: str, table_name: str) -> str:
                    '''基于文本描述的查询条件，生成sql代码'''
                    return bridge.call_java_function('gen_sql', query_text, table_name)
                
                def exec_sql(sql_code: str) -> List[Dict[str, Any]]:
                    '''输入可执行的SQL代码，返回SQL查询结果'''
                    return bridge.call_java_function('exec_sql', sql_code)

                def steps_summary(summary_title: str) -> str:
                    '''总结执行情况：自动获取行动计划的执行情况，输出总结文本'''
                    return bridge.call_java_function('steps_summary', summary_title)

                # 可视化函数
                def vis_textbox(content: str) -> None:
                    '''输入文本内容，在前端对话界面渲染1个文本框'''
                    bridge.call_java_function('vis_textbox', content)

                def vis_textblock(title: str, value: float) -> None:
                    '''输入标题和数值，在前端对话界面渲染1个指标信息块'''
                    bridge.call_java_function('vis_textblock', title, value)

                def vis_single_bar(title: str, x_labels: List[str], y_data: List[float]) -> None:
                    '''输入标题、X轴标签列表和Y轴数据列表，在前端对话界面渲染1个单柱状图'''
                    bridge.call_java_function('vis_single_bar', title, x_labels, y_data)

                def vis_clustered_bar(title: str, x_labels: List[str], bar_a_label: str, bar_b_label: str,
                                    group_a: List[float], group_b: List[float]) -> None:
                    '''输入标题、X轴标签列表，a、b两组数据的标签和数据，在前端对话界面渲染1个二分组柱状图'''
                    bridge.call_java_function('vis_clustered_bar', title, x_labels, bar_a_label, bar_b_label, group_a, group_b)

                def vis_pie_chart(title: str, labels: List[str], data: List[float]) -> None:
                    '''输入标题、标签列表和数据列表，在前端对话界面渲染1个饼状图'''
                    bridge.call_java_function('vis_pie_chart', title, labels, data)

                def vis_table(title: str, data: List[Dict[str, Any]]) -> None:
                    '''输入表格标题和表格数据，在前端对话界面渲染1个二维表格'''
                    bridge.call_java_function('vis_table', title, data)
                """;

        Files.writeString(tempDir.resolve("system_functions.py"), systemFunctionsCode, StandardCharsets.UTF_8);
    }

    /**
     * 生成主执行文件
     */
    private void createMainPythonFile(Path tempDir, String userPythonCode) throws IOException {
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
        log.info("代码：" + mainCode);

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
    private void handlePythonExecution(Process pythonProcess, SubEventReporter reporter, String userId) {
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
                    handlePythonOutput(line, writer, reporter, userId);
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
    private void handlePythonOutput(String line, PrintWriter writer, SubEventReporter reporter, String userId) {
        try {
            if (line.startsWith("JAVA_REQUEST:")) {
                // 处理Java函数调用请求
                handleJavaFunctionCall(line.substring(13), writer, reporter, userId);
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
    private void handleJavaFunctionCall(String requestJson, PrintWriter writer, SubEventReporter reporter,
            String userId) {
        try {
            Map<String, Object> request = objectMapper.readValue(requestJson, Map.class);
            String functionName = (String) request.get("function");
            List<Object> args = (List<Object>) request.get("args");
            Integer requestId = (Integer) request.get("id");

            // 调用对应的Java函数
            Object result = callJavaFunction(functionName, args, reporter, userId);

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
     * @@functionCase
     */
    private Object callJavaFunction(String functionName, List<Object> args, SubEventReporter reporter, String userId) {
        switch (functionName) {
            case "report_step":
                reporter.reportStep((String) args.get(0));
                // reporter.reportStepResult(new StepResultData());
                return null;

            case "report_progress":
                // 这里可以实现更细粒度的进度报告
                reporter.reportStep((String) args.get(0));
                return null;

            case "gen_sql":
//                return genSQLV14(args, userId);
                return genSQLCAICT(args, userId);

            case "exec_sql":
                Object execResult = functionUtil.executeSQL((String) args.get(0));
                // 检测SQL执行结果是否为空
                if (execResult == null) {
                    throw DataAccessException.emptyQueryResult("SQL查询返回null结果");
                }
                if (execResult instanceof List) {
                    List<?> resultList = (List<?>) execResult;
//                    if (resultList.isEmpty()) {
//                        throw DataAccessException.emptyQueryResult("SQL查询返回空结果集");
//                    }
                }
                return execResult;

            case "explain_sys_func":
                return functionUtil.sysQueryAnswer((String) args.get(0));

            case "vis_textbox":
                functionUtil.visTextBox((String) args.get(0), reporter);
                // 收集可视化结果用于测试
                collectVisualizationForTest("vis_textbox", createTextBoxResult((String) args.get(0)));
                return null;

            case "steps_summary":
                return functionUtil.stepSummary((String) args.get(0));

            case "vis_textblock":
                // 支持数字和字符串两种类型的值参数
                Object valueArg = args.get(1);
                log.info("valueArg: {}", valueArg);
                if (valueArg instanceof Number) {
                    functionUtil.visTextBlock((String) args.get(0), ((Number) valueArg).doubleValue(), reporter);
                    // 收集可视化结果用于测试
                    collectVisualizationForTest("vis_textblock",
                            createIndicatorBlockResult((String) args.get(0), ((Number) valueArg).doubleValue()));
                } else {
                    // 如果是字符串，尝试解析为数字，如果解析失败则使用特殊的字符串显示方法
                    String valueStr = valueArg.toString();
                    try {
                        // 尝试解析为纯数字
                        double numValue = Double.parseDouble(valueStr);
                        functionUtil.visTextBlock((String) args.get(0), numValue, reporter);
                        // 收集可视化结果用于测试
                        collectVisualizationForTest("vis_textblock",
                                createIndicatorBlockResult((String) args.get(0), numValue));
                    } catch (NumberFormatException e) {
                        // 如果无法解析为数字，使用文本框显示
                        functionUtil.visTextBox((String) args.get(0) + ": " + valueStr, reporter);
                        // 收集可视化结果用于测试
                        collectVisualizationForTest("vis_textbox",
                                createTextBoxResult((String) args.get(0) + ": " + valueStr));
                    }
                }
                return null;

            case "vis_single_bar":
                List<String> xLabels = (List<String>) args.get(1);
                List<Double> yData = convertToDoubleList(args.get(2));
                functionUtil.visSingleBar((String) args.get(0), xLabels, yData, reporter);
                // 收集可视化结果用于测试
                collectVisualizationForTest("vis_single_bar",
                        createBarChartResult((String) args.get(0), xLabels, yData));
                return null;

            case "vis_clustered_bar":
                List<String> clusteredXLabels = (List<String>) args.get(1);
                List<Double> barAData = convertToDoubleList(args.get(4));
                List<Double> barBData = convertToDoubleList(args.get(5));
                functionUtil.visClusteredBar((String) args.get(0),
                        clusteredXLabels,
                        (String) args.get(2),
                        (String) args.get(3),
                        barAData,
                        barBData,
                        reporter);
                // 收集可视化结果用于测试
                collectVisualizationForTest("vis_clustered_bar", createClusteredBarChartResult((String) args.get(0),
                        clusteredXLabels, (String) args.get(2), (String) args.get(3), barAData, barBData));
                return null;

            case "vis_pie_chart":
                List<String> pieLabels = (List<String>) args.get(1);
                List<Double> pieData = convertToDoubleList(args.get(2));
                functionUtil.visPieChart((String) args.get(0), pieLabels, pieData, reporter);
                // 收集可视化结果用于测试
                collectVisualizationForTest("vis_pie_chart", createPieChartResult((String) args.get(0)));
                return null;

            case "vis_table":
                functionUtil.visTable((String) args.get(0),
                        (List<Map<String, Object>>) args.get(1), reporter);
                // 收集可视化结果用于测试
                collectVisualizationForTest("vis_table", createTableResult((String) args.get(0)));
                return null;

            default:
                throw new IllegalArgumentException("未知的函数: " + functionName);
        }
    }

    @Nullable
    private String genSQLCAICT(List<Object> args, String userId) {
        String pythonCode = bufferUtil.getPythonCode(userId);
        String historyStr = bufferUtil.getField(userId, "historyStr");
        String question = bufferUtil.getField(userId, "question");
        String tables = bufferUtil.getField(userId, "tables");
        String sqlResult = functionUtil.genSQLCAICT((String) args.get(0), (String) args.get(1), pythonCode,
                historyStr, question, tables);

        // 收集SQL结果用于测试
        log.info("🚀 [gen_sql] 即将调用collectSQLForTest, sqlResult: {}",
                sqlResult != null ? sqlResult.substring(0, Math.min(sqlResult.length(), 50)) + "..." : "null");
        collectSQLForTest(sqlResult);
        return sqlResult;
    }
    @Nullable
    private String genSQLCAICTSilicon(List<Object> args, String userId) {
        String pythonCode = bufferUtil.getPythonCode(userId);
        String historyStr = bufferUtil.getField(userId, "historyStr");
        String question = bufferUtil.getField(userId, "question");
        String tables = bufferUtil.getField(userId, "tables");
        String sqlResult = functionUtil.genSQLCAICTSilicon((String) args.get(0), (String) args.get(1), pythonCode,
                historyStr, question, tables);

        // 收集SQL结果用于测试
        log.info("🚀 [gen_sql] 即将调用collectSQLForTest, sqlResult: {}",
                sqlResult != null ? sqlResult.substring(0, Math.min(sqlResult.length(), 50)) + "..." : "null");
        collectSQLForTest(sqlResult);
        return sqlResult;
    }

    @Nullable
    private String genSQLV14(List<Object> args, String userId) {
        String pythonCode = bufferUtil.getPythonCode(userId);
        String historyStr = bufferUtil.getField(userId, "historyStr");
        String question = bufferUtil.getField(userId, "question");
        String sqlResult = functionUtil.genSQLV14((String) args.get(0), (String) args.get(1), pythonCode,
                historyStr, question);

        // 收集SQL结果用于测试
        log.info("🚀 [gen_sql] 即将调用collectSQLForTest, sqlResult: {}",
                sqlResult != null ? sqlResult.substring(0, Math.min(sqlResult.length(), 50)) + "..." : "null");
        collectSQLForTest(sqlResult);
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
     * 生成步骤总结
     */
    private String generateStepsSummary(List<String> stepStatus, String summaryTitle) {
        StringBuilder summary = new StringBuilder();
        summary.append(summaryTitle).append("\n");
        summary.append("=".repeat(summaryTitle.length())).append("\n\n");

        int successCount = 0;
        int totalCount = stepStatus.size();

        for (int i = 0; i < stepStatus.size(); i++) {
            String status = stepStatus.get(i);
            summary.append(String.format("%d. %s\n", i + 1, status));
            if (status.toLowerCase().contains("success") || status.toLowerCase().contains("成功")) {
                successCount++;
            }
        }

        summary.append(String.format("\n总结: %d/%d 步骤成功完成", successCount, totalCount));

        return summary.toString();
    }

    /**
     * 收集SQL结果用于测试
     */
    private void collectSQLForTest(String sql) {
        log.info("🔧 [数据收集] collectSQLForTest被调用, SQL: {}",
                sql != null ? sql.substring(0, Math.min(sql.length(), 50)) + "..." : "null");

        // 使用静态回调机制收集SQL
        TestDataCollectorCallback.collectSQL(sql);
    }

    /**
     * 收集可视化结果用于测试
     */
    private void collectVisualizationForTest(String functionName, Object result) {
        log.info("🔧 [数据收集] collectVisualizationForTest被调用, function: {}, result: {}",
                functionName, result);

        // 使用静态回调机制收集可视化结果
        TestDataCollectorCallback.collectVisualization(functionName, result);
    }

    /**
     * 创建文本框结果对象
     */
    private Map<String, Object> createTextBoxResult(String content) {
        Map<String, Object> result = new HashMap<>();
        result.put("type", "MDText");
        result.put("content", content);
        return result;
    }

    /**
     * 创建指标信息块结果对象
     */
    private Map<String, Object> createIndicatorBlockResult(String label, double value) {
        Map<String, Object> result = new HashMap<>();
        result.put("type", "IndicatorBlock");
        result.put("label", label);
        result.put("value", value);
        result.put("unit", "");
        return result;
    }

    /**
     * 创建柱状图结果对象
     */
    private Map<String, Object> createBarChartResult(String title, List<String> tags, List<Double> value) {
        Map<String, Object> result = new HashMap<>();
        result.put("type", "BarChart");
        result.put("title", title);
        result.put("tags", tags != null ? tags : new ArrayList<>());
        result.put("value", value != null ? value : new ArrayList<>());
        result.put("unit", "");
        return result;
    }

    /**
     * 创建二分组柱状图结果对象
     */
    private Map<String, Object> createClusteredBarChartResult(String title, List<String> tags,
            String barLabel1, String barLabel2, List<Double> barValue1, List<Double> barValue2) {
        Map<String, Object> result = new HashMap<>();
        result.put("type", "ClusteredBarChart");
        result.put("title", title);
        result.put("tags", tags != null ? tags : new ArrayList<>());
        result.put("barLabel1", barLabel1 != null ? barLabel1 : "");
        result.put("barLabel2", barLabel2 != null ? barLabel2 : "");
        result.put("barValue1", barValue1 != null ? barValue1 : new ArrayList<>());
        result.put("barValue2", barValue2 != null ? barValue2 : new ArrayList<>());
        result.put("unit", "");
        return result;
    }

    /**
     * 创建饼图结果对象
     */
    private Map<String, Object> createPieChartResult(String title) {
        Map<String, Object> result = new HashMap<>();
        result.put("type", "PieChart");
        result.put("title", title);
        return result;
    }

    /**
     * 创建表格结果对象
     */
    private Map<String, Object> createTableResult(String title) {
        Map<String, Object> result = new HashMap<>();
        result.put("type", "Table");
        result.put("title", title);
        return result;
    }

    /**
     * 安全地将Object转换为List<Double>
     * 支持各种数字类型的List：Integer, Long, Float, Double等
     */
    private List<Double> convertToDoubleList(Object obj) {
        if (obj == null) {
            return new ArrayList<>();
        }

        if (!(obj instanceof List)) {
            log.warn("⚠️ [类型转换] 期望List类型，实际类型: {}", obj.getClass().getSimpleName());
            return new ArrayList<>();
        }

        List<?> sourceList = (List<?>) obj;
        List<Double> result = new ArrayList<>();

        for (Object item : sourceList) {
            if (item == null) {
                result.add(0.0);
                continue;
            }

            try {
                if (item instanceof Number) {
                    // 所有Number子类都可以安全转换为Double
                    result.add(((Number) item).doubleValue());
                } else if (item instanceof String) {
                    // 尝试解析字符串为数字
                    result.add(Double.parseDouble((String) item));
                } else {
                    // 其他类型，尝试转换为字符串再解析
                    result.add(Double.parseDouble(item.toString()));
                }
            } catch (NumberFormatException e) {
                log.warn("⚠️ [类型转换] 无法将 '{}' (类型: {}) 转换为Double，使用默认值0.0",
                        item, item.getClass().getSimpleName());
                result.add(0.0);
            }
        }

        log.info("✅ [类型转换] 成功转换 {} 个元素到Double列表", result.size());
        return result;
    }

}
