package com.mt.agent.workflow.api.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mt.agent.workflow.api.entity.DbConfig;
import com.mt.agent.workflow.api.entity.SchemaVersion;
import com.mt.agent.workflow.api.entity.SqlExecution;
import com.mt.agent.workflow.api.entity.SqlGuardPolicy;
import com.mt.agent.workflow.api.infra.DbConnectionPoolManager;
import com.mt.agent.workflow.api.infra.ExternalDbExecutor;
import com.mt.agent.workflow.api.infra.SqlParserUtil;
import com.mt.agent.workflow.api.mapper.DbConfigMapper;
import com.mt.agent.workflow.api.mapper.SchemaVersionMapper;
import com.mt.agent.workflow.api.mapper.SqlExecutionMapper;
import com.mt.agent.workflow.api.service.QueryCacheService;
import com.mt.agent.workflow.api.service.SqlExecutionService;
import com.mt.agent.workflow.api.service.SqlGuardService;
import com.mt.agent.workflow.api.service.impl.QueryCacheServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class SqlExecutionServiceImpl implements SqlExecutionService {

	@Autowired
	private DbConfigMapper dbConfigMapper;
	@Autowired
	private SqlExecutionMapper sqlExecutionMapper;
	@Autowired
	private SqlGuardService sqlGuardService;
	@Autowired
	private QueryCacheService queryCacheService;
	@Autowired
	private SchemaVersionMapper schemaVersionMapper;
	@Autowired
	private ObjectMapper objectMapper;

	private final byte[] masterKey = com.mt.agent.workflow.api.util.CryptoKeyProvider.getMasterKey();

	@Override
	public SqlExecution executeReadOnly(Long dbConfigId, String sql) {
		DbConfig cfg = dbConfigMapper.selectById(dbConfigId);
		if (cfg == null) throw new IllegalArgumentException("配置不存在");
		SqlGuardPolicy policy = sqlGuardService.getEffectivePolicy(0L, dbConfigId); // 使用默认租户ID
		sqlGuardService.validate(sql, policy);
		String safeSql = SqlParserUtil.ensureLimit(sql, policy.getMaxScanRows() == null ? 1000 : policy.getMaxScanRows().intValue());

		ExternalDbExecutor.ExecOptions opt = new ExternalDbExecutor.ExecOptions();
		opt.queryTimeoutSeconds = (policy.getMaxTimeoutMs() == null ? 30_000 : policy.getMaxTimeoutMs()) / 1000;
		opt.maxRows = (policy.getMaxScanRows() == null ? 1000 : policy.getMaxScanRows().intValue());

		DataSource ds = DbConnectionPoolManager.getOrCreate(dbConfigId, cfg, masterKey);
		try {
			ExternalDbExecutor.QueryResult qr = ExternalDbExecutor.query(ds, safeSql, opt);
			return createCompleteExecutionRecord(dbConfigId, safeSql, null, 1, qr.durationMs, qr.rowCount, null);
		} catch (RuntimeException e) {
			createCompleteExecutionRecord(dbConfigId, safeSql, null, 2, 0L, 0L, e.getMessage());
			throw e;
		}
	}

	@Override
	public SqlExecutionResult executeWithResult(Long dbConfigId, String sql) {
		DbConfig cfg = dbConfigMapper.selectById(dbConfigId);
		if (cfg == null) throw new IllegalArgumentException("配置不存在");

		// 获取最新schema版本
		SchemaVersion schemaVersion = getLatestSchemaVersion(dbConfigId);
		Long schemaVersionId = schemaVersion != null ? schemaVersion.getId() : null;

		// 计算SQL hash用于缓存
		String sqlHash = ((QueryCacheServiceImpl) queryCacheService).calculateSqlHash(sql);

		// 检查缓存
		if (schemaVersionId != null) {
			var cachedResult = queryCacheService.findPreview(0L, dbConfigId, schemaVersionId, sqlHash); // 使用默认租户ID
			if (cachedResult.isPresent()) {
				log.info("使用缓存的查询结果, sqlHash: {}", sqlHash);
				// 从缓存构造QueryResult
				ExternalDbExecutor.QueryResult result = parseJsonResult(cachedResult.get());
				// 创建一条完整的执行记录并直接插入
				SqlExecution exec = createCompleteExecutionRecord(dbConfigId, sql, schemaVersionId, 1, 0L, result.rowCount, null);
				return new SqlExecutionResult(exec, result);
			}
		}

		// 执行SQL
		SqlGuardPolicy policy = sqlGuardService.getEffectivePolicy(0L, dbConfigId); // 使用默认租户ID
		sqlGuardService.validate(sql, policy);
		String safeSql = SqlParserUtil.ensureLimit(sql, policy.getMaxScanRows() == null ? 1000 : policy.getMaxScanRows().intValue());

		ExternalDbExecutor.ExecOptions opt = new ExternalDbExecutor.ExecOptions();
		opt.queryTimeoutSeconds = (policy.getMaxTimeoutMs() == null ? 30_000 : policy.getMaxTimeoutMs()) / 1000;
		opt.maxRows = (policy.getMaxScanRows() == null ? 1000 : policy.getMaxScanRows().intValue());

		DataSource ds = DbConnectionPoolManager.getOrCreate(dbConfigId, cfg, masterKey);
		try {
			ExternalDbExecutor.QueryResult qr = ExternalDbExecutor.query(ds, safeSql, opt);
			SqlExecution exec = createCompleteExecutionRecord(dbConfigId, safeSql, schemaVersionId, 1, qr.durationMs, qr.rowCount, null);

			// 保存到缓存
			if (schemaVersionId != null) {
				String resultJson = buildResultPreviewJson(qr);
				queryCacheService.savePreview(0L, dbConfigId, schemaVersionId, sqlHash, resultJson, 3600_000L); // 1小时缓存，使用默认租户ID
			}

			return new SqlExecutionResult(exec, qr);
		} catch (RuntimeException e) {
			SqlExecution exec = createCompleteExecutionRecord(dbConfigId, safeSql, schemaVersionId, 2, 0L, 0L, e.getMessage());
			throw e;
		}
	}

	private SqlExecution createCompleteExecutionRecord(Long dbConfigId, String sql, Long schemaVersionId, int status, long durationMs, long affectedRows, String errorMsg) {
		SqlExecution exec = new SqlExecution();
		// 使用默认租户ID，因为tenantId字段已标记为非持久化
		exec.setTenantId(0L);
		exec.setDbConfigId(dbConfigId);
		exec.setSchemaVersionId(schemaVersionId);
		exec.setSqlText(sql);
		exec.setStatus(status);
		exec.setDurationMs(durationMs);
		exec.setAffectedRows(affectedRows);
		exec.setErrorMsg(errorMsg);
		exec.setCreatedAtMs(System.currentTimeMillis());
		exec.setFinishedAtMs(System.currentTimeMillis()); // 创建即完成
		sqlExecutionMapper.insert(exec);
		return exec;
	}

	@Override
	public ExternalDbExecutor.QueryResult getExecutionResult(Long executionId) {
		SqlExecution exec = sqlExecutionMapper.selectById(executionId);
		if (exec == null) {
			throw new IllegalArgumentException("执行记录不存在");
		}
		
		if (exec.getStatus() != 1) {
			throw new IllegalStateException("查询未成功执行，无法获取结果");
		}
		
		// 尝试从缓存获取
		DbConfig cfg = dbConfigMapper.selectById(exec.getDbConfigId());
		if (cfg != null && exec.getSchemaVersionId() != null) {
			String sqlHash = ((QueryCacheServiceImpl) queryCacheService).calculateSqlHash(exec.getSqlText());
			var cachedResult = queryCacheService.findPreview(0L, exec.getDbConfigId(), exec.getSchemaVersionId(), sqlHash); // 使用默认租户ID
			if (cachedResult.isPresent()) {
				return parseJsonResult(cachedResult.get());
			}
		}
		
		throw new IllegalStateException("查询结果已过期或不可用");
	}
	
	private SqlExecution createExecutionRecord(Long dbConfigId, String sql, Long schemaVersionId) {
		SqlExecution exec = new SqlExecution();
		exec.setDbConfigId(dbConfigId);
		exec.setSchemaVersionId(schemaVersionId);
		exec.setSqlText(sql);
		exec.setStatus(0);
		exec.setCreatedAtMs(System.currentTimeMillis());
		sqlExecutionMapper.insert(exec);
		return exec;
	}
	
	private SchemaVersion getLatestSchemaVersion(Long dbConfigId) {
		return schemaVersionMapper.selectOne(
			new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SchemaVersion>()
				.eq(SchemaVersion::getDbConfigId, dbConfigId)
				.eq(SchemaVersion::getStatus, 1)
				.orderByDesc(SchemaVersion::getVersionNo)
				.last("limit 1")
		);
	}
	
	private String buildResultPreviewJson(ExternalDbExecutor.QueryResult qr) {
		try {
			Map<String, Object> preview = new HashMap<>();
			preview.put("rows", qr.rows);
			preview.put("rowCount", qr.rowCount);
			preview.put("durationMs", qr.durationMs);
			
			// 提取列头信息
			if (!qr.rows.isEmpty()) {
				List<String> columns = List.copyOf(qr.rows.get(0).keySet());
				preview.put("columns", columns);
			}
			
			return objectMapper.writeValueAsString(preview);
		} catch (Exception e) {
			log.error("构建结果预览JSON失败: {}", e.getMessage(), e);
			return "{\"error\":\"" + e.getMessage() + "\"}";
		}
	}
	
	private ExternalDbExecutor.QueryResult parseJsonResult(String jsonResult) {
		try {
			@SuppressWarnings("unchecked")
			Map<String, Object> data = objectMapper.readValue(jsonResult, Map.class);
			
			ExternalDbExecutor.QueryResult result = new ExternalDbExecutor.QueryResult();
			result.rows = (List<Map<String, Object>>) data.get("rows");
			result.rowCount = ((Number) data.get("rowCount")).longValue();
			result.durationMs = data.containsKey("durationMs") ? ((Number) data.get("durationMs")).longValue() : 0L;
			
			return result;
		} catch (Exception e) {
			log.error("解析缓存结果失败: {}", e.getMessage(), e);
			throw new RuntimeException("解析查询结果失败", e);
		}
	}
}


