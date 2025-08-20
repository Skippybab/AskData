package com.mt.agent.model;

import lombok.Data;
import java.io.Serializable;

/**
 * 推荐问题模型类
 */
@Data
public class RecommendedQuestion implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 问题ID
     */
    private String questionId;

    /**
     * 问题描述
     */
    private String questionDesc;
}