package com.mt.agent.model;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * 聊天消息中的任务结果模型类
 * 设计为动态结构，可以支持任意JSON结构
 */
@Data
@NoArgsConstructor
public class ChatTaskResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 类型字段，用于标识任务结果的类型
     */
    private String type;

    /**
     * 存储所有其他动态属性
     */
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<>();

    /**
     * 获取所有动态属性
     */
    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    /**
     * 设置动态属性
     */
    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

    /**
     * 获取指定名称的属性
     */
    public Object getProperty(String name) {
        return this.additionalProperties.get(name);
    }

    /**
     * 判断是否包含指定属性
     */
    public boolean hasProperty(String name) {
        return this.additionalProperties.containsKey(name);
    }

    /**
     * 类型常量
     */
    public static class ResultType {
        public static final String INDICATOR_BLOCK = "IndicatorBlock";
        public static final String BAR_CHART = "BarChart";
        public static final String TEXT_LIST = "TextList";
    }
}