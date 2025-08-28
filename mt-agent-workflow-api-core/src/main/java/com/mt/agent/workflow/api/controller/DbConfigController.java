package com.mt.agent.workflow.api.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mt.agent.workflow.api.entity.DbConfig;
import com.mt.agent.workflow.api.mapper.DbConfigMapper;
import com.mt.agent.workflow.api.service.DbConfigService;
import com.mt.agent.workflow.api.service.SchemaSyncService;
import com.mt.agent.workflow.api.util.Result;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/db")
@CrossOrigin
public class DbConfigController {

    @Autowired
    private DbConfigService dbConfigService;
    @Autowired
    private SchemaSyncService schemaSyncService;
    @Autowired
    private DbConfigMapper dbConfigMapper;

    @PostMapping("/config")
    public Result<DbConfig> saveConfig(@RequestBody DbConfig config, HttpServletRequest request) {
        try {
            // 使用默认用户ID
            Long userId = 1L;
            config.setUserId(userId);
            DbConfig saved = dbConfigService.createOrUpdate(config);
            return Result.success(saved);
        } catch (Exception e) {
            return Result.error("保存失败: " + e.getMessage());
        }
    }

    @GetMapping("/config/{id}")
    public Result<DbConfig> getConfig(@PathVariable Long id, HttpServletRequest request) {
        try {
            Long userId = 1L;
            DbConfig cfg = dbConfigService.getById(userId, id);
            if (cfg == null) {
                return Result.error("配置不存在");
            }
            cfg.setPasswordCipher(null);
            cfg.setRawPassword(null);
            return Result.success(cfg);
        } catch (Exception e) {
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    @GetMapping("/configs")
    public Result<IPage<DbConfig>> listConfigs(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String host,
            HttpServletRequest request
    ) {
        try {
            
            Page<DbConfig> page = new Page<>(current, size);
            LambdaQueryWrapper<DbConfig> qw = new LambdaQueryWrapper<>();
            if (name != null && !name.isEmpty()) {
                qw.like(DbConfig::getName, name);
            }
            if (host != null && !host.isEmpty()) {
                qw.like(DbConfig::getHost, host);
            }
            // 只显示启用状态的配置
            qw.eq(DbConfig::getStatus, 1);
            qw.orderByAsc(DbConfig::getId);
            IPage<DbConfig> result = dbConfigMapper.selectPage(page, qw);
            if (result.getRecords() != null) {
                result.getRecords().forEach(cfg -> {
                    cfg.setPasswordCipher(null);
                    cfg.setRawPassword(null);
                });
            }
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    @PostMapping("/config/{id}/verify")
    public Result<Boolean> verify(@PathVariable Long id, HttpServletRequest request) {
        try {
            Long userId = 1L;

            
            boolean ok = dbConfigService.verifyConnection(userId, id);
            return ok ? Result.success(true) : Result.error("连接失败");
        } catch (SecurityException e) {
            return Result.error("操作失败: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            return Result.error("参数错误: " + e.getMessage());
        } catch (RuntimeException e) {
            return Result.error("连接失败: " + e.getMessage());
        } catch (Exception e) {
            return Result.error("服务器内部错误: " + e.getMessage());
        }
    }

    @PostMapping("/schema/{id}/sync")
    public Result<Long> startSync(@PathVariable Long id) {

        return Result.success(schemaSyncService.startSync(id).getId());
    }

    @DeleteMapping("/config/{id}")
    public Result<Boolean> deleteConfig(@PathVariable Long id, HttpServletRequest request) {
        try {
            Long userId = 1L;
            // 删除配置
            boolean deleted = dbConfigService.deleteConfig(userId, id);
            return deleted ? Result.success(true) : Result.error("删除失败");
        } catch (Exception e) {
            return Result.error("删除失败: " + e.getMessage());
        }
    }
    
    /**
     * 更新数据库配置状态
     */
    @PutMapping("/config/{id}/status")
    public Result<Boolean> updateConfigStatus(
            @PathVariable Long id,
            @RequestBody Map<String, Integer> requestBody,
            HttpServletRequest request) {
        try {
            Long userId = 1L;
            Integer status = requestBody.get("status");
            if (status == null) {
                return Result.error("状态参数不能为空");
            }
            boolean success = dbConfigService.updateStatus(userId, id, status);
            return success ? Result.success(true) : Result.error("更新失败");
        } catch (Exception e) {
            // log.error("更新数据库配置状态失败: {}", e.getMessage(), e); // Original code had this line commented out
            return Result.error("更新失败: " + e.getMessage());
        }
    }
    
    /**
     * 重新加密数据库密码（修复密钥不一致）
     */
    @PostMapping("/config/{id}/re-encrypt")
    public Result<Boolean> reEncryptPassword(
            @PathVariable Long id,
            @RequestBody Map<String, String> requestBody,
            HttpServletRequest request) {
        try {
            Long userId = 1L;
            String newPassword = requestBody.get("password");
            if (newPassword == null || newPassword.isEmpty()) {
                return Result.error("新密码不能为空");
            }
            
            // 获取现有配置
            DbConfig config = dbConfigService.getById(userId, id);
            if (config == null) {
                return Result.error("数据库配置不存在");
            }
            
            // 设置新密码并重新加密
            config.setRawPassword(newPassword);
            DbConfig updated = dbConfigService.createOrUpdate(config);
            
            return updated != null ? Result.success(true) : Result.error("重新加密失败");
        } catch (Exception e) {
            // log.error("重新加密数据库密码失败: {}", e.getMessage(), e); // Original code had this line commented out
            return Result.error("重新加密失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取所有启用的数据库配置
     */
    @GetMapping("/configs/enabled")
    public Result<List<DbConfig>> getEnabledConfigs(HttpServletRequest request) {
        try {
            List<DbConfig> configs = dbConfigService.getEnabledConfigs();
            // 隐藏敏感信息
            configs.forEach(config -> {
                config.setPasswordCipher(null);
                config.setRawPassword(null);
            });
            return Result.success(configs);
        } catch (Exception e) {
            return Result.error("获取失败: " + e.getMessage());
        }
    }
}


