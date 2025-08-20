package com.mt.agent.repository.consensus.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 单轮对话记录表
 * </p>
 *
 * @author zzq
 * @since 2025-04-24
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("cons_conversation_record")
@ApiModel(value="ConsConversationRecord对象", description="单轮对话记录表")
@Builder
public class ConsConversationRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "单轮对话的id")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "用户id")
    @TableField("user_id")
    private Long userId;

    @ApiModelProperty(value = "客户问题原文")
    @TableField("question")
    private String question;

    @ApiModelProperty(value = "用户意图的详细文本")
    @TableField("intent_text")
    private String intentText;

    @ApiModelProperty(value = "生成对话的时间")
    @TableField("chat_time")
    private String chatTime;

    @ApiModelProperty(value = "数据源唯一标识")
    @TableField("datasource_id")
    private Long datasourceId;

    @ApiModelProperty(value = "系统功能编号")
    @TableField("fun_code")
    private String funCode;

    @ApiModelProperty(value = "该轮对话的概括性总结")
    @TableField("summary")
    private String summary;

    @ApiModelProperty(value = "工作流执行结果")
    @TableField("result")
    private String result;

    @ApiModelProperty(value = "针对可视化组件的操作")
    @TableField("operations")
    private String operations;

    @ApiModelProperty(value = "对话的状态")
    @TableField("status")
    private Integer status;

    @ApiModelProperty(value = "记录的有效状态：0.无效已过期 1.有效")
    @TableField("available")
    private Integer available;

}
