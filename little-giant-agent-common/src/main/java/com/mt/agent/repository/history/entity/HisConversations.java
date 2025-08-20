package com.mt.agent.repository.history.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
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
 * 历史会话表
 * </p>
 *
 * @author wsx
 * @since 2025-07-10
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("his_conversations")
@ApiModel(value="HisConversations对象", description="历史会话表")
public class HisConversations implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "会话ID（UUID生成）")
    @TableId(value = "id")
    private String id;

    @ApiModelProperty(value = "关联用户ID")
    @TableField("user_id")
    private Long userId;

    @ApiModelProperty(value = "会话状态 1.会话中 2.已关闭")
    @TableField("status")
    private Short status;

    @ApiModelProperty(value = "自动生成的会话摘要")
    @TableField("title")
    private String title;

}
