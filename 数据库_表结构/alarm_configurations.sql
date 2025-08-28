-- ----------------------------
-- Table structure for table alarm_configurations
-- ----------------------------
DROP TABLE IF EXISTS `alarm_configurations`;
CREATE TABLE `alarm_configurations` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` longtext CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '''名称''',
  `workshop_id` bigint NOT NULL COMMENT '''车间id''',
  `type` bigint NOT NULL COMMENT '''类型 1:预警 2:报警''',
  `effective` bigint NOT NULL COMMENT '''是否生效 1:生效 2:不生效''',
  `description` bigint NOT NULL COMMENT '''描述''',
  `level` bigint NOT NULL COMMENT '''级别 1:重大风险 2:较大风险 3:一般风险 4:低风险''',
  `status` bigint NOT NULL COMMENT '''状态 1:启用 -1:删除''',
  `create_user` bigint NOT NULL COMMENT '''创建人''',
  `update_user` bigint NOT NULL COMMENT '''更新人''',
  `alarm_time` bigint NOT NULL COMMENT '''触发报警时间单位秒''',
  `created_at` datetime(3) NOT NULL,
  `updated_at` datetime(3) NOT NULL,
  `business_type` bigint NOT NULL DEFAULT '1' COMMENT '''业务类型 1:预报警设置 2漏铝报警设置 3离岗报警设置 4 打卡报警设置''',
  `camera_id` bigint NOT NULL COMMENT '''摄像头id''',
  `camera_name` longtext CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '''摄像头名称''',
  `person_upper_limit` bigint NOT NULL COMMENT '''人数上限''',
  `person_lower_limit` bigint NOT NULL COMMENT '''人数下限''',
  `alarm_duration` bigint NOT NULL DEFAULT '1' COMMENT '''持续告警时间 时间单位秒 ''',
  `source` bigint NOT NULL DEFAULT '1' COMMENT '''数据来源 1:页面生成 2:手动添加漏铝事件生成''',
  `group_id` bigint DEFAULT NULL COMMENT '''分组id''',
  `risk_level` bigint DEFAULT NULL COMMENT '''几级事件''',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=80 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='告警配置表';

