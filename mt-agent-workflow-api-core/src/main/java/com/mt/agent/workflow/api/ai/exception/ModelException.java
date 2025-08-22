package com.mt.agent.workflow.api.ai.exception;

/**
 * 模型相关异常的基类
 */
public class ModelException extends RuntimeException {

    public ModelException(String message) {
        super(message);
    }

    public ModelException(String message, Throwable cause) {
        super(message, cause);
    }
}

