package com.mt.agent.model.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
/**
 * 意图过滤结果类
 * @Author: zzq
 * @Date: 2025/4/22 10:43
 */
@Data
public class IntentFilterDTO {

    //todo 1.2版本可以删掉
    @ApiModelProperty(value = "用户意图文本")
    private String intentText;

    @ApiModelProperty(value = "是否安全")
    private String isSafe;
}
