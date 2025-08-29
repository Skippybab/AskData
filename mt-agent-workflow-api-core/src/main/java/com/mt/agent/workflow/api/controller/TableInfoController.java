package com.mt.agent.workflow.api.controller;

import com.mt.agent.workflow.api.service.TableInfoService;
import com.mt.agent.workflow.api.util.Result;
import com.mt.agent.workflow.api.util.BufferUtil;
import com.mt.agent.workflow.api.util.TableSelectionHashUtil;
import com.mt.agent.workflow.api.entity.TableInfo;
import com.mt.agent.workflow.api.mapper.TableInfoMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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
    
    @Autowired
    private BufferUtil bufferUtil;
    
    @Autowired
    private TableInfoMapper tableInfoMapper;

    /**
     * 获取数据库的表列表
     * 直接从table_info表查询，返回真实的表记录
     * 
     * @param dbConfigId 数据库配置ID
     * @return 表信息列表
     */
    @GetMapping("/list")
    public Result<List<Map<String, Object>>> getTableList(@RequestParam Long dbConfigId) {
//        log.info("📊 [表信息] 获取表列表请求, dbConfigId: {}", dbConfigId);
        
        try {
            // 参数验证
            if (dbConfigId == null) {
                log.error("📊 [表信息] 数据库配置ID不能为空");
                return Result.error("数据库配置ID不能为空");
            }
            
            // 直接从table_info表查询启用的表
            QueryWrapper<TableInfo> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("db_config_id", dbConfigId);
            queryWrapper.eq("enabled", 1);
            queryWrapper.orderBy(true, true, "table_name"); // 按表名排序
            
            List<TableInfo> tableInfos = tableInfoMapper.selectList(queryWrapper);
            
            if (tableInfos.isEmpty()) {
                log.warn("📊 [表信息] 未找到可用的表信息, dbConfigId: {}，可能需要先同步表结构", dbConfigId);
                return Result.success(List.of()); // 返回空列表
            }
            
            // 转换为前端需要的格式
            List<Map<String, Object>> tableList = tableInfos.stream()
                .map(tableInfo -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", tableInfo.getId()); // 使用真实的数据库ID
                    map.put("name", tableInfo.getTableName());
                    map.put("tableName", tableInfo.getTableName());
                    map.put("comment", tableInfo.getTableComment() != null ? tableInfo.getTableComment() : "");
                    return map;
                })
                .collect(Collectors.toList());
            
//            log.info("📊 [表信息] 成功获取到 {} 个表, 表ID范围: {}-{}",
//                    tableList.size(),
//                    tableInfos.stream().mapToLong(TableInfo::getId).min().orElse(0),
//                    tableInfos.stream().mapToLong(TableInfo::getId).max().orElse(0));
            
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
            String tableInfo = tableInfoService.getStandardTableNameForDify(dbConfigId, tableId, userId);
            
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
     * 获取格式化的表信息
     * 支持获取所有启用表的信息或指定表的信息
     * 优先返回用户自定义的表信息版本
     * 
     * @param request 请求体包含dbConfigId和可选的tableIds
     * @return 格式化的表信息
     */
    @PostMapping("/formatted-info")
    public Result<Map<String, String>> getFormattedTableInfo(@RequestBody Map<String, Object> request) {
        try {
            log.info("📊 [表信息] 获取格式化表信息请求: {}", request);
            
            // 解析请求参数
            Long dbConfigId = null;
            if (request.get("dbConfigId") != null) {
                dbConfigId = Long.valueOf(request.get("dbConfigId").toString());
            }
            
            List<Long> tableIds = null;
            if (request.get("tableIds") instanceof List) {
                tableIds = ((List<?>) request.get("tableIds")).stream()
                    .map(id -> Long.valueOf(id.toString()))
                    .collect(java.util.stream.Collectors.toList());
            }
            
            // 参数验证
            if (dbConfigId == null) {
                log.error("📊 [表信息] 数据库配置ID不能为空");
                return Result.error("数据库配置ID不能为空");
            }
            
            // 使用默认用户ID
            Long userId = 1L;
            String userIdStr = userId.toString();
            
            String tableInfo;
            String tableSchema;
            boolean isCustom = false;
            
            // 生成表选择的哈希值来标识唯一的表组合
            String currentTableHash = TableSelectionHashUtil.generateTableSelectionHash(dbConfigId, tableIds);
            
            // 优先尝试获取用户自定义版本
            String customTableInfo = bufferUtil.getField(userIdStr, "custom_table_info");
            String customTableSchema = bufferUtil.getField(userIdStr, "custom_table_schema");
            String cachedTableHash = bufferUtil.getField(userIdStr, "table_selection_hash");
            
            if (customTableInfo != null && customTableSchema != null && 
                currentTableHash.equals(cachedTableHash)) {
                // 使用用户自定义版本
                log.info("📊 [表信息] 使用用户自定义的表信息版本");
                tableInfo = customTableInfo;
                tableSchema = customTableSchema;
                isCustom = true;
            } else {
                // 使用自动生成版本
                log.info("📊 [表信息] 使用自动生成的表信息版本");
                
                if (tableIds != null && !tableIds.isEmpty()) {
                    // 获取指定表的信息
//                    log.info("📊 [表信息] 获取指定表的格式化信息, tableIds: {}", tableIds);
                    tableInfo = tableInfoService.getSelectedTablesFormattedForDify(dbConfigId, tableIds, userId);
                    tableSchema = tableInfoService.getSelectedTablesFormattedForExecutor(dbConfigId, tableIds, userId);
                } else {
                    // 获取所有启用表的信息
                    log.info("📊 [表信息] 获取所有启用表的格式化信息");
                    tableInfo = tableInfoService.getEnabledTablesFormattedForDify(dbConfigId, userId);
                    tableSchema = tableInfoService.getEnabledTablesFormattedForExecutor(dbConfigId, userId);
                }
                
                // 存储当前表选择的哈希值和自动生成的表信息到缓存
                bufferUtil.setField(userIdStr, "table_selection_hash", currentTableHash, 24, TimeUnit.HOURS);
                bufferUtil.setField(userIdStr, "current_table_info", tableInfo, 24, TimeUnit.HOURS);
                bufferUtil.setField(userIdStr, "TableSchema_result", tableSchema, 24, TimeUnit.HOURS);
                
                // 增加缓存设置完成的日志
                log.info("📊 [表信息] 缓存更新完成: userId={}, tableHash={}, tableInfo长度={}, tableSchema长度={}", 
                    userId, currentTableHash, tableInfo.length(), tableSchema.length());
            }
            
            // 构建返回结果
            Map<String, String> result = new HashMap<>();
            result.put("tableInfo", tableInfo != null ? tableInfo : "暂无表信息");
            result.put("tableSchema", tableSchema != null ? tableSchema : "暂无表结构信息");
            result.put("isCustom", String.valueOf(isCustom)); // 标识是否为用户自定义版本
            
//            log.info("📊 [表信息] 成功获取格式化表信息, tableInfo长度: {}, tableSchema长度: {}, 是否自定义: {}",
//                    tableInfo != null ? tableInfo.length() : 0,
//                    tableSchema != null ? tableSchema.length() : 0,
//                    isCustom);
            
            return Result.success(result);
            
        } catch (Exception e) {
            log.error("📊 [表信息] 获取格式化表信息失败: {}", e.getMessage(), e);
            return Result.error("获取格式化表信息失败: " + e.getMessage());
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
    
    /**
     * 保存用户自定义的表信息到缓存
     * 
     * @param request 请求体包含dbConfigId、tableIds、customTableInfo、customTableSchema
     * @return 保存结果
     */
    @PostMapping("/save-custom-info")
    public Result<String> saveCustomTableInfo(@RequestBody Map<String, Object> request) {
        try {
            log.info("📊 [自定义表信息] 保存用户自定义表信息请求: {}", request);
            
            // 解析请求参数
            Long dbConfigId = null;
            if (request.get("dbConfigId") != null) {
                dbConfigId = Long.valueOf(request.get("dbConfigId").toString());
            }
            
            List<Long> tableIds = null;
            if (request.get("tableIds") instanceof List) {
                tableIds = ((List<?>) request.get("tableIds")).stream()
                    .map(id -> Long.valueOf(id.toString()))
                    .collect(java.util.stream.Collectors.toList());
            }
            
            String customTableInfo = request.get("customTableInfo") != null ? 
                request.get("customTableInfo").toString() : null;
            String customTableSchema = request.get("customTableSchema") != null ? 
                request.get("customTableSchema").toString() : null;
            
            // 参数验证
            if (dbConfigId == null) {
                log.error("📊 [自定义表信息] 数据库配置ID不能为空");
                return Result.error("数据库配置ID不能为空");
            }
            
            if (customTableInfo == null || customTableInfo.trim().isEmpty()) {
                log.error("📊 [自定义表信息] 自定义表信息不能为空");
                return Result.error("自定义表信息不能为空");
            }
            
            if (customTableSchema == null || customTableSchema.trim().isEmpty()) {
                log.error("📊 [自定义表信息] 自定义表结构不能为空");
                return Result.error("自定义表结构不能为空");
            }
            
            // 使用默认用户ID
            Long userId = 1L;
            String userIdStr = userId.toString();
            
            // 生成表选择的哈希值
            String tableHash = TableSelectionHashUtil.generateTableSelectionHash(dbConfigId, tableIds);
            
            // 保存到缓存（设置24小时过期时间）
            bufferUtil.setField(userIdStr, "custom_table_info", customTableInfo, 24, TimeUnit.HOURS);
            bufferUtil.setField(userIdStr, "custom_table_schema", customTableSchema, 24, TimeUnit.HOURS);
            bufferUtil.setField(userIdStr, "table_selection_hash", tableHash, 24, TimeUnit.HOURS);
            
//            log.info("📊 [自定义表信息] 用户自定义表信息保存成功, tableHash: {}, tableInfo长度: {}, tableSchema长度: {}",
//                    tableHash,
//                    customTableInfo.length(),
//                    customTableSchema.length());
            
            return Result.success("自定义表信息保存成功");
            
        } catch (Exception e) {
            log.error("📊 [自定义表信息] 保存用户自定义表信息失败: {}", e.getMessage(), e);
            return Result.error("保存自定义表信息失败: " + e.getMessage());
        }
    }
    
    /**
     * 删除用户自定义表信息，恢复使用自动生成版本
     * 
     * @param request 请求体包含dbConfigId和可选的tableIds
     * @return 重置结果
     */
    @PostMapping("/reset-custom-info")
    public Result<String> resetCustomTableInfo(@RequestBody Map<String, Object> request) {
        try {
            log.info("📊 [自定义表信息] 重置用户自定义表信息请求: {}", request);
            
            // 解析请求参数
            Long dbConfigId = null;
            if (request.get("dbConfigId") != null) {
                dbConfigId = Long.valueOf(request.get("dbConfigId").toString());
            }
            
            // 参数验证
            if (dbConfigId == null) {
                log.error("📊 [自定义表信息] 数据库配置ID不能为空");
                return Result.error("数据库配置ID不能为空");
            }
            
            // 使用默认用户ID
            Long userId = 1L;
            String userIdStr = userId.toString();
            
            // 删除自定义表信息缓存
            bufferUtil.deleteField(userIdStr, "custom_table_info");
            bufferUtil.deleteField(userIdStr, "custom_table_schema");
            
            log.info("📊 [自定义表信息] 用户自定义表信息重置成功");
            
            return Result.success("已重置为自动生成的表信息");
            
        } catch (Exception e) {
            log.error("📊 [自定义表信息] 重置用户自定义表信息失败: {}", e.getMessage(), e);
            return Result.error("重置自定义表信息失败: " + e.getMessage());
        }
    }
    

}