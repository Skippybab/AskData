package com.mt.agent.model.workflow;

import lombok.Data;

/**
 * 节点入参
 */
@Data
public class NodeInputsParam {

    /**
     * 字段名
     */
    private String fieldName;

    /**
     * 字段类型
     */
    private String fieldType;

    /**
     * 来源类型
     */
    private Integer sourceType;

    /**
     * 默认值
     */
    private Object defaultValue;

    /**
     * 值来源，格式为"nodeId.fieldName"
     */
    private String valueSource;

}