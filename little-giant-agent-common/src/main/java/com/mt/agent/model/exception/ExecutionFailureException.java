package com.mt.agent.model.exception;

/**
 * 执行失败异常
 * 处理除数据访问外的其他Python代码执行失败情况
 *
 * @author lfz
 * @date 2025/1/7
 */
public class ExecutionFailureException extends PythonExecutionException {

    public static final String ERROR_TYPE = "EXECUTION_FAILURE";

    // 错误子类型
    public static final String SYNTAX_ERROR = "SYNTAX_ERROR";
    public static final String RUNTIME_ERROR = "RUNTIME_ERROR";
    public static final String TIMEOUT_ERROR = "TIMEOUT_ERROR";
    public static final String PROCESS_ERROR = "PROCESS_ERROR";
    public static final String UNKNOWN_ERROR = "UNKNOWN_ERROR";

    public ExecutionFailureException(String message, String errorCode) {
        super(message, ERROR_TYPE, errorCode);
    }

    public ExecutionFailureException(String message, String errorCode, Throwable cause) {
        super(message, ERROR_TYPE, errorCode, cause);
    }

    /**
     * 创建语法错误异常
     */
    public static ExecutionFailureException syntaxError(String details) {
        return new ExecutionFailureException(
                "方案执行失败，原因为：Python代码语法错误 - " + details + "，请引导用户重试",
                SYNTAX_ERROR);
    }

    /**
     * 创建运行时错误异常
     */
    public static ExecutionFailureException runtimeError(String details) {
        return new ExecutionFailureException(
                "方案执行失败，原因为：代码运行时错误 - " + details + "，请引导用户重试",
                RUNTIME_ERROR);
    }

    /**
     * 创建超时错误异常
     */
    public static ExecutionFailureException timeoutError(String details) {
        return new ExecutionFailureException(
                "方案执行失败，原因为：执行超时 - " + details + "，请引导用户重试",
                TIMEOUT_ERROR);
    }

    /**
     * 创建进程错误异常
     */
    public static ExecutionFailureException processError(String details) {
        return new ExecutionFailureException(
                "方案执行失败，原因为：进程执行异常 - " + details + "，请引导用户重试",
                PROCESS_ERROR);
    }

    /**
     * 创建未知错误异常
     */
    public static ExecutionFailureException unknownError(String details) {
        return new ExecutionFailureException(
                "方案执行失败，原因为：未知错误 - " + details + "，请引导用户重试",
                UNKNOWN_ERROR);
    }
}