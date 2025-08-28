-- ----------------------------
-- Table structure for table view_alarm_data
-- ----------------------------
DROP TABLE IF EXISTS `view_alarm_data`;
CREATE TABLE `view_alarm_data` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `workshop_id` bigint NOT NULL COMMENT '''车间id''',
  `effective` bigint NOT NULL COMMENT '''是否生效 1:生效 2:不生效''',
  `type` bigint NOT NULL COMMENT '''类型 1:超员 2:脱岗''',
  `status` bigint NOT NULL COMMENT '''状态 1:未处理 2:已处理 -1:删除''',
  `start_time` datetime(3) NOT NULL COMMENT '''开始时间''',
  `end_time` datetime(3) DEFAULT NULL COMMENT '''结束时间''',
  `people_num` bigint NOT NULL COMMENT '''人数''',
  `feedback` longtext NOT NULL COMMENT '''反馈信息''',
  `content` longtext NOT NULL COMMENT '''处理方式''',
  `detect_frame` longtext COMMENT '''检测JPEG图片BASE64的字符串,无时为None''',
  `created_at` datetime(3) NOT NULL COMMENT '''创建时间''',
  `updated_at` datetime(3) NOT NULL COMMENT '''更新时间''',
  `alarm_configuration_id` bigint NOT NULL COMMENT '''报警配置id''',
  `business_type` bigint NOT NULL DEFAULT '3' COMMENT '''业务类型 1:预报警设置 2漏铝报警设置 3离岗报警设置 4-熔铸单元节点设置''',
  `risk_score` double DEFAULT '0' COMMENT '''风险分值 ''',
  `level` bigint NOT NULL COMMENT '''级别 1:重大风险 2:较大风险 3:一般风险 4:低风险''',
  `alarm_configuration_name` longtext COMMENT '''报警配置名称''',
  `doing` bigint NOT NULL COMMENT '''是否处理 0:报警中 1:已完成''',
  `source` bigint DEFAULT '3' COMMENT '''数据来源 3离岗报警设置 4 打卡报警设置''',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=200 DEFAULT CHARSET=utf8mb3 COMMENT='视频类的告警 视频类告警记录表， 视频类报警有人员的离岗、脱岗、超员等报警';

