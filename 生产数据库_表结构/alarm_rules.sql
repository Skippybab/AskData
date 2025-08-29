-- ----------------------------
-- Table structure for table alarm_rules
-- ----------------------------
DROP TABLE IF EXISTS `alarm_rules`;
CREATE TABLE `alarm_rules` (
  `alarm_configuration_id` bigint NOT NULL COMMENT '''报警配置id''',
  `device_point_id` bigint NOT NULL COMMENT '''设备点位id''',
  `type` bigint NOT NULL COMMENT '''1:上限 2:下限 3:等值4:范围''',
  `device_type` bigint NOT NULL DEFAULT '1' COMMENT '''设备类型 1:plc 2:水听器 3：热成像''',
  `business_type` bigint NOT NULL DEFAULT '1' COMMENT '''业务类型 1:预报警设置 2漏铝报警设置 3离岗报警设置 4-熔铸单元节点设置''',
  `value` double NOT NULL COMMENT '''对比值''',
  `min_value` double NOT NULL COMMENT '''最小值''',
  `max_value` double NOT NULL COMMENT '''最大值''',
  `created_at` datetime(3) NOT NULL,
  `updated_at` datetime(3) NOT NULL,
  `twinkle_sign` bigint NOT NULL DEFAULT '1' COMMENT '''报警点位闪烁标识，1:不闪烁 2:闪烁''',
  `level` bigint DEFAULT NULL COMMENT '''值类型 0:无(默认) 1: 低低报 2: 低报 3:高报 4 高高报''',
  `device_point_name` longtext COMMENT '''设备点位名称'''
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='告警规则表';

