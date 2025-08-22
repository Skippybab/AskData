package com.mt.agent.workflow.api.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mt.agent.workflow.api.entity.ApiConfig;

import java.util.List;

/**
 * API配置服务接口
 */
public interface ApiConfigService {
    
    /**
     * 分页查询API配置
     */
    IPage<ApiConfig> getApiConfigPage(Page<ApiConfig> page, String apiName, Integer status, Long userId);
    
    /**
     * 获取API配置详情
     */
    ApiConfig getApiConfigById(Long id);
    
    /**
     * 根据API路径获取配置
     */
    ApiConfig getApiConfigByPath(String apiPath);
    
    /**
     * 创建API配置
     */
    ApiConfig createApiConfig(ApiConfig apiConfig);
    
    /**
     * 更新API配置
     */
    boolean updateApiConfig(ApiConfig apiConfig);
    
    /**
     * 删除API配置
     */
    boolean deleteApiConfig(Long id, Long userId);
    
    /**
     * 生成API密钥
     */
    String generateApiKey();
    
    /**
     * 验证API密钥
     */
    boolean validateApiKey(String apiPath, String apiKey);
    
    /**
     * 更新调用统计
     */
    void updateCallStatistics(Long id);
    
    /**
     * 获取用户的所有API配置
     */
    List<ApiConfig> getUserApiConfigs(Long userId);
    
    /**
     * 切换API状态
     */
    boolean toggleApiStatus(Long id, Long userId);
}