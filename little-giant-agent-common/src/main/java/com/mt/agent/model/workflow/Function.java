package com.mt.agent.model.workflow;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;


/**
 * 功能定义
 * @Author: zzq
 * @Date: 2025/5/13 11:49
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Function {




    /**
     * 功能名称
     */
    private String funName;

    /**
     * 入参列表
     */
    private List<String> inputs;

    /**
     * 出参列表
     */
    private List<String> outputs;
}
