package com.mt.agent.consensus.model.consensus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 任务输出项
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OutputItem implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 输出标题
     */
    private String outputTitle;

    /**
     * 可视化样式
     */
    private String visualStyle;
}