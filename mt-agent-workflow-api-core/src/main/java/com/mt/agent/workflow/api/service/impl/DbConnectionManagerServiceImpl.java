package com.mt.agent.workflow.api.service.impl;

import com.mt.agent.workflow.api.entity.DbConfig;
import com.mt.agent.workflow.api.service.DbConnectionManagerService;
import com.mt.agent.workflow.api.service.DbConfigService;
import com.mt.agent.workflow.api.util.PasswordCipherService;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import javax.sql.DataSource;
import java.sql.Connection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 数据库连接池管理服务实现
 */
@Slf4j
@Service
public class DbConnectionManagerServiceImpl implements DbConnectionManagerService {
    
    @Autowired
    private DbConfigService dbConfigService;
    
    // 存储所有活跃的连接池
    private final Map<Long, HikariDataSource> connectionPools = new ConcurrentHashMap<>();
    
    // 统一密钥提供
    private final byte[] masterKey = com.mt.agent.workflow.api.util.CryptoKeyProvider.getMasterKey();
    
    @PostConstruct
    public void init() {
        log.info("数据库连接池管理服务启动");
        // 启动时自动初始化所有启用的数据库连接
        initializeEnabledConnections();
    }
    
    @PreDestroy
    public void destroy() {
        log.info("数据库连接池管理服务关闭，清理所有连接池");
        // 关闭所有连接池
        connectionPools.values().forEach(HikariDataSource::close);
        connectionPools.clear();
    }
    
    @Override
    public void initializeEnabledConnections() {
        try {
            log.info("开始初始化所有启用的数据库连接");
            List<DbConfig> enabledConfigs = dbConfigService.getEnabledConfigs();
            
            for (DbConfig config : enabledConfigs) {
                try {
                    if (createConnectionPool(config)) {
                        log.info("成功为数据库配置 {} ({}) 创建连接池", config.getId(), config.getName());
                    } else {
                        log.warn("为数据库配置 {} ({}) 创建连接池失败", config.getId(), config.getName());
                    }
                } catch (Exception e) {
                    log.error("为数据库配置 {} ({}) 创建连接池时发生错误: {}", 
                            config.getId(), config.getName(), e.getMessage());
                }
            }
            log.info("数据库连接初始化完成，共处理 {} 个配置", enabledConfigs.size());
        } catch (Exception e) {
            log.error("初始化数据库连接失败: {}", e.getMessage(), e);
        }
    }
    
    @Override
    public boolean createConnectionPool(DbConfig config) {
        try {
            // 如果连接池已存在，先关闭
            closeConnectionPool(config.getId());
            
            // 解密密码
            String password;
            try {
                log.debug("开始解密数据库配置 {} 的密码", config.getId());
                password = PasswordCipherService.decryptToStringFromString(masterKey, config.getPasswordCipher());
                log.debug("数据库配置 {} 密码解密成功", config.getId());
            } catch (Exception decryptError) {
                log.warn("密码解密失败，尝试使用明文密码: {}", decryptError.getMessage());
                log.debug("解密失败详情 - 配置ID: {}, 密码长度: {}, 错误: {}", 
                         config.getId(), 
                         config.getPasswordCipher() != null ? config.getPasswordCipher().length() : 0,
                         decryptError.getClass().getSimpleName());
                
                if (config.getRawPassword() != null && !config.getRawPassword().isEmpty()) {
                    password = config.getRawPassword();
                    log.info("使用明文密码作为备用方案");
                } else {
                    log.error("密码解密失败，且无明文密码可用。配置ID: {}, 错误: {}", 
                             config.getId(), decryptError.getMessage());
                    throw new RuntimeException("密码解密失败，且无明文密码可用: " + decryptError.getMessage());
                }
            }
            
            // 构建JDBC URL
            String jdbcUrl = String.format("jdbc:mysql://%s:%d/%s?characterEncoding=utf-8&serverTimezone=Asia/Shanghai&connectTimeout=10000&socketTimeout=10000",
                    config.getHost(), config.getPort(), config.getDatabaseName());
            
            // 创建HikariCP配置
            HikariConfig hikariConfig = new HikariConfig();
            hikariConfig.setJdbcUrl(jdbcUrl);
            hikariConfig.setUsername(config.getUsername());
            hikariConfig.setPassword(password);
            hikariConfig.setPoolName("HikariPool-" + config.getId());
            hikariConfig.setMaximumPoolSize(5);
            hikariConfig.setMinimumIdle(1);
            hikariConfig.setIdleTimeout(300000); // 5分钟
            hikariConfig.setConnectionTimeout(10000); // 10秒
            hikariConfig.setMaxLifetime(1800000); // 30分钟
            hikariConfig.setLeakDetectionThreshold(60000); // 1分钟
            hikariConfig.setValidationTimeout(5000); // 5秒
            
            // 创建数据源
            HikariDataSource dataSource = new HikariDataSource(hikariConfig);
            
            // 测试连接
            try (Connection conn = dataSource.getConnection()) {
                if (conn.isValid(5)) {
                    connectionPools.put(config.getId(), dataSource);
                    log.info("数据库配置 {} ({}) 连接池创建成功", config.getId(), config.getName());
                    return true;
                } else {
                    dataSource.close();
                    log.error("数据库配置 {} ({}) 连接验证失败", config.getId(), config.getName());
                    return false;
                }
            }
        } catch (Exception e) {
            log.error("为数据库配置 {} ({}) 创建连接池失败: {}", 
                    config.getId(), config.getName(), e.getMessage());
            return false;
        }
    }
    
