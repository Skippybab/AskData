package com.mt.agent.buffer.model;

import com.mt.agent.utils.DateExtendUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Buffer implements Serializable {
    private static final long serialVersionUID = 1L;

    private DataSource dataSource;

    /**
     * 选择的数据源
     */
    @Data
    public static class DataSource implements Serializable {
        private static final long serialVersionUID = 1L;
        private String dataSourceIndex; // 数据源编号
        private String dataSourceName; // 数据源名称
        private String dataSourceDesc; // 数据源描述
        private String dbUrl; // 数据库url
        private String tableName; // 表名
        private String fieldDetail; // 表字段信息
        private Integer count; // 记录条数

    }

    /**
     * 历史日志记录
     * 用于记录系统操作和用户交互的历史记录
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HistoryLog implements Serializable {
        private static final long serialVersionUID = 1L;

        /**
         * 日志时间
         */
        private String timestamp;

        /**
         * 角色（系统/用户）
         */
        private String role;

        /**
         * 日志内容
         */
        private String content;


        /**
         * 创建一个基础的历史日志记录
         */
        public static HistoryLog createBasicLog(String role, String content, String type) {
            HistoryLog log = new HistoryLog();
            log.setTimestamp(DateExtendUtil.localDateTimeToStringDate(LocalDateTime.now()));
            log.setRole(role);
            log.setContent(content);
            return log;
        }

        public String toString() {
            // 时间, 角色, 内容
            // 2025-05-15 12:00:00|用户|请帮我查询通用设备制造业的营收情况
            String processedContent = content != null ? content.replaceAll("\\r?\\n", " ") : "";
            return timestamp + "|" + role + "|" + processedContent;
        }
    }


    /**
     * 上一次的规划的方案
     */
    public static class previousPlan implements Serializable {
        private static final long serialVersionUID = 1L;

    }

}
