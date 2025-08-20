package com.mt.agent.repository.value.service;

import com.mt.agent.repository.value.entity.ValueRecommendRelation;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mt.agent.repository.value.vo.RecommendQuestionVo;

import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author zzq
 * @since 2025-04-07
 */
public interface IValueRecommendRelationService extends IService<ValueRecommendRelation> {

    public List<RecommendQuestionVo> getRecommendQuestionList(String questionId);
}
