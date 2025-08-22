package com.cultivate.ai.exception;

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

