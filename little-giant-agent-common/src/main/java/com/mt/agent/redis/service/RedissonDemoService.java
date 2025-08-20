package com.mt.agent.redis.service;

import com.mt.agent.redis.util.RedissonUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Redisson示例服务类，演示Redisson的常见使用场景
 *
 * @author AI助手
 * @since 2024-03-24
 */
@Service
public class RedissonDemoService {

    @Autowired
    private RedissonUtil redissonUtil;

    private static final String KEY_PREFIX = "mt:agent:";

    /**
     * 存储数据示例-默认过期时间30分钟
     *
     * @param key   键
     * @param value 值
     */
    public void saveData(String key, String value) {
        String cacheKey = KEY_PREFIX + key;
        // 保存数据，30分钟过期
        redissonUtil.set(cacheKey, value, 30, TimeUnit.MINUTES);
    }

    /**
     * 存储数据示例，带过期时间
     *
     * @param key      键
     * @param value    值
     * @param duration 过期时间
     * @param timeUnit 时间单位
     */
    public void saveDataWithExpiration(String key, String value, long duration, TimeUnit timeUnit) {
        String cacheKey = KEY_PREFIX + key;
        // 保存数据，并指定过期时间
        redissonUtil.set(cacheKey, value, duration, timeUnit);
    }

    /**
     * 存储数据示例，不设置过期时间
     *
     * @param key   键
     * @param value 值
     */
    public void saveDataWithoutExpiration(String key, String value) {
        String cacheKey = KEY_PREFIX + key;
        // 保存数据，不设置过期时间
        redissonUtil.set(cacheKey, value);
    }

    /**
     * 获取数据示例
     *
     * @param key 键
     * @return 值
     */
    public String getData(String key) {
        String cacheKey = KEY_PREFIX + key;
        return redissonUtil.get(cacheKey);
    }

    /**
     * Map操作示例
     *
     * @param id   主键ID
     * @param data 数据Map
     */
    public void saveMapData(String id, Map<String, Object> data) {
        String cacheKey = KEY_PREFIX + "map:" + id;
        redissonUtil.putAll(cacheKey, data);
        // 设置过期时间
        redissonUtil.expire(cacheKey, 1, TimeUnit.HOURS);
    }



}