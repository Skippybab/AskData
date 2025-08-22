package com.cultivate.redis.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * Redisson配置类
 * 使用Redisson Spring Boot Starter自动配置
 * 
 * @author AI助手
 * @since 2024-03-24
 */
@Slf4j
@Configuration
public class RedissonConfig {

    @Autowired
    private Environment env;

    @PostConstruct
    public void init() {
        log.info("Redis配置已加载: host={}, port={}, database={}",
                env.getProperty("spring.data.redis.host"),
                env.getProperty("spring.data.redis.port"),
                env.getProperty("spring.data.redis.database"));
        log.info("Redisson使用Spring Boot自动配置，基于application-redis.yml");
    }
}