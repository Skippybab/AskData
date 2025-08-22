package com.mt.agent.workflow.api.controller;

import com.mt.agent.workflow.api.entity.TableInfo;
import com.mt.agent.workflow.api.service.TableInfoService;
import com.mt.agent.workflow.api.util.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * 表信息控制器
 * 提供数据库表信息查询接口
 */
@Slf4j
@RestController
@RequestMapping("/api/table-info")
@CrossOrigin
public class TableInfoController {

    @Autowired
    private TableInfoService tableInfoService;

    /**
     * 获取数据库的表列表
     * 
     * @param dbConfigId 数据库配置ID
     * @return 表信息列表
     */
    @GetMapping("/list")
    public Result<List<Map<String, Object>>> getTableList(@RequestParam Long dbConfigId) {
        log.info("📊 [表信息] 获取表列表请求, dbConfigId: {}", dbConfigId);
        
        try {
            // 使用默认用户ID
            Long userId = 1L;
            
            // 参数验证
            if (dbConfigId == null) {
                log.error("📊 [表信息] 数据库配置ID不能为空");
                return Result.error("数据库配置ID不能为空");
            }
            
            // 获取启用的表信息DDL
            String tablesDdl = tableInfoService.getEnabledTablesDdl(dbConfigId, userId);
            
            if (tablesDdl == null || tablesDdl.trim().isEmpty()) {
                log.warn("📊 [表信息] 未找到可用的表信息, dbConfigId: {}", dbConfigId);
                return Result.success(List.of()); // 返回空列表而不是错误
            }
            
            // 解析DDL获取表名列表（简化实现）
            List<Map<String, Object>> tableList = parseTableNamesFromDdl(tablesDdl);
            
            log.info("📊 [表信息] 成功获取到 {} 个表", tableList.size());
            return Result.success(tableList);
            
        } catch (Exception e) {
            log.error("📊 [表信息] 获取表列表失败: {}", e.getMessage(), e);
            return Result.error("获取表列表失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取指定表的详细信息
     * 
     * @param dbConfigId 数据库配置ID
     * @param tableId 表ID
     * @return 表详细信息
     */
    @GetMapping("/detail")
    public Result<String> getTableDetail(@RequestParam Long dbConfigId, @RequestParam Long tableId) {
        try {
            // 使用默认用户ID
            Long userId = 1L;
            
            // 参数验证
            if (dbConfigId == null || tableId == null) {
                log.error("📊 [表信息] 数据库配置ID和表ID不能为空");
                return Result.error("数据库配置ID和表ID不能为空");
            }
            
            // 获取单个表的格式化信息
            String tableInfo = tableInfoService.getStandardTableNameFormat(dbConfigId, tableId, userId);
            
            if (tableInfo == null || tableInfo.trim().isEmpty()) {
                log.warn("📊 [表信息] 未找到表信息, dbConfigId: {}, tableId: {}", dbConfigId, tableId);
                return Result.error("未找到指定的表信息");
            }
            return Result.success(tableInfo);
            
        } catch (Exception e) {
            log.error("📊 [表信息] 获取表详细信息失败: {}", e.getMessage(), e);
            return Result.error("获取表详细信息失败: " + e.getMessage());
        }
    }
    
    /**
     * 从DDL字符串中解析表名列表
     * 这是一个简化的实现，主要用于前端表选择
     */
    private List<Map<String, Object>> parseTableNamesFromDdl(String ddl) {
        List<Map<String, Object>> tables = new java.util.ArrayList<>();
        
        try {
            // 简单的DDL解析，提取CREATE TABLE语句中的表名
            String[] lines = ddl.split("\n");
            
            int tableId = 1;
            for (String line : lines) {
                String trimmedLine = line.trim().toUpperCase();
                if (trimmedLine.startsWith("CREATE TABLE")) {
                    // 提取表名：CREATE TABLE `table_name` 或 CREATE TABLE table_name
                    String tableName = extractTableName(line);
                    if (tableName != null && !tableName.isEmpty()) {
                        Map<String, Object> tableMap = new HashMap<>();
                        tableMap.put("id", tableId++);
                        tableMap.put("name", tableName);
                        tableMap.put("tableName", tableName);
                        tableMap.put("comment", ""); // 可以后续扩展提取注释
                        tables.add(tableMap);
                    }
                }
            }
            
            log.debug("📊 [表信息] 从DDL中解析出 {} 个表", tables.size());
            
        } catch (Exception e) {
            log.error("📊 [表信息] 解析DDL失败: {}", e.getMessage(), e);
        }
        
        return tables;
    }
    
    /**
     * 从CREATE TABLE语句中提取表名
     */
    private String extractTableName(String createTableLine) {
        try {
            // 匹配 CREATE TABLE `table_name` 或 CREATE TABLE table_name
            String upperLine = createTableLine.toUpperCase().trim();
            if (!upperLine.startsWith("CREATE TABLE")) {
                return null;
            }
            
            // 移除CREATE TABLE前缀
            String remaining = createTableLine.substring(12).trim();
            
            // 处理反引号包围的表名
            if (remaining.startsWith("`")) {
                int endIndex = remaining.indexOf("`", 1);
                if (endIndex > 0) {
                    return remaining.substring(1, endIndex);
                }
            }
            
            // 处理没有反引号的表名
            String[] parts = remaining.split("\\s+");
            if (parts.length > 0) {
                String tableName = parts[0];
                // 移除可能的反引号
                tableName = tableName.replace("`", "");
                return tableName;
            }
            
        } catch (Exception e) {
            log.warn("📊 [表信息] 提取表名失败: {}", createTableLine, e);
        }
        
        return null;
    }
    
    /**
     * 获取表字段信息
     * 
     * @param dbConfigId 数据库配置ID
     * @param tableId 表ID
     * @return 字段信息列表
     */
    @GetMapping("/columns")
    public Result<List<Map<String, Object>>> getTableColumns(@RequestParam Long dbConfigId, @RequestParam Long tableId) {
        try {
            log.info("📊 [字段管理] 获取表字段信息, dbConfigId: {}, tableId: {}", dbConfigId, tableId);
            
            List<Map<String, Object>> columns = tableInfoService.getTableColumns(dbConfigId, tableId);
            
            log.info("📊 [字段管理] 成功获取到 {} 个字段", columns.size());
            return Result.success(columns);
            
        } catch (Exception e) {
            log.error("📊 [字段管理] 获取字段信息失败: {}", e.getMessage(), e);
            return Result.error("获取字段信息失败: " + e.getMessage());
        }
    }
    
    /**
     * 更新字段备注
     * 
     * @param dbConfigId 数据库配置ID
     * @param tableId 表ID
     * @param columnName 字段名
     * @param comment 备注信息
     * @return 更新结果
     */
    @PutMapping("/columns/comment")
    public Result<String> updateColumnComment(@RequestParam Long dbConfigId, 
                                            @RequestParam Long tableId,
                                            @RequestParam String columnName,
                                            @RequestParam String comment) {
        try {
            log.info("📊 [字段管理] 更新字段备注, dbConfigId: {}, tableId: {}, columnName: {}", 
                    dbConfigId, tableId, columnName);
            
            boolean success = tableInfoService.updateColumnComment(dbConfigId, tableId, columnName, comment);
            
            if (success) {
                log.info("📊 [字段管理] 字段备注更新成功");
                return Result.success("字段备注更新成功");
            } else {
                log.warn("📊 [字段管理] 字段备注更新失败");
                return Result.error("字段备注更新失败");
            }
            
        } catch (Exception e) {
            log.error("📊 [字段管理] 更新字段备注失败: {}", e.getMessage(), e);
            return Result.error("更新字段备注失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取表访问权限
     * 根据项目需求，用户登录后对所有数据操作均有权限，无需权限控制
     * 
     * @param dbConfigId 数据库配置ID
     * @param tableId 表ID
     * @return 权限信息
     */
    @GetMapping("/permission")
    public Result<Map<String, Object>> getTablePermission(@RequestParam Long dbConfigId, @RequestParam Long tableId) {
        try {
            log.info("📊 [权限管理] 获取表访问权限, dbConfigId: {}, tableId: {}", dbConfigId, tableId);
            
            // 根据项目需求，用户登录后无需任何权限控制，默认拥有所有权限
            Map<String, Object> permission = new HashMap<>();
            permission.put("hasQueryPermission", true);
            permission.put("hasInsertPermission", true);
            permission.put("hasUpdatePermission", true);
            permission.put("hasDeletePermission", true);
            
            log.info("📊 [权限管理] 成功获取权限信息（默认全部权限开放）");
            return Result.success(permission);
            
        } catch (Exception e) {
            log.error("📊 [权限管理] 获取权限信息失败: {}", e.getMessage(), e);
            return Result.error("获取权限信息失败: " + e.getMessage());
        }
    }
    
    /**
     * 更新表访问权限
     * 根据项目需求，用户登录后无需权限控制，此接口保留但不执行实际权限操作
     * 
     * @param dbConfigId 数据库配置ID
     * @param tableId 表ID
     * @param enabled 是否启用
     * @return 更新结果
     */
    @PutMapping("/permission")
    public Result<String> updateTablePermission(@RequestParam Long dbConfigId, 
                                              @RequestParam Long tableId,
                                              @RequestParam Boolean enabled) {
        try {
            log.info("📊 [权限管理] 更新表访问权限, dbConfigId: {}, tableId: {}, enabled: {}", 
                    dbConfigId, tableId, enabled);
            
            // 根据项目需求，用户登录后无需权限控制，直接返回成功
            log.info("📊 [权限管理] 权限更新成功（无权限控制模式）");
            return Result.success("权限设置成功，系统当前无权限限制");
            
        } catch (Exception e) {
            log.error("📊 [权限管理] 更新权限失败: {}", e.getMessage(), e);
            return Result.error("更新权限失败: " + e.getMessage());
        }
    }
    
    /**
     * 批量启用数据库下的所有表
     * 解决用户需要手动开启数据库表权限的问题
     * 
     * @param dbConfigId 数据库配置ID
     * @return 更新结果
     */
    @PostMapping("/enable-all")
    public Result<String> enableAllTables(@RequestParam Long dbConfigId) {
        try {
            log.info("📊 [批量启用] 开始批量启用所有表, dbConfigId: {}", dbConfigId);
            
            // 参数验证
            if (dbConfigId == null) {
                log.error("📊 [批量启用] 数据库配置ID不能为空");
                return Result.error("数据库配置ID不能为空");
            }
            
            int enabledCount = tableInfoService.enableAllTables(dbConfigId);
            
            log.info("📊 [批量启用] 成功启用 {} 个表", enabledCount);
            return Result.success(String.format("成功启用 %d 个表", enabledCount));
            
        } catch (Exception e) {
            log.error("📊 [批量启用] 批量启用表失败: {}", e.getMessage(), e);
            return Result.error("批量启用表失败: " + e.getMessage());
        }
    }
}