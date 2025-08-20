package com.mt.agent.consensus.util;

import com.mt.agent.consensus.model.ConsensusItem;
import com.mt.agent.consensus.model.ConsensusParameterItem;
import com.mt.agent.consensus.model.ConsensusStatusManage;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 基于Redis的共识状态管理缓存工具类
 * 用于存储和管理ConsensusStatusManage对象
 */
@Slf4j
@Component
public class ConsensusStatusManageUtil {

    private static final String KEY_PREFIX = "mt:consensus_status:";
    private static final String STATUS_FIELD = "status_manage";

    // 默认过期时间（分钟）
    public static final long DEFAULT_EXPIRE_TIME = 30;

    private final RedissonClient redissonClient;

    @Autowired
    public ConsensusStatusManageUtil(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    /**
     * 获取用户缓存键前缀
     *
     * @param userId 用户ID
     * @return 缓存键前缀
     */
    private String getUserStatusKeyPrefix(String userId) {
        return KEY_PREFIX + userId + ":";
    }

    /**
     * 获取用户共识状态管理缓存键
     *
     * @param userId 用户ID
     * @return 缓存键
     */
    private String getStatusKey(String userId) {
        return getUserStatusKeyPrefix(userId) + STATUS_FIELD;
    }

    /**
     * 初始化用户共识状态管理缓存
     *
     * @param userId 用户ID
     */
    public void initUserStatusManage(String userId) {
        log.info("初始化用户共识状态管理缓存: {}", userId);
        ConsensusStatusManage statusManage = createNewStatusManage();
        RBucket<ConsensusStatusManage> statusBucket = redissonClient.getBucket(getStatusKey(userId));
        statusBucket.set(statusManage, DEFAULT_EXPIRE_TIME, TimeUnit.MINUTES);
        log.info("用户共识状态管理缓存初始化完成: {}", userId);
    }

    /**
     * 创建新的共识状态管理对象
     *
     * @return 新的共识状态管理对象
     */
    private ConsensusStatusManage createNewStatusManage() {
        ConsensusStatusManage statusManage = new ConsensusStatusManage();
        statusManage.setDialogStatus(ConsensusStatusManage.NO_TASK);
        statusManage.setCurrentConsensusId(0);
        statusManage.setConsensusItems(new ArrayList<>());
        return statusManage;
    }

    /**
     * 清空用户共识状态管理缓存
     *
     * @param userId 用户ID
     */
    public void clearUserStatusManage(String userId) {
        log.info("清空用户共识状态管理缓存: {}", userId);
        redissonClient.getBucket(getStatusKey(userId)).delete();
    }

    /**
     * 获取用户共识状态管理缓存
     *
     * @param userId 用户ID
     * @return 共识状态管理对象
     */
    public ConsensusStatusManage getConsensusStatusManage(String userId) {
        log.debug("获取用户共识状态管理缓存: {}", userId);
        String statusKey = getStatusKey(userId);
        RBucket<ConsensusStatusManage> bucket = redissonClient.getBucket(statusKey);

        ConsensusStatusManage statusManage = bucket.get();

        // 如果缓存不存在则初始化
        if (statusManage == null) {
            statusManage = createNewStatusManage();
            bucket.set(statusManage, DEFAULT_EXPIRE_TIME, TimeUnit.MINUTES);
        }

        return statusManage;
    }

    /**
     * 更新用户共识状态管理缓存
     *
     * @param userId       用户ID
     * @param statusManage 共识状态管理对象
     */
    public void updateConsensusStatusManage(String userId, ConsensusStatusManage statusManage) {
        log.info("更新用户共识状态管理缓存: {}", userId);
        String statusKey = getStatusKey(userId);
        RBucket<ConsensusStatusManage> bucket = redissonClient.getBucket(statusKey);
        bucket.set(statusManage, DEFAULT_EXPIRE_TIME, TimeUnit.MINUTES);
    }

    /**
     * 设置共识状态管理缓存过期时间
     *
     * @param userId     用户ID
     * @param expireTime 过期时间
     * @param timeUnit   时间单位
     */
    public void setStatusManageExpire(String userId, long expireTime, TimeUnit timeUnit) {
        log.info("设置共识状态管理缓存过期时间: {}, {} {}", userId, expireTime, timeUnit);
        String statusKey = getStatusKey(userId);
        RBucket<ConsensusStatusManage> bucket = redissonClient.getBucket(statusKey);
        bucket.expire(expireTime, timeUnit);
    }

    /**
     * 检查用户共识状态管理缓存是否存在
     *
     * @param userId 用户ID
     * @return 是否存在
     */
    public boolean hasUserStatusManage(String userId) {
        String statusKey = getStatusKey(userId);
        return redissonClient.getBucket(statusKey).isExists();
    }

    /**
     * 获取共识状态管理缓存剩余过期时间（毫秒）
     *
     * @param userId 用户ID
     * @return 剩余时间（毫秒），如果不存在则返回-1
     */
    public long getStatusManageExpireTime(String userId) {
        String statusKey = getStatusKey(userId);
        return redissonClient.getBucket(statusKey).remainTimeToLive();
    }

    /**
     * 更新对话状态
     *
     * @param userId       用户ID
     * @param dialogStatus 对话状态
     */
    public void updateDialogStatus(String userId, Integer dialogStatus) {
        ConsensusStatusManage statusManage = getConsensusStatusManage(userId);
        statusManage.setDialogStatus(dialogStatus);
        updateConsensusStatusManage(userId, statusManage);
    }

    /**
     * 更新当前共识序号
     *
     * @param userId      用户ID
     * @param consensusId 共识序号
     */
    public void updateCurrentConsensusId(String userId, Integer consensusId) {
        log.info("更新当前共识序号: {}, consensusId: {}", userId, consensusId);
        ConsensusStatusManage statusManage = getConsensusStatusManage(userId);
        statusManage.setCurrentConsensusId(consensusId);
        updateConsensusStatusManage(userId, statusManage);
    }

    /**
     * 添加共识项
     *
     * @param userId 用户ID
     * @param name   共识名称
     * @param status 共识状态
     * @return 添加的共识项ID
     */
    public Integer addConsensusItem(String userId, String name, Integer status) {
        ConsensusStatusManage statusManage = getConsensusStatusManage(userId);
        List<ConsensusItem> items = statusManage.getConsensusItems();

        if (items == null) {
            items = new ArrayList<>();
            statusManage.setConsensusItems(items);
        }

        // 生成新的共识序号
        int newId = items.isEmpty() ? 1
                : items.stream()
                        .mapToInt(ConsensusItem::getId)
                        .max()
                        .orElse(0) + 1;

        // 创建新的共识项
        ConsensusItem newItem = new ConsensusItem();
        newItem.setName(name);
        newItem.setId(newId);
        newItem.setStatus(status);
        newItem.setParameters(new ArrayList<>());

        // 添加到列表
        items.add(newItem);
        updateConsensusStatusManage(userId, statusManage);

        return newId;
    }

    /**
     * 更新共识项状态
     *
     * @param userId      用户ID
     * @param consensusId 共识序号
     * @param status      新状态
     * @return 是否更新成功
     */
    public boolean updateConsensusItemStatus(String userId, Integer consensusId, Integer status) {
        ConsensusStatusManage statusManage = getConsensusStatusManage(userId);

        if (statusManage.getConsensusItems() == null) {
            return false;
        }

        for (ConsensusItem item : statusManage.getConsensusItems()) {
            if (item.getId().equals(consensusId)) {
                item.setStatus(status);
                updateConsensusStatusManage(userId, statusManage);
                return true;
            }
        }

        return false;
    }

    /**
     * 获取共识项
     *
     * @param userId      用户ID
     * @param consensusId 共识序号
     * @return 共识项，如果不存在则返回null
     */
    public ConsensusItem getConsensusItem(String userId, Integer consensusId) {
        log.debug("获取共识项: {}, consensusId: {}", userId, consensusId);
        ConsensusStatusManage statusManage = getConsensusStatusManage(userId);

        if (statusManage.getConsensusItems() == null) {
            return null;
        }

        for (ConsensusItem item : statusManage.getConsensusItems()) {
            if (item.getId().equals(consensusId)) {
                return item;
            }
        }

        return null;
    }

    /**
     * 删除共识项
     *
     * @param userId      用户ID
     * @param consensusId 共识序号
     * @return 是否删除成功
     */
    public boolean removeConsensusItem(String userId, Integer consensusId) {
        log.info("删除共识项: {}, consensusId: {}", userId, consensusId);
        ConsensusStatusManage statusManage = getConsensusStatusManage(userId);

        if (statusManage.getConsensusItems() == null) {
            return false;
        }

        boolean removed = statusManage.getConsensusItems().removeIf(item -> item.getId().equals(consensusId));
        if (removed) {
            updateConsensusStatusManage(userId, statusManage);
        }

        return removed;
    }

    /**
     * 添加参数项
     *
     * @param userId      用户ID
     * @param consensusId 共识序号
     * @param paramName   参数名称
     * @param status      参数状态
     * @return 是否添加成功
     */
    public boolean addParameterItem(String userId, Integer consensusId, String paramName, Integer status) {
        ConsensusStatusManage statusManage = getConsensusStatusManage(userId);

        if (statusManage.getConsensusItems() == null) {
            return false;
        }

        for (ConsensusItem item : statusManage.getConsensusItems()) {
            if (item.getId().equals(consensusId)) {
                if (item.getParameters() == null) {
                    item.setParameters(new ArrayList<>());
                }

                // 检查参数是否已存在
                for (ConsensusParameterItem param : item.getParameters()) {
                    if (param.getName().equals(paramName)) {
                        param.setStatus(status);
                        updateConsensusStatusManage(userId, statusManage);
                        return true;
                    }
                }

                // 添加新参数
                ConsensusParameterItem paramItem = new ConsensusParameterItem();
                paramItem.setName(paramName);
                paramItem.setStatus(status);
                item.getParameters().add(paramItem);

                updateConsensusStatusManage(userId, statusManage);
                return true;
            }
        }

        return false;
    }

    /**
     * 更新参数状态
     *
     * @param userId      用户ID
     * @param consensusId 共识序号
     * @param paramName   参数名称
     * @param status      新状态
     * @return 是否更新成功
     */
    public boolean updateParameterStatus(String userId, Integer consensusId, String paramName, Integer status) {
        ConsensusStatusManage statusManage = getConsensusStatusManage(userId);

        if (statusManage.getConsensusItems() == null) {
            return false;
        }

        for (ConsensusItem item : statusManage.getConsensusItems()) {
            if (item.getId().equals(consensusId) && item.getParameters() != null) {
                for (ConsensusParameterItem param : item.getParameters()) {
                    if (param.getName().equals(paramName)) {
                        param.setStatus(status);
                        updateConsensusStatusManage(userId, statusManage);
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * 获取参数项
     *
     * @param userId      用户ID
     * @param consensusId 共识序号
     * @param paramName   参数名称
     * @return 参数项，如果不存在则返回null
     */
    public ConsensusParameterItem getParameterItem(String userId, Integer consensusId, String paramName) {
        log.debug("获取参数项: {}, consensusId: {}, paramName: {}", userId, consensusId, paramName);
        ConsensusStatusManage statusManage = getConsensusStatusManage(userId);

        if (statusManage.getConsensusItems() == null) {
            return null;
        }

        for (ConsensusItem item : statusManage.getConsensusItems()) {
            if (item.getId().equals(consensusId) && item.getParameters() != null) {
                for (ConsensusParameterItem param : item.getParameters()) {
                    if (param.getName().equals(paramName)) {
                        return param;
                    }
                }
            }
        }

        return null;
    }
}
