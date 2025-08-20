package com.mt.agent.repository.data.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import java.io.Serializable;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * <p>
 * 小巨人企业经营信息表
 * </p>
 *
 * @author lfz
 * @since 2025-03-17
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("data_little_giant_business_info")
@ApiModel(value = "DataLittleGiantBusinessInfo对象", description = "小巨人企业经营信息表")
@AllArgsConstructor
@NoArgsConstructor
public class DataLittleGiantBusinessInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "数据编号")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "企业所属区编码")
    @TableField("district_code")
    private String districtCode;

    @ApiModelProperty(value = "所属地市")
    @TableField("city")
    private String city;

    @ApiModelProperty(value = "所属区县")
    @TableField("region")
    private String region;

    @ApiModelProperty(value = "注册时间")
    @TableField("registered_time")
    private String registeredTime;

    @ApiModelProperty(value = "注册资本（万元）")
    @TableField("registered_capital")
    private Double registeredCapital;

    @ApiModelProperty(value = "所属行业")
    @TableField("industry_code")
    private String industryCode;

    @ApiModelProperty(value = "从事细分市场年限")
    @TableField("market_experience")
    private String marketExperience;

    @ApiModelProperty(value = "年份")
    @TableField("year")
    private String year;

    @ApiModelProperty(value = "全职员工数量(人)")
    @TableField("employee_num")
    private Integer employeeNum;

    @ApiModelProperty(value = "营业收入（万元）")
    @TableField("total_revenue")
    private Double totalRevenue;

    @ApiModelProperty(value = "主营业务收入（万元）")
    @TableField("main_revenue")
    private Double mainRevenue;

    @ApiModelProperty(value = "主营业务收入占比")
    @TableField("main_revenue_ratio")
    private Double mainRevenueRatio;

    @ApiModelProperty(value = "主持国际/国家标准")
    @TableField("preside_international_std")
    private Integer presideInternationalStd;

    @ApiModelProperty(value = "主持行业标准")
    @TableField("preside_industry_std")
    private Integer presideIndustryStd;

    @ApiModelProperty(value = "参与制修订标准总数")
    @TableField("participate_std")
    private Integer participateStd;

    @ApiModelProperty(value = "有效发明专利数量")
    @TableField("valid_patents")
    private Integer validPatents;

    @ApiModelProperty(value = "销售费用（万元）")
    @TableField("sales_expenses")
    private Double salesExpenses;

    @ApiModelProperty(value = "管理费用（万元）")
    @TableField("management_expenses")
    private Double managementExpenses;

    @ApiModelProperty(value = "主营业务成本（万元）")
    @TableField("main_business_cost")
    private Double mainBusinessCost;

    @ApiModelProperty(value = "毛利率百分比")
    @TableField("gross_margin_pct")
    private Double grossMarginPct;

    @ApiModelProperty(value = "人均营业收入（万元）")
    @TableField("revenue_per_capita")
    private Double revenuePerCapita;

    @ApiModelProperty(value = "出口额（万元）")
    @TableField("export_amount")
    private Double exportAmount;

    @ApiModelProperty(value = "研发费用总额（万元）")
    @TableField("rd_expense_total")
    private Double rdExpenseTotal;

    @ApiModelProperty(value = "研发费用占比")
    @TableField("rd_revenue_ratio")
    private Double rdRevenueRatio;

    @ApiModelProperty(value = "营业收入增长率")
    @TableField("revenue_growth_rate")
    private Double revenueGrowthRate;

    @ApiModelProperty(value = "净利润总额（万元）")
    @TableField("net_profit")
    private Double netProfit;

    @ApiModelProperty(value = "净利润增长率")
    @TableField("profit_growth_rate")
    private Double profitGrowthRate;


    public DataLittleGiantBusinessInfo(
            String districtCode, String city, String region, String registeredTime,
            Double registeredCapital, String industryCode, String marketExperience, String year,
            Integer employeeNum, Double totalRevenue, Double mainRevenue, Double mainRevenueRatio,
            Integer presideInternationalStd, Integer presideIndustryStd, Integer participateStd,
            Integer validPatents, Double salesExpenses, Double managementExpenses, Double mainBusinessCost,
            Double grossMarginPct, Double revenuePerCapita, Double exportAmount, Double rdExpenseTotal,
            Double rdRevenueRatio, Double revenueGrowthRate, Double netProfit, Double profitGrowthRate) {
        this.districtCode = districtCode;
        this.city = city;
        this.region = region;
        this.registeredTime = registeredTime;
        this.registeredCapital = registeredCapital;
        this.industryCode = industryCode;
        this.marketExperience = marketExperience;
        this.year = year;
        this.employeeNum = employeeNum;
        this.totalRevenue = totalRevenue;
        this.mainRevenue = mainRevenue;
        this.mainRevenueRatio = mainRevenueRatio;
        this.presideInternationalStd = presideInternationalStd;
        this.presideIndustryStd = presideIndustryStd;
        this.participateStd = participateStd;
        this.validPatents = validPatents;
        this.salesExpenses = salesExpenses;
        this.managementExpenses = managementExpenses;
        this.mainBusinessCost = mainBusinessCost;
        this.grossMarginPct = grossMarginPct;
        this.revenuePerCapita = revenuePerCapita;
        this.exportAmount = exportAmount;
        this.rdExpenseTotal = rdExpenseTotal;
        this.rdRevenueRatio = rdRevenueRatio;
        this.revenueGrowthRate = revenueGrowthRate;
        this.netProfit = netProfit;
        this.profitGrowthRate = profitGrowthRate;
    }


    // 检查数据合理性
    public boolean checkDataReasonability() {
        return districtCode != null && city != null && region != null && industryCode != null && year != null
                && employeeNum != null && totalRevenue != null && mainRevenue != null && mainRevenueRatio != null && validPatents != null
                && salesExpenses != null && managementExpenses != null && mainBusinessCost != null && grossMarginPct != null
                && revenuePerCapita != null && exportAmount != null && rdExpenseTotal != null && netProfit != null;
    }

}
