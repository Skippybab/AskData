-- ----------------------------
-- Table structure for table users
-- ----------------------------
DROP TABLE IF EXISTS `users`;
CREATE TABLE `users` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` longtext NOT NULL COMMENT '''用户名''',
  `nickname` longtext NOT NULL COMMENT '''昵称''',
  `password` longtext NOT NULL COMMENT '''密码''',
  `system` bigint NOT NULL COMMENT '''是否系统用户''',
  `role_id` bigint DEFAULT NULL COMMENT '''角色''',
  `created_at` datetime(3) NOT NULL COMMENT '''创建时间''',
  `updated_at` datetime(3) NOT NULL COMMENT '''更新时间''',
  `external_id` bigint DEFAULT NULL,
  `phone_number` varchar(30) DEFAULT NULL COMMENT '''手机号码''',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=28 DEFAULT CHARSET=utf8mb3;

