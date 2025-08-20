package com.mt.agent.consensus.util;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 基于Redis的共识缓存工具类
 * 使用JSON格式存储，支持灵活的数据结构变更
 */
@Slf4j
@Component
public class ConsensusUtil {

    private static final String KEY_PREFIX = "coze:consensus:";

    // 默认过期时间（分钟）
    public static final long DEFAULT_EXPIRE_TIME = 30;

    private final RedissonClient redissonClient;

    @Autowired
    public ConsensusUtil(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    /**
     * 通过key获取Redis缓存中的JSON数据，并转换为JSONObject对象
     *
     * @param key Redis缓存键
     * @return JSONObject对象，如果缓存不存在则返回null
     */
    public JSONObject getConsensusJsonByKey(String key) {
        log.debug("通过key获取共识JSON数据: {}", key);

        try {
            RBucket<String> bucket = redissonClient.getBucket(key,StringCodec.INSTANCE);
            String jsonStr = bucket.get();

            if (jsonStr == null || jsonStr.trim().isEmpty()) {
                log.warn("缓存中不存在该key的数据: {}", key);
                return null;
            }

            // 使用hutool工具将JSON字符串转换为JSONObject
            JSONObject jsonObject = JSONUtil.parseObj(jsonStr);
            log.debug("成功获取并转换JSON数据: {}", key);
            return jsonObject;

        } catch (Exception e) {
            log.error("获取或转换JSON数据时发生异常, key: {}, 异常信息: {}", key, e.getMessage(), e);
            return null;
        }
    }

    /**
     * 通过key删除Redis缓存中的数据
     *
     * @param key Redis缓存键
     * @return true表示删除成功，false表示删除失败或缓存不存在
     */
    public boolean deleteConsensusJsonByKey(String key) {
        log.info("删除共识JSON缓存数据: {}", key);

        try {
            RBucket<String> bucket = redissonClient.getBucket(key);
            boolean result = bucket.delete();

            if (result) {
                log.info("成功删除缓存数据: {}", key);
            } else {
                log.warn("缓存数据不存在或删除失败: {}", key);
            }

            return result;

        } catch (Exception e) {
            log.error("删除缓存数据时发生异常, key: {}, 异常信息: {}", key, e.getMessage(), e);
            return false;
        }
    }

    // 删除用户共识缓存
    public boolean deleteConsensusJsonByUserId(String userId) {
        return deleteConsensusJsonByKey(getUserConsensusKey(userId));
    }

    /**
     * 保存JSON对象到Redis缓存
     *
     * @param key        Redis缓存键
     * @param jsonObject JSON对象
     * @param expireTime 过期时间
     * @param timeUnit   时间单位
     * @return true表示保存成功，false表示保存失败
     */
    public boolean saveConsensusJson(String key, JSONObject jsonObject, long expireTime, TimeUnit timeUnit) {
        log.info("保存共识JSON数据到缓存: {}", key);

        try {
            String jsonStr = JSONUtil.toJsonStr(jsonObject);
            RBucket<String> bucket = redissonClient.getBucket(key);
            bucket.set(jsonStr, expireTime, timeUnit);
            log.info("成功保存JSON数据到缓存: {}", key);
            return true;

        } catch (Exception e) {
            log.error("保存JSON数据到缓存时发生异常, key: {}, 异常信息: {}", key, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 保存JSON对象到Redis缓存（使用默认过期时间）
     *
     * @param key        Redis缓存键
     * @param jsonObject JSON对象
     * @return true表示保存成功，false表示保存失败
     */
    public boolean saveConsensusJson(String key, JSONObject jsonObject) {
        return saveConsensusJson(key, jsonObject, -1, TimeUnit.MINUTES);
    }

    /**
     * 获取用户共识缓存键（保持与原有逻辑兼容）
     *
     * @param userId 用户ID
     * @return 缓存键
     */
    public String getUserConsensusKey(String userId) {
        return KEY_PREFIX + userId + ":consensus";
    }

    /**
     * 计算并更新共识数据的层级状态
     * 根据子项状态自动计算父级状态
     *
     * @param consensus 共识JSON对象
     * @return 更新后的共识JSON对象
     */
    public JSONObject calculateAndUpdateStatus(JSONObject consensus) {
        if (consensus == null) {
            return consensus;
        }

        try {
            // 1. 计算taskOutput的状态
            JSONObject taskOutput = consensus.getJSONObject("taskOutput");
            if (taskOutput != null) {
                String outputStatus = calculateArrayItemsStatus(taskOutput.getJSONArray("output"));
                taskOutput.put("status", outputStatus);
            }

            // 2. 计算taskSteps的状态
            JSONObject taskSteps = consensus.getJSONObject("taskSteps");
            if (taskSteps != null) {
                String stepsStatus = calculateArrayItemsStatus(taskSteps.getJSONArray("steps"));
                taskSteps.put("status", stepsStatus);
            }

            // 3. 计算taskInputs的状态
            JSONObject taskInputs = consensus.getJSONObject("taskInputs");
            if (taskInputs != null) {
                String inputsStatus = calculateArrayItemsStatus(taskInputs.getJSONArray("inputs"));
                taskInputs.put("status", inputsStatus);
            }

            // 4. 计算整体状态
            String overallStatus = calculateOverallStatus(consensus);
            consensus.put("status", overallStatus);

            log.debug("更新共识状态完成，整体状态: {}", overallStatus);

        } catch (Exception e) {
            log.error("计算共识状态时发生异常: {}", e.getMessage(), e);
        }

        return consensus;
    }

    /**
     * 计算数组中所有元素的状态
     * 状态判断规则：
     * - 只有当所有元素的status都为CONFIRMED时，返回CONFIRMED
     * - 只有当所有元素的status都为UNKNOWN时，返回UNKNOWN
     * - 其他情况返回KNOWN
     *
     * @param array JSONArray对象
     * @return 数组整体状态
     */
    private String calculateArrayItemsStatus(cn.hutool.json.JSONArray array) {
        if (array == null || array.isEmpty()) {
            return "UNKNOWN";
        }

        boolean allConfirmed = true;
        boolean allUnknown = true;

        for (Object item : array) {
            if (item instanceof JSONObject) {
                JSONObject jsonItem = (JSONObject) item;
                String itemStatus = jsonItem.getStr("status", "UNKNOWN");

                if (!"CONFIRMED".equals(itemStatus)) {
                    allConfirmed = false;
                }
                if (!"UNKNOWN".equals(itemStatus)) {
                    allUnknown = false;
                }
            } else {
                // 如果数组中的元素不是JSONObject，则认为状态未知
                allConfirmed = false;
                allUnknown = false;
            }
        }

        if (allConfirmed) {
            return "CONFIRMED";
        } else if (allUnknown) {
            return "UNKNOWN";
        } else {
            return "KNOWN";
        }
    }

    /**
     * 计算整体共识状态
     * 状态判断规则：
     * - 只有当所有顶级组件的status都为CONFIRMED时，返回CONFIRMED
     * - 只有当所有顶级组件的status都为UNKNOWN时，返回UNKNOWN
     * - 其他情况返回KNOWN
     *
     * @param consensus 共识JSON对象
     * @return 整体状态
     */
    private String calculateOverallStatus(JSONObject consensus) {
        String taskNameStatus = consensus.getJSONObject("taskName") != null
                ? consensus.getJSONObject("taskName").getStr("status", "UNKNOWN")
                : "UNKNOWN";
        String taskOutputStatus = consensus.getJSONObject("taskOutput") != null
                ? consensus.getJSONObject("taskOutput").getStr("status", "UNKNOWN")
                : "UNKNOWN";
        String taskStepsStatus = consensus.getJSONObject("taskSteps") != null
                ? consensus.getJSONObject("taskSteps").getStr("status", "UNKNOWN")
                : "UNKNOWN";
        String taskInputsStatus = consensus.getJSONObject("taskInputs") != null
                ? consensus.getJSONObject("taskInputs").getStr("status", "UNKNOWN")
                : "UNKNOWN";

        // 检查是否所有组件都为CONFIRMED
        if ("CONFIRMED".equals(taskNameStatus) && "CONFIRMED".equals(taskOutputStatus) &&
                "CONFIRMED".equals(taskStepsStatus) && "CONFIRMED".equals(taskInputsStatus)) {
            return "CONFIRMED";
        }

        // 检查是否所有组件都为UNKNOWN
        if ("UNKNOWN".equals(taskNameStatus) && "UNKNOWN".equals(taskOutputStatus) &&
                "UNKNOWN".equals(taskStepsStatus) && "UNKNOWN".equals(taskInputsStatus)) {
            return "UNKNOWN";
        }

        // 其他情况返回KNOWN
        return "KNOWN";
    }

    /**
     * 获取共识整体状态（智能计算版本）
     * 会根据子项状态自动计算父级状态
     *
     * @param userId 用户ID
     * @return 整体状态字符串：UNKNOWN、KNOWN 或 CONFIRMED
     */
    public String getOverallConsensusStatus(String userId) {
        String cacheKey = getUserConsensusKey(userId);
        JSONObject consensus = getConsensusJsonByKey(cacheKey);

        if (consensus == null) {
            return "UNKNOWN";
        }

        // 自动计算并更新状态
        consensus = calculateAndUpdateStatus(consensus);

        // 保存更新后的状态
        saveConsensusJson(cacheKey, consensus);

        return consensus.getStr("status", "UNKNOWN");
    }

    /**
     * 获取共识信息
     *
     * @author lfz
     * @date 2025/6/10 11:54
     * @param userId 用户id
     * @return
     */
    public JSONObject getConsensus(String userId) {
        String cacheKey = getUserConsensusKey(userId);
        return  getConsensusJsonByKey(cacheKey);
    }
}