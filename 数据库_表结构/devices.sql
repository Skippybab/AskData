-- ----------------------------
-- Table structure for table devices
-- ----------------------------
DROP TABLE IF EXISTS `devices`;
CREATE TABLE `devices` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `device_name` varchar(255) DEFAULT NULL COMMENT '''名称''',
  `device_channel_id` bigint DEFAULT NULL COMMENT '''设备通道id''',
  `device_status_name` varchar(255) DEFAULT NULL COMMENT '''设备状态名称''',
  `device_status_value` double DEFAULT NULL COMMENT '''设备状态值''',
  `device_control_name` varchar(255) DEFAULT NULL COMMENT '''设备控制名称''',
  `device_control_value` double DEFAULT NULL COMMENT '''设备控制值''',
  `status` bigint DEFAULT NULL COMMENT '''状态 1正常,-1被删除''',
  `created_at` datetime(3) NOT NULL,
  `updated_at` datetime(3) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=22 DEFAULT CHARSET=utf8mb3;

