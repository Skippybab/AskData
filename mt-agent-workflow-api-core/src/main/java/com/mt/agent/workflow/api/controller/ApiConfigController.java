package com.mt.agent.workflow.api.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mt.agent.workflow.api.entity.ApiConfig;
import com.mt.agent.workflow.api.service.ApiConfigService;
import com.mt.agent.workflow.api.util.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * API配置管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/api-config")
@CrossOrigin
public class ApiConfigController {
    
    @Autowired
    private ApiConfigService apiConfigService;
    
    /**
     * 分页查询API配置列表
     */
    @GetMapping("/page")
    public Result<IPage<ApiConfig>> getApiConfigPage(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String apiName,
            @RequestParam(required = false) Integer status,
            HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            if (userId == null) {
                userId = 1L; // 临时默认用户ID
            }
            
            Page<ApiConfig> page = new Page<>(current, size);
            IPage<ApiConfig> result = apiConfigService.getApiConfigPage(page, apiName, status, userId);
            return Result.success(result);
        } catch (Exception e) {
            log.error("查询API配置列表失败", e);
            return Result.error("查询失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取用户的所有API配置
     */
    @GetMapping("/list")
    public Result<List<ApiConfig>> getUserApiConfigs(HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            if (userId == null) {
                userId = 1L; // 临时默认用户ID
            }
            
            List<ApiConfig> configs = apiConfigService.getUserApiConfigs(userId);
            return Result.success(configs);
        } catch (Exception e) {
            log.error("获取用户API配置失败", e);
            return Result.error("获取失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取API配置详情
     */
    @GetMapping("/{id}")
    public Result<ApiConfig> getApiConfig(@PathVariable Long id) {
        try {
            ApiConfig config = apiConfigService.getApiConfigById(id);
            if (config == null) {
                return Result.error("API配置不存在");
            }
            return Result.success(config);
        } catch (Exception e) {
            log.error("获取API配置详情失败", e);
            return Result.error("获取失败：" + e.getMessage());
        }
    }
    
    /**
     * 创建API配置
     */
    @PostMapping
    public Result<ApiConfig> createApiConfig(@RequestBody ApiConfig apiConfig, HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            if (userId == null) {
                userId = 1L; // 临时默认用户ID
            }
            apiConfig.setUserId(userId);
            
            ApiConfig created = apiConfigService.createApiConfig(apiConfig);
            return Result.success(created);
        } catch (Exception e) {
            log.error("创建API配置失败", e);
            return Result.error("创建失败：" + e.getMessage());
        }
    }
    
    /**
     * 更新API配置
     */
    @PutMapping("/{id}")
    public Result<Boolean> updateApiConfig(@PathVariable Long id, @RequestBody ApiConfig apiConfig) {
        try {
            apiConfig.setId(id);
            boolean success = apiConfigService.updateApiConfig(apiConfig);
            return success ? Result.success(true) : Result.error("更新失败");
        } catch (Exception e) {
            log.error("更新API配置失败", e);
            return Result.error("更新失败：" + e.getMessage());
        }
    }
    
    /**
     * 删除API配置
     */
    @DeleteMapping("/{id}")
    public Result<Boolean> deleteApiConfig(@PathVariable Long id, HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            if (userId == null) {
                userId = 1L; // 临时默认用户ID
            }
            
            boolean success = apiConfigService.deleteApiConfig(id, userId);
            return success ? Result.success(true) : Result.error("删除失败");
        } catch (Exception e) {
            log.error("删除API配置失败", e);
            return Result.error("删除失败：" + e.getMessage());
        }
    }
    
    /**
     * 切换API状态
     */
    @PutMapping("/{id}/toggle-status")
    public Result<Boolean> toggleApiStatus(@PathVariable Long id, HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            if (userId == null) {
                userId = 1L; // 临时默认用户ID
            }
            
            boolean success = apiConfigService.toggleApiStatus(id, userId);
            return success ? Result.success(true) : Result.error("切换状态失败");
        } catch (Exception e) {
            log.error("切换API状态失败", e);
            return Result.error("切换失败：" + e.getMessage());
        }
    }
    
    /**
     * 生成新的API密钥
     */
    @PostMapping("/{id}/regenerate-key")
    public Result<Map<String, String>> regenerateApiKey(@PathVariable Long id, HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            if (userId == null) {
                userId = 1L; // 临时默认用户ID
            }
            
            ApiConfig config = apiConfigService.getApiConfigById(id);
            if (config == null || !config.getUserId().equals(userId)) {
                return Result.error("API配置不存在或无权限");
            }
            
            String newKey = apiConfigService.generateApiKey();
            config.setApiKey(newKey);
            apiConfigService.updateApiConfig(config);
            
            Map<String, String> result = new HashMap<>();
            result.put("apiKey", newKey);
            return Result.success(result);
        } catch (Exception e) {
            log.error("重新生成API密钥失败", e);
            return Result.error("生成失败：" + e.getMessage());
        }
    }
}