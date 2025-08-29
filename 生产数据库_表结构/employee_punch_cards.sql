-- ----------------------------
-- Table structure for table employee_punch_cards
-- ----------------------------
DROP TABLE IF EXISTS `employee_punch_cards`;
CREATE TABLE `employee_punch_cards` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `workshop_id` bigint NOT NULL,
  `type` bigint NOT NULL,
  `external_id` bigint DEFAULT NULL,
  `created_at` datetime(3) NOT NULL,
  `name` longtext,
  `position` longtext,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=981 DEFAULT CHARSET=utf8mb3 COMMENT='员工打卡记录表，记录了员工在车间的打卡信息，包括打卡类型、时间等。';

