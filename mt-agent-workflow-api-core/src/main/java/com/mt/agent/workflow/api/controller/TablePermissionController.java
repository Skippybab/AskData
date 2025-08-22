package com.mt.agent.workflow.api.controller;

import com.mt.agent.workflow.api.util.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 表权限管理控制器
 * 根据项目需求，用户登录后无需权限控制，所有接口返回默认权限开放状态
 */
@Slf4j
@RestController
@RequestMapping("/api/table-permission")
@CrossOrigin
public class TablePermissionController {
    
    /**
     * 检查用户是否有表的查询权限
     * 根据项目需求，用户登录后无需权限控制，默认返回有权限
     */
    @GetMapping("/check")
    public Result<Map<String, Object>> checkPermission(
            @RequestParam Long userId,
            @RequestParam Long dbConfigId,
            @RequestParam String tableName) {
        try {
            log.info("📋 [权限检查] 用户权限检查请求, userId: {}, dbConfigId: {}, tableName: {}", 
                    userId, dbConfigId, tableName);
            
            // 根据项目需求，用户登录后无需权限控制，默认有权限
            Map<String, Object> result = new HashMap<>();
            result.put("hasPermission", true);
            result.put("userId", userId);
            result.put("dbConfigId", dbConfigId);
            result.put("tableName", tableName);
            result.put("message", "系统当前无权限限制，默认拥有所有权限");
            
            return Result.success(result);
        } catch (Exception e) {
            log.error("检查表权限失败: {}", e.getMessage(), e);
            return Result.error("检查权限失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取用户有权限的表列表
     * 根据项目需求，用户登录后无需权限控制，返回空列表（表示所有表都可访问）
     */
    @GetMapping("/tables")
    public Result<Map<String, Object>> getUserAccessibleTables(
            @RequestParam Long userId,
            @RequestParam Long dbConfigId) {
        try {
            log.info("📋 [权限检查] 获取用户可访问表列表请求, userId: {}, dbConfigId: {}", 
                    userId, dbConfigId);
            
            // 根据项目需求，用户登录后无需权限控制，所有表都可访问
            Map<String, Object> result = new HashMap<>();
            result.put("accessibleTables", List.of());  // 空列表表示所有表都可访问
            result.put("userId", userId);
            result.put("dbConfigId", dbConfigId);
            result.put("count", 0);
            result.put("message", "系统当前无权限限制，所有已启用的表都可访问");
            
            return Result.success(result);
        } catch (Exception e) {
            log.error("获取用户可访问表列表失败: {}", e.getMessage(), e);
            return Result.error("获取表列表失败: " + e.getMessage());
        }
    }
    
    /**
     * 为用户授权表访问权限
     * 根据项目需求，用户登录后无需权限控制，直接返回成功
     */
    @PostMapping("/grant")
    public Result<Map<String, Object>> grantPermission(@RequestBody Map<String, Object> request) {
        try {
            Long userId = Long.valueOf(request.get("userId").toString());
            Long dbConfigId = Long.valueOf(request.get("dbConfigId").toString());
            String tableName = (String) request.get("tableName");
            Integer permissionType = (Integer) request.getOrDefault("permissionType", 1);
            
            log.info("📋 [权限授予] 权限授予请求, userId: {}, dbConfigId: {}, tableName: {}, permissionType: {}", 
                    userId, dbConfigId, tableName, permissionType);
            
            // 根据项目需求，用户登录后无需权限控制，直接返回成功
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("userId", userId);
            result.put("dbConfigId", dbConfigId);
            result.put("tableName", tableName);
            result.put("permissionType", permissionType);
            result.put("message", "权限授予成功，系统当前无权限限制");
            
            return Result.success(result);
        } catch (Exception e) {
            log.error("授权表权限失败: {}", e.getMessage(), e);
            return Result.error("授权失败: " + e.getMessage());
        }
    }
    
    /**
     * 撤销用户的表访问权限
     * 根据项目需求，用户登录后无需权限控制，直接返回成功
     */
    @PostMapping("/revoke")
    public Result<Map<String, Object>> revokePermission(@RequestBody Map<String, Object> request) {
        try {
            Long userId = Long.valueOf(request.get("userId").toString());
            Long dbConfigId = Long.valueOf(request.get("dbConfigId").toString());
            String tableName = (String) request.get("tableName");
            
            log.info("📋 [权限撤销] 权限撤销请求, userId: {}, dbConfigId: {}, tableName: {}", 
                    userId, dbConfigId, tableName);
            
            // 根据项目需求，用户登录后无需权限控制，直接返回成功
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("userId", userId);
            result.put("dbConfigId", dbConfigId);
            result.put("tableName", tableName);
            result.put("message", "权限撤销成功，系统当前无权限限制，实际上所有用户都有权限");
            
            return Result.success(result);
        } catch (Exception e) {
            log.error("撤销表权限失败: {}", e.getMessage(), e);
            return Result.error("撤销权限失败: " + e.getMessage());
        }
    }
}
