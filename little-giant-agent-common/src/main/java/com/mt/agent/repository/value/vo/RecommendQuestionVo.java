package com.mt.agent.repository.value.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 推荐问题返回对象
 * @Author: zzq
 * @Date: 2025/4/7 14:29
 */
@Data
@Builder
public class RecommendQuestionVo {
    // 问题id
    private String questionId;
    // 拼接后的问题
    private String questionDesc;
}
