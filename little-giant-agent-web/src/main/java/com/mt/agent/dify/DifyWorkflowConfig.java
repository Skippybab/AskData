package com.mt.agent.dify;

import lombok.Data;

/**
 * 单个Dify工作流配置
 * 用于配置单个工作流的连接信息
 *
 * @author wsx
 * @date 2025/1/28
 */
@Data
public class DifyWorkflowConfig {

    /**
     * 工作流名称（唯一标识）
     */
    private String name;

    /**
     * Dify API基础地址
     */
    private String baseUrl;

    /**
     * Dify API Key
     */
    private String apiKey;

    /**
     * 请求超时时间（毫秒）
     */
    private int timeout = 1000000;

    /**
     * 连接超时时间（毫秒）
     */
    private int connectTimeout = 1000000;

    /**
     * 读取超时时间（毫秒）
     */
    private int readTimeout = 1000000;

    /**
     * 工作流描述
     */
    private String description;

    /**
     * 是否为默认工作流
     */
    private boolean defaultWorkflow = false;

    /**
     * 检查配置是否有效
     *
     * @return true表示配置有效，false表示配置无效
     */
    public boolean isValid() {
        return name != null && !name.trim().isEmpty() &&
                baseUrl != null && !baseUrl.trim().isEmpty() &&
                apiKey != null && !apiKey.trim().isEmpty();
    }

    /**
     * 获取脱敏的API密钥
     *
     * @return 脱敏后的API密钥
     */
    public String getMaskedApiKey() {
        if (apiKey != null && apiKey.length() > 8) {
            return apiKey.substring(0, 8) + "****";
        }
        return "****";
    }
}