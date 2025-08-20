package com.mt.agent.workflow.api.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mt.agent.workflow.api.entity.ApiConfig;
import com.mt.agent.workflow.api.service.ApiConfigService;
import com.mt.agent.workflow.api.util.Result;
import com.mt.agent.workflow.api.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * API配置管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/api-config")
@RequiredArgsConstructor
public class ApiConfigController {
    
    private final ApiConfigService apiConfigService;
    private final SecurityUtil securityUtil;
    
    /**
     * 分页查询API配置列表
     */
    @GetMapping("/page")
    public Result<IPage<ApiConfig>> getApiConfigPage(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String apiName,
            @RequestParam(required = false) Integer status) {
        
        Long userId = securityUtil.getCurrentUserId();
        Page<ApiConfig> page = new Page<>(current, size);
        IPage<ApiConfig> result = apiConfigService.getApiConfigPage(page, apiName, status, userId);
        
        return Result.success(result);
    }
    
    /**
     * 获取用户的所有API配置
     */
    @GetMapping("/list")
    public Result<List<ApiConfig>> getUserApiConfigs() {
        Long userId = securityUtil.getCurrentUserId();
        List<ApiConfig> configs = apiConfigService.getUserApiConfigs(userId);
        return Result.success(configs);
    }
    
    /**
     * 获取API配置详情
     */
    @GetMapping("/{id}")
    public Result<ApiConfig> getApiConfigById(@PathVariable Long id) {
        ApiConfig config = apiConfigService.getApiConfigById(id);
        if (config == null) {
            return Result.error("API配置不存在");
        }
        
        // 验证权限
        Long userId = securityUtil.getCurrentUserId();
        if (!config.getUserId().equals(userId)) {
            return Result.error("无权访问该API配置");
        }
        
        return Result.success(config);
    }
    
    /**
     * 创建API配置
     */
    @PostMapping("/create")
    public Result<ApiConfig> createApiConfig(@RequestBody ApiConfig apiConfig) {
        Long userId = securityUtil.getCurrentUserId();
        apiConfig.setUserId(userId);
        
        // 验证必填字段
        if (apiConfig.getApiName() == null || apiConfig.getApiName().trim().isEmpty()) {
            return Result.error("API名称不能为空");
        }
        if (apiConfig.getDbConfigId() == null) {
            return Result.error("数据库配置不能为空");
        }
        
        ApiConfig created = apiConfigService.createApiConfig(apiConfig);
        log.info("用户{}创建API配置: {}", userId, created.getApiName());
        
        return Result.success(created);
    }
    
    /**
     * 更新API配置
     */
    @PutMapping("/update")
    public Result<String> updateApiConfig(@RequestBody ApiConfig apiConfig) {
        if (apiConfig.getId() == null) {
            return Result.error("API配置ID不能为空");
        }
        
        // 验证权限
        ApiConfig existing = apiConfigService.getApiConfigById(apiConfig.getId());
        if (existing == null) {
            return Result.error("API配置不存在");
        }
        
        Long userId = securityUtil.getCurrentUserId();
        if (!existing.getUserId().equals(userId)) {
            return Result.error("无权修改该API配置");
        }
        
        // 保留不可修改的字段
        apiConfig.setUserId(existing.getUserId());
        apiConfig.setApiPath(existing.getApiPath());
        apiConfig.setApiKey(existing.getApiKey());
        apiConfig.setCreateTime(existing.getCreateTime());
        apiConfig.setCallCount(existing.getCallCount());
        apiConfig.setLastCallTime(existing.getLastCallTime());
        
        boolean success = apiConfigService.updateApiConfig(apiConfig);
        
        return success ? Result.success("更新成功") : Result.error("更新失败");
    }
    
    /**
     * 删除API配置
     */
    @DeleteMapping("/{id}")
    public Result<String> deleteApiConfig(@PathVariable Long id) {
        Long userId = securityUtil.getCurrentUserId();
        boolean success = apiConfigService.deleteApiConfig(id, userId);
        
        return success ? Result.success("删除成功") : Result.error("删除失败");
    }
    
    /**
     * 切换API状态（启用/禁用）
     */
    @PutMapping("/status/{id}")
    public Result<String> toggleApiStatus(@PathVariable Long id) {
        Long userId = securityUtil.getCurrentUserId();
        boolean success = apiConfigService.toggleApiStatus(id, userId);
        
        return success ? Result.success("状态切换成功") : Result.error("状态切换失败");
    }
    
    /**
     * 重新生成API密钥
     */
    @PostMapping("/regenerate-key/{id}")
    public Result<String> regenerateApiKey(@PathVariable Long id) {
        // 验证权限
        ApiConfig config = apiConfigService.getApiConfigById(id);
        if (config == null) {
            return Result.error("API配置不存在");
        }
        
        Long userId = securityUtil.getCurrentUserId();
        if (!config.getUserId().equals(userId)) {
            return Result.error("无权操作该API配置");
        }
        
        // 重新生成密钥
        String newKey = apiConfigService.generateApiKey();
        config.setApiKey(newKey);
        boolean success = apiConfigService.updateApiConfig(config);
        
        if (success) {
            log.info("用户{}重新生成API{}的密钥", userId, id);
            return Result.success(newKey);
        } else {
            return Result.error("重新生成密钥失败");
        }
    }
    
    /**
     * 验证API密钥
     */
    @PostMapping("/validate")
    public Result<Boolean> validateApiKey(
            @RequestParam String apiPath,
            @RequestParam String apiKey) {
        
        boolean valid = apiConfigService.validateApiKey(apiPath, apiKey);
        return Result.success(valid);
    }
}