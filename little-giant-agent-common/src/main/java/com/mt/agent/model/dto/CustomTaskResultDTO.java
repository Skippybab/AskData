package com.mt.agent.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 定制任务执行结果
 *
 * @author lfz
 * @date 2025/4/23 16:30
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CustomTaskResultDTO {

    /**
     * 执行是否成功
     */
    private Boolean isSuccess;

    /**
     * 结果类型，说明result字段中的泛型类型
     * 目前的类型有：
     *     SQL执行结果：List<Map>
     *     大模型文本理解执行结果：String
     */
    private String resultType;

    /**
     * 执行结果
     */
    private List<Map<String,Object>> result;

    /**
     * 错误信息，兜底模块使用
     */
    private String errorMsg;

}
