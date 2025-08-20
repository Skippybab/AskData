package com.mt.agent.redis.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.codec.SerializationCodec;
import org.redisson.config.Config;
import org.redisson.codec.JsonJacksonCodec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.annotation.PostConstruct;

/**
 * Redisson配置类
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
    }

    /**
     * 创建RedissonClient实例
     *
     * @return RedissonClient实例
     */
    @Bean
    public RedissonClient redissonClient() {
        String host = env.getProperty("spring.data.redis.host");
        String port = env.getProperty("spring.data.redis.port", "6379");
        String password = env.getProperty("spring.data.redis.password");
        String database = env.getProperty("spring.data.redis.database", "0");
        String timeout = env.getProperty("spring.data.redis.timeout", "3000");

        // 创建配置
        Config config = new Config();

        // 设置JSON编解码器，避免Kryo序列化兼容性问题
        config.setCodec(new JsonJacksonCodec());
//        config.setCodec(new SerializationCodec());

        // 单节点配置
        config.useSingleServer()
                .setAddress("redis://" + host + ":" + port)
                .setPassword(password)
                .setConnectTimeout(Integer.parseInt(timeout))
                .setDatabase(Integer.parseInt(database));

        log.info("Redisson配置完成，使用JSON编解码器避免序列化兼容性问题");

        // 创建并返回RedissonClient实例
        return Redisson.create(config);
    }
}