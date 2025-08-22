# Redis操作指南

本模块使用Redisson实现了对Redis的各种操作，包括基本的键值存储、哈希表、列表、集合以及分布式锁等功能。

## 配置说明

Redis配置位于`application-redis.yml`文件中，主要配置项包括：

```yaml
spring:
  data:
    redis:
      host: 113.45.193.155  # Redis服务器地址
      port: 6379             # Redis服务端口
      password: Mobirit1709! # Redis密码
      database: 0            # 使用的数据库索引
      timeout: 3000          # 连接超时时间（毫秒）
```

## 使用方法

### 基础API

项目中提供了`RedissonUtil`工具类，封装了常用的Redis操作方法：

1. **字符串操作**
   ```java
   // 存储字符串
   redissonUtil.set("key", "value");
   
   // 存储字符串并设置过期时间
   redissonUtil.set("key", "value", 30, TimeUnit.MINUTES);
   
   // 获取字符串
   String value = redissonUtil.get("key");
   
   // 删除键
   boolean deleted = redissonUtil.delete("key");
   ```

2. **Map操作**
   ```java
   // 获取Map对象
   RMap<String, Object> map = redissonUtil.getMap("mapKey");
   
   // 存储Map数据
   Map<String, Object> dataMap = new HashMap<>();
   dataMap.put("field1", "value1");
   dataMap.put("field2", "value2");
   redissonUtil.putAll("mapKey", dataMap);
   ```

3. **分布式锁**
   ```java
   // 获取锁
   RLock lock = redissonUtil.getLock("lockKey");
   
   // 尝试获取锁
   boolean locked = redissonUtil.tryLock("lockKey", 5, 30, TimeUnit.SECONDS);
   
   // 释放锁
   redissonUtil.unlock("lockKey");
   ```

4. **其他操作**
   ```java
   // 检查键是否存在
   boolean exists = redissonUtil.exists("key");
   
   // 设置过期时间
   boolean expired = redissonUtil.expire("key", 1, TimeUnit.HOURS);
   ```

### 示例代码

在`RedissonDemoService`类中提供了一些示例用法：

1. **存储和获取数据**
   ```java
   // 存储数据
   redissonDemoService.saveData("userProfile", userProfileJson);
   
   // 获取数据
   String userProfile = redissonDemoService.getData("userProfile");
   ```

2. **使用分布式锁处理订单**
   ```java
   boolean success = redissonDemoService.processOrderWithLock("ORD12345");
   ```

## 使用注意事项

1. 在使用Redis进行数据缓存时，建议遵循以下规则：
   - 键名使用冒号分隔，例如：`业务:模块:功能:ID`
   - 设置合理的过期时间，避免缓存过期风暴
   - 关键业务应考虑缓存击穿、缓存穿透和缓存雪崩问题

2. 使用分布式锁时：
   - 加锁和解锁要在同一个线程中进行
   - 要设置合适的获取锁超时时间和持有锁时间
   - 解锁前检查锁是否由当前线程持有
   - 在finally块中释放锁，避免死锁

3. 对于大量数据的存储，应考虑使用专门的数据结构，如RList、RSet、RMap等

## 后续优化方向

1. 添加Redis集群配置支持
2. 实现更多数据结构的操作（如有序集合、BitMap等）
3. 添加Redis消息队列功能
4. 实现基于Redis的分布式缓存框架
5. 开发Redis监控和统计功能 
