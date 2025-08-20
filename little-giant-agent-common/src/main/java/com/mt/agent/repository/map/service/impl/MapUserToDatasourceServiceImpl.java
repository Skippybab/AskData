package com.mt.agent.repository.map.service.impl;

import com.mt.agent.repository.map.entity.MapUserToDatasource;
import com.mt.agent.repository.map.mapper.MapUserToDatasourceMapper;
import com.mt.agent.repository.map.service.IMapUserToDatasourceService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 用户和数据源的映射关系表 服务实现类
 * </p>
 *
 * @author lfz
 * @since 2025-04-10
 */
@Service
public class MapUserToDatasourceServiceImpl extends ServiceImpl<MapUserToDatasourceMapper, MapUserToDatasource> implements IMapUserToDatasourceService {

}
