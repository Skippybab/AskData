package com.mt.agent.model.questionYaml;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 问题实体类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Question {
    /**
     * 问题ID
     */
    private String id;
    
    /**
     * 问题名称
     */
    private String name;
    
    /**
     * 问题参数列表
     */
    private List<String> params;
}