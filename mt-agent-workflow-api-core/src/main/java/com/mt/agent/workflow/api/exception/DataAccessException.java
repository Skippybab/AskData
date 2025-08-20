package com.mt.agent.workflow.api.exception;

/**
 * 数据访问异常
 * 用于处理查询结果为空、数组越界等数据访问相关的异常
 */
public class DataAccessException extends RuntimeException {
    
    private final String errorCode;
    
    public DataAccessException(String message) {
        super(message);
        this.errorCode = "DATA_ACCESS_ERROR";
    }
    
    public DataAccessException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "DATA_ACCESS_ERROR";
    }
    
    public DataAccessException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    // 静态工厂方法
    public static DataAccessException emptyQueryResult(String message) {
        return new DataAccessException(message, "EMPTY_RESULT");
    }
    
    public static DataAccessException arrayIndexOutOfBounds(String message) {
        return new DataAccessException(message, "INDEX_OUT_OF_BOUNDS");
    }
    
    public static DataAccessException noDataAvailable(String message) {
        return new DataAccessException(message, "NO_DATA_AVAILABLE");
    }
    
    public static DataAccessException invalidDataFormat(String message) {
        return new DataAccessException(message, "INVALID_DATA_FORMAT");
    }
    
    public static DataAccessException databaseConnectionError(String message) {
        return new DataAccessException(message, "DATABASE_CONNECTION_ERROR");
    }
    
    public static DataAccessException sqlExecutionError(String message) {
        return new DataAccessException(message, "SQL_EXECUTION_ERROR");
    }
}
