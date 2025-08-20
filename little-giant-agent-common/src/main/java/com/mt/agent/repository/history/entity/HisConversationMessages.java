package com.mt.agent.repository.history.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import java.time.LocalDateTime;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * <p>
 * 历史会话消息表
 * </p>
 *
 * @author wsx
 * @since 2025-07-10
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("his_conversation_messages")
@ApiModel(value="HisConversationMessages对象", description="历史会话消息表")
public class HisConversationMessages implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "关联会话ID")
    @TableField("conversation_id")
    private String conversationId;

    @ApiModelProperty(value = "消息角色")
    @TableField("role")
    private String role;

    @ApiModelProperty(value = "消息内容（支持长文本）")
    @TableField("content")
    private String content;

    @ApiModelProperty(value = "时间")
    @TableField("created_at")
    private String createdAt;

    @ApiModelProperty(value = "思考过程")
    @TableField("think")
    private String think;

    @ApiModelProperty(value = "消息ID，用于其他平台存储")
    @TableField("message_id")
    private String messageId;

}
