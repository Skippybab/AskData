package com.mt.agent.repository.data.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * <p>
 * 数据源配置表
 * </p>
 *
 * @author lfz
 * @since 2025-03-17
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("data_source_config")
@ApiModel(value="DataSourceConfig对象", description="数据源配置表")
public class DataSourceConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "编号")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "表名")
    @TableField("table_name")
    private String tableName;

    @ApiModelProperty(value = "说明")
    @TableField("illustrate")
    private String illustrate;

    @ApiModelProperty(value = "标签")
    @TableField("label")
    private String label;

    @ApiModelProperty(value = "导入时间")
    @TableField("create_time")
    private String createTime;

    @ApiModelProperty(value = "上传用户")
    @TableField("user_id")
    private Long userId;

    @ApiModelProperty(value = "字段说明")
    @TableField("field_description")
    private String fieldDescription;

    @ApiModelProperty(value = "字段详情说明")
    @TableField("field_detail")
    private String fieldDetail;

    @ApiModelProperty(value = "记录条数")
    @TableField("count")
    private Integer count;
}
