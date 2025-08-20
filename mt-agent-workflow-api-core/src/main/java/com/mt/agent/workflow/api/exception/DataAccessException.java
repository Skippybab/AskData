package com.mt.agent.workflow.api.exception;

/**
 * 数据访问异常
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
    
    public String getErrorCode() {
        return errorCode;
    }
    
    // 静态工厂方法
    public static DataAccessException emptyQueryResult(String message) {
        return new DataAccessException(message, "EMPTY_RESULT");
    }
    
    public static DataAccessException noDataAvailable(String message) {
        return new DataAccessException(message, "NO_DATA");
    }
    
    public static DataAccessException arrayIndexOutOfBounds(String message) {
        return new DataAccessException(message, "INDEX_OUT_OF_BOUNDS");
    }
    
    public static DataAccessException sqlExecutionError(String message) {
        return new DataAccessException(message, "SQL_ERROR");
    }
}
