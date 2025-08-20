package com.mt.agent.repository.value.service.impl;

import com.mt.agent.repository.value.entity.ValueQuestionDetail;
import com.mt.agent.repository.value.entity.ValueRecommendRelation;
import com.mt.agent.repository.value.mapper.ValueQuestionDetailMapper;
import com.mt.agent.repository.value.mapper.ValueRecommendRelationMapper;
import com.mt.agent.repository.value.service.IValueRecommendRelationService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mt.agent.repository.value.vo.RecommendQuestionVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author zzq
 * @since 2025-04-07
 */
@Service
public class ValueRecommendRelationServiceImpl extends ServiceImpl<ValueRecommendRelationMapper, ValueRecommendRelation> implements IValueRecommendRelationService {

    @Autowired
    private ValueRecommendRelationMapper valueRecommendRelationMapper;

    @Override
    public List<RecommendQuestionVo> getRecommendQuestionList(String questionId) {
        // 前两个问题
        List<ValueQuestionDetail> list = valueRecommendRelationMapper.getRecommendQuestionList(questionId).stream().limit(2).toList();
        List<RecommendQuestionVo> recommendQuestionVoList = new ArrayList<>();
        for(ValueQuestionDetail valueQuestionDetail : list){
            recommendQuestionVoList.add(RecommendQuestionVo.builder().questionId(valueQuestionDetail.getQuestionId()).questionDesc(valueQuestionDetail.getQuestionTemplate()).build());
        }

        return recommendQuestionVoList;
    }
}
