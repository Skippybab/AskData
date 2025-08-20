package com.mt.agent.model.questionYaml;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 问题配置实体类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionConfig {
    /**
     * 问题列表
     */
    private List<Question> questions;
}