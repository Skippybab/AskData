package com.mt.agent.ai.exception;

/**
 * 无效配置异常
 */
public class InvalidConfigurationException extends ModelException {

    public InvalidConfigurationException(String message) {
        super("无效的配置: " + message);
    }

    public InvalidConfigurationException(String message, Throwable cause) {
        super("无效的配置: " + message, cause);
    }
}