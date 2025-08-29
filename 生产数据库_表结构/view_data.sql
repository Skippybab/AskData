-- ----------------------------
-- Table structure for table view_data
-- ----------------------------
DROP TABLE IF EXISTS `view_data`;
CREATE TABLE `view_data` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `workshop_id` bigint NOT NULL COMMENT '''车间id''',
  `people_num` bigint NOT NULL COMMENT '''人数''',
  `created_at` datetime(3) NOT NULL COMMENT '''创建时间''',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=26471 DEFAULT CHARSET=utf8mb3;

