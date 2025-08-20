package com.mt.agent.repository.value.mapper;

import com.mt.agent.repository.value.entity.ValueQuestionDetail;
import com.mt.agent.repository.value.entity.ValueRecommendRelation;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mt.agent.repository.value.vo.RecommendQuestionVo;

import java.util.List;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author zzq
 * @since 2025-04-07
 */
public interface ValueRecommendRelationMapper extends BaseMapper<ValueRecommendRelation> {
    public List<ValueQuestionDetail> getRecommendQuestionList(String questionId);

}
