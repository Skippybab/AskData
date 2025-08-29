-- ----------------------------
-- Table structure for table workshop_nodes
-- ----------------------------
DROP TABLE IF EXISTS `workshop_nodes`;
CREATE TABLE `workshop_nodes` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '''编号''',
  `workshop_id` bigint NOT NULL COMMENT '''车间ID''',
  `node_type` bigint NOT NULL COMMENT '''节点类型：1-1号炉熔炼中 2-2号炉熔炼中 3-1号炉放水中 4-2号炉放水中 5-铸造准备 6-铸造期间 7-铸造结束''',
  `running` bigint NOT NULL DEFAULT '1' COMMENT '''节点运行状态：1未开启 2-开启''',
  `remark` varchar(500) DEFAULT NULL COMMENT '''备注''',
  `create_user` bigint NOT NULL COMMENT '''创建人''',
  `update_user` bigint NOT NULL COMMENT '''更新人''',
  `created_at` datetime(3) NOT NULL COMMENT '''创建时间''',
  `updated_at` datetime(3) NOT NULL COMMENT '''更新时间''',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '''状态 1:启用 -1:删除''',
  `begin_time` datetime(3) DEFAULT NULL COMMENT '''开启时间''',
  `end_time` datetime(3) DEFAULT NULL COMMENT '''结束时间''',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=29 DEFAULT CHARSET=utf8mb3 COMMENT='车间节点配置表，记录了车间的各个节点状态和运行情况。节点类型包括熔炼、放水、铸造等。只记录当前情况，不记录节点历史数据。';

