package com.mt.agent.dify;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Dify工作流配置管理器
 * 管理多个Dify工作流的配置信息
 *
 * @author wsx
 * @date 2025/1/28
 */
@Slf4j
@Data
@Component
@ConfigurationProperties(prefix = "dify")
public class DifyConfigManager {

    @Autowired
    private DifyConfig defaultConfig;

    /**
     * 多个工作流配置
     */
    private Map<String, DifyWorkflowConfig> workflows = new HashMap<>();

    /**
     * 缓存已验证的配置
     */
    private final Map<String, DifyWorkflowConfig> validatedConfigs = new ConcurrentHashMap<>();

    /**
     * 默认工作流名称
     */
    private static final String DEFAULT_WORKFLOW_NAME = "default";

    /**
     * 初始化配置管理器
     */
    @PostConstruct
    public void init() {
        log.info("初始化Dify配置管理器...");

        // 将现有的默认配置转换为工作流配置
        DifyWorkflowConfig defaultWorkflowConfig = createDefaultWorkflowConfig();
        workflows.put(DEFAULT_WORKFLOW_NAME, defaultWorkflowConfig);
        validatedConfigs.put(DEFAULT_WORKFLOW_NAME, defaultWorkflowConfig);

        // 验证所有配置的工作流
        validateAllWorkflows();

        log.info("Dify配置管理器初始化完成，共配置{}个工作流", workflows.size());
        logWorkflowSummary();
    }

    /**
     * 从默认配置创建默认工作流配置
     */
    private DifyWorkflowConfig createDefaultWorkflowConfig() {
        DifyWorkflowConfig config = new DifyWorkflowConfig();
        config.setName(DEFAULT_WORKFLOW_NAME);
        config.setBaseUrl(defaultConfig.getBaseUrl());
        config.setApiKey(defaultConfig.getApiKey());
        config.setTimeout(defaultConfig.getTimeout());
        config.setConnectTimeout(defaultConfig.getConnectTimeout());
        config.setReadTimeout(defaultConfig.getReadTimeout());
        config.setDescription("默认Dify工作流配置（兼容原有配置）");
        config.setDefaultWorkflow(true);
        return config;
    }

    /**
     * 验证所有工作流配置
     */
    private void validateAllWorkflows() {
        workflows.forEach((name, config) -> {
            if (config.isValid()) {
                validatedConfigs.put(name, config);
                log.info("工作流配置验证成功: {} [{}]", name, config.getMaskedApiKey());
            } else {
                log.warn("工作流配置验证失败: {} - 配置信息不完整", name);
            }
        });
    }

    /**
     * 打印工作流配置摘要
     */
    private void logWorkflowSummary() {
        if (validatedConfigs.isEmpty()) {
            log.warn("警告：没有有效的Dify工作流配置");
            return;
        }

        log.info("=== Dify工作流配置摘要 ===");
        validatedConfigs.forEach((name, config) -> {
            log.info("工作流: {} | 地址: {} | API密钥: {} | 默认: {}",
                    name, config.getBaseUrl(), config.getMaskedApiKey(), config.isDefaultWorkflow());
        });
        log.info("========================");
    }

    /**
     * 获取指定工作流的配置
     *
     * @param workflowName 工作流名称
     * @return 工作流配置，如果不存在则返回默认配置
     */
    public DifyWorkflowConfig getWorkflowConfig(String workflowName) {
        // 如果未指定工作流名称，返回默认配置
        if (workflowName == null || workflowName.trim().isEmpty()) {
            return getDefaultWorkflowConfig();
        }

        // 查找指定的工作流配置
        DifyWorkflowConfig config = validatedConfigs.get(workflowName);
        if (config != null) {
            log.debug("使用工作流配置: {}", workflowName);
            return config;
        }

        // 如果找不到指定的工作流，记录警告并返回默认配置
        log.warn("找不到工作流配置: {}，使用默认配置", workflowName);
        return getDefaultWorkflowConfig();
    }

    /**
     * 获取默认工作流配置
     *
     * @return 默认工作流配置
     */
    public DifyWorkflowConfig getDefaultWorkflowConfig() {
        return validatedConfigs.get(DEFAULT_WORKFLOW_NAME);
    }

    /**
     * 获取所有可用的工作流名称
     *
     * @return 工作流名称列表
     */
    public Set<String> getAvailableWorkflowNames() {
        return Collections.unmodifiableSet(validatedConfigs.keySet());
    }

    /**
     * 检查指定工作流是否存在
     *
     * @param workflowName 工作流名称
     * @return true表示存在，false表示不存在
     */
    public boolean hasWorkflow(String workflowName) {
        return validatedConfigs.containsKey(workflowName);
    }

    /**
     * 获取工作流配置详情（用于调试和监控）
     *
     * @return 配置详情Map
     */
    public Map<String, Object> getConfigSummary() {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalWorkflows", validatedConfigs.size());
        summary.put("defaultWorkflow", DEFAULT_WORKFLOW_NAME);

        Map<String, Object> workflowDetails = new LinkedHashMap<>();
        validatedConfigs.forEach((name, config) -> {
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("baseUrl", config.getBaseUrl());
            details.put("apiKey", config.getMaskedApiKey());
            details.put("timeout", config.getTimeout());
            details.put("description", config.getDescription());
            details.put("isDefault", config.isDefaultWorkflow());
            workflowDetails.put(name, details);
        });

        summary.put("workflows", workflowDetails);
        return summary;
    }

    /**
     * 动态添加工作流配置（用于运行时配置）
     *
     * @param workflowConfig 工作流配置
     * @return true表示添加成功，false表示添加失败
     */
    public boolean addWorkflowConfig(DifyWorkflowConfig workflowConfig) {
        if (workflowConfig == null || !workflowConfig.isValid()) {
            log.warn("无法添加无效的工作流配置");
            return false;
        }

        String name = workflowConfig.getName();
        if (validatedConfigs.containsKey(name)) {
            log.warn("工作流配置已存在: {}", name);
            return false;
        }

        workflows.put(name, workflowConfig);
        validatedConfigs.put(name, workflowConfig);

        log.info("动态添加工作流配置成功: {} [{}]", name, workflowConfig.getMaskedApiKey());
        return true;
    }

    /**
     * 移除工作流配置（不能移除默认配置）
     *
     * @param workflowName 工作流名称
     * @return true表示移除成功，false表示移除失败
     */
    public boolean removeWorkflowConfig(String workflowName) {
        if (DEFAULT_WORKFLOW_NAME.equals(workflowName)) {
            log.warn("不能移除默认工作流配置");
            return false;
        }

        if (!validatedConfigs.containsKey(workflowName)) {
            log.warn("工作流配置不存在: {}", workflowName);
            return false;
        }

        workflows.remove(workflowName);
        validatedConfigs.remove(workflowName);

        log.info("移除工作流配置成功: {}", workflowName);
        return true;
    }
}