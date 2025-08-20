package com.mt.agent.consensus.model.consensus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 任务输入项
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InputItem implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 输入标题
     */
    private String inputTitle;
}