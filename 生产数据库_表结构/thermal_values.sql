-- ----------------------------
-- Table structure for table thermal_values
-- ----------------------------
DROP TABLE IF EXISTS `thermal_values`;
CREATE TABLE `thermal_values` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `view_id` bigint DEFAULT NULL COMMENT '''视频id''',
  `ip` varchar(255) DEFAULT NULL COMMENT '''ip地址''',
  `max_temp_str` varchar(255) DEFAULT NULL COMMENT '''最高温度''',
  `max_temp` double DEFAULT NULL COMMENT '''最高温度''',
  `min_temp_str` varchar(255) DEFAULT NULL COMMENT '''最低温度''',
  `min_temp` double DEFAULT NULL COMMENT '''最低温度''',
  `status` bigint DEFAULT NULL COMMENT '''是否在线 1:在线 -1:离线''',
  `default` double DEFAULT NULL COMMENT '''对比温度''',
  `value` double DEFAULT NULL,
  `created_at` datetime(3) NOT NULL,
  `updated_at` datetime(3) NOT NULL COMMENT '''更新时间''',
  `view_name` longtext COMMENT '''视频名称''',
  `workshop_id` bigint DEFAULT NULL COMMENT '''所属车间id''',
  `workshop_name` longtext COMMENT '''所属车间名称''',
  `workshop_status` bigint DEFAULT NULL COMMENT '''所属车间状态 5-铸造准备、6-铸造期间、7-铸造结束''',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;

