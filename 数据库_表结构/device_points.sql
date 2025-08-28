-- ----------------------------
-- Table structure for table device_points
-- ----------------------------
DROP TABLE IF EXISTS `device_points`;
CREATE TABLE `device_points` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` longtext COMMENT '''设备编码''',
  `device_channel_id` bigint DEFAULT NULL COMMENT '''设备通道id''',
  `workshop_id` bigint DEFAULT NULL COMMENT '''所属车间id''',
  `device_id` bigint DEFAULT NULL COMMENT '''设备id''',
  `describe` longtext COMMENT '''iot描述,这里对应设备名称''',
  `value` double DEFAULT NULL COMMENT '''属性点名称的值''',
  `value_type` double DEFAULT NULL COMMENT '''属性点名称的值数据类型0:无效型1:布尔型2:浮点型3:整型4:字符型''',
  `alarm_value` double DEFAULT NULL COMMENT '''报警值0表示无报警非0表示有报警''',
  `quality_stamp` double DEFAULT NULL COMMENT '''测点质量戳''',
  `alarm_name` longtext COMMENT '''测点报警集合''',
  `show` bigint DEFAULT NULL COMMENT '''是否显示 1显示,-1不显示''',
  `offline_show` bigint DEFAULT NULL COMMENT '''离线显示 1显示,-1不显示''',
  `online_status` bigint DEFAULT NULL COMMENT '''在线状态 1正常,-1断开''',
  `unity` longtext COMMENT '''测点单位''',
  `location` longtext COMMENT '''测点位置''',
  `upper_limit` longtext COMMENT '''测点上限''',
  `lower_limit` longtext COMMENT '''测点下限''',
  `remark` longtext COMMENT '''备注''',
  `position` bigint DEFAULT NULL COMMENT '''位号''',
  `signal_type` longtext COMMENT '''信号类型 1: 开关量 2: 模拟量 ''',
  `screen_index_id` bigint DEFAULT NULL COMMENT '''屏显标签id''',
  `status` bigint DEFAULT NULL COMMENT '''状态 1正常,-1被删除''',
  `created_at` datetime(3) NOT NULL,
  `updated_at` datetime(3) NOT NULL,
  `gov_unit` longtext COMMENT '''上报测点单位''',
  `gov_unit_rate` double DEFAULT NULL COMMENT '''上报测点单位转换比例''',
  `province_point_name` longtext COMMENT '''省标平台''',
  `quota_id` varchar(50) DEFAULT NULL COMMENT '''上报省厅id''',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=741 DEFAULT CHARSET=utf8mb3 COMMENT='设备点位表，记录了PLC设备的所有点位信息，注意，这里的name字段是设备编码，通常是一个唯一的标识符。describe才是点位的名称，如温度, 压力、电磁阀等。设备点位也叫传感器点位，记录了设备的各种属性和状态信息。每个点位可以有不同的数据类型，如布尔型、浮点型、整型等。传感器异常：online_status为-1,这个表不存点位的值数据，只存点位的属性信息、以及在线状态相关信息';

