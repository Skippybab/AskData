package com.mt.agent.model.workflow;

import lombok.Data;
import java.util.List;

/**
 * 节点配置信息
 */
@Data
public class NodeConfig {
    /**
     * 节点ID
     */
    private String nodeId;

    /**
     * 节点类型
     */
    private String nodeType;

    /**
     * 是否为初始节点
     */
    private Boolean isRoot;

    /**
     * 节点描述
     */
    private String desc;

    /**
     * 入参列表
     */
    private List<NodeInputsParam> inputs;

    /**
     * 出参列表
     */
    private List<NodeOutputsParam> outputs;

    /**
     * 下一节点列表
     */
    private List<String> nextNodes;

}