package com.mt.agent.workflow.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mt.agent.workflow.api.entity.DbConfig;
import com.mt.agent.workflow.api.entity.SchemaVersion;
import com.mt.agent.workflow.api.entity.TableInfo;
import com.mt.agent.workflow.api.infra.DbConnectionPoolManager;
import com.mt.agent.workflow.api.infra.ExternalDbIntrospector;
import com.mt.agent.workflow.api.infra.MySqlIntrospector;
import com.mt.agent.workflow.api.mapper.DbConfigMapper;
import com.mt.agent.workflow.api.mapper.SchemaVersionMapper;
import com.mt.agent.workflow.api.mapper.TableInfoMapper;
import com.mt.agent.workflow.api.service.SchemaSyncService;
import com.mt.agent.workflow.api.util.CryptoKeyProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.util.List;

@Slf4j
@Service
public class SchemaSyncServiceImpl implements SchemaSyncService {

	@Autowired
	private SchemaVersionMapper schemaVersionMapper;
	@Autowired
	private DbConfigMapper dbConfigMapper;
	@Autowired
	private TableInfoMapper tableInfoMapper;

	private final ExternalDbIntrospector introspector = new MySqlIntrospector();

	@Override
	@Transactional
	public SchemaVersion startSync(Long dbConfigId) {
		log.info("开始同步数据库结构，dbConfigId: {}", dbConfigId);
		
		DbConfig cfg = dbConfigMapper.selectById(dbConfigId);
		if (cfg == null) {
			log.error("数据库配置不存在，dbConfigId: {}", dbConfigId);
			throw new IllegalArgumentException("配置不存在");
		}
		
		// tenantId字段已标记为非持久化，使用默认值0
		cfg.setTenantId(0L);

		// 获取最新版本号
		Integer maxNo = schemaVersionMapper.selectList(new LambdaQueryWrapper<SchemaVersion>()
				.eq(SchemaVersion::getDbConfigId, dbConfigId)
		).stream().map(SchemaVersion::getVersionNo).filter(v -> v != null).max(Integer::compareTo).orElse(0);

		// 创建新的schema版本记录
		SchemaVersion v = new SchemaVersion();
		v.setTenantId(0L); // 使用默认租户ID
		v.setDbConfigId(dbConfigId);
		v.setStatus(0); // 准备中
		v.setCreatedAtMs(System.currentTimeMillis());
		v.setVersionNo(maxNo + 1);
		schemaVersionMapper.insert(v);
		
		log.info("创建schema版本记录，versionNo: {}", v.getVersionNo());

		try {
			// 连接外部数据库
			DataSource ds = DbConnectionPoolManager.getOrCreate(dbConfigId, cfg, CryptoKeyProvider.getMasterKey());
			List<ExternalDbIntrospector.TableMeta> tables = introspector.listTables(ds, cfg.getDatabaseName());
			
			log.info("获取到 {} 个表，开始同步", tables.size());
			
			// 清理旧的表信息，避免重复数据
			LambdaQueryWrapper<TableInfo> deleteWrapper = new LambdaQueryWrapper<>();
			deleteWrapper.eq(TableInfo::getDbConfigId, dbConfigId);
			int deletedCount = tableInfoMapper.delete(deleteWrapper);
			log.info("清理旧表信息，删除 {} 条记录", deletedCount);
			
			int tc = 0;
			int successCount = 0;
			int failedCount = 0;
			
			for (ExternalDbIntrospector.TableMeta t : tables) {
				try {
									TableInfo ti = new TableInfo();
				ti.setDbConfigId(dbConfigId);
				ti.setTableName(t.tableName);
				ti.setTableComment(t.tableComment);
				ti.setEnabled(1); // 默认启用所有表，无需手动开启权限

					// 获取并设置DDL
					try {
						String ddl = introspector.getTableDdl(ds, t.tableName);
						ti.setTableDdl(ddl);
						successCount++;
						log.debug("成功获取表 {} 的DDL", t.tableName);
					} catch (Exception e) {
						log.warn("获取表 {} 的DDL失败: {}", t.tableName, e.getMessage());
						failedCount++;
						// 即使DDL获取失败，也保存表的基本信息
						ti.setTableDdl(null);
					}

					tableInfoMapper.insert(ti);
					tc++;
				} catch (Exception e) {
					log.error("同步表 {} 失败: {}", t.tableName, e.getMessage(), e);
					failedCount++;
				}
			}

			log.info("同步完成，总表数: {}, 成功: {}, 失败: {}", tc, successCount, failedCount);

			// 更新schema版本状态
			v.setTableCount(tc);
			v.setColumnCount(null); // DDL中已包含列信息，不再单独统计
			v.setStatus(1); // 成功
			v.setFinishedAtMs(System.currentTimeMillis());
			schemaVersionMapper.updateById(v);
			
			log.info("数据库结构同步成功完成，dbConfigId: {}, 版本: {}", dbConfigId, v.getVersionNo());
			return v;
			
		} catch (Exception e) {
			log.error("数据库结构同步失败，dbConfigId: {}", dbConfigId, e);
			
			// 更新schema版本状态为失败
			v.setStatus(2); // 失败
			v.setFinishedAtMs(System.currentTimeMillis());
			schemaVersionMapper.updateById(v);
			
			throw new RuntimeException("数据库结构同步失败: " + e.getMessage(), e);
		}
	}
}


