package com.mt.agent.workflow.api.util;

import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

/**
 * 表选择哈希工具类
 * 用于生成表选择组合的唯一哈希值，确保缓存键的一致性
 */
@Slf4j
public class TableSelectionHashUtil {
    
    /**
     * 生成表选择的哈希值，用于标识唯一的表组合
     * 
     * @param dbConfigId 数据库配置ID
     * @param tableIds 表ID列表
     * @return 哈希值
     */
    public static String generateTableSelectionHash(Long dbConfigId, List<Long> tableIds) {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("db:").append(dbConfigId);
            
            if (tableIds != null && !tableIds.isEmpty()) {
                // 对表ID进行排序以确保相同的表组合产生相同的哈希值
                tableIds.stream().sorted().forEach(id -> sb.append(",t:").append(id));
            } else {
                sb.append(",all_tables");
            }
            
            String input = sb.toString();
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return hexString.toString();
            
        } catch (Exception e) {
            log.error("🔍 [表选择哈希] 生成表选择哈希值失败: {}", e.getMessage(), e);
            // 如果哈希生成失败，返回一个基于输入的简单字符串
            return "db" + dbConfigId + "_tables" + (tableIds != null ? tableIds.size() : 0);
        }
    }
}
