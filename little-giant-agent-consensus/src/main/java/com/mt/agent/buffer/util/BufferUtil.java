package com.mt.agent.buffer.util;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.mt.agent.buffer.model.Buffer;
import com.mt.agent.buffer.model.Buffer.HistoryLog;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 基于Redis的中间变量缓存工具类
 * 使用Hash结构存储，支持按用户ID隔离数据
 */
@Slf4j
@Component
public class BufferUtil {
    private static final String KEY_PREFIX = "coze:buffer:";
    private static final String DATASOURCE_FIELD = "datasource";
    private static final String STATUS_FIELD = "status";
    private static final String PARAM_FIELD = "param";
    private static final String HISTORY_LOG = "historyLog";
    private static final String STEP_OUTPUT_LIMITS = "stepOutputLimits";
    private static final String PYTHON_CODE = "pythonCode";
    private static final String REPLY_CODE = "reply";
    private static final String TABLES = "tables";

    // 默认过期时间（分钟）
    public static final long DEFAULT_EXPIRE_TIME = 10;
    public static final long DATASOURCE_EXPIRE_TIME = 20;

    // 域字段

    // 数据源域字段
    private static final String DATASOURCE_DATA_SOURCE_INDEX_FIELD = "dataSourceIndex";
    private static final String DATASOURCE_DATA_SOURCE_NAME_FIELD = "dataSourceName";
    private static final String DATASOURCE_DATA_SOURCE_DESC_FIELD = "dataSourceDesc";
    private static final String DATASOURCE_DB_URL_FIELD = "dbUrl";
    private static final String DATASOURCE_TABLE_NAME_FIELD = "tableName";
    private static final String DATASOURCE_FIELD_DETAIL_FIELD = "fieldDetail";
    private static final String DATASOURCE_COUNT_FIELD = "count";

    private final RedissonClient redissonClient;

