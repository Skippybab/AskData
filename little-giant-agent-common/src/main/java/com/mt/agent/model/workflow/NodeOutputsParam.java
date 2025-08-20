package com.mt.agent.model.workflow;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 节点出参
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NodeOutputsParam {

    /**
     * 字段名
     */
    private String fieldName;

    /**
     * 字段类型
     */
    private String fieldType;

    /**
     * 值
     */
    private Object value;

}