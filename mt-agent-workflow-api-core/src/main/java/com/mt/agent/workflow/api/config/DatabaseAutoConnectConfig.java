package com.mt.agent.workflow.api.config;

import com.mt.agent.workflow.api.service.DbConnectionManagerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 数据库自动连接配置
 * 在应用启动时自动初始化所有启用的数据库连接
 */
@Slf4j
@Component
public class DatabaseAutoConnectConfig implements ApplicationRunner {
    
    @Autowired
    private DbConnectionManagerService dbConnectionManagerService;
    
    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("开始初始化数据库自动连接...");
        
        try {
            // 延迟启动，确保其他服务都已初始化
            Thread.sleep(2000);
            
            // 初始化所有启用的数据库连接
            dbConnectionManagerService.initializeEnabledConnections();
            
            log.info("数据库自动连接初始化完成");
        } catch (Exception e) {
            log.error("数据库自动连接初始化失败: {}", e.getMessage(), e);
        }
    }
}
