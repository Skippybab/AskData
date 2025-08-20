package com.mt.agent.dify;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import cn.hutool.json.JSONObject;

import jakarta.annotation.PreDestroy;
import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Dify客户端工厂
 * 根据工作流名称动态创建和管理DifyClient实例
 *
 * @author wsx
 * @date 2025/1/28
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DifyClientFactory {

    private final DifyConfigManager configManager;

    /**
     * 客户端实例缓存
     */
    private final ConcurrentHashMap<String, DifyClient> clientCache = new ConcurrentHashMap<>();

    /**
     * 对象映射器（复用）
     */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 获取指定工作流的DifyClient实例
     *
     * @param workflowName 工作流名称，如果为null或空则使用默认工作流
     * @return DifyClient实例
     */
    public DifyClient getClient(String workflowName) {
        // 规范化工作流名称
        String normalizedWorkflowName = normalizeWorkflowName(workflowName);

        // 从缓存中获取客户端实例
        return clientCache.computeIfAbsent(normalizedWorkflowName, this::createClient);
    }

    /**
     * 获取默认工作流的DifyClient实例
     *
     * @return 默认DifyClient实例
     */
    public DifyClient getDefaultClient() {
        return getClient(null);
    }

    /**
     * 规范化工作流名称
     *
     * @param workflowName 原始工作流名称
     * @return 规范化后的工作流名称
     */
    private String normalizeWorkflowName(String workflowName) {
        if (workflowName == null || workflowName.trim().isEmpty()) {
            return "default";
        }
        return workflowName.trim();
    }

    /**
     * 创建DifyClient实例
     *
     * @param workflowName 工作流名称
     * @return 新创建的DifyClient实例
     */
    private DifyClient createClient(String workflowName) {
        log.info("为工作流 '{}' 创建DifyClient实例", workflowName);

        // 获取工作流配置
        DifyWorkflowConfig workflowConfig = configManager.getWorkflowConfig(workflowName);
        if (workflowConfig == null) {
            log.error("无法获取工作流配置: {}", workflowName);
            throw new RuntimeException("无法获取工作流配置: " + workflowName);
        }

        // 创建专用的DifyConfig实例
        DifyConfig specificConfig = createDifyConfigFromWorkflow(workflowConfig);

        try {
            // 使用反射创建DifyClient实例并注入配置
            DifyClient client = new DifyClient(specificConfig);

            // 调用初始化方法
            initializeDifyClient(client);

            log.info("DifyClient实例创建成功: {} [{}]", workflowName, workflowConfig.getMaskedApiKey());
            return client;

        } catch (Exception e) {
            log.error("创建DifyClient实例失败: {}", workflowName, e);
            throw new RuntimeException("创建DifyClient实例失败: " + workflowName, e);
        }
    }

    /**
     * 从工作流配置创建DifyConfig实例
     *
     * @param workflowConfig 工作流配置
     * @return DifyConfig实例
     */
    private DifyConfig createDifyConfigFromWorkflow(DifyWorkflowConfig workflowConfig) {
        DifyConfig config = new DifyConfig();
        config.setBaseUrl(workflowConfig.getBaseUrl());
        config.setApiKey(workflowConfig.getApiKey());
        config.setTimeout(workflowConfig.getTimeout());
        config.setConnectTimeout(workflowConfig.getConnectTimeout());
        config.setReadTimeout(workflowConfig.getReadTimeout());
        return config;
    }

    /**
     * 初始化DifyClient实例
     *
     * @param client DifyClient实例
     */
    private void initializeDifyClient(DifyClient client) {
        try {
            // 通过反射调用init方法
            client.getClass().getDeclaredMethod("init").invoke(client);
        } catch (Exception e) {
            log.warn("无法调用DifyClient的init方法，尝试手动初始化: {}", e.getMessage());
            // 如果反射调用失败，可以在这里添加手动初始化逻辑
        }
    }

    /**
     * 检查工作流是否可用
     *
     * @param workflowName 工作流名称
     * @return true表示可用，false表示不可用
     */
    public boolean isWorkflowAvailable(String workflowName) {
        try {
            DifyClient client = getClient(workflowName);
            return client != null && client.isAvailable();
        } catch (Exception e) {
            log.warn("检查工作流可用性失败: {}", workflowName, e);
            return false;
        }
    }

    /**
     * 获取工作流客户端配置信息
     *
     * @param workflowName 工作流名称
     * @return 配置信息字符串
     */
    public String getWorkflowClientInfo(String workflowName) {
        try {
            DifyClient client = getClient(workflowName);
            return client != null ? client.getConfigInfo() : "客户端不可用";
        } catch (Exception e) {
            return "获取配置信息失败: " + e.getMessage();
        }
    }

    /**
     * 清除指定工作流的客户端缓存
     *
     * @param workflowName 工作流名称
     */
    public void clearClientCache(String workflowName) {
        String normalizedName = normalizeWorkflowName(workflowName);
        DifyClient removedClient = clientCache.remove(normalizedName);
        if (removedClient != null) {
            log.info("清除工作流客户端缓存: {}", normalizedName);
            // 可以在这里添加客户端资源清理逻辑
        }
    }

    /**
     * 清除所有客户端缓存
     */
    public void clearAllClientCache() {
        log.info("清除所有DifyClient缓存，共{}个实例", clientCache.size());
        clientCache.clear();
    }

    /**
     * 获取缓存状态信息
     *
     * @return 缓存状态信息
     */
    public String getCacheStatus() {
        return String.format("DifyClient缓存状态: 共%d个实例 %s",
                clientCache.size(), clientCache.keySet());
    }

    /**
     * 预热指定工作流的客户端（可选的性能优化）
     *
     * @param workflowName 工作流名称
     */
    public void warmupClient(String workflowName) {
        try {
            DifyClient client = getClient(workflowName);
            if (client.isAvailable()) {
                log.info("工作流客户端预热成功: {}", workflowName);
            } else {
                log.warn("工作流客户端预热失败，客户端不可用: {}", workflowName);
            }
        } catch (Exception e) {
            log.error("工作流客户端预热异常: {}", workflowName, e);
        }
    }

    /**
     * 预热所有可用工作流的客户端
     */
    public void warmupAllClients() {
        log.info("开始预热所有工作流客户端...");
        configManager.getAvailableWorkflowNames().forEach(this::warmupClient);
        log.info("工作流客户端预热完成");
    }

    /**
     * 销毁资源
     */
    @PreDestroy
    public void destroy() {
        log.info("销毁DifyClientFactory，清理{}个客户端实例", clientCache.size());
        clientCache.values().forEach(client -> {
            try {
                // 尝试调用客户端的销毁方法
                client.getClass().getDeclaredMethod("destroy").invoke(client);
            } catch (Exception e) {
                log.debug("销毁客户端实例时的异常（可忽略）: {}", e.getMessage());
            }
        });
        clientCache.clear();
    }
}