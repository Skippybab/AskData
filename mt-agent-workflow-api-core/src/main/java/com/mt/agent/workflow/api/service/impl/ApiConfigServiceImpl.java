package com.mt.agent.workflow.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mt.agent.workflow.api.entity.ApiConfig;
import com.mt.agent.workflow.api.mapper.ApiConfigMapper;
import com.mt.agent.workflow.api.service.ApiConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * API配置服务实现类
 */
@Slf4j
@Service
public class ApiConfigServiceImpl implements ApiConfigService {
    
    @Autowired
    private ApiConfigMapper apiConfigMapper;
    
    @Override
    public IPage<ApiConfig> getApiConfigPage(Page<ApiConfig> page, String apiName, Integer status, Long userId) {
        QueryWrapper<ApiConfig> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        
        if (apiName != null && !apiName.trim().isEmpty()) {
            wrapper.like("api_name", apiName);
        }
        
        if (status != null) {
            wrapper.eq("status", status);
        }
        
        wrapper.orderByDesc("create_time");
        return apiConfigMapper.selectPage(page, wrapper);
    }
    
    @Override
    public ApiConfig getApiConfigById(Long id) {
        return apiConfigMapper.selectById(id);
    }
    
    @Override
    public ApiConfig getApiConfigByPath(String apiPath) {
        QueryWrapper<ApiConfig> wrapper = new QueryWrapper<>();
        wrapper.eq("api_path", apiPath);
        wrapper.eq("status", 1); // 只查询启用的API
        return apiConfigMapper.selectOne(wrapper);
    }
    
    @Override
    @Transactional
    public ApiConfig createApiConfig(ApiConfig apiConfig) {
        // 检查API路径是否已存在
        QueryWrapper<ApiConfig> wrapper = new QueryWrapper<>();
        wrapper.eq("api_path", apiConfig.getApiPath());
        Long count = apiConfigMapper.selectCount(wrapper);
        if (count > 0) {
            throw new RuntimeException("API路径已存在");
        }
        
        // 生成API密钥
        if (apiConfig.getApiKey() == null || apiConfig.getApiKey().isEmpty()) {
            apiConfig.setApiKey(generateApiKey());
        }
        
        // 设置默认值
        apiConfig.setStatus(1); // 默认启用
        apiConfig.setCallCount(0L);
        apiConfig.setCreateTime(new Date());
        apiConfig.setUpdateTime(new Date());
        
        if (apiConfig.getRateLimit() == null) {
            apiConfig.setRateLimit(60); // 默认每分钟60次
        }
        
        if (apiConfig.getTimeout() == null) {
            apiConfig.setTimeout(30); // 默认30秒超时
        }
        
        apiConfigMapper.insert(apiConfig);
        return apiConfig;
    }
    
    @Override
    @Transactional
    public boolean updateApiConfig(ApiConfig apiConfig) {
        ApiConfig existing = apiConfigMapper.selectById(apiConfig.getId());
        if (existing == null) {
            return false;
        }
        
        // 如果修改了API路径，检查是否重复
        if (!existing.getApiPath().equals(apiConfig.getApiPath())) {
            QueryWrapper<ApiConfig> wrapper = new QueryWrapper<>();
            wrapper.eq("api_path", apiConfig.getApiPath());
            wrapper.ne("id", apiConfig.getId());
            Long count = apiConfigMapper.selectCount(wrapper);
            if (count > 0) {
                throw new RuntimeException("API路径已存在");
            }
        }
        
        apiConfig.setUpdateTime(new Date());
        return apiConfigMapper.updateById(apiConfig) > 0;
    }
    
    @Override
    @Transactional
    public boolean deleteApiConfig(Long id, Long userId) {
        QueryWrapper<ApiConfig> wrapper = new QueryWrapper<>();
        wrapper.eq("id", id);
        wrapper.eq("user_id", userId);
        return apiConfigMapper.delete(wrapper) > 0;
    }
    
    @Override
    public String generateApiKey() {
        // 生成32位的API密钥
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return "sk-" + uuid;
    }
    
    @Override
    public boolean validateApiKey(String apiPath, String apiKey) {
        if (apiPath == null || apiKey == null) {
            return false;
        }
        
        QueryWrapper<ApiConfig> wrapper = new QueryWrapper<>();
        wrapper.eq("api_path", apiPath);
        wrapper.eq("api_key", apiKey);
        wrapper.eq("status", 1); // 只验证启用的API
        
        ApiConfig config = apiConfigMapper.selectOne(wrapper);
        return config != null;
    }
    
    @Override
    @Transactional
    public void updateCallStatistics(Long id) {
        ApiConfig config = apiConfigMapper.selectById(id);
        if (config != null) {
            config.setCallCount(config.getCallCount() + 1);
            config.setLastCallTime(new Date());
            apiConfigMapper.updateById(config);
        }
    }
    
    @Override
    public List<ApiConfig> getUserApiConfigs(Long userId) {
        QueryWrapper<ApiConfig> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        wrapper.orderByDesc("create_time");
        return apiConfigMapper.selectList(wrapper);
    }
    
    @Override
    @Transactional
    public boolean toggleApiStatus(Long id, Long userId) {
        QueryWrapper<ApiConfig> wrapper = new QueryWrapper<>();
        wrapper.eq("id", id);
        wrapper.eq("user_id", userId);
        
        ApiConfig config = apiConfigMapper.selectOne(wrapper);
        if (config == null) {
            return false;
        }
        
        config.setStatus(config.getStatus() == 1 ? 0 : 1);
        config.setUpdateTime(new Date());
        return apiConfigMapper.updateById(config) > 0;
    }
}