    @Autowired
    public BufferUtil(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    /**
     * 获取用户缓存键前缀
     * 
     * @param userId 用户ID
     * @return 缓存键前缀
     */
    private String getUserBufferKeyPrefix(String userId) {
        return KEY_PREFIX + userId + ":";
    }

    /**
     * 获取用户数据源缓存键
     * 
     * @param userId 用户ID
     * @return 缓存键
     */
    private String getDataSourceKey(String userId) {
        return getUserBufferKeyPrefix(userId) + DATASOURCE_FIELD;
    }

    /**
     * 获取用户历史日志缓存键
     * 
     * @param userId 用户ID
     * @return 缓存键
     */
    private String getHistoryLogKey(String userId) {
        return getUserBufferKeyPrefix(userId) + HISTORY_LOG;
    }


    /**
     * 清空用户缓存
     * 
     * @param userId 用户ID
     */
    public void clearUserCache(String userId) {
        log.info("清空id为{}的用户缓存: ", userId);
        // 删除翻译器1生成代码的缓存

        redissonClient.getBucket(getPythonCodePrefix(userId), StringCodec.INSTANCE).delete();
        // 删除用户历史对话
        redissonClient.getBucket(getHistoryLogKey(userId)).delete();


    }

    /**
     * 获取用户数据源缓存
     * 
     * @param userId 用户ID
     * @return 数据源对象
     */
    public Buffer.DataSource getDataSource(String userId) {
        log.debug("获取用户数据源缓存: {}", userId);
        String dataSourceKey = getDataSourceKey(userId);
        RBucket<Buffer.DataSource> bucket = redissonClient.getBucket(dataSourceKey);

        Buffer.DataSource dataSource = bucket.get();

        // 如果缓存不存在则初始化
        if (dataSource == null) {
            dataSource = new Buffer.DataSource();
            // dataSource.defaultInit();
            bucket.set(dataSource); // 不设置过期时间，永久保存
        }

        return dataSource;
    }

    /**
     * 更新用户数据源缓存（无过期时间）
     * 
     * @param userId     用户ID
     * @param dataSource 数据源对象
     */
    public void updateDataSource(String userId, Buffer.DataSource dataSource) {
        log.info("更新用户数据源缓存: {}, 永久存储", userId);
        String dataSourceKey = getDataSourceKey(userId);
        RBucket<Buffer.DataSource> bucket = redissonClient.getBucket(dataSourceKey);

        bucket.set(dataSource); // 不设置过期时间，永久保存
    }

    /**
     * 设置数据源过期时间
     * 可以用于为原本永久的数据源设置过期时间
     * 
     * @param userId     用户ID
     * @param expireTime 过期时间
     * @param timeUnit   时间单位
     */
    public void setDataSourceExpire(String userId, long expireTime, TimeUnit timeUnit) {
        log.info("设置数据源缓存过期时间: {}, {} {}", userId, expireTime, timeUnit);
        String dataSourceKey = getDataSourceKey(userId);
        RBucket<Buffer.DataSource> bucket = redissonClient.getBucket(dataSourceKey);
        bucket.expire(expireTime, timeUnit);
    }

    /**
     * 移除数据源过期时间，使其永久保存
     * 
     * @param userId 用户ID
     */
    public void removeDataSourceExpire(String userId) {
        log.info("移除数据源缓存过期时间，设为永久: {}", userId);
        String dataSourceKey = getDataSourceKey(userId);
        RBucket<Buffer.DataSource> bucket = redissonClient.getBucket(dataSourceKey);
        bucket.clearExpire();
    }

    /**
     * 初始化数据源信息
     * 
     * @param userId 用户ID
     */
    public void initDataSource(String userId) {
        log.info("初始化数据源信息: {}", userId);
        Buffer.DataSource dataSource = new Buffer.DataSource();
        // dataSource.defaultInit();
        updateDataSource(userId, dataSource);
    }

    /**
     * 清除数据源信息
     *
     * @param userId 用户ID
     */
    public void clearDataSourceCache(String userId) {
        log.info("清除数据源信息: {}", userId);
        String dataSourceKey = getDataSourceKey(userId);
        RBucket<Buffer.DataSource> bucket = redissonClient.getBucket(dataSourceKey);
        bucket.delete();
    }

    /**
     * 更新数据源表名
     * 
     * @param userId    用户ID
     * @param tableName 表名
     */
    public void updateDataSourceTableName(String userId, String tableName) {
        log.info("更新数据源表名: {}, tableName: {}", userId, tableName);
        Buffer.DataSource dataSource = getDataSource(userId);
        dataSource.setTableName(tableName);
        updateDataSource(userId, dataSource);
    }

    /**
     * 更新数据源名称
     * 
     * @param userId         用户ID
     * @param dataSourceName 数据源名称
     */
    public void updateDataSourceName(String userId, String dataSourceName) {
        log.info("更新数据源名称: {}, dataSourceName: {}", userId, dataSourceName);
        Buffer.DataSource dataSource = getDataSource(userId);
        dataSource.setDataSourceName(dataSourceName);
        updateDataSource(userId, dataSource);
    }

    /**
     * 更新数据源描述
     * 
     * @param userId         用户ID
     * @param dataSourceDesc 数据源描述
     */
    public void updateDataSourceDesc(String userId, String dataSourceDesc) {
        log.info("更新数据源描述: {}, dataSourceDesc: {}", userId, dataSourceDesc);
        Buffer.DataSource dataSource = getDataSource(userId);
        dataSource.setDataSourceDesc(dataSourceDesc);
        updateDataSource(userId, dataSource);
    }

    /**
     * 检查用户缓存是否存在
     * 
     * @param userId 用户ID
     * @return 是否存在
     */
    public boolean hasUserCache(String userId) {
        String dataSourceKey = getDataSourceKey(userId);
        return redissonClient.getBucket(dataSourceKey).isExists();
    }

    /**
     * 检查数据源缓存是否存在
     * 
     * @param userId 用户ID
     * @return 是否存在
     */
    public boolean hasDataSourceCache(String userId) {
        String dataSourceKey = getDataSourceKey(userId);
        return redissonClient.getBucket(dataSourceKey).isExists();
    }

    /**
     * 获取数据源缓存剩余过期时间（毫秒）
     * 
     * @param userId 用户ID
     * @return 剩余时间（毫秒），如果不存在则返回-1
     */
    public long getDataSourceExpireTime(String userId) {
        String dataSourceKey = getDataSourceKey(userId);
        return redissonClient.getBucket(dataSourceKey).remainTimeToLive();
    }

    /**
     * 批量清除匹配模式的缓存
     * 
     * @param pattern 匹配模式，例如 "mt:buffer:*"
     * @return 清除的键数量
     */
    public int clearCacheByPattern(String pattern) {
        log.info("批量清除缓存, pattern: {}", pattern);
        int count = 0;
        for (String key : redissonClient.getKeys().getKeysByPattern(pattern)) {
            redissonClient.getBucket(key).delete();
            count++;
        }
        log.info("批量清除缓存完成, 共清除 {} 个键", count);
        return count;
    }

    /**
     * 导出用户缓存为Buffer对象（用于兼容旧代码）
     * 
     * @param userId 用户ID
     * @return Buffer对象
     */
    public Buffer getUserCache(String userId) {
        Buffer buffer = new Buffer();
        buffer.setDataSource(getDataSource(userId));
        return buffer;
    }

    /**
     * 设置指定域的过期时间
     * 
     * @param userId     用户ID
     * @param field      域名称（例如"question"、"datasource"等）
     * @param expireTime 过期时间
     * @param timeUnit   时间单位
     */
    public void setFieldExpire(String userId, String field, long expireTime, TimeUnit timeUnit) {
        log.info("设置域[{}]的过期时间: {}, {} {}", field, userId, expireTime, timeUnit);
        String fieldKey = getUserBufferKeyPrefix(userId) + field;
        redissonClient.getBucket(fieldKey).expire(expireTime, timeUnit);
    }

    /**
     * 移除指定域的过期时间，使其永久保存
     * 
     * @param userId 用户ID
     * @param field  域名称（例如"question"、"datasource"等）
     */
    public void removeFieldExpire(String userId, String field) {
        log.info("移除域[{}]的过期时间，设为永久: {}", field, userId);
        String fieldKey = getUserBufferKeyPrefix(userId) + field;
        redissonClient.getBucket(fieldKey).clearExpire();
    }

    /**
     * 获取指定域的剩余过期时间
     * 
     * @param userId 用户ID
     * @param field  域名称（例如"question"、"datasource"等）
     * @return 剩余时间（毫秒），如果为永久则返回-1，如果不存在则返回-2
     */
    public long getFieldExpireTime(String userId, String field) {
        String fieldKey = getUserBufferKeyPrefix(userId) + field;
        return redissonClient.getBucket(fieldKey).remainTimeToLive();
    }

    /**
     * 检查指定域是否设置了过期时间
     * 
     * @param userId 用户ID
     * @param field  域名称
     * @return 是否设置了过期时间
     */
    public boolean hasFieldExpire(String userId, String field) {
        return getFieldExpireTime(userId, field) > 0;
    }

    /**
     * 创建或更新自定义域（可设置过期时间）
     * 
     * @param userId     用户ID
     * @param field      域名称
     * @param value      域值（必须实现Serializable接口）
     * @param expireTime 过期时间（如果小于等于0则永久保存）
     * @param timeUnit   时间单位
     * @param <T>        值类型
     */
    public <T> void setField(String userId, String field, T value, long expireTime, TimeUnit timeUnit) {
        log.info("设置自定义域: {}, {}", userId, field);
        String fieldKey = getUserBufferKeyPrefix(userId) + field;
        RBucket<T> bucket = redissonClient.getBucket(fieldKey);

        if (expireTime <= 0) {
            // 永久保存
            bucket.set(value);
            log.info("自定义域设置为永久保存: {}, {}", userId, field);
        } else {
            // 设置过期时间
            bucket.set(value, expireTime, timeUnit);
            log.info("自定义域设置过期时间: {}, {}, {} {}", userId, field, expireTime, timeUnit);
        }
    }

    /**
     * 创建或更新自定义域（永久保存）
     * 
     * @param userId 用户ID
     * @param field  域名称
     * @param value  域值（必须实现Serializable接口）
     * @param <T>    值类型
     */
    public <T> void setFieldPermanent(String userId, String field, T value) {
        setField(userId, field, value, 0, null);
    }

    /**
     * 获取自定义域的值
     * 
     * @param userId 用户ID
     * @param field  域名称
     * @param <T>    值类型
     * @return 域值，如果不存在则返回null
     */
    @SuppressWarnings("unchecked")
    public <T> T getField(String userId, String field) {
        log.debug("获取自定义域: 用户id:{}, {}", userId, field);
        String fieldKey = getUserBufferKeyPrefix(userId) + field;
        return (T) redissonClient.getBucket(fieldKey).get();
    }

    /**
     * 删除自定义域
     * 
     * @param userId 用户ID
     * @param field  域名称
     * @return 是否成功删除
     */
    public boolean deleteField(String userId, String field) {
        log.info("删除自定义域: {}, {}", userId, field);
        String fieldKey = getUserBufferKeyPrefix(userId) + field;
        return redissonClient.getBucket(fieldKey).delete();
    }

    /**
     * 检查自定义域是否存在
     * 
     * @param userId 用户ID
     * @param field  域名称
     * @return 是否存在
     */
    public boolean hasField(String userId, String field) {
        String fieldKey = getUserBufferKeyPrefix(userId) + field;
        return redissonClient.getBucket(fieldKey).isExists();
    }

    /**
     * 生成步骤状态管理的key
     * 
     * @param userId   用户ID
     * @param taskName 任务名称
     * @param stepName 步骤名称
     * @return Redis key
     */
    private String getStepStatusKey(String userId, String taskName, String stepName) {
        return getUserBufferKeyPrefix(userId) + STATUS_FIELD + ":" + taskName + ":" + stepName;
    }

    /**
     * 生成步骤输入/输出管理的key
     * 
     * @param userId 用户ID
     * @param name   参数名（如taskName:stepName:input/output）
     * @return Redis key
     */
    private String getStepParamKey(String userId, String name) {
        return getUserBufferKeyPrefix(userId) + PARAM_FIELD + ":" + name;
    }

    /**
     * 设置步骤执行状态
     * 
     * @param userId   用户ID
     * @param taskName 任务名称
     * @param stepName 步骤名称
     * @param status   状态（StepStatus枚举的name字符串）
     */
    public void setStepStatus(String userId, String taskName, String stepName, String status) {
        String key = getStepStatusKey(userId, taskName, stepName);
        redissonClient.getBucket(key).set(status);
    }

    /**
     * 获取步骤执行状态
     * 
     * @param userId   用户ID
     * @param taskName 任务名称
     * @param stepName 步骤名称
     * @return 状态字符串，未设置返回null
     */
    public String getStepStatus(String userId, String taskName, String stepName) {
        String key = getStepStatusKey(userId, taskName, stepName);
        return (String) redissonClient.getBucket(key).get();
    }

    /**
     * 删除步骤执行状态
     * 
     * @param userId   用户ID
     * @param taskName 任务名称
     * @param stepName 步骤名称
     */
    public void deleteStepStatus(String userId, String taskName, String stepName) {
        String key = getStepStatusKey(userId, taskName, stepName);
        redissonClient.getBucket(key).delete();
    }

    /**
     * 设置步骤输入/输出参数（自动设置默认过期时间）
     * 
     * @param userId 用户ID
     * @param name   参数名（如taskName:stepName:input/output）
     * @param value  参数值（建议实现Serializable）
     */
    public void setStepParam(String userId, String name, Object value) {
        String key = getStepParamKey(userId, name);
        // 设置默认过期时间
        redissonClient.getBucket(key).set(value, DEFAULT_EXPIRE_TIME, TimeUnit.MINUTES);
    }

    /**
     * 获取步骤输入/输出参数
     * 
     * @param userId 用户ID
     * @param name   参数名
     * @param <T>    参数类型
     * @return 参数值
     */
    @SuppressWarnings("unchecked")
    public <T> T getStepParam(String userId, String name) {
        String key = getStepParamKey(userId, name);
        return (T) redissonClient.getBucket(key).get();
    }

    /**
     * 删除步骤输入/输出参数
     * 
     * @param userId 用户ID
     * @param name   参数名
     */
    public void deleteStepParam(String userId, String name) {
        String key = getStepParamKey(userId, name);
        redissonClient.getBucket(key).delete();
    }

    /**
     * 清空某个用户下所有param参数缓存
     * 
     * @param userId 用户ID
     * @return 清除的key数量
     */
    public int clearAllStepParams(String userId) {
        String pattern = getUserBufferKeyPrefix(userId) + PARAM_FIELD + ":*";
        int count = 0;
        for (String key : redissonClient.getKeys().getKeysByPattern(pattern)) {
            redissonClient.getBucket(key).delete();
            count++;
        }
        log.info("清空用户[{}]所有param缓存，共{}个", userId, count);
        return count;
    }

    /**
     * 添加历史日志
     * 
     * @param userId     用户ID
     * @param historyLog 日志记录
     */
    public void addHistoryLog(String userId, HistoryLog historyLog) {
        String key = getHistoryLogKey(userId);
        RBucket<List<HistoryLog>> bucket = redissonClient.getBucket(key);

        List<HistoryLog> logs = bucket.get();
        if (logs == null) {
            logs = new ArrayList<>();
        }

        logs.add(historyLog);
        bucket.set(logs);
    }


    /**
     * 获取最近的N条历史日志
     * 
     * @param userId 用户ID
     * @param limit  获取数量
     * @return 历史日志列表
     */
    public List<HistoryLog> getRecentHistoryLogs(String userId, int limit) {
        String key = getHistoryLogKey(userId);
        RBucket<List<HistoryLog>> bucket = redissonClient.getBucket(key);

        List<HistoryLog> allLogs = bucket.get();
        if (allLogs == null) {
            return new ArrayList<>();
        }

        return allLogs.stream()
                .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    // 获取所有历史日志
    public List<HistoryLog> getAllHistoryLogs(String userId) {
        String key = getHistoryLogKey(userId);
        RBucket<List<HistoryLog>> bucket = redissonClient.getBucket(key);

        List<HistoryLog> allLogs = bucket.get();
        if (allLogs == null) {
            return new ArrayList<>();
        }

        return allLogs;
    }

    /**
     * 获取当前任务的对话历史
     *
     * @author lfz
     * @date 2025/5/21 19:19
     * @param userId 用户id
     * @return 对话历史文本，可直接用于模型提示词模版输入
     */
    public String getTaskHistoryLogs(String userId) {
        String key = getHistoryLogKey(userId);
        RBucket<List<HistoryLog>> bucket = redissonClient.getBucket(key);

        List<HistoryLog> allLogs = bucket.get();

        if(allLogs != null){
            StringBuilder sb = new StringBuilder();
            sb.append("时间|角色|对话内容\n");
            for (HistoryLog log : allLogs) {
                sb.append(log.toString()).append("\n");
            }
            return sb.substring(0, sb.length()-1);
        }else {
            return "无";
        }
    }


    /**
     * 清空用户的所有历史日志
     * 
     * @param userId 用户ID
     */
    public void clearAllHistoryLogs(String userId) {
        String key = getHistoryLogKey(userId);
        redissonClient.getBucket(key).delete();
        log.info("清空用户所有历史日志: userId={}", userId);
    }

    /**
     * 设置历史日志的过期时间
     * 
     * @param userId     用户ID
     * @param expireTime 过期时间
     * @param timeUnit   时间单位
     */
    public void setHistoryLogExpire(String userId, long expireTime, TimeUnit timeUnit) {
        String key = getHistoryLogKey(userId);
        redissonClient.getBucket(key).expire(expireTime, timeUnit);
        log.info("设置历史日志过期时间: userId={}, expireTime={}, timeUnit={}", userId, expireTime, timeUnit);
    }

    /**
     * 清除步骤输出限制说明
     *
     * @param userId 用户ID
     * @return 是否成功清除
     */
    public boolean clearStepOutputLimits(String userId) {
        log.info("清除步骤输出限制说明: {}", userId);
        String key = getUserBufferKeyPrefix(userId) + STEP_OUTPUT_LIMITS;
        return redissonClient.getBucket(key).delete();
    }

    /**
     * 设置步骤输出限制说明的过期时间
     *
     * @param userId     用户ID
     * @param expireTime 过期时间
     * @param timeUnit   时间单位
     */
    public void setStepOutputLimitsExpire(String userId, long expireTime, TimeUnit timeUnit) {
        log.info("设置步骤输出限制说明过期时间: {}, {} {}", userId, expireTime, timeUnit);
        String key = getUserBufferKeyPrefix(userId) + STEP_OUTPUT_LIMITS;
        redissonClient.getBucket(key).expire(expireTime, timeUnit);
    }

    /**
     * 保存Python代码
     *
     * @param userId     用户ID
     * @param pythonCode Python代码
     */
    public void savePythonCode(String userId, String pythonCode) {
        String key = getPythonCodePrefix(userId);
        RBucket<String> bucket = redissonClient.getBucket(key, StringCodec.INSTANCE);
        bucket.set(pythonCode);
    }

    /**
     * 保存兜底回复
     *
     * @param userId     用户ID
     * @param reply 兜底回复
     */
    public void saveOverReply(String userId, String reply) {
        String key = getOverReplyPrefix(userId);
        RBucket<String> bucket = redissonClient.getBucket(key, StringCodec.INSTANCE);
        bucket.set(reply);
    }

    // 保存表信息
    public void saveTables(String userId, String tables) {
        String key = getTablesPrefix(userId);
        RBucket<String> bucket = redissonClient.getBucket(key, StringCodec.INSTANCE);
        bucket.set(tables);
    }

    public String getPythonCodePrefix(String userId) {
        return getUserBufferKeyPrefix(userId) + PYTHON_CODE;
    }

    public String getOverReplyPrefix(String userId) {
        return getUserBufferKeyPrefix(userId) + REPLY_CODE;
    }

    public String getTablesPrefix(String userId) {
        return getUserBufferKeyPrefix(userId) + TABLES;
    }

    /**
     * 获取Python代码
     *
     * @param userId 用户ID
     * @return Python代码，如果不存在则返回null
     */
    public String getPythonCode(String userId) {
        String key = getPythonCodePrefix(userId);
        String pyCode = (String) redissonClient.getBucket(key, StringCodec.INSTANCE).get();
//        log.info("获取Python代码:\n{}", pyCode);
        return pyCode;
    }

    /**
     * 清除Python代码
     *
     * @param userId 用户ID
     * @return 是否成功清除
     */
    public boolean clearPythonCode(String userId) {
        log.info("id为{}的用户清除Python代码", userId);
        String key = getPythonCodePrefix(userId);
        return redissonClient.getBucket(key, StringCodec.INSTANCE).delete();
    }

    /**
     * 清除上一次回复
     *
     * @param userId 用户ID
     * @return 是否成功清除
     */
    public boolean clearOverReply(String userId) {
        log.info("id为{}的用户清除上一次回复", userId);
        String key = getOverReplyPrefix(userId);
        return redissonClient.getBucket(key, StringCodec.INSTANCE).delete();
    }

    public boolean clearTables(String userId) {
        log.info("id为{}的用户清除表字段缓存", userId);
        String key = getTablesPrefix(userId);
        return redissonClient.getBucket(key, StringCodec.INSTANCE).delete();
    }





    /**
     * 设置Python代码的过期时间
     *
     * @param userId     用户ID
     * @param expireTime 过期时间
     * @param timeUnit   时间单位
     */
    public void setPythonCodeExpire(String userId, long expireTime, TimeUnit timeUnit) {
        log.info("id为{}的用户设置Python代码过期时间: {} {}", userId, expireTime, timeUnit);
        String key = getPythonCodePrefix(userId);
        redissonClient.getBucket(key, StringCodec.INSTANCE).expire(expireTime, timeUnit);
    }

}
