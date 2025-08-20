package com.mt.agent.repository.data.service.impl;

import com.mt.agent.repository.data.entity.DataLittleGiantBusinessInfo;
import com.mt.agent.repository.data.mapper.DataLittleGiantBusinessInfoMapper;
import com.mt.agent.repository.data.service.IDataLittleGiantBusinessInfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 小巨人企业经营信息表 服务实现类
 * </p>
 *
 * @author lfz
 * @since 2025-03-17
 */
@Service
public class DataLittleGiantBusinessInfoServiceImpl extends ServiceImpl<DataLittleGiantBusinessInfoMapper, DataLittleGiantBusinessInfo> implements IDataLittleGiantBusinessInfoService {

    @Autowired
    private DataLittleGiantBusinessInfoMapper dataLittleGiantBusinessInfoMapper;
    @Override
    public String getLastYear() {
        return dataLittleGiantBusinessInfoMapper.getLastYear();
    }

    @Override
    public String getRecommendCity() {
        // 只保留文字相关内容
        String recommendCity = dataLittleGiantBusinessInfoMapper.getRecommendCity();
        return recommendCity.replaceAll("[^\\u4e00-\\u9fa5]", "");
    }

    @Override
    public String getRecommendIndustry() {
        String recommendIndustry = dataLittleGiantBusinessInfoMapper.getRecommendIndustry();
        return recommendIndustry.replaceAll("[^\\u4e00-\\u9fa5]", "");
    }

    @Override
    public String getRecommendEnterprise(String industryCode) {
        String recommendEnterprise = dataLittleGiantBusinessInfoMapper.getRecommendEnterprise(industryCode);
        return recommendEnterprise.replaceAll("[^\\u4e00-\\u9fa5]", "");
    }
}
