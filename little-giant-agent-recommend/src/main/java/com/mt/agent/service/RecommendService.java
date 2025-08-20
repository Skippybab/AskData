package com.mt.agent.service;

import com.mt.agent.model.Result;

public interface RecommendService {

    /**
     * 根据问题模版编号获取推荐问题
     *
     * @author zzq
     * @date 2025/4/9 15:51
     * @param questionId 问题模版编号
     * @param userId 用户id
     */
    Result recommendQuestion(String questionId, String userId);
}
