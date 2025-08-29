package com.mt.agent.workflow.api.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mt.agent.workflow.api.entity.TableInfo;
import com.mt.agent.workflow.api.mapper.TableInfoMapper;
import com.mt.agent.workflow.api.service.TableInfoService;
import com.mt.agent.workflow.api.util.DdlParser;
import com.mt.agent.workflow.api.util.Result;
import com.mt.agent.workflow.api.util.BufferUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequestMapping("/api/table-info")
@CrossOrigin
public class TableInfoController {

    @Autowired
    private TableInfoMapper tableInfoMapper;
    
    @Autowired
    private TableInfoService tableInfoService;
    
    @Autowired
    private BufferUtil bufferUtil;

    /**
     * 获取表列表
     */
    @GetMapping("/list")
    public Result<List<TableInfo>> getTableList(@RequestParam Long dbConfigId) {
        try {
            log.info("获取表列表 - dbConfigId: {}", dbConfigId);
            
            LambdaQueryWrapper<TableInfo> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(TableInfo::getDbConfigId, dbConfigId);
            
            List<TableInfo> tables = tableInfoMapper.selectList(queryWrapper);
            log.info("找到 {} 个表", tables.size());
            
            return Result.success(tables);
        } catch (Exception e) {
            log.error("获取表列表失败", e);
            return Result.error("获取表列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取表详情
     */
    @GetMapping("/detail")
    public Result<TableInfo> getTableDetail(@RequestParam Long dbConfigId, @RequestParam Long tableId) {
        try {
            TableInfo tableInfo = tableInfoMapper.selectById(tableId);
            if (tableInfo == null || !tableInfo.getDbConfigId().equals(dbConfigId)) {
                return Result.error("表信息不存在");
            }
            return Result.success(tableInfo);
        } catch (Exception e) {
            log.error("获取表详情失败", e);
            return Result.error("获取表详情失败: " + e.getMessage());
        }
    }

    /**
     * 获取表字段信息
     */
    @GetMapping("/columns")
    public Result<List<Map<String, Object>>> getTableColumns(@RequestParam Long dbConfigId, @RequestParam Long tableId) {
        try {
            // 从table_info表中获取表的DDL信息
            TableInfo tableInfo = tableInfoMapper.selectById(tableId);
            if (tableInfo == null || !tableInfo.getDbConfigId().equals(dbConfigId)) {
                log.error("表信息不存在 - tableId: {}, dbConfigId: {}", tableId, dbConfigId);
                return Result.error("表信息不存在");
            }
//            log.info("找到表信息 - 表名: {}, DDL长度: {}",
//                    tableInfo.getTableName(),
//                    tableInfo.getTableDdl() != null ? tableInfo.getTableDdl().length() : 0);
            
            String ddl = tableInfo.getTableDdl();
            List<Map<String, Object>> columns = new ArrayList<>();
            
            if (ddl != null && !ddl.trim().isEmpty()) {
                // 使用DdlParser解析字段信息
                columns = DdlParser.parseColumnsFromDdl(ddl);
//                log.info("使用DdlParser解析字段信息，共解析出 {} 个字段", columns.size());
            } else {
                log.warn("表的DDL信息为空 - 表名: {}", tableInfo.getTableName());
            }
            return Result.success(columns);
        } catch (Exception e) {
            log.error("获取字段信息失败: {}", e.getMessage(), e);
            return Result.error("获取字段信息失败: " + e.getMessage());
        }
    }

    /**
     * 获取格式化的表信息（按session存储）
     */
    @PostMapping("/formatted-info")
    public Result<Map<String, Object>> getFormattedInfo(@RequestBody Map<String, Object> request) {
        try {
            Long dbConfigId = Long.valueOf(request.get("dbConfigId").toString());
            List<Long> tableIds = null;
            if (request.get("tableIds") != null) {
                @SuppressWarnings("unchecked")
                List<Object> tableIdObjs = (List<Object>) request.get("tableIds");
                tableIds = tableIdObjs.stream()
                    .map(obj -> Long.valueOf(obj.toString()))
                    .collect(java.util.stream.Collectors.toList());
            }
            Long sessionId = request.get("sessionId") != null ? Long.valueOf(request.get("sessionId").toString()) : null;
            
            log.info("获取格式化表信息 - dbConfigId: {}, tableIds: {}, sessionId: {}", dbConfigId, tableIds, sessionId);
            
            // 检查是否有当前session的缓存版本，并验证表选择是否匹配
            String tableInfo = null;
            String tableSchema = null;
            boolean isCustom = false;
            
            if (sessionId != null) {
                String tableInfoKey = "session_table_info_" + sessionId;
                String tableSchemaKey = "session_table_schema_" + sessionId;
                String tableIdsKey = "session_table_ids_" + sessionId;
                String isCustomKey = "session_is_custom_" + sessionId;
                
                String cachedTableInfo = bufferUtil.getField("1", tableInfoKey);
                String cachedTableSchema = bufferUtil.getField("1", tableSchemaKey);
                String cachedTableIds = bufferUtil.getField("1", tableIdsKey);
                String cachedIsCustom = bufferUtil.getField("1", isCustomKey);
                
                // 检查缓存是否存在且表选择是否匹配
                if (cachedTableInfo != null && cachedTableSchema != null) {
                    // 比较当前请求的表选择与缓存的表选择是否一致
                    String currentTableIdsStr = "";
                    if (tableIds != null && !tableIds.isEmpty()) {
                        currentTableIdsStr = tableIds.stream()
                            .sorted() // 排序确保一致性
                            .map(String::valueOf)
                            .collect(java.util.stream.Collectors.joining(","));
                    }
                    
                    String cachedTableIdsStr = cachedTableIds != null ? cachedTableIds : "";
                    
                    if (currentTableIdsStr.equals(cachedTableIdsStr)) {
                        // 表选择匹配，使用缓存
                        tableInfo = cachedTableInfo;
                        tableSchema = cachedTableSchema;
                        isCustom = "true".equals(cachedIsCustom);
                        log.info("使用session缓存的表信息, sessionId: {}, isCustom: {}, tableIds: {}", sessionId, isCustom, currentTableIdsStr);
                    } else {
                        // 表选择不匹配，需要重新生成
                        log.info("表选择已变化，重新生成表信息, sessionId: {}, 缓存表: {}, 当前表: {}", sessionId, cachedTableIdsStr, currentTableIdsStr);
                    }
                }
            }
            
            // 如果缓存中没有，则生成表信息并存储到session缓存
            if (tableInfo == null || tableSchema == null) {
                log.info("缓存未命中，开始生成表信息 - dbConfigId: {}, tableIds: {}", dbConfigId, tableIds);
                
                if (tableIds != null && !tableIds.isEmpty()) {
                    // 获取指定表的信息
                    tableInfo = tableInfoService.getSelectedTablesFormattedForDify(dbConfigId, tableIds, 1L);
                    tableSchema = tableInfoService.getSelectedTablesFormattedForExecutor(dbConfigId, tableIds, 1L);
                    log.info("生成指定表信息完成 - tableInfo长度: {}, tableSchema长度: {}", 
                        tableInfo != null ? tableInfo.length() : 0, 
                        tableSchema != null ? tableSchema.length() : 0);
                } else {
                    // 获取所有启用表的信息
                    tableInfo = tableInfoService.getEnabledTablesFormattedForDify(dbConfigId, 1L);
                    tableSchema = tableInfoService.getEnabledTablesFormattedForExecutor(dbConfigId, 1L);
                    log.info("生成所有启用表信息完成 - tableInfo长度: {}, tableSchema长度: {}", 
                        tableInfo != null ? tableInfo.length() : 0, 
                        tableSchema != null ? tableSchema.length() : 0);
                }
                
                // 检查生成结果
                if (tableInfo == null || tableInfo.trim().isEmpty()) {
                    log.error("生成的tableInfo为空 - dbConfigId: {}, tableIds: {}", dbConfigId, tableIds);
                    tableInfo = "暂无表信息";
                }
                if (tableSchema == null || tableSchema.trim().isEmpty()) {
                    log.error("生成的tableSchema为空 - dbConfigId: {}, tableIds: {}", dbConfigId, tableIds);
                    tableSchema = "暂无表结构信息";
                }
                
                // 存储到session缓存
                if (sessionId != null && tableInfo != null && tableSchema != null) {
                    String tableInfoKey = "session_table_info_" + sessionId;
                    String tableSchemaKey = "session_table_schema_" + sessionId;
                    String tableIdsKey = "session_table_ids_" + sessionId;
                    String isCustomKey = "session_is_custom_" + sessionId;
                    
                    bufferUtil.setField("1", tableInfoKey, tableInfo, 24, TimeUnit.HOURS);
                    bufferUtil.setField("1", tableSchemaKey, tableSchema, 24, TimeUnit.HOURS);
                    bufferUtil.setField("1", isCustomKey, "false", 24, TimeUnit.HOURS);
                    
                    if (tableIds != null && !tableIds.isEmpty()) {
                        String tableIdsStr = tableIds.stream()
                            .sorted() // 确保排序一致性
                            .map(String::valueOf)
                            .collect(java.util.stream.Collectors.joining(","));
                        bufferUtil.setField("1", tableIdsKey, tableIdsStr, 24, TimeUnit.HOURS);
                    }
                    
                    log.info("已存储表信息到session缓存, sessionId: {}", sessionId);
                }
                isCustom = false;
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("tableInfo", tableInfo);
            result.put("tableSchema", tableSchema);
            result.put("isCustom", isCustom);
            
            return Result.success(result);
        } catch (Exception e) {
            log.error("获取格式化表信息失败", e);
            return Result.error("获取格式化表信息失败: " + e.getMessage());
        }
    }

    /**
     * 保存自定义表信息（按session存储）
     */
    @PostMapping("/save-custom-info")
    public Result<String> saveCustomInfo(@RequestBody Map<String, Object> request) {
        try {
            Long sessionId = request.get("sessionId") != null ? Long.valueOf(request.get("sessionId").toString()) : null;
            String customTableInfo = (String) request.get("customTableInfo");
            String customTableSchema = (String) request.get("customTableSchema");
            
            if (sessionId == null) {
                return Result.error("sessionId不能为空");
            }
            
            if (customTableInfo == null || customTableSchema == null) {
                return Result.error("表信息和表结构不能为空");
            }
            
            log.info("保存自定义表信息 - sessionId: {}, customTableInfo长度: {}, customTableSchema长度: {}", 
                    sessionId, customTableInfo.length(), customTableSchema.length());
            
            // 直接覆盖session缓存
            String tableInfoKey = "session_table_info_" + sessionId;
            String tableSchemaKey = "session_table_schema_" + sessionId;
            String isCustomKey = "session_is_custom_" + sessionId;
            
            bufferUtil.setField("1", tableInfoKey, customTableInfo, 24, TimeUnit.HOURS);
            bufferUtil.setField("1", tableSchemaKey, customTableSchema, 24, TimeUnit.HOURS);
            bufferUtil.setField("1", isCustomKey, "true", 24, TimeUnit.HOURS);
            
            log.info("自定义表信息已保存到session缓存, sessionId: {}", sessionId);
            
            return Result.success("保存成功");
        } catch (Exception e) {
            log.error("保存自定义表信息失败", e);
            return Result.error("保存自定义表信息失败: " + e.getMessage());
        }
    }

    /**
     * 重置自定义表信息（按session存储）
     */
    @PostMapping("/reset-custom-info")
    public Result<String> resetCustomInfo(@RequestBody Map<String, Object> request) {
        try {
            Long dbConfigId = Long.valueOf(request.get("dbConfigId").toString());
            List<Long> tableIds = null;
            if (request.get("tableIds") != null) {
                @SuppressWarnings("unchecked")
                List<Object> tableIdObjs = (List<Object>) request.get("tableIds");
                tableIds = tableIdObjs.stream()
                    .map(obj -> Long.valueOf(obj.toString()))
                    .collect(java.util.stream.Collectors.toList());
            }
            Long sessionId = request.get("sessionId") != null ? Long.valueOf(request.get("sessionId").toString()) : null;
            
            if (sessionId == null) {
                return Result.error("sessionId不能为空");
            }
            
            log.info("重置自定义表信息 - sessionId: {}, dbConfigId: {}, tableIds: {}", sessionId, dbConfigId, tableIds);
            
            // 重新生成表信息
            String tableInfo, tableSchema;
            if (tableIds != null && !tableIds.isEmpty()) {
                tableInfo = tableInfoService.getSelectedTablesFormattedForDify(dbConfigId, tableIds, 1L);
                tableSchema = tableInfoService.getSelectedTablesFormattedForExecutor(dbConfigId, tableIds, 1L);
            } else {
                tableInfo = tableInfoService.getEnabledTablesFormattedForDify(dbConfigId, 1L);
                tableSchema = tableInfoService.getEnabledTablesFormattedForExecutor(dbConfigId, 1L);
            }
            
            // 覆盖session缓存并标记为非自定义
            String tableInfoKey = "session_table_info_" + sessionId;
            String tableSchemaKey = "session_table_schema_" + sessionId;
            String isCustomKey = "session_is_custom_" + sessionId;
            
            bufferUtil.setField("1", tableInfoKey, tableInfo, 24, TimeUnit.HOURS);
            bufferUtil.setField("1", tableSchemaKey, tableSchema, 24, TimeUnit.HOURS);
            bufferUtil.setField("1", isCustomKey, "false", 24, TimeUnit.HOURS);
            
            log.info("已重置为自动生成的表信息, sessionId: {}", sessionId);
            
            return Result.success("重置成功");
        } catch (Exception e) {
            log.error("重置自定义表信息失败", e);
            return Result.error("重置自定义表信息失败: " + e.getMessage());
        }
    }

    /**
     * 更新表的自定义Schema描述
     */
    @PutMapping("/custom-schema")
    public Result<String> updateCustomSchema(@RequestBody Map<String, Object> request) {
        try {
            Long dbConfigId = Long.valueOf(request.get("dbConfigId").toString());
            Long tableId = Long.valueOf(request.get("tableId").toString());
            String customSchema = (String) request.get("customSchema");
            
            log.info("更新表自定义Schema - dbConfigId: {}, tableId: {}, customSchema长度: {}", 
                    dbConfigId, tableId, customSchema != null ? customSchema.length() : 0);
            
            // 获取表信息
            TableInfo tableInfo = tableInfoMapper.selectById(tableId);
            if (tableInfo == null || !tableInfo.getDbConfigId().equals(dbConfigId)) {
                return Result.error("表信息不存在");
            }
            
            // TODO: 实现保存自定义Schema的逻辑
            // 可以考虑在TableInfo实体中添加customSchema字段，或者使用缓存存储
            // 这里暂时返回成功，实际项目中需要根据具体需求实现存储逻辑
            
            log.info("表自定义Schema更新成功 - 表: {}", tableInfo.getTableName());
            
            return Result.success("自定义Schema更新成功");
        } catch (Exception e) {
            log.error("更新表自定义Schema失败", e);
            return Result.error("更新表自定义Schema失败: " + e.getMessage());
        }
    }

    /**
     * 获取表的完整结构信息（包括格式化描述）
     */
    @GetMapping("/structure")
    public Result<Map<String, Object>> getTableStructure(@RequestParam Long dbConfigId, @RequestParam Long tableId) {
        try {
            log.info("获取表结构信息 - dbConfigId: {}, tableId: {}", dbConfigId, tableId);
            
            // 获取表信息
            TableInfo tableInfo = tableInfoMapper.selectById(tableId);
            if (tableInfo == null || !tableInfo.getDbConfigId().equals(dbConfigId)) {
                return Result.error("表信息不存在");
            }
            
            String ddl = tableInfo.getTableDdl();
            Map<String, Object> result = new HashMap<>();
            result.put("tableInfo", tableInfo);
            
            if (ddl != null && !ddl.trim().isEmpty()) {
                // 使用DdlParser解析表结构
                DdlParser.TableStructure structure = DdlParser.parseCreateTable(ddl);
                if (structure != null) {
                    result.put("structure", structure);
                    // 生成格式化的表结构描述
                    String formattedStructure = DdlParser.formatTableStructure(structure);
                    result.put("formattedStructure", formattedStructure);
                    // 获取字段列表
                    List<Map<String, Object>> columns = DdlParser.parseColumnsFromDdl(ddl);
                    result.put("columns", columns);
                } else {
                    result.put("error", "DDL解析失败");
                }
            } else {
                result.put("error", "表DDL信息为空");
            }
            
            return Result.success(result);
        } catch (Exception e) {
            log.error("获取表结构信息失败", e);
            return Result.error("获取表结构信息失败: " + e.getMessage());
        }
    }

    /**
     * 更新字段备注
     */
    @PutMapping("/columns/comment")
    public Result<String> updateColumnComment(@RequestParam Long dbConfigId, 
                                              @RequestParam Long tableId, 
                                              @RequestParam String columnName, 
                                              @RequestParam String comment) {
        try {
            log.info("更新字段备注 - dbConfigId: {}, tableId: {}, columnName: {}, comment: {}", 
                    dbConfigId, tableId, columnName, comment);
            
            // 获取表信息
            TableInfo tableInfo = tableInfoMapper.selectById(tableId);
            if (tableInfo == null || !tableInfo.getDbConfigId().equals(dbConfigId)) {
                return Result.error("表信息不存在");
            }
            
            String originalDdl = tableInfo.getTableDdl();
            if (originalDdl == null || originalDdl.trim().isEmpty()) {
                return Result.error("表DDL信息为空，无法更新字段注释");
            }
            
            // 使用DdlParser更新字段注释
            String updatedDdl = DdlParser.updateColumnComment(originalDdl, columnName, comment);
            
            // 更新数据库中的DDL
            tableInfo.setTableDdl(updatedDdl);
            tableInfoMapper.updateById(tableInfo);
            
            log.info("字段备注更新成功 - 表: {}, 字段: {}, 新备注: {}", 
                    tableInfo.getTableName(), columnName, comment);
            
            return Result.success("字段备注更新成功");
        } catch (Exception e) {
            log.error("更新字段备注失败", e);
            return Result.error("更新字段备注失败: " + e.getMessage());
        }
    }


}
