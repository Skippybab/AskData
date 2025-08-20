package com.mt.agent.repository.value.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 问题详情数据表
 * </p>
 *
 * @author zzq
 * @since 2025-04-07
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("value_question_detail")
@ApiModel(value="ValueQuestionDetail对象", description="问题详情数据表")
public class ValueQuestionDetail implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "数据编号")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "问题编号")
    private String questionId;

    @ApiModelProperty(value = "问题名称")
    private String questionName;

    @ApiModelProperty(value = "问题拼接模版")
    private String questionTemplate;


}
