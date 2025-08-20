package com.mt.agent.repository.file.entity;

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
 * 工作流索引表
 * </p>
 *
 * @author lfz
 * @since 2025-03-24
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("file_question_template")
@ApiModel(value = "FileQuestionTemplate对象", description = "工作流索引表")
public class FileQuestionTemplate implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "数据编号")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "问题编号")
    private String questionNum;

    @ApiModelProperty(value = "问题名称")
    private String questionName;

    @ApiModelProperty(value = "文件地址")
    private String filePath;

    @ApiModelProperty(value = "模板类型 1.通用模板 2.个人模板")
    private Integer templateType;

    @ApiModelProperty(value = "用户id")
    private Long userId;

    @ApiModelProperty(value = "创建时间")
    private String createTime;

    @ApiModelProperty(value = "更新时间")
    private String updateTime;

}
