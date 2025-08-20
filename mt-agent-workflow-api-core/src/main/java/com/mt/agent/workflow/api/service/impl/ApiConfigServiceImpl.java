package com.mt.agent.workflow.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mt.agent.workflow.api.entity.ApiConfig;
import com.mt.agent.workflow.api.mapper.ApiConfigMapper;
import com.mt.agent.workflow.api.service.ApiConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * API配置服务实现
 */
@Slf4j
@Service
public class ApiConfigServiceImpl extends ServiceImpl<ApiConfigMapper, ApiConfig> implements ApiConfigService {
    
    @Override
    public IPage<ApiConfig> getApiConfigPage(Page<ApiConfig> page, String apiName, Integer status, Long userId) {
        QueryWrapper<ApiConfig> wrapper = new QueryWrapper<>();
        if (apiName != null && !apiName.trim().isEmpty()) {
            wrapper.like("api_name", apiName);
        }
        if (status != null) {
            wrapper.eq("status", status);
        }
        if (userId != null) {
            wrapper.eq("user_id", userId);
        }
        wrapper.orderByDesc("create_time");
        return this.page(page, wrapper);
    }
    
    @Override
    public ApiConfig getApiConfigById(Long id) {
        return this.getById(id);
    }
    
    @Override
    public ApiConfig createApiConfig(ApiConfig apiConfig) {
        // 生成唯一的API路径和密钥
        if (apiConfig.getApiPath() == null || apiConfig.getApiPath().trim().isEmpty()) {
            apiConfig.setApiPath(generateApiPath());
        }
        if (apiConfig.getApiKey() == null || apiConfig.getApiKey().trim().isEmpty()) {
            apiConfig.setApiKey(generateApiKey());
        }
        
        // 设置默认值
        if (apiConfig.getStatus() == null) {
            apiConfig.setStatus(1); // 默认启用
        }
        if (apiConfig.getRateLimit() == null) {
            apiConfig.setRateLimit(60); // 默认每分钟60次
        }
        if (apiConfig.getTimeout() == null) {
            apiConfig.setTimeout(30); // 默认30秒超时
        }
        if (apiConfig.getCallCount() == null) {
            apiConfig.setCallCount(0L);
        }
        
        Date now = new Date();
        apiConfig.setCreateTime(now);
        apiConfig.setUpdateTime(now);
        
        this.save(apiConfig);
        log.info("创建API配置 - 用户: {}, 名称: {}, 路径: {}", 
                apiConfig.getUserId(), apiConfig.getApiName(), apiConfig.getApiPath());
        
        return apiConfig;
    }
    
    @Override
    public boolean updateApiConfig(ApiConfig apiConfig) {
        apiConfig.setUpdateTime(new Date());
        return this.updateById(apiConfig);
    }
    
    @Override
    public ApiConfig getApiConfigByPath(String apiPath) {
        QueryWrapper<ApiConfig> wrapper = new QueryWrapper<>();
        wrapper.eq("api_path", apiPath);
        return this.getOne(wrapper);
    }
    
    @Override
    public boolean deleteApiConfig(Long id, Long userId) {
        QueryWrapper<ApiConfig> wrapper = new QueryWrapper<>();
        wrapper.eq("id", id);
        if (userId != null) {
            wrapper.eq("user_id", userId); // 确保只能删除自己的API
        }
        
        boolean result = this.remove(wrapper);
        if (result) {
            log.info("删除API配置 - ID: {}, 用户: {}", id, userId);
        }
        return result;
    }
    
    @Override
    public String generateApiKey() {
        return "sk_" + UUID.randomUUID().toString().replace("-", "").toLowerCase();
    }
    
    @Override
    public boolean validateApiKey(String apiPath, String apiKey) {
        if (apiKey == null || apiKey.trim().isEmpty() || apiPath == null || apiPath.trim().isEmpty()) {
            return false;
        }
        
        QueryWrapper<ApiConfig> wrapper = new QueryWrapper<>();
        wrapper.eq("api_path", apiPath);
        wrapper.eq("api_key", apiKey);
        wrapper.eq("status", 1); // 只查询启用的API
        
        ApiConfig config = this.getOne(wrapper);
        if (config != null) {
            log.debug("API密钥验证成功 - API路径: {}, 用户: {}", apiPath, config.getUserId());
            return true;
        } else {
            log.warn("API密钥验证失败 - 路径: {}, 密钥: {}...", 
                    apiPath, apiKey.substring(0, Math.min(8, apiKey.length())));
            return false;
        }
    }
    
    @Override
    @Transactional
    public void updateCallStatistics(Long id) {
        ApiConfig config = this.getById(id);
        if (config != null) {
            config.setCallCount(config.getCallCount() + 1);
            config.setLastCallTime(new Date());
            config.setUpdateTime(new Date());
            this.updateById(config);
        }
    }
    
    @Override
    public List<ApiConfig> getUserApiConfigs(Long userId) {
        QueryWrapper<ApiConfig> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        wrapper.orderByDesc("create_time");
        return this.list(wrapper);
    }
    
    @Override
    public boolean toggleApiStatus(Long id, Long userId) {
        ApiConfig config = this.getById(id);
        if (config != null) {
            // 验证权限
            if (userId != null && !config.getUserId().equals(userId)) {
                log.warn("用户{}无权切换API{}的状态", userId, id);
                return false;
            }
            
            // 切换状态
            config.setStatus(config.getStatus() == 1 ? 0 : 1);
            config.setUpdateTime(new Date());
            boolean result = this.updateById(config);
            
            log.info("切换API状态 - ID: {}, 新状态: {}", id, config.getStatus() == 1 ? "启用" : "禁用");
            return result;
        }
        return false;
    }
    
    /**
     * 生成唯一的API路径
     */
    private String generateApiPath() {
        String path;
        int attempts = 0;
        do {
            path = "api_" + UUID.randomUUID().toString().substring(0, 8).toLowerCase();
            attempts++;
            if (attempts > 10) {
                throw new RuntimeException("无法生成唯一的API路径");
            }
        } while (getApiConfigByPath(path) != null);
        
        return path;
    }
}