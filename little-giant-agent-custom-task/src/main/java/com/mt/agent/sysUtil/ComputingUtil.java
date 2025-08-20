package com.mt.agent.sysUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class ComputingUtil {

    // 统一的精度和舍入模式常量
    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    /**
     * 将Object转换为BigDecimal，用于内部计算
     */
    private static BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        return new BigDecimal(value.toString().trim());
    }

    /**
     * BigDecimal转换为double，统一保留两位小数
     */
    private static double toDouble(BigDecimal value) {
        return value.setScale(SCALE, ROUNDING_MODE).doubleValue();
    }

    /**
     * 数组求和
     *
     * @param values 需要求和的值列表
     * @return 求和结果，保留两位小数
     */
    public static double sum(List<Object> values) {
        if (values == null || values.isEmpty()) {
            return 0.00;
        }

        BigDecimal sum = values.stream()
                .map(ComputingUtil::toBigDecimal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return toDouble(sum);
    }

    /**
     * 数组求平均
     *
     * @param values 需要求平均的值列表
     * @return 平均值，保留两位小数
     */
    public static double avg(List<Object> values) {
        if (values == null || values.isEmpty()) {
            return 0.00;
        }

        BigDecimal sum = values.stream()
                .map(ComputingUtil::toBigDecimal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal avg = sum.divide(new BigDecimal(values.size()), SCALE + 2, ROUNDING_MODE);

        return toDouble(avg);
    }

    /**
     * 数值计算变化率 (新值-旧值)/旧值
     *
     * @param lastData 旧值
     * @param nextData 新值
     * @return 增长率（百分比形式，如0.25表示25%），保留两位小数
     */
    public static double changeRate(Object lastData, Object nextData) {
        if (lastData == null || nextData == null) {
            throw new IllegalArgumentException("【执行模块-计算工具类-计算变化率】计算变化率的值不能为空");
        }

        BigDecimal last = toBigDecimal(lastData);
        BigDecimal next = toBigDecimal(nextData);

        if (last.compareTo(BigDecimal.ZERO) == 0) {
            if (next.compareTo(BigDecimal.ZERO) == 0) {
                return 0.00;
            }
            return 1.00; // 100%增长率
        }

        BigDecimal result = next.subtract(last)
                .divide(last, SCALE + 2, ROUNDING_MODE);
        return toDouble(result);
    }

    /**
     * 数值计算占比 部分/整体
     *
     * @param part  部分值
     * @param total 总体值
     * @return 占比（百分比形式，如0.25表示25%），保留两位小数
     */
    public static double percentage(Object part, Object total) {
        if (part == null || total == null) {
            throw new IllegalArgumentException("【执行模块-计算工具类-计算占比】计算占比的值不能为空");
        }

        BigDecimal partValue = toBigDecimal(part);
        BigDecimal totalValue = toBigDecimal(total);

        if (totalValue.compareTo(BigDecimal.ZERO) == 0) {
            if (partValue.compareTo(BigDecimal.ZERO) == 0) {
                return 0.00;
            }
            throw new IllegalArgumentException("【执行模块-计算工具类-计算占比】总体值不能为0");
        }

        BigDecimal result = partValue.divide(totalValue, SCALE + 2, ROUNDING_MODE);
        return toDouble(result);
    }


}
