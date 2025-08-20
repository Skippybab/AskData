package com.mt.agent.test.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.TimeUnit;

/**
 * Python执行器
 * 用于执行Python代码并获取执行结果
 *
 * @author MT Agent Team
 * @version 1.0
 * @since 2025-01-15
 */
@Slf4j
@Component
public class PythonExecutor {

    /**
     * Python执行结果
     */
    public static class PythonResult {
        private boolean success;
        private String output;
        private String error;
        private long executionTime;

        public PythonResult(boolean success, String output, String error, long executionTime) {
            this.success = success;
            this.output = output;
            this.error = error;
            this.executionTime = executionTime;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getOutput() {
            return output;
        }

        public String getError() {
            return error;
        }

        public long getExecutionTime() {
            return executionTime;
        }
    }

    /**
     * 执行Python代码
     *
     * @param pythonCode Python代码
     * @return 执行结果
     */
    public PythonResult executePython(String pythonCode) {
        return executePython(pythonCode, 30); // 默认30秒超时
    }

    /**
     * 执行Python代码
     *
     * @param pythonCode     Python代码
     * @param timeoutSeconds 超时时间（秒）
     * @return 执行结果
     */
    public PythonResult executePython(String pythonCode, int timeoutSeconds) {
        Path tempFile = null;
        long startTime = System.currentTimeMillis();

        try {
            // 创建临时Python文件
            tempFile = Files.createTempFile("test_python_", ".py");
            Files.write(tempFile, pythonCode.getBytes(), StandardOpenOption.WRITE);

            // 执行Python脚本
            ProcessBuilder processBuilder = new ProcessBuilder("python", tempFile.toString());
            processBuilder.redirectErrorStream(false);

            Process process = processBuilder.start();

            // 读取输出
            StringBuilder output = new StringBuilder();
            StringBuilder error = new StringBuilder();

            // 启动线程读取输出和错误流
            Thread outputReader = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        output.append(line).append("\n");
                    }
                } catch (IOException e) {
                    log.error("读取Python输出时出错: {}", e.getMessage());
                }
            });

            Thread errorReader = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        error.append(line).append("\n");
                    }
                } catch (IOException e) {
                    log.error("读取Python错误输出时出错: {}", e.getMessage());
                }
            });

            outputReader.start();
            errorReader.start();

            // 等待进程完成
            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);

            // 等待读取线程完成
            outputReader.join(1000);
            errorReader.join(1000);

            long executionTime = System.currentTimeMillis() - startTime;

            if (!finished) {
                process.destroyForcibly();
                return new PythonResult(false, output.toString(),
                        "执行超时（超过" + timeoutSeconds + "秒）", executionTime);
            }

            int exitCode = process.exitValue();
            boolean success = exitCode == 0;

            return new PythonResult(success, output.toString(), error.toString(), executionTime);

        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            log.error("执行Python代码时出错: {}", e.getMessage(), e);
            return new PythonResult(false, "", "执行异常: " + e.getMessage(), executionTime);
        } finally {
            // 清理临时文件
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException e) {
                    log.warn("删除临时文件失败: {}", e.getMessage());
                }
            }
        }
    }

    /**
     * 检查Python环境是否可用
     *
     * @return true表示Python环境可用
     */
    public boolean isPythonAvailable() {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder("python", "--version");
            Process process = processBuilder.start();
            boolean finished = process.waitFor(5, TimeUnit.SECONDS);

            if (!finished) {
                process.destroyForcibly();
                return false;
            }

            return process.exitValue() == 0;
        } catch (Exception e) {
            log.error("检查Python环境时出错: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 获取Python版本信息
     *
     * @return Python版本信息
     */
    public String getPythonVersion() {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder("python", "--version");
            Process process = processBuilder.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line);
                }
            }

            boolean finished = process.waitFor(5, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return "获取版本超时";
            }

            return output.toString().trim();
        } catch (Exception e) {
            log.error("获取Python版本时出错: {}", e.getMessage());
            return "获取版本失败: " + e.getMessage();
        }
    }
}