package com.mt.agent.workflow.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mt.agent.workflow.api.entity.DbConfig;

import com.mt.agent.workflow.api.mapper.DbConfigMapper;
import com.mt.agent.workflow.api.service.AuditService;
import com.mt.agent.workflow.api.service.DbConfigService;
import com.mt.agent.workflow.api.util.PasswordCipherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.ArrayList;
import java.util.Objects;

@Slf4j
@Service
public class DbConfigServiceImpl implements DbConfigService {

	@Autowired
	private DbConfigMapper dbConfigMapper;

	@Autowired
	private AuditService auditService;

	// 统一密钥提供
	private final byte[] masterKey = com.mt.agent.workflow.api.util.CryptoKeyProvider.getMasterKey();

	@Override
	@Transactional
	public DbConfig createOrUpdate(DbConfig config) {
		// 基础必填校验
		if (config.getName() == null || config.getName().isEmpty()) throw new IllegalArgumentException("名称不能为空");
		if (config.getHost() == null || config.getHost().isEmpty()) throw new IllegalArgumentException("主机不能为空");
		if (config.getPort() == null) throw new IllegalArgumentException("端口不能为空");
		if (config.getDatabaseName() == null || config.getDatabaseName().isEmpty()) throw new IllegalArgumentException("数据库名不能为空");
		if (config.getUsername() == null || config.getUsername().isEmpty()) throw new IllegalArgumentException("账号不能为空");

		// tenantId字段已标记为非持久化，使用默认值0
		config.setTenantId(0L);
		
		// 如果userId为空，使用默认值1（临时解决方案）
		if (config.getUserId() == null) {
			config.setUserId(1L);
		}
		
		// 设置默认状态为启用
		if (config.getStatus() == null) {
			config.setStatus(1);
		}

		// 仅当提供 rawPassword 时才更新密文
		if (config.getRawPassword() != null && !config.getRawPassword().isEmpty()) {
			// 注意：PasswordCipherService 现在应该返回String
			String cipher = PasswordCipherService.encryptToString(masterKey, config.getRawPassword());
			config.setPasswordCipher(cipher);
		}

		// 新建时必须提供口令
		if (config.getId() == null && (config.getPasswordCipher() == null || config.getPasswordCipher().isEmpty())) {
			throw new IllegalArgumentException("密码不能为空");
		}

		if (config.getId() == null) {
			dbConfigMapper.insert(config);
			auditService.log("create_db_config", "db_config", config.getId(), null, null);
		} else {
			dbConfigMapper.updateById(config);
			auditService.log("update_db_config", "db_config", config.getId(), null, null);
		}
		return config;
	}

	@Override
	public boolean verifyConnection(Long userId, Long dbConfigId) {
		// 移除权限检查，实现最小闭环
		// if (!checkAccess(userId, dbConfigId, "use")) {
		// 	throw new SecurityException("没有权限访问该数据库配置");
		// }
		DbConfig cfg = dbConfigMapper.selectById(dbConfigId);
		if (cfg == null) {
			throw new IllegalArgumentException("数据库配置不存在");
		}
		try {
			String pwd;
			try {
				pwd = PasswordCipherService.decryptToStringFromString(masterKey, cfg.getPasswordCipher());
			} catch (Exception decryptError) {
				log.warn("密码解密失败，尝试使用明文密码: {}", decryptError.getMessage());
				// 如果解密失败，可能是密钥不一致，尝试使用原始密码
				if (cfg.getRawPassword() != null && !cfg.getRawPassword().isEmpty()) {
					pwd = cfg.getRawPassword();
				} else {
					throw new RuntimeException("密码解密失败，且无明文密码可用: " + decryptError.getMessage());
				}
			}
			
			// 动态构建JDBC URL
			String url = String.format("jdbc:mysql://%s:%d/%s?characterEncoding=utf-8&serverTimezone=Asia/Shanghai&connectTimeout=10000&socketTimeout=10000",
					cfg.getHost(), cfg.getPort(), cfg.getDatabaseName());
			try (java.sql.Connection conn = java.sql.DriverManager.getConnection(url, cfg.getUsername(), pwd)) {
				boolean isValid = conn.isValid(10); // 增加超时时间到10秒
				if (!isValid) {
					throw new RuntimeException("数据库连接验证失败");
				}
				return true;
			}
		} catch (java.sql.SQLException e) {
			String errorMsg = "数据库连接失败: " + e.getMessage();
			if (e.getMessage().contains("Access denied")) {
				errorMsg = "用户名或密码错误";
			} else if (e.getMessage().contains("Unknown database")) {
				errorMsg = "数据库不存在: " + cfg.getDatabaseName();
			} else if (e.getMessage().contains("Communications link failure")) {
				errorMsg = "网络连接失败，请检查主机地址和端口";
			}
			throw new RuntimeException(errorMsg, e);
		} catch (Exception e) {
			throw new RuntimeException("连接测试失败: " + e.getMessage(), e);
		}
	}



	/**
	 * 更新数据库配置状态
	 */
	@Override
	@Transactional
	public boolean updateStatus(Long userId, Long id, Integer status) {
		try {
			DbConfig config = dbConfigMapper.selectById(id);
			if (config == null) {
				throw new IllegalArgumentException("数据库配置不存在");
			}
			
			config.setStatus(status);
			int result = dbConfigMapper.updateById(config);
			if (result > 0) {
				auditService.log("update_db_config_status", "db_config", id, null, null);
				log.info("更新数据库配置 {} 状态为: {}", id, status);
			}
			return result > 0;
		} catch (Exception e) {
			log.error("更新数据库配置状态失败: {}", e.getMessage(), e);
			return false;
		}
	}

	/**
	 * 获取所有启用的数据库配置
	 */
	@Override
	public List<DbConfig> getEnabledConfigs() {
		try {
			LambdaQueryWrapper<DbConfig> wrapper = new LambdaQueryWrapper<>();
			wrapper.eq(DbConfig::getStatus, 1); // 只查询启用状态的配置
			return dbConfigMapper.selectList(wrapper);
		} catch (Exception e) {
			log.error("获取启用的数据库配置失败: {}", e.getMessage(), e);
			return new ArrayList<>();
		}
	}



	@Override
	public DbConfig getById(Long userId, Long id) {
		// 移除权限检查，实现最小闭环
		// if (!checkAccess(userId, id, "use")) {
		// 	throw new SecurityException("没有权限访问该数据库配置");
		// }
		return dbConfigMapper.selectById(id);
	}

	@Override
	@Transactional
	public boolean deleteConfig(Long userId, Long id) {
		// 删除相关的ACL记录
		// 由于已移除权限控制，直接删除配置
		
		// 删除数据库配置
		int result = dbConfigMapper.deleteById(id);
		if (result > 0) {
			auditService.log("delete_db_config", "db_config", id, null, null);
		}
		return result > 0;
	}
	
	@Override
	public DbConfig getDbConfig(Long dbConfigId) {
		// 内部使用，不检查权限
		return dbConfigMapper.selectById(dbConfigId);
	}
}


