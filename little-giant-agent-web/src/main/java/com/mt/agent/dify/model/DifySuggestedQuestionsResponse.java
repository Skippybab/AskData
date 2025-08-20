package com.mt.agent.dify.model;

import lombok.Data;

import java.util.List;

/**
 * Dify建议问题响应模型
 *
 * @author wsx
 * @date 2025/6/30
 */
@Data
public class DifySuggestedQuestionsResponse {

    /**
     * 建议问题列表
     */
    private List<String> data;

    /**
     * 对象类型，固定为"list"
     */
    private String object;
} 