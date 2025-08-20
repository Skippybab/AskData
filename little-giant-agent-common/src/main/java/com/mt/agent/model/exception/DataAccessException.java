package com.mt.agent.model.exception;

/**
 * 数据访问异常
 * 主要处理数组越界、数据库查无数据等情况
 *
 * @author lfz
 * @date 2025/1/7
 */
public class DataAccessException extends PythonExecutionException {

    public static final String ERROR_TYPE = "DATA_ACCESS_ERROR";

    // 错误子类型
    public static final String ARRAY_INDEX_OUT_OF_BOUNDS = "ARRAY_INDEX_OUT_OF_BOUNDS";
    public static final String EMPTY_QUERY_RESULT = "EMPTY_QUERY_RESULT";
    public static final String NO_DATA_AVAILABLE = "NO_DATA_AVAILABLE";

    public DataAccessException(String message, String errorCode) {
        super(message, ERROR_TYPE, errorCode);
    }

    public DataAccessException(String message, String errorCode, Throwable cause) {
        super(message, ERROR_TYPE, errorCode, cause);
    }

    /**
     * 创建数组越界异常
     */
    public static DataAccessException arrayIndexOutOfBounds(String details) {
        return new DataAccessException(
                "数据库查无数据：" + details,
                ARRAY_INDEX_OUT_OF_BOUNDS);
    }

    /**
     * 创建查询结果为空异常
     */
    public static DataAccessException emptyQueryResult(String details) {
        return new DataAccessException(
                "数据库查无数据：" + details,
                EMPTY_QUERY_RESULT);
    }

    /**
     * 创建无数据可用异常
     */
    public static DataAccessException noDataAvailable(String details) {
        return new DataAccessException(
                "数据库查无数据：" + details,
                NO_DATA_AVAILABLE);
    }
}