package com.mt.agent.workflow.api.redis.util;

import org.redisson.api.RBucket;
import org.redisson.api.RLock;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Redisson工具类，提供对Redis的各种操作
 *
 * @author AI助手
 * @since 2024-03-24
 */
@Component
public class RedissonUtil {

    @Autowired
    private RedissonClient redissonClient;

    public RedissonUtil(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    /**
     * 获取字符串对象
     *
     * @param key 键
     * @return 值
     */
    public <T> T get(String key) {
        RBucket<T> bucket = redissonClient.getBucket(key);
        return bucket.get();
    }

    /**
     * 设置字符串对象
     *
     * @param key   键
     * @param value 值
     */
    public <T> void set(String key, T value) {
        RBucket<T> bucket = redissonClient.getBucket(key);
        bucket.set(value);
    }

    /**
     * 设置字符串对象，并指定过期时间
     *
     * @param key     键
     * @param value   值
     * @param timeout 过期时间
     * @param unit    时间单位
     */
    public <T> void set(String key, T value, long timeout, TimeUnit unit) {
        RBucket<T> bucket = redissonClient.getBucket(key);
        bucket.set(value, timeout, unit);
    }

    /**
     * 删除对象
     *
     * @param key 键
     * @return 成功返回true，失败返回false
     */
    public boolean delete(String key) {
        return redissonClient.getBucket(key).delete();
    }

    /**
     * 获取Map对象
     *
     * @param key 键
     * @return Map对象
     */
    public <K, V> RMap<K, V> getMap(String key) {
        return redissonClient.getMap(key);
    }

    /**
     * 将所有给定的值添加到Map中
     *
     * @param key 键
     * @param map 值
     */
    public <K, V> void putAll(String key, Map<K, V> map) {
        RMap<K, V> rMap = redissonClient.getMap(key);
        rMap.putAll(map);
    }

    /**
     * 获取分布式锁
     *
     * @param key 锁的名称
     * @return 锁对象
     */
    public RLock getLock(String key) {
        return redissonClient.getLock(key);
    }

    /**
     * 尝试获取锁
     *
     * @param key       锁的名称
     * @param waitTime  等待时间
     * @param leaseTime 持有锁的时间
     * @param unit      时间单位
     * @return 是否获取成功
     */
    public boolean tryLock(String key, long waitTime, long leaseTime, TimeUnit unit) throws InterruptedException {
        RLock lock = redissonClient.getLock(key);
        return lock.tryLock(waitTime, leaseTime, unit);
    }

    /**
     * 释放锁
     *
     * @param key 锁的名称
     */
    public void unlock(String key) {
        RLock lock = redissonClient.getLock(key);
        // 判断要解锁的锁是否被当前线程持有
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }

    /**
     * 检查key是否存在
     *
     * @param key 键
     * @return 是否存在
     */
    public boolean exists(String key) {
        return redissonClient.getBucket(key).isExists();
    }

    /**
     * 设置过期时间
     *
     * @param key     键
     * @param timeout 过期时间
     * @param unit    时间单位
     * @return 设置成功返回true，设置失败返回false
     */
    public boolean expire(String key, long timeout, TimeUnit unit) {
        return redissonClient.getBucket(key).expire(timeout, unit);
    }

    /**
     * 获取原始的RedissonClient对象，用于特殊操作
     *
     * @return RedissonClient实例
     */
    public RedissonClient getRedissonClient() {
        return redissonClient;
    }
}
