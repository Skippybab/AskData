package com.mt.agent.repository.data.service;

import com.mt.agent.repository.data.entity.DataLittleGiantBusinessInfo;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 小巨人企业经营信息表 服务类
 * </p>
 *
 * @author lfz
 * @since 2025-03-17
 */
public interface IDataLittleGiantBusinessInfoService extends IService<DataLittleGiantBusinessInfo> {

    public String getLastYear();

    public String getRecommendCity();

    public String getRecommendIndustry();

    public String getRecommendEnterprise(String industryCode);
}
