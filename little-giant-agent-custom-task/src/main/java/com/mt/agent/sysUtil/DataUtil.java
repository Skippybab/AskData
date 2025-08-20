package com.mt.agent.sysUtil;

import cn.hutool.json.JSONException;
import com.mt.agent.enums.ViewComponentType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据可视化工具类
 * 用于将各种数据格式转换为前端可视化所需的标准化格式
 */
@Slf4j
@Component
public class DataUtil {

    /**
     * 从List<Map<String, Object>>中提取指定属性名的所有值，返回List<String>
     * @param dataList
     * @param propertyName
     * @return
     */
    public static List<String> extractStringListFromDataList(List<Map<String, Object>> dataList, String propertyName) {
        List<String> result = new ArrayList<>();
        if (dataList == null || propertyName == null) {
            return result;
        }

        for (Map<String, Object> map : dataList) {
            if (map != null && map.containsKey(propertyName)) {
                result.add(String.valueOf(map.get(propertyName)));
            }
        }
        return result;
    }

    /**
     * 从List<Map<String, Object>>中提取指定属性名的所有值，返回List<Double>
     * @param dataList
     * @param propertyName
     * @return
     */
    public static List<Double> extractDoubleListFromDataList(List<Map<String, Object>> dataList, String propertyName) {
        List<Double> result = new ArrayList<>();
        if (dataList == null || propertyName == null) {
            return result;
        }

        for (Map<String, Object> map : dataList) {
            if (map != null && map.containsKey(propertyName)) {
                result.add(Double.valueOf(String.valueOf(map.get(propertyName))));
            }
        }
        return result;
    }

}
