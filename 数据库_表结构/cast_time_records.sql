-- ----------------------------
-- Table structure for table cast_time_records
-- ----------------------------
DROP TABLE IF EXISTS `cast_time_records`;
CREATE TABLE `cast_time_records` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '''编号''',
  `workshop_id` bigint NOT NULL COMMENT '''车间id''',
  `workshop_name` longtext NOT NULL COMMENT '''车间名称''',
  `cast_node_type` bigint NOT NULL DEFAULT '5' COMMENT '''铸造节点 5铸造准备 6铸造期间 7铸造结束''',
  `duration` bigint NOT NULL COMMENT '''持续时长min''',
  `begin_time` datetime(3) NOT NULL COMMENT '''开始时间''',
  `end_time` datetime(3) NOT NULL COMMENT '''结束时间''',
  `aluminum_leakage_num` bigint NOT NULL DEFAULT '0' COMMENT '''漏铝报警次数''',
  `off_post_num` bigint NOT NULL DEFAULT '0' COMMENT '''离岗报警次数''',
  `pre_alarm_num` bigint NOT NULL DEFAULT '0' COMMENT '''预报警次数''',
  `remark` varchar(500) DEFAULT NULL COMMENT '''备注''',
  `create_user` bigint NOT NULL COMMENT '''创建人''',
  `update_user` bigint NOT NULL COMMENT '''更新人''',
  `created_at` datetime(3) NOT NULL COMMENT '''创建时间''',
  `updated_at` datetime(3) NOT NULL COMMENT '''更新时间''',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '''状态 1:启用 -1:删除''',
  `running` bigint NOT NULL DEFAULT '1' COMMENT '''节点运行状态：1未开启 2-开启''',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=671 DEFAULT CHARSET=utf8mb3 COMMENT='铸造时间记录表，记录了铸造过程中各个节点的时间信息，包括铸造准备、铸造期间和铸造结束等。 也记录了铸造过程中发生的报警信息，如漏铝报警、离岗报警和预报警等。通过这个表可以查，某天的铸造时间记录，和铸造过程中发生的报警信息。也可以对比不同日期的报警趋势';

