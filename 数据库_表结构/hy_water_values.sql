-- ----------------------------
-- Table structure for table hy_water_values
-- ----------------------------
DROP TABLE IF EXISTS `hy_water_values`;
CREATE TABLE `hy_water_values` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `d_id` longtext COMMENT '''设备id''',
  `data` longtext COMMENT '''数据''',
  `ts` double DEFAULT NULL COMMENT '''data中最后一行数据对应的服务器时间''',
  `sp` double DEFAULT NULL COMMENT '''data中每行数据的时间间隔(秒)''',
  `vm_in` double DEFAULT NULL COMMENT '''data 中所有数据的最小值''',
  `vm_ax` double DEFAULT NULL COMMENT '''data 中所有数据的最大值''',
  `status` bigint DEFAULT NULL COMMENT '''状态 1正常,-1被删除''',
  `created_at` datetime(3) NOT NULL,
  `updated_at` datetime(3) NOT NULL,
  `box_id` bigint DEFAULT NULL COMMENT '''盒子id''',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;

