package com.mt.agent.model.exception;

/**
 * Python代码执行异常基类
 * 用于区分不同类型的Python执行失败
 *
 * @author lfz
 * @date 2025/1/7
 */
public class PythonExecutionException extends RuntimeException {

    private final String errorType;
    private final String errorCode;

    public PythonExecutionException(String message, String errorType, String errorCode) {
        super(message);
        this.errorType = errorType;
        this.errorCode = errorCode;
    }

    public PythonExecutionException(String message, String errorType, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorType = errorType;
        this.errorCode = errorCode;
    }

    public String getErrorType() {
        return errorType;
    }

    public String getErrorCode() {
        return errorCode;
    }
}