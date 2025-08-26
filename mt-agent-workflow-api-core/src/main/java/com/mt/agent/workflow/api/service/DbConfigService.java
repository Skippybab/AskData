package com.mt.agent.workflow.api.service;

import com.mt.agent.workflow.api.entity.DbConfig;
import java.util.List;

public interface DbConfigService {
    DbConfig createOrUpdate(DbConfig config);
    boolean verifyConnection(Long userId, Long dbConfigId);

    DbConfig getById(Long userId, Long id);
    boolean deleteConfig(Long userId, Long id);
	
	/**
	 * 更新数据库配置状态
	 */
	boolean updateStatus(Long userId, Long id, Integer status);
	
	/**
	 * 获取所有启用的数据库配置
	 */
	List<DbConfig> getEnabledConfigs();
	
	/**
	 * 根据ID获取数据库配置（不检查权限，内部使用）
	 */
	DbConfig getDbConfig(Long dbConfigId);
}


