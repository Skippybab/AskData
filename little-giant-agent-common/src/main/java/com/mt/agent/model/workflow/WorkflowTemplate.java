package com.mt.agent.model.workflow;

import lombok.Data;

import java.util.List;

/**
 * 工作流模板
 */
@Data
public class WorkflowTemplate {
    /**
     * 工作流编号
     */
    private String funCode;

    /**
     * 模板类型（1-通用模板、2-个人模板）
     */
    private Integer templateType;

    /**
     * 节点配置列表
     */
    private List<NodeConfig> nodes;
}