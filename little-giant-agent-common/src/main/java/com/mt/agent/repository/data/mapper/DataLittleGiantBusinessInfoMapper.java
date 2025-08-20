package com.mt.agent.repository.data.mapper;

import com.mt.agent.repository.data.entity.DataLittleGiantBusinessInfo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * <p>
 * 小巨人企业经营信息表 Mapper 接口
 * </p>
 *
 * @author lfz
 * @since 2025-03-17
 */
public interface DataLittleGiantBusinessInfoMapper extends BaseMapper<DataLittleGiantBusinessInfo> {
    public String getLastYear();

    public String getRecommendCity();

    public String getRecommendIndustry();

    public String getRecommendEnterprise(String industryCode);

}
