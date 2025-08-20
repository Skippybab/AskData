package com.mt.agent.repository.data.service.impl;

import com.mt.agent.repository.data.entity.DataSourceConfig;
import com.mt.agent.repository.data.mapper.DataSourceConfigMapper;
import com.mt.agent.repository.data.service.IDataSourceConfigService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 数据源配置表 服务实现类
 * </p>
 *
 * @author lfz
 * @since 2025-03-17
 */
@Service
public class DataSourceConfigServiceImpl extends ServiceImpl<DataSourceConfigMapper, DataSourceConfig> implements IDataSourceConfigService {

}
