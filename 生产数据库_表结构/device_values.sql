-- ----------------------------
-- Table structure for table device_values
-- ----------------------------
DROP TABLE IF EXISTS `device_values`;
CREATE TABLE `device_values` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `workshop_id` bigint DEFAULT NULL COMMENT '''所属车间id''',
  `device_point_code` longtext COMMENT '''设备点位编码''',
  `value` double DEFAULT NULL COMMENT '''属性点名称的值''',
  `alarm_value` double DEFAULT NULL COMMENT '''报警值 0表示无报警,非0表示有报警''',
  `quality_stamp` double DEFAULT NULL COMMENT '''测点质量戳''',
  `created_at` datetime(3) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;

