package com.mt.agent.workflow.api.dto;

import lombok.Builder;
import lombok.Data;

/**
 * DTO for holding the result of a Python code execution.
 */
@Data
@Builder
public class PythonExecutionResult {

    /**
     * Whether the execution was successful.
     */
    private boolean success;

    /**
     * The result data from the execution, typically a JSON string representing the query output.
     */
    private String data;

    /**
     * The error message if the execution failed.
     */
    private String errorMessage;

    /**
     * The type of error that occurred.
     */
    private String errorType;

    /**
     * Static factory method for a successful result.
     *
     * @param data The result data.
     * @return A successful PythonExecutionResult.
     */
    public static PythonExecutionResult success(String data) {
        return PythonExecutionResult.builder()
                .success(true)
                .data(data)
                .build();
    }

    /**
     * Static factory method for a failed result.
     *
     * @param errorMessage The error message.
     * @param errorType    The type of error.
     * @return A failed PythonExecutionResult.
     */
    public static PythonExecutionResult failure(String errorMessage, String errorType) {
        return PythonExecutionResult.builder()
                .success(false)
                .errorMessage(errorMessage)
                .errorType(errorType)
                .build();
    }
}
