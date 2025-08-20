package com.mt.agent.workflow.api.service.impl;

import com.mt.agent.workflow.api.dto.PythonExecutionResult;
import com.mt.agent.workflow.api.entity.ChatMessage;
import com.mt.agent.workflow.api.entity.DbConfig;
import com.mt.agent.workflow.api.exception.ExecutionFailureException;
import com.mt.agent.workflow.api.infra.python.DatabaseConnectionInjector;
import com.mt.agent.workflow.api.mapper.ChatMessageMapper;
import com.mt.agent.workflow.api.service.DbConfigService;
import com.mt.agent.workflow.api.service.PythonExecutorService;
import com.mt.agent.workflow.api.service.PythonResultCollector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.Comparator;
import java.util.HashMap;
import com.mt.agent.workflow.api.service.impl.SubEventReporter;
import java.util.HashSet;
import java.util.Set;
import java.util.Map;
import java.util.List;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mt.agent.workflow.api.util.FunctionUtil;
import com.mt.agent.workflow.api.util.BufferUtil;

/**
 * 新一代Python代码执行服务实现
 * 核心职责: 接收Python代码，准备隔离的执行环境，注入数据库连接，执行并返回结果。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PythonExecutorServiceImpl implements PythonExecutorService {
    
    private final DatabaseConnectionInjector connectionInjector;
    private final PythonResultCollector resultCollector;
    private final ChatMessageMapper chatMessageMapper;
    private final DbConfigService dbConfigService;
    private final ObjectMapper objectMapper;
    private final FunctionUtil functionUtil;
    private final BufferUtil bufferUtil;

    @Value("${python.executor.path:python}")
    private String pythonExecutablePath;

    @Value("${python.executor.timeout:300}")
    private int executionTimeoutSeconds;

    @Value("${python.executor.temp-dir:./temp/py_exec}")
    private String tempDirBase;
    
    @Override
    public PythonExecutionResult executePythonCodeWithResult(Long messageId, Long dbConfigId) {
        log.info("🐍 [Python执行器] 开始执行Python代码, messageId: {}, dbConfigId: {}", messageId, dbConfigId);
        
        ChatMessage chatMessage = chatMessageMapper.selectById(messageId);
        if (chatMessage == null || chatMessage.getPythonCode() == null || chatMessage.getPythonCode().trim().isEmpty()) {
            log.error("🐍 [Python执行器] 未找到消息或Python代码为空, messageId: {}", messageId);
            return PythonExecutionResult.failure("未找到消息或Python代码为空", "INVALID_INPUT");
        }

        log.info("🐍 [Python执行器] 获取到ChatMessage, userId: {}, pythonCode长度: {}", 
                chatMessage.getUserId(), chatMessage.getPythonCode().length());
        log.debug("🐍 [Python执行器] Python代码内容: {}", chatMessage.getPythonCode());

        // 检查userId是否为空
        if (chatMessage.getUserId() == null) {
            log.error("🐍 [Python执行器] ChatMessage的userId为空，messageId: {}", messageId);
            return PythonExecutionResult.failure("用户ID为空，无法执行Python代码", "INVALID_USER");
        }

        // 使用消息中的userId来获取数据库配置
        log.info("🐍 [Python执行器] 获取数据库配置, userId: {}, dbConfigId: {}", chatMessage.getUserId(), dbConfigId);
        DbConfig dbConfig = dbConfigService.getById(chatMessage.getUserId(), dbConfigId);
        if (dbConfig == null) {
            log.error("🐍 [Python执行器] 未找到数据库配置, userId: {}, dbConfigId: {}", chatMessage.getUserId(), dbConfigId);
            return PythonExecutionResult.failure("未找到数据库配置", "INVALID_CONFIG");
        }
        log.info("🐍 [Python执行器] 数据库配置获取成功, 数据库: {}:{}/{}", dbConfig.getHost(), dbConfig.getPort(), dbConfig.getDatabaseName());

        // 将dbConfigId设置到BufferUtil中，供FunctionUtil使用
        String userId = String.valueOf(chatMessage.getUserId());
        bufferUtil.setField(userId, "dbConfigId", dbConfigId);
        log.info("🐍 [Python执行器] 设置用户{}的dbConfigId: {}", userId, dbConfigId);

        Path tempDir = null;
        Process pythonProcess = null;
        PythonExecutionResult result = null;
        
        try {
            // 创建临时目录
            tempDir = Files.createDirectories(Path.of(tempDirBase, String.valueOf(messageId)));
            log.info("🐍 [Python执行器] 创建临时目录: {}", tempDir);

            // 准备执行环境
            log.info("🐍 [Python执行器] 开始准备执行环境");
            prepareExecutionEnvironment(tempDir, chatMessage.getPythonCode(), dbConfig);
            log.info("🐍 [Python执行器] 执行环境准备完成");

            // 启动Python进程
            log.info("🐍 [Python执行器] 启动Python进程");
            pythonProcess = startPythonProcess(tempDir);
            log.info("🐍 [Python执行器] Python进程启动成功, PID: {}", getProcessId(pythonProcess));

            // 处理Python进程执行并收集结果
            log.info("🐍 [Python执行器] 开始处理Python进程执行");
            result = handlePythonExecutionAndCollectResult(pythonProcess, chatMessage);
            log.info("🐍 [Python执行器] Python进程执行处理完成, 成功: {}", result.isSuccess());

            // 等待执行完成
            log.info("🐍 [Python执行器] 等待Python进程完成, 超时时间: {}秒", executionTimeoutSeconds);
            boolean finished = pythonProcess.waitFor(executionTimeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                log.error("🐍 [Python执行器] Python代码执行超时, messageId: {}", messageId);
                pythonProcess.destroyForcibly();
                return PythonExecutionResult.failure("Python代码执行超时(" + executionTimeoutSeconds + "秒)", "TIMEOUT");
            }

            int exitCode = pythonProcess.exitValue();
            log.info("🐍 [Python执行器] Python进程退出, exitCode: {}", exitCode);
            
            if (exitCode != 0) {
                log.error("🐍 [Python执行器] Python代码执行失败, messageId: {}, exit code: {}", messageId, exitCode);
                return analyzePythonExecutionError(exitCode, chatMessage.getPythonCode());
            }

            // 返回收集到的结果
            log.info("🐍 [Python执行器] Python执行成功, 结果长度: {}", 
                    result.getData() != null ? result.getData().length() : 0);
            log.debug("🐍 [Python执行器] 执行结果: {}", result.getData());
            return result != null ? result : PythonExecutionResult.failure("未能收集到执行结果", "COLLECTION_ERROR");

        } catch (IOException e) {
            log.error("🐍 [Python执行器] Python执行环境准备失败, messageId: {}", messageId, e);
            return PythonExecutionResult.failure("执行环境准备失败: " + e.getMessage(), "IO_ERROR");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("🐍 [Python执行器] Python执行线程被中断, messageId: {}", messageId, e);
            return PythonExecutionResult.failure("执行被中断", "INTERRUPTED");
        } catch (Exception e) {
            log.error("🐍 [Python执行器] Python执行过程中发生未知错误, messageId: {}", messageId, e);
            return analyzeGenericException(e, chatMessage.getPythonCode());
        } finally {
            // 清理资源
            if (pythonProcess != null && pythonProcess.isAlive()) {
                log.info("🐍 [Python执行器] 强制终止Python进程");
                pythonProcess.destroyForcibly();
            }
            if (tempDir != null) {
                log.info("🐍 [Python执行器] 清理临时目录: {}", tempDir);
                cleanup(tempDir);
            }
            // 清理缓冲区中的dbConfigId
            try {
                bufferUtil.setField(userId, "dbConfigId", null);
                log.info("🐍 [Python执行器] 清理用户{}的dbConfigId", userId);
            } catch (Exception e) {
                log.warn("🐍 [Python执行器] 清理用户{}的dbConfigId失败: {}", userId, e.getMessage());
            }
        }
    }

    /**
     * 分析Python执行错误
     */
    private PythonExecutionResult analyzePythonExecutionError(int exitCode, String pythonCode) {
        String errorType = "EXECUTION_ERROR";
        String errorMessage = "Python代码执行失败，退出码: " + exitCode;

        // 根据退出码和可能的错误信息进行分类
        if (exitCode == 1) {
            errorType = "RUNTIME_ERROR";
            errorMessage = "Python代码运行时错误";
        } else if (exitCode == 2) {
            errorType = "SYNTAX_ERROR";
            errorMessage = "Python代码语法错误";
        } else if (exitCode == 126) {
            errorType = "PERMISSION_ERROR";
            errorMessage = "Python执行权限错误";
        } else if (exitCode == 127) {
            errorType = "COMMAND_NOT_FOUND";
            errorMessage = "Python解释器未找到";
        }

        return PythonExecutionResult.failure(errorMessage, errorType);
    }

    /**
     * 分析通用异常
     */
    private PythonExecutionResult analyzeGenericException(Exception e, String pythonCode) {
        String errorType = "UNKNOWN_ERROR";
        String errorMessage = e.getMessage();

        // 根据异常类型进行分类
        if (e instanceof SecurityException) {
            errorType = "SECURITY_ERROR";
            errorMessage = "安全限制错误: " + e.getMessage();
        } else if (e instanceof IllegalArgumentException) {
            errorType = "INVALID_ARGUMENT";
            errorMessage = "参数错误: " + e.getMessage();
        } else if (e instanceof RuntimeException) {
            errorType = "RUNTIME_ERROR";
            errorMessage = "运行时错误: " + e.getMessage();
        }

        return PythonExecutionResult.failure(errorMessage, errorType);
    }

    private void prepareExecutionEnvironment(Path tempDir, String userPythonCode, DbConfig dbConfig) throws IOException {
        log.info("🐍 [Python执行器] 开始准备执行环境, 临时目录: {}", tempDir);
        log.debug("🐍 [Python执行器] 用户Python代码长度: {}", userPythonCode.length());
        
        // 创建数据库配置文件
        log.info("🐍 [Python执行器] 创建数据库配置文件");
        connectionInjector.createDbConfigProperties(dbConfig, tempDir);

        // 创建Java Bridge模块
        log.info("🐍 [Python执行器] 创建Java Bridge模块");
        createJavaBridge(tempDir);

        // 创建系统函数模块
        log.info("🐍 [Python执行器] 创建系统函数模块");
        createSystemFunctions(tempDir);

        // 复制数据库执行器
        log.info("🐍 [Python执行器] 复制数据库执行器");
        try (InputStream scriptStream = getClass().getResourceAsStream("/python/db_executor.py")) {
            if (scriptStream == null) {
                log.error("🐍 [Python执行器] 无法找到资源文件: /python/db_executor.py");
                throw new IOException("无法找到资源文件: /python/db_executor.py");
            }
            Files.copy(scriptStream, tempDir.resolve("db_executor.py"), StandardCopyOption.REPLACE_EXISTING);
            log.info("🐍 [Python执行器] 数据库执行器复制成功");
        }

        // 生成main.py文件
        log.info("🐍 [Python执行器] 生成main.py文件");
        String mainPyContent = buildMainPyContent(userPythonCode);
        Files.writeString(tempDir.resolve("main.py"), mainPyContent);
        log.info("🐍 [Python执行器] main.py文件生成成功, 长度: {}", mainPyContent.length());
        log.debug("🐍 [Python执行器] main.py内容: {}", mainPyContent);
        
        log.info("🐍 [Python执行器] 执行环境准备完成");
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
                import json

                # SQL生成函数
                def gen_sql(query_text: str, table_name: str) -> str:
                    '''基于文本描述的查询条件，生成sql代码'''
                    try:
                        result = bridge.call_java_function('gen_sql', query_text, table_name)
                        print(f"INFO: 生成的SQL: {result}")
                        return result
                    except Exception as e:
                        print(f"ERROR: SQL生成失败: {e}")
                        raise e
                
                def exec_sql(sql_code: str) -> List[Dict[str, Any]]:
                    '''输入可执行的SQL代码，返回SQL查询结果'''
                    try:
                        print(f"INFO: 执行SQL: {sql_code}")
                        result = bridge.call_java_function('exec_sql', sql_code)
                        print(f"INFO: SQL执行成功，返回 {len(result) if isinstance(result, list) else '未知'} 条记录")
                        return result
                    except Exception as e:
                        print(f"ERROR: SQL执行失败: {e}")
                        raise e

                def steps_summary(summary_title: str) -> str:
                    '''总结执行情况：自动获取行动计划的执行情况，输出总结文本'''
                    try:
                        result = bridge.call_java_function('steps_summary', summary_title)
                        print(f"INFO: 步骤总结: {result}")
                        return result
                    except Exception as e:
                        print(f"ERROR: 步骤总结失败: {e}")
                        raise e

                # 可视化函数
                def vis_textbox(content: str) -> None:
                    '''输入文本内容，在前端对话界面渲染1个文本框'''
                    try:
                        print(f"INFO: 创建文本框: {content[:50]}...")
                        bridge.call_java_function('vis_textbox', content)
                        print("INFO: 文本框创建成功")
                    except Exception as e:
                        print(f"ERROR: 文本框创建失败: {e}")
                        raise e

                def vis_textblock(title: str, value: float) -> None:
                    '''输入标题和数值，在前端对话界面渲染1个指标信息块'''
                    try:
                        print(f"INFO: 创建指标块: {title} = {value}")
                        bridge.call_java_function('vis_textblock', title, value)
                        print("INFO: 指标块创建成功")
                    except Exception as e:
                        print(f"ERROR: 指标块创建失败: {e}")
                        raise e

                def vis_single_bar(title: str, x_labels: List[str], y_data: List[float]) -> None:
                    '''输入标题、X轴标签列表和Y轴数据列表，在前端对话界面渲染1个单柱状图'''
                    try:
                        print(f"INFO: 创建柱状图: {title}, 数据点: {len(x_labels)}")
                        bridge.call_java_function('vis_single_bar', title, x_labels, y_data)
                        print("INFO: 柱状图创建成功")
                    except Exception as e:
                        print(f"ERROR: 柱状图创建失败: {e}")
                        raise e

                def vis_clustered_bar(title: str, x_labels: List[str], bar_a_label: str, bar_b_label: str,
                                    group_a: List[float], group_b: List[float]) -> None:
                    '''输入标题、X轴标签列表，a、b两组数据的标签和数据，在前端对话界面渲染1个二分组柱状图'''
                    try:
                        print(f"INFO: 创建分组柱状图: {title}")
                        bridge.call_java_function('vis_clustered_bar', title, x_labels, bar_a_label, bar_b_label, group_a, group_b)
                        print("INFO: 分组柱状图创建成功")
                    except Exception as e:
                        print(f"ERROR: 分组柱状图创建失败: {e}")
                        raise e

                def vis_pie_chart(title: str, labels: List[str], data: List[float]) -> None:
                    '''输入标题、标签列表和数据列表，在前端对话界面渲染1个饼状图'''
                    try:
                        print(f"INFO: 创建饼图: {title}, 数据点: {len(labels)}")
                        bridge.call_java_function('vis_pie_chart', title, labels, data)
                        print("INFO: 饼图创建成功")
                    except Exception as e:
                        print(f"ERROR: 饼图创建失败: {e}")
                        raise e

                def vis_table(title: str, data: List[Dict[str, Any]]) -> None:
                    '''输入表格标题和表格数据，在前端对话界面渲染1个二维表格'''
                    try:
                        print(f"INFO: 创建表格: {title}, 行数: {len(data)}")
                        bridge.call_java_function('vis_table', title, data)
                        print("INFO: 表格创建成功")
                    except Exception as e:
                        print(f"ERROR: 表格创建失败: {e}")
                        raise e

                # 工具函数
                def print_json(data: Any) -> None:
                    '''格式化打印JSON数据'''
                    try:
                        json_str = json.dumps(data, ensure_ascii=False, indent=2)
                        print(f"INFO: JSON数据:\\n{json_str}")
                    except Exception as e:
                        print(f"ERROR: JSON格式化失败: {e}")

                def print_table(data: List[Dict[str, Any]], title: str = "数据表格") -> None:
                    '''格式化打印表格数据'''
                    try:
                        if not data:
                            print(f"INFO: {title} - 空数据")
                            return
                        
                        # 获取所有列名
                        columns = list(data[0].keys())
                        
                        # 打印表头
                        header = " | ".join(columns)
                        separator = "-" * len(header)
                        print(f"INFO: {title}")
                        print(f"INFO: {separator}")
                        print(f"INFO: {header}")
                        print(f"INFO: {separator}")
                        
                        # 打印数据行
                        for row in data[:10]:  # 只显示前10行
                            row_str = " | ".join(str(row.get(col, "")) for col in columns)
                            print(f"INFO: {row_str}")
                        
                        if len(data) > 10:
                            print(f"INFO: ... 还有 {len(data) - 10} 行数据")
                    except Exception as e:
                        print(f"ERROR: 表格格式化失败: {e}")
                """;

        Files.writeString(tempDir.resolve("system_functions.py"), systemFunctionsCode, StandardCharsets.UTF_8);
    }

    private String buildMainPyContent(String userPythonCode) {
        // 预处理用户代码，确保正确的缩进
        String processedUserCode = preprocessUserCode(userPythonCode);

        // 检测是否需要自动调用函数
        String functionCall = detectAndGenerateFunctionCall(userPythonCode);

        return "# -*- coding: utf-8 -*-\n"
             + "import json\n"
             + "import sys\n"
             + "import traceback\n"
             + "from java_bridge import bridge, report\n"
             + "from system_functions import *\n"
             + "from db_executor import execute_query_and_get_json, gen_sql\n\n"
             + "def main():\n"
             + "    try:\n"
             + "        bridge.report_step(\"开始执行Python代码\\n\")\n"
             + "        # 执行用户代码\n"
             + processedUserCode
             + "        # 自动调用检测到的函数\n"
             + functionCall
             + "        # 确保有最终输出\n"
             + "        if 'total_profit' in locals():\n"
             + "            print(f\"计算结果: {total_profit}\")\n"
             + "        elif 'result' in locals():\n"
             + "            # 将查询结果转换为JSON格式返回给前端\n"
             + "            if isinstance(result, list) and len(result) > 0:\n"
             + "                # 构造前端期望的JSON格式\n"
             + "                response_data = {\n"
             + "                    \"dataType\": \"python_dict_list\",\n"
             + "                    \"parsedData\": json.dumps(result, ensure_ascii=False)\n"
             + "                }\n"
             + "                print(json.dumps(response_data, ensure_ascii=False))\n"
             + "            elif result is not None:\n"
             + "                # 对于单个值的结果，也构造JSON格式\n"
             + "                response_data = {\n"
             + "                    \"dataType\": \"python_dict_list\",\n"
             + "                    \"parsedData\": json.dumps([{\"value\": result}], ensure_ascii=False)\n"
             + "                }\n"
             + "                print(json.dumps(response_data, ensure_ascii=False))\n"
             + "            else:\n"
             + "                print(f\"查询结果: {result}\")\n"
             + "        elif 'average_profit' in locals():\n"
             + "            print(f\"平均利润: {average_profit}\")\n"
             + "        bridge.report_step(\"Python代码执行完成\\n\")\n"
             + "    except Exception as e:\n"
             + "        print(f\"Execution failed: {e}\", file=sys.stderr)\n"
             + "        traceback.print_exc(file=sys.stderr)\n"
             + "        sys.exit(1)\n\n"
             + "if __name__ == '__main__':\n"
             + "    main()\n";
    }

    /**
     * 检测用户代码中的函数定义并生成相应的函数调用
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

        for (String functionName : definedFunctions) {
            if (!calledFunctions.contains(functionName)) {
                // 添加函数调用，使用正确的缩进
                functionCalls.append("        ").append(functionName).append("()\n");
                log.info("🚀 [函数调用] 为未调用的函数生成自动调用: {}()", functionName);
            }
        }

        return functionCalls.toString();
    }

    /**
     * 检测指定行是否调用了指定函数
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

    private String indentUserCode(String userCode) {
        StringBuilder indentedCode = new StringBuilder();
        for (String line : userCode.split("\r?\n")) {
            indentedCode.append("        ").append(line).append("\n");
        }
        return indentedCode.toString();
    }

    private Process startPythonProcess(Path tempDir) throws IOException {
        ProcessBuilder processBuilder = new ProcessBuilder(pythonExecutablePath, "main.py");
        processBuilder.directory(tempDir.toFile());
        processBuilder.redirectErrorStream(true);

        // 设置环境变量确保Python使用UTF-8编码
        Map<String, String> env = processBuilder.environment();
        env.put("PYTHONIOENCODING", "utf-8");

        log.info("启动Python进程于目录: {} , 命令: {}", tempDir, String.join(" ", processBuilder.command()));
        return processBuilder.start();
    }

    /**
     * 处理Python进程执行并收集结果
     */
    private PythonExecutionResult handlePythonExecutionAndCollectResult(Process pythonProcess, ChatMessage chatMessage) {
        log.info("🐍 [Python执行器] 开始处理Python进程执行和结果收集");
        
        // 获取Python进程的输入输出流，明确指定UTF-8编码
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(pythonProcess.getInputStream(), StandardCharsets.UTF_8));
        PrintWriter writer = new PrintWriter(
                new OutputStreamWriter(pythonProcess.getOutputStream(), StandardCharsets.UTF_8), true);

        // 用于收集输出的StringBuilder
        StringBuilder outputCollector = new StringBuilder();
        StringBuilder errorCollector = new StringBuilder();

        log.info("🐍 [Python执行器] 启动输出处理线程");

        // 启动输出处理线程
        CompletableFuture<Void> outputHandler = CompletableFuture.runAsync(() -> {
            try {
                String line;
                int lineCount = 0;
                while ((line = reader.readLine()) != null) {
                    lineCount++;
                    // 收集所有输出，只过滤Java Bridge通信相关的行
                    if (!line.startsWith("JAVA_REQUEST:") && !line.startsWith("JAVA_RESPONSE:")) {
                        outputCollector.append(line).append(System.lineSeparator());
                        log.debug("🐍 [Python执行器] 收集输出行{}: {}", lineCount, line);
                    } else {
                        log.debug("🐍 [Python执行器] 过滤Java Bridge行{}: {}", lineCount, line);
                    }
                    handlePythonOutput(line, writer);
                }
                log.info("🐍 [Python执行器] 输出处理完成, 总行数: {}, 收集行数: {}", lineCount, outputCollector.toString().split("\n").length);
            } catch (IOException e) {
                log.error("🐍 [Python执行器] 处理Python输出时发生错误", e);
            } finally {
                try {
                    reader.close();
                    log.debug("🐍 [Python执行器] 输入流已关闭");
                } catch (IOException e) {
                    log.error("🐍 [Python执行器] 关闭Python输入流时发生错误", e);
                }
            }
        });

        // 启动错误流处理线程
        CompletableFuture<Void> errorHandler = CompletableFuture.runAsync(() -> {
            try (BufferedReader errorReader = new BufferedReader(
                    new InputStreamReader(pythonProcess.getErrorStream(), StandardCharsets.UTF_8))) {
                String line;
                int errorLineCount = 0;
                while ((line = errorReader.readLine()) != null) {
                    errorLineCount++;
                    errorCollector.append(line).append(System.lineSeparator());
                    log.debug("🐍 [Python执行器] 错误流行{}: {}", errorLineCount, line);
                }
                log.info("🐍 [Python执行器] 错误流处理完成, 错误行数: {}", errorLineCount);
            } catch (IOException e) {
                log.error("🐍 [Python执行器] 处理Python错误流时发生错误", e);
            }
        });

        // 等待输出处理完成
        try {
            log.info("🐍 [Python执行器] 等待输出和错误流处理完成, 超时时间: {}秒", executionTimeoutSeconds);
            CompletableFuture.allOf(outputHandler, errorHandler).get(executionTimeoutSeconds, TimeUnit.SECONDS);
            log.info("🐍 [Python执行器] 输出和错误流处理完成");
        } catch (Exception e) {
            log.error("🐍 [Python执行器] Python输出处理超时或失败", e);
            // 如果输出处理失败，尝试优雅关闭Python进程
            if (pythonProcess.isAlive()) {
                log.info("🐍 [Python执行器] 尝试优雅关闭Python进程");
                pythonProcess.destroy();
                try {
                    if (!pythonProcess.waitFor(5, TimeUnit.SECONDS)) {
                        log.info("🐍 [Python执行器] 强制终止Python进程");
                        pythonProcess.destroyForcibly();
                    }
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    pythonProcess.destroyForcibly();
                }
            }
            // 如果流处理失败，返回错误结果
            updateExecutionResult(chatMessage, "Python输出处理失败: " + e.getMessage(), false);
            return PythonExecutionResult.failure("Python输出处理失败: " + e.getMessage(), "STREAM_ERROR");
        } finally {
            try {
                writer.close();
                log.debug("🐍 [Python执行器] 输出流已关闭");
            } catch (Exception e) {
                log.error("🐍 [Python执行器] 关闭Python输出流时发生错误", e);
            }
        }

        // 等待进程完成并获取退出码
        try {
            log.info("🐍 [Python执行器] 等待进程完成");
            int exitCode = pythonProcess.waitFor();
            String output = outputCollector.toString().trim();
            String error = errorCollector.toString().trim();

            log.info("🐍 [Python执行器] 进程完成, exitCode: {}, 输出长度: {}, 错误长度: {}", 
                    exitCode, output.length(), error.length());
            log.debug("🐍 [Python执行器] 收集的输出: {}", output);
            log.debug("🐍 [Python执行器] 收集的错误: {}", error);

            if (exitCode == 0) {
                log.info("🐍 [Python执行器] Python脚本执行成功, messageId: {}", chatMessage.getId());
                
                // 尝试从输出中提取JSON格式的查询结果
                String finalResult = extractJsonResultFromOutput(output);
                
                updateExecutionResult(chatMessage, finalResult, true);
                return PythonExecutionResult.success(finalResult);
            } else {
                log.error("🐍 [Python执行器] Python脚本执行失败, messageId: {}, exit code: {}, 错误: {}", 
                        chatMessage.getId(), exitCode, error);
                updateExecutionResult(chatMessage, error, false);
                return PythonExecutionResult.failure(error, "EXECUTION_ERROR");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("🐍 [Python执行器] 等待Python进程完成时被中断, messageId: {}", chatMessage.getId(), e);
            updateExecutionResult(chatMessage, e.getMessage(), false);
            return PythonExecutionResult.failure(e.getMessage(), "INTERRUPTED");
        }
    }

    /**
     * 从Python输出中提取JSON格式的查询结果
     */
    private String extractJsonResultFromOutput(String output) {
        try {
            // 按行分割输出
            String[] lines = output.split("\\n");
            
            // 从后往前查找JSON格式的查询结果
            for (int i = lines.length - 1; i >= 0; i--) {
                String line = lines[i].trim();
                if (line.startsWith("{") && line.endsWith("}")) {
                    try {
                        // 尝试解析JSON
                        Map<String, Object> jsonData = objectMapper.readValue(line, Map.class);
                        if (jsonData.containsKey("dataType") && "python_dict_list".equals(jsonData.get("dataType"))) {
                            log.info("🔍 [Python执行器] 成功提取JSON格式的查询结果");
                            return line;
                        }
                    } catch (Exception jsonException) {
                        // 不是有效的JSON，继续查找
                        continue;
                    }
                }
            }
            
            // 如果没有找到JSON格式的查询结果，返回原始输出
            log.info("🔍 [Python执行器] 未找到JSON格式的查询结果，返回原始输出");
            return output;
        } catch (Exception e) {
            log.error("🔍 [Python执行器] 提取JSON结果失败: {}", e.getMessage(), e);
            return output;
        }
    }

    /**
     * 更新执行结果到数据库
     */
    private void updateExecutionResult(ChatMessage chatMessage, String result, boolean success) {
        try {
            log.info("🔍 [Python执行器] 开始更新执行结果到数据库, messageId: {}, success: {}, result长度: {}", 
                chatMessage.getId(), success, result != null ? result.length() : 0);
            
            chatMessage.setExecutionResult(result);
            chatMessage.setExecutionStatus(success ? 1 : 2); // 1=成功, 2=失败
            chatMessage.setStatus(success ? 1 : 2); // 更新主状态字段
            if (!success) {
                chatMessage.setErrorMessage(result);
            }
            
            chatMessageMapper.updateById(chatMessage);
            log.info("🔍 [Python执行器] 执行结果已更新到数据库, messageId: {}, success: {}", chatMessage.getId(), success);
            
            if (success && result != null) {
                log.debug("🔍 [Python执行器] 执行结果前200字符: {}", 
                    result.substring(0, Math.min(200, result.length())));
            }
        } catch (Exception e) {
            log.error("🔍 [Python执行器] 更新执行结果到数据库失败, messageId: {}", chatMessage.getId(), e);
        }
    }

    /**
     * 处理Python输出
     */
    private void handlePythonOutput(String line, PrintWriter writer) {
        try {
            if (line.startsWith("JAVA_REQUEST:")) {
                // 处理Java函数调用请求
                handleJavaFunctionCall(line.substring(13), writer);
            } else if (line.startsWith("ERROR:")) {
                // 处理Python错误输出
                String errorMsg = line.substring(6);
                log.error("Python错误: {}", errorMsg);
                reportError(errorMsg);
            } else if (line.startsWith("INFO:")) {
                // 处理Python信息输出
                String infoMsg = line.substring(5);
                log.info("Python信息: {}", infoMsg);
                reportProgress(infoMsg);
            } else if (line.startsWith("STEP:")) {
                // 处理Python步骤输出
                String stepMsg = line.substring(5);
                log.info("Python步骤: {}", stepMsg);
                reportStep(stepMsg);
            } else {
                // 检查是否是JSON格式的查询结果
                if (line.trim().startsWith("{") && line.trim().endsWith("}")) {
                    try {
                        // 尝试解析JSON
                        Map<String, Object> jsonData = objectMapper.readValue(line, Map.class);
                        if (jsonData.containsKey("dataType") && "python_dict_list".equals(jsonData.get("dataType"))) {
                            log.info("🔍 [Python执行器] 检测到JSON格式的查询结果");
                            // 这是查询结果，不需要作为普通输出处理
                            return;
                        }
                    } catch (Exception jsonException) {
                        // 不是有效的JSON，继续作为普通输出处理
                    }
                }
                
                // 普通输出
                log.info("Python输出: {}", line);
                reportProgress(line);
            }
        } catch (Exception e) {
            log.error("处理Python输出失败: {}", line, e);
        }
    }

    /**
     * 报告步骤信息
     */
    private void reportStep(String message) {
        // 这里可以集成SubEventReporter来报告步骤
        log.info("执行步骤: {}", message);
        // TODO: 集成SubEventReporter
        // if (reporter != null) {
        //     reporter.reportStep(message);
        // }
    }

    /**
     * 报告进度信息
     */
    private void reportProgress(String message) {
        // 这里可以集成SubEventReporter来报告进度
        log.info("执行进度: {}", message);
        // TODO: 集成SubEventReporter
        // if (reporter != null) {
        //     reporter.reportProgress(message);
        // }
    }

    /**
     * 报告错误信息
     */
    private void reportError(String message) {
        // 这里可以集成SubEventReporter来报告错误
        log.error("执行错误: {}", message);
        // TODO: 集成SubEventReporter
        // if (reporter != null) {
        //     reporter.reportError(message);
        // }
    }

    /**
     * 处理Java函数调用
     */
    private void handleJavaFunctionCall(String requestJson, PrintWriter writer) {
        try {
            Map<String, Object> request = objectMapper.readValue(requestJson, Map.class);
            String functionName = (String) request.get("function");
            List<Object> args = (List<Object>) request.get("args");
            Integer requestId = (Integer) request.get("id");

            log.info("收到Java函数调用请求: function={}, args={}, requestId={}", functionName, args, requestId);

            // 调用对应的Java函数
            Object result = callJavaFunction(functionName, args);

            // 返回结果给Python
            Map<String, Object> response = Map.of(
                    "id", requestId,
                    "result", result != null ? result : "");

            String responseJson = objectMapper.writeValueAsString(response);

            // 发送响应到Python进程
            writer.println("JAVA_RESPONSE:" + responseJson);
            writer.flush();

            log.info("Java函数调用完成: function={}, result={}", functionName, 
                    result != null ? (result.toString().length() > 100 ? result.toString().substring(0, 100) + "..." : result.toString()) : "null");

        } catch (Exception e) {
            log.error("处理Java函数调用失败: {}", e.getMessage(), e);
            // 发送错误响应
            Integer requestId = extractRequestIdSafely(requestJson);
            sendErrorResponse(writer, e.getMessage(), requestId);
        }
    }

    /**
     * 安全地提取请求ID
     */
    private Integer extractRequestIdSafely(String requestJson) {
        try {
            Map<String, Object> request = objectMapper.readValue(requestJson, Map.class);
            return (Integer) request.get("id");
        } catch (Exception e) {
            log.warn("提取请求ID失败: {}", e.getMessage());
            return -1;
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
     */
    private Object callJavaFunction(String functionName, List<Object> args) {
        log.info("🐍 [Python执行器] 调用Java函数: function={}, args={}", functionName, args);
        
        try {
            switch (functionName) {
                case "gen_sql":
                    // 调用SQL生成函数 - 集成AI服务
                    log.info("🐍 [Python执行器] 调用gen_sql函数");
                    Object genSqlResult = handleGenSql(args);
                    log.info("🐍 [Python执行器] gen_sql函数调用完成, 结果: {}", genSqlResult);
                    return genSqlResult;
                    
                case "exec_sql":
                    // 调用SQL执行函数
                    log.info("🐍 [Python执行器] 调用exec_sql函数");
                    Object execSqlResult = handleExecSql(args);
                    log.info("🐍 [Python执行器] exec_sql函数调用完成, 结果类型: {}", 
                            execSqlResult != null ? execSqlResult.getClass().getSimpleName() : "null");
                    return execSqlResult;
                    
                case "report_step":
                case "report_progress":
                    // 报告步骤，这里可以记录日志
                    log.info("🐍 [Python执行器] 调用{}函数: {}", functionName, args.get(0));
                    return null;
                    
                case "vis_textbox":
                    // 可视化文本框
                    log.info("🐍 [Python执行器] 调用vis_textbox函数");
                    return handleVisTextbox(args);
                    
                case "vis_textblock":
                    // 可视化指标块
                    log.info("🐍 [Python执行器] 调用vis_textblock函数");
                    return handleVisTextblock(args);
                    
                case "vis_table":
                    // 可视化表格
                    log.info("🐍 [Python执行器] 调用vis_table函数");
                    return handleVisTable(args);
                    
                case "steps_summary":
                    // 步骤总结
                    log.info("🐍 [Python执行器] 调用steps_summary函数");
                    return handleStepsSummary(args);
                    
                default:
                    log.error("🐍 [Python执行器] 未知的函数: {}", functionName);
                    throw new IllegalArgumentException("未知的函数: " + functionName);
            }
        } catch (Exception e) {
            log.error("🐍 [Python执行器] Java函数调用失败: function={}, args={}, error={}", functionName, args, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 处理gen_sql函数调用
     */
    private String handleGenSql(List<Object> args) {
        String queryText = (String) args.get(0);
        String tableName = (String) args.get(1);
        
        log.info("🐍 [Python执行器] 生成SQL: queryText={}, tableName={}", queryText, tableName);
        
        try {
            // 这里应该调用真正的AI服务来生成SQL
            // 暂时使用简单的模板生成，实际应该调用DifyService或其他AI服务
            String sql = generateSimpleSql(queryText, tableName);
            log.info("🐍 [Python执行器] 生成的SQL: {}", sql);
            return sql;
        } catch (Exception e) {
            log.error("🐍 [Python执行器] SQL生成失败: {}", e.getMessage(), e);
            throw new RuntimeException("SQL生成失败: " + e.getMessage());
        }
    }

    /**
     * 处理exec_sql函数调用
     */
    private Object handleExecSql(List<Object> args) {
        String sql = (String) args.get(0);
        
        log.info("🔍 [Python执行器] 开始执行SQL: {}", sql);
        
        try {
            // 调用FunctionUtil执行SQL
            Object result = functionUtil.executeSQL(sql);
            log.info("🔍 [Python执行器] SQL执行成功，结果类型: {}, 结果大小: {}", 
                    result != null ? result.getClass().getSimpleName() : "null",
                    result instanceof List ? ((List<?>) result).size() : "N/A");
            
            // 详细记录结果信息
            if (result instanceof List) {
                List<?> resultList = (List<?>) result;
                log.info("🔍 [Python执行器] SQL查询返回 {} 行数据", resultList.size());
                if (!resultList.isEmpty() && resultList.get(0) instanceof Map) {
                    Map<?, ?> firstRow = (Map<?, ?>) resultList.get(0);
                    log.info("🔍 [Python执行器] 第一行数据包含 {} 列: {}", 
                        firstRow.size(), firstRow.keySet());
                }
            }
            
            log.debug("🔍 [Python执行器] SQL执行结果详情: {}", result);
            return result;
        } catch (Exception e) {
            log.error("🔍 [Python执行器] SQL执行失败: {}", e.getMessage(), e);
            throw new RuntimeException("SQL执行失败: " + e.getMessage());
        }
    }

    /**
     * 处理可视化文本框
     */
    private Object handleVisTextbox(List<Object> args) {
        String content = (String) args.get(0);
        log.info("🐍 [Python执行器] 创建文本框: content={}", content);
        // 这里可以集成可视化服务
        return Map.of("type", "textbox", "content", content);
    }

    /**
     * 处理可视化指标块
     */
    private Object handleVisTextblock(List<Object> args) {
        String title = (String) args.get(0);
        Object value = args.get(1);
        log.info("🐍 [Python执行器] 创建指标块: title={}, value={}", title, value);
        // 这里可以集成可视化服务
        return Map.of("type", "textblock", "title", title, "value", value);
    }

    /**
     * 处理可视化表格
     */
    private Object handleVisTable(List<Object> args) {
        String title = (String) args.get(0);
        List<Map<String, Object>> data = (List<Map<String, Object>>) args.get(1);
        log.info("🐍 [Python执行器] 创建表格: title={}, dataSize={}", title, data != null ? data.size() : 0);
        // 这里可以集成可视化服务
        return Map.of("type", "table", "title", title, "data", data);
    }

    /**
     * 处理步骤总结
     */
    private String handleStepsSummary(List<Object> args) {
        String summaryTitle = (String) args.get(0);
        log.info("🐍 [Python执行器] 生成步骤总结: title={}", summaryTitle);
        // 这里可以生成执行步骤的总结
        return "执行步骤总结: " + summaryTitle + " - 已完成";
    }

    /**
     * 生成简单的SQL查询（临时实现，实际应该调用AI服务）
     */
    private String generateSimpleSql(String queryText, String tableName) {
        log.info("🐍 [Python执行器] 开始生成SQL, queryText: {}, tableName: {}", queryText, tableName);
        
        String sql;
        // 简单的SQL生成逻辑，实际应该调用AI服务
        if (queryText.toLowerCase().contains("count") || queryText.contains("数量") || queryText.contains("总数")) {
            sql = "SELECT COUNT(*) as count FROM " + tableName;
            log.info("🐍 [Python执行器] 生成COUNT查询: {}", sql);
        } else if (queryText.toLowerCase().contains("all") || queryText.contains("全部") || queryText.contains("所有")) {
            sql = "SELECT * FROM " + tableName + " LIMIT 100";
            log.info("🐍 [Python执行器] 生成ALL查询: {}", sql);
        } else if (queryText.contains("净利润") && queryText.contains("总和")) {
            // 针对净利润总和的特殊处理
            if (queryText.contains("广州") && queryText.contains("2024")) {
                sql = "SELECT SUM(net_profit) as total_net_profit FROM " + tableName + " WHERE city = '广州市' AND year = 2024";
                log.info("🐍 [Python执行器] 生成2024年广州净利润总和查询: {}", sql);
            } else if (queryText.contains("广州")) {
                sql = "SELECT SUM(net_profit) as total_net_profit FROM " + tableName + " WHERE city = '广州市'";
                log.info("🐍 [Python执行器] 生成广州净利润总和查询: {}", sql);
            } else {
                sql = "SELECT SUM(net_profit) as total_net_profit FROM " + tableName;
                log.info("🐍 [Python执行器] 生成净利润总和查询: {}", sql);
            }
        } else if (queryText.contains("净利润")) {
            // 针对净利润查询的特殊处理
            if (queryText.contains("广州") && queryText.contains("2024")) {
                sql = "SELECT net_profit FROM " + tableName + " WHERE city = '广州市' AND year = 2024 LIMIT 50";
                log.info("🐍 [Python执行器] 生成2024年广州净利润查询: {}", sql);
            } else if (queryText.contains("广州")) {
                sql = "SELECT net_profit FROM " + tableName + " WHERE city = '广州市' LIMIT 50";
                log.info("🐍 [Python执行器] 生成广州净利润查询: {}", sql);
            } else {
                sql = "SELECT net_profit FROM " + tableName + " LIMIT 50";
                log.info("🐍 [Python执行器] 生成净利润查询: {}", sql);
            }
        } else {
            sql = "SELECT * FROM " + tableName + " LIMIT 50";
            log.info("🐍 [Python执行器] 生成默认查询: {}", sql);
        }
        
        log.info("🐍 [Python执行器] SQL生成完成: {}", sql);
        return sql;
    }
    
    private void cleanup(Path tempDir) {
        try {
            Files.walk(tempDir)
                 .sorted(Comparator.reverseOrder())
                 .map(Path::toFile)
                 .forEach(File::delete);
            log.info("成功清理临时目录: {}", tempDir);
        } catch (IOException e) {
            log.warn("清理临时目录失败: {}", tempDir, e);
        }
    }

    /**
     * 获取进程ID（如果可能）
     */
    private String getProcessId(Process process) {
        try {
            // 尝试获取进程ID，这在某些JVM实现中可能不可用
            if (process.getClass().getName().equals("java.lang.ProcessImpl")) {
                Field pidField = process.getClass().getDeclaredField("pid");
                pidField.setAccessible(true);
                return String.valueOf(pidField.getInt(process));
            }
        } catch (Exception e) {
            // 忽略异常，返回未知
        }
        return "unknown";
    }
    
    @Override
    public void executePythonCode(String pythonCode, HashMap<String, Object> paramMap,
                                 SubEventReporter reporter, String userId) {
        throw new UnsupportedOperationException("This method is deprecated. Use executePythonCodeWithResult instead.");
    }

    @Override
    public Object executePythonCodeWithResult(String pythonCode, HashMap<String, Object> paramMap, String userId) {
        throw new UnsupportedOperationException("This method is deprecated. Use executePythonCodeWithResult(Long, Long) instead.");
    }
}