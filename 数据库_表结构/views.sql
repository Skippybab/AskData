-- ----------------------------
-- Table structure for table views
-- ----------------------------
DROP TABLE IF EXISTS `views`;
CREATE TABLE `views` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `no` varchar(255) DEFAULT NULL COMMENT '''编号''',
  `name` varchar(255) DEFAULT NULL COMMENT '''设备国标编码''',
  `channel` bigint NOT NULL COMMENT '''通道''',
  `workshop_id` bigint NOT NULL COMMENT '''车间id''',
  `create_user` bigint NOT NULL COMMENT '''创建人''',
  `update_user` bigint NOT NULL COMMENT '''更新人''',
  `status` bigint NOT NULL COMMENT '''状态 1:启用 -1:删除''',
  `effective` bigint NOT NULL COMMENT '''是否生效 1:生效 2:不生效''',
  `model` longtext NOT NULL COMMENT '''型号''',
  `brand` longtext NOT NULL COMMENT '''品牌''',
  `need_human_num` bigint NOT NULL COMMENT '''是否需要人数统计 1:需要 0 不需要''',
  `monitor_plc` bigint NOT NULL COMMENT '''是否监控plc 1:需要 0 不需要''',
  `created_at` datetime(3) NOT NULL,
  `updated_at` datetime(3) NOT NULL,
  `max_person` bigint NOT NULL COMMENT ':''监控最大人数''',
  `min_person` bigint NOT NULL COMMENT ':''监控最小人数''',
  `category` bigint NOT NULL COMMENT '''设备类型 1:离岗摄像头',
  `offline_time` longtext COMMENT '''离线时间''',
  `online_status` bigint DEFAULT NULL COMMENT '''是否在线 1:在线 -1:离线''',
  `thermal_ip` varchar(255) DEFAULT NULL COMMENT '''热成像摄像头ip''',
  `thermal_port` varchar(255) DEFAULT NULL COMMENT '''热成像摄像头端口''',
  `thermal_user` varchar(255) DEFAULT NULL COMMENT '''热成像摄像头用户名''',
  `thermal_psd` varchar(255) DEFAULT NULL COMMENT '''热成像摄像头密码''',
  `device_id` varchar(255) DEFAULT NULL COMMENT '''通道国标编码''',
  `channel_id` varchar(255) DEFAULT NULL COMMENT '''名称''',
  `lat` longtext COMMENT '''经度''',
  `lng` longtext COMMENT '''纬度''',
  `place` longtext COMMENT '''安装位置''',
  `rtsp` varchar(255) DEFAULT NULL COMMENT '''rtsp配置地址''',
  `extra` longtext COMMENT '''额外信息''',
  `thermal_alarm_value` bigint DEFAULT NULL COMMENT '''热成像报警阈值''',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb3 COMMENT='摄像头、视频等相关信息的记录表 , 也叫视频类的监控设备信息';

