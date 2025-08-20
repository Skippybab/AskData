package com.mt.agent.workflow.api.infra;

import com.mt.agent.workflow.api.entity.DbConfig;
import com.mt.agent.workflow.api.util.PasswordCipherService;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DbConnectionPoolManager {

    private static final Map<Long, HikariDataSource> POOLS = new ConcurrentHashMap<>();

    public static DataSource getOrCreate(Long dbConfigId, DbConfig cfg, byte[] masterKey) {
        return POOLS.computeIfAbsent(dbConfigId, id -> create(cfg, masterKey));
    }

    public static void evict(Long dbConfigId) {
        HikariDataSource ds = POOLS.remove(dbConfigId);
        if (ds != null) {
            ds.close();
        }
    }

    private static HikariDataSource create(DbConfig cfg, byte[] masterKey) {
        // 解密密码
        String password = com.mt.agent.workflow.api.util.PasswordCipherService.decryptToStringFromString(masterKey, cfg.getPasswordCipher());
        String jdbcUrl = buildJdbcUrl(cfg);
        HikariConfig hc = new HikariConfig();
        hc.setJdbcUrl(jdbcUrl);
        hc.setUsername(cfg.getUsername());
        hc.setPassword(password);
        hc.setMaximumPoolSize(5);
        hc.setMinimumIdle(0);
        hc.setIdleTimeout(60_000);
        hc.setConnectionTimeout(10_000);
        hc.setMaxLifetime(10 * 60_000);
        hc.setReadOnly(true);
        return new HikariDataSource(hc);
    }

    private static String buildJdbcUrl(DbConfig cfg) {
        // 目前仅支持 MySQL，后续可扩展方言
        if ("mysql".equalsIgnoreCase(cfg.getDbType())) {
            return "jdbc:mysql://" + cfg.getHost() + ":" + cfg.getPort() + "/" + cfg.getDatabaseName() + "?useSSL=false&characterEncoding=utf-8&serverTimezone=Asia/Shanghai";
        }
        throw new IllegalArgumentException("Unsupported db_type: " + cfg.getDbType());
    }
}