    @Override
    public void closeConnectionPool(Long dbConfigId) {
        HikariDataSource dataSource = connectionPools.remove(dbConfigId);
        if (dataSource != null) {
            try {
                dataSource.close();
                log.info("数据库配置 {} 的连接池已关闭", dbConfigId);
            } catch (Exception e) {
                log.error("关闭数据库配置 {} 的连接池时发生错误: {}", dbConfigId, e.getMessage());
            }
        }
    }
    
    @Override
    public Connection getConnection(Long dbConfigId) {
        HikariDataSource dataSource = connectionPools.get(dbConfigId);
        if (dataSource != null) {
            try {
                return dataSource.getConnection();
            } catch (Exception e) {
                log.error("获取数据库配置 {} 的连接失败: {}", dbConfigId, e.getMessage());
                throw new RuntimeException("获取数据库连接失败: " + e.getMessage());
            }
        } else {
            throw new RuntimeException("数据库配置 " + dbConfigId + " 的连接池不存在");
        }
    }
    
    @Override
    public boolean testConnection(DbConfig config) {
        try {
            // 解密密码
            String password;
            try {
                password = PasswordCipherService.decryptToStringFromString(masterKey, config.getPasswordCipher());
            } catch (Exception decryptError) {
                log.warn("密码解密失败，尝试使用明文密码: {}", decryptError.getMessage());
                if (config.getRawPassword() != null && !config.getRawPassword().isEmpty()) {
                    password = config.getRawPassword();
                } else {
                    throw new RuntimeException("密码解密失败，且无明文密码可用: " + decryptError.getMessage());
                }
            }
            
            // 构建JDBC URL
            String jdbcUrl = String.format("jdbc:mysql://%s:%d/%s?characterEncoding=utf-8&serverTimezone=Asia/Shanghai&connectTimeout=10000&socketTimeout=10000",
                    config.getHost(), config.getPort(), config.getDatabaseName());
            
            // 测试连接
            try (Connection conn = java.sql.DriverManager.getConnection(jdbcUrl, config.getUsername(), password)) {
                return conn.isValid(10);
            }
        } catch (Exception e) {
            log.error("测试数据库配置 {} ({}) 连接失败: {}", 
                    config.getId(), config.getName(), e.getMessage());
            return false;
        }
    }
    
    @Override
    public List<String> getActiveConnectionPools() {
        return connectionPools.keySet().stream()
                .map(id -> "数据库配置 " + id + " 的连接池")
                .toList();
    }
}
