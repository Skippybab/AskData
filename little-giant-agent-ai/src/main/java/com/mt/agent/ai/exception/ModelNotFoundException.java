package com.mt.agent.ai.exception;

/**
 * 模型未找到异常
 */
public class ModelNotFoundException extends ModelException {

    public ModelNotFoundException(String modelCode) {
        super("模型未找到: " + modelCode);
    }
}