package com.mt.agent.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 预设任务执行结果
 *
 * @author lfz
 * @date 2025/4/24 11:52
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PresetsTaskResultDTO {

    /**
     * 工作流Id
     */
    private String templateCode;

    /**
     * 执行是否成功
     */
    private Boolean success;

    /**
     * 执行结果
     */
    private List<Map<String, Object>> result;

    /**
     * 结果说明或错误信息
     */
    private String message;


}
