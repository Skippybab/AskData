package com.mt.agent.workflow.api.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import lombok.extern.slf4j.Slf4j;
import jakarta.annotation.PostConstruct;

/**
 * Python执行器配置
 * 
 * 注意：PythonDirectExecutorService使用@Service注解，
 * Spring会自动管理依赖注入，无需手动配置Bean
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "python.executor.enabled", havingValue = "true", matchIfMissing = true)
public class PythonExecutorConfig {
    
    @PostConstruct
    public void init() {
        log.info("🔧 [Python执行器配置] Python执行器已启用");
        log.info("🔧 [Python执行器配置] 使用PythonDirectExecutorService实现");
    }
}
