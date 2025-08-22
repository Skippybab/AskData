package com.mt.agent.workflow.api.exception;

/**
 * 执行失败异常
 */
public class ExecutionFailureException extends RuntimeException {
    
    private final String errorCode;
    
    public ExecutionFailureException(String message) {
        super(message);
        this.errorCode = "EXECUTION_FAILURE";
    }
    
    public ExecutionFailureException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public ExecutionFailureException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "EXECUTION_FAILURE";
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    // 静态工厂方法
    public static ExecutionFailureException timeoutError(String message) {
        return new ExecutionFailureException(message, "TIMEOUT");
    }
    
    public static ExecutionFailureException syntaxError(String message) {
        return new ExecutionFailureException(message, "SYNTAX_ERROR");
    }
    
    public static ExecutionFailureException runtimeError(String message) {
        return new ExecutionFailureException(message, "RUNTIME_ERROR");
    }
    
    public static ExecutionFailureException processError(String message) {
        return new ExecutionFailureException(message, "PROCESS_ERROR");
    }
}
