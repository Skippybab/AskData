package com.mt.agent.workflow.api.controller;

import com.mt.agent.workflow.api.service.TablePermissionService;
import com.mt.agent.workflow.api.util.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 表权限管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/table-permission")
@CrossOrigin
public class TablePermissionController {
    
    @Autowired
    private TablePermissionService tablePermissionService;
    
    /**
     * 检查用户是否有表的查询权限
     */
    @GetMapping("/check")
    public Result<Map<String, Object>> checkPermission(
            @RequestParam Long userId,
            @RequestParam Long dbConfigId,
            @RequestParam String tableName) {
        try {
            boolean hasPermission = tablePermissionService.hasQueryPermission(userId, dbConfigId, tableName);
            Map<String, Object> result = new HashMap<>();
            result.put("hasPermission", hasPermission);
            result.put("userId", userId);
            result.put("dbConfigId", dbConfigId);
            result.put("tableName", tableName);
            
            return Result.success(result);
        } catch (Exception e) {
            log.error("检查表权限失败: {}", e.getMessage(), e);
            return Result.error("检查权限失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取用户有权限的表列表
     */
    @GetMapping("/tables")
    public Result<Map<String, Object>> getUserAccessibleTables(
            @RequestParam Long userId,
            @RequestParam Long dbConfigId) {
        try {
            List<String> accessibleTables = tablePermissionService.getUserAccessibleTables(userId, dbConfigId);
            Map<String, Object> result = new HashMap<>();
            result.put("accessibleTables", accessibleTables);
            result.put("userId", userId);
            result.put("dbConfigId", dbConfigId);
            result.put("count", accessibleTables.size());
            
            return Result.success(result);
        } catch (Exception e) {
            log.error("获取用户可访问表列表失败: {}", e.getMessage(), e);
            return Result.error("获取表列表失败: " + e.getMessage());
        }
    }
    
    /**
     * 为用户授权表访问权限
     */
    @PostMapping("/grant")
    public Result<Map<String, Object>> grantPermission(@RequestBody Map<String, Object> request) {
        try {
            Long userId = Long.valueOf(request.get("userId").toString());
            Long dbConfigId = Long.valueOf(request.get("dbConfigId").toString());
            String tableName = (String) request.get("tableName");
            Integer permissionType = (Integer) request.getOrDefault("permissionType", 1);
            
            boolean success = tablePermissionService.grantTablePermission(userId, dbConfigId, tableName, permissionType);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", success);
            result.put("userId", userId);
            result.put("dbConfigId", dbConfigId);
            result.put("tableName", tableName);
            result.put("permissionType", permissionType);
            
            return success ? Result.success(result) : Result.error("授权失败");
        } catch (Exception e) {
            log.error("授权表权限失败: {}", e.getMessage(), e);
            return Result.error("授权失败: " + e.getMessage());
        }
    }
    
    /**
     * 撤销用户的表访问权限
     */
    @PostMapping("/revoke")
    public Result<Map<String, Object>> revokePermission(@RequestBody Map<String, Object> request) {
        try {
            Long userId = Long.valueOf(request.get("userId").toString());
            Long dbConfigId = Long.valueOf(request.get("dbConfigId").toString());
            String tableName = (String) request.get("tableName");
            
            boolean success = tablePermissionService.revokeTablePermission(userId, dbConfigId, tableName);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", success);
            result.put("userId", userId);
            result.put("dbConfigId", dbConfigId);
            result.put("tableName", tableName);
            
            return success ? Result.success(result) : Result.error("撤销权限失败");
        } catch (Exception e) {
            log.error("撤销表权限失败: {}", e.getMessage(), e);
            return Result.error("撤销权限失败: " + e.getMessage());
        }
    }
}
