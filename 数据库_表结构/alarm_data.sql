-- ----------------------------
-- Table structure for table alarm_data
-- ----------------------------
DROP TABLE IF EXISTS `alarm_data`;
CREATE TABLE `alarm_data` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `alarm_configuration_id` bigint NOT NULL COMMENT '''报警配置id''',
  `type` bigint NOT NULL COMMENT '''类型 1:预警 2:报警''',
  `business_type` bigint NOT NULL DEFAULT '1' COMMENT '''业务类型 1:预报警设置 2漏铝报警设置 3离岗报警设置 4-熔铸单元节点设置''',
  `effective` bigint NOT NULL COMMENT '''是否生效 1:生效 2:不生效''',
  `alarm_value` longtext NOT NULL COMMENT '''报警内''',
  `other_alarm_value` longtext NOT NULL COMMENT '''其他数据''',
  `start_time` datetime(3) NOT NULL COMMENT '''开始时间''',
  `end_time` datetime(3) DEFAULT NULL COMMENT '''结束时间''',
  `doing` bigint NOT NULL COMMENT '''是否处理 0:报警中 1:已完成''',
  `status` bigint NOT NULL COMMENT '''状态 1:未处理 2:已处理 -1:删除''',
  `feedback` longtext NOT NULL COMMENT '''反馈信息''',
  `lock` bigint NOT NULL DEFAULT '0' COMMENT '''是否锁定 0:未触发 ，1:手法触发,2:手动取消, 3:自动取消 4 全局锁''',
  `hot_image` longtext COMMENT '''检测JPEG图片BASE64的字符串,无时为None''',
  `called_to_phone` varchar(191) NOT NULL DEFAULT '13503239023' COMMENT '''处置人联系电话''',
  `content` longtext NOT NULL COMMENT '''处理方式''',
  `created_at` datetime(3) NOT NULL COMMENT '''创建时间''',
  `updated_at` datetime(3) NOT NULL COMMENT '''更新时间''',
  `hy_water_file` longtext COMMENT '''水听器声音文件''',
  `hy_water_file_sync` bigint NOT NULL DEFAULT '0' COMMENT '''记录文件是否同步  0:未同步 ，1:已同步,2:同步失败''',
  `push_done` bigint NOT NULL COMMENT '''是否推送过结束信息 0:否 1:是''',
  `disposal_person_name` longtext NOT NULL COMMENT '''处置人''',
  `level` bigint NOT NULL COMMENT '''级别 1:重大风险 2:较大风险 3:一般风险 4:低风险''',
  `data_source` bigint NOT NULL DEFAULT '1' COMMENT '''数据来源 1:系统生成 2:手动添加''',
  `create_user` bigint NOT NULL COMMENT '''创建人''',
  `update_user` bigint NOT NULL COMMENT '''更新人''',
  `risk_score` double DEFAULT '0' COMMENT '''风险分值 ''',
  `alarm_configuration_record` longtext,
  `workshop_id` bigint NOT NULL COMMENT '''车间id''',
  `alarm_configuration_name` longtext COMMENT '''报警配置名称''',
  PRIMARY KEY (`id`),
  KEY `idx_start_time` (`start_time`),
  KEY `idx_end_time` (`end_time`)
) ENGINE=InnoDB AUTO_INCREMENT=611 DEFAULT CHARSET=utf8mb3 COMMENT='告警事件表 存的是PLC相关的告警和漏铝告（报）警，漏铝告警也是水听器告警、声纹盒子报警、热成像报警， 存的是根据告警配置产生的告警事件';

