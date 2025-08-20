package com.mt.agent.workflow.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mt.agent.workflow.api.entity.TablePermission;
import com.mt.agent.workflow.api.mapper.TablePermissionMapper;
import com.mt.agent.workflow.api.service.TablePermissionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 表权限服务实现类
 */
@Slf4j
@Service
public class TablePermissionServiceImpl implements TablePermissionService {
    
    @Autowired
    private TablePermissionMapper tablePermissionMapper;
    
    @Override
    public boolean hasQueryPermission(Long userId, Long dbConfigId, String tableName) {
        try {
            LambdaQueryWrapper<TablePermission> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(TablePermission::getUserId, userId)
                   .eq(TablePermission::getDbConfigId, dbConfigId)
                   .eq(TablePermission::getTableName, tableName)
                   .eq(TablePermission::getPermissionType, 1) // 查询权限
                   .eq(TablePermission::getEnabled, 1);
            
            TablePermission permission = tablePermissionMapper.selectOne(wrapper);
            return permission != null;
            
        } catch (Exception e) {
            log.error("检查表权限失败: userId={}, dbConfigId={}, tableName={}", userId, dbConfigId, tableName, e);
            return false;
        }
    }
    
    @Override
    public List<String> getUserAccessibleTables(Long userId, Long dbConfigId) {
        try {
            LambdaQueryWrapper<TablePermission> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(TablePermission::getUserId, userId)
                   .eq(TablePermission::getDbConfigId, dbConfigId)
                   .eq(TablePermission::getPermissionType, 1) // 查询权限
                   .eq(TablePermission::getEnabled, 1)
                   .select(TablePermission::getTableName);
            
            List<TablePermission> permissions = tablePermissionMapper.selectList(wrapper);
            return permissions.stream()
                    .map(TablePermission::getTableName)
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            log.error("获取用户可访问表列表失败: userId={}, dbConfigId={}", userId, dbConfigId, e);
            return List.of();
        }
    }
    
    @Override
    public boolean grantTablePermission(Long userId, Long dbConfigId, String tableName, Integer permissionType) {
        try {
            // 检查是否已存在权限记录
            LambdaQueryWrapper<TablePermission> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(TablePermission::getUserId, userId)
                   .eq(TablePermission::getDbConfigId, dbConfigId)
                   .eq(TablePermission::getTableName, tableName)
                   .eq(TablePermission::getPermissionType, permissionType);
            
            TablePermission existingPermission = tablePermissionMapper.selectOne(wrapper);
            
            if (existingPermission != null) {
                // 更新现有权限
                existingPermission.setEnabled(1);
                existingPermission.setUpdatedAt(new Date());
                return tablePermissionMapper.updateById(existingPermission) > 0;
            } else {
                // 创建新权限
                TablePermission permission = new TablePermission();
                permission.setTenantId(0L); // 默认租户
                permission.setUserId(userId);
                permission.setDbConfigId(dbConfigId);
                permission.setTableName(tableName);
                permission.setPermissionType(permissionType);
                permission.setEnabled(1);
                permission.setCreatedAt(new Date());
                permission.setUpdatedAt(new Date());
                
                return tablePermissionMapper.insert(permission) > 0;
            }
            
        } catch (Exception e) {
            log.error("授权表权限失败: userId={}, dbConfigId={}, tableName={}", userId, dbConfigId, tableName, e);
            return false;
        }
    }
    
    @Override
    public boolean revokeTablePermission(Long userId, Long dbConfigId, String tableName) {
        try {
            LambdaQueryWrapper<TablePermission> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(TablePermission::getUserId, userId)
                   .eq(TablePermission::getDbConfigId, dbConfigId)
                   .eq(TablePermission::getTableName, tableName);
            
            return tablePermissionMapper.delete(wrapper) > 0;
            
        } catch (Exception e) {
            log.error("撤销表权限失败: userId={}, dbConfigId={}, tableName={}", userId, dbConfigId, tableName, e);
            return false;
        }
    }
}
