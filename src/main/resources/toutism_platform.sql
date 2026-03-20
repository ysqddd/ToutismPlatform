/*
 Navicat Premium Data Transfer

 Source Server         : localhost_3306
 Source Server Type    : MySQL
 Source Server Version : 80034
 Source Host           : localhost:3306
 Source Schema         : toutism_platform

 Target Server Type    : MySQL
 Target Server Version : 80034
 File Encoding         : 65001

 Date: 20/03/2026 10:23:17
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for employee
-- ----------------------------
DROP TABLE IF EXISTS `employee`;
CREATE TABLE `employee`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `username` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户名',
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '密码',
  `real_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `email` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '邮箱',
  `phone` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `department` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `position` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `status` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `role_id` bigint(0) NOT NULL COMMENT '关联角色ID',
  `created_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
  `updated_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_emp_username`(`username`) USING BTREE,
  UNIQUE INDEX `uk_emp_email`(`email`) USING BTREE,
  INDEX `idx_status`(`status`) USING BTREE,
  INDEX `idx_role_id`(`role_id`) USING BTREE,
  CONSTRAINT `fk_employee_role` FOREIGN KEY (`role_id`) REFERENCES `role` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of employee
-- ----------------------------
INSERT INTO `employee` VALUES (1, 'admin2', '$2a$10$Ar./7IEJYtWyZX.QpVcAkem5.NlpucW6t7RAGqcD67YUqh5m7qoA6', '系统管理员2', 'admin2@example.com', '13800138001', '管理部', '系统管理员', '正常', 1, '2026-03-14 08:12:48', '2026-03-14 08:12:48');
INSERT INTO `employee` VALUES (2, 'admin', '$2a$10$Xv8KvyuP5oLMhAle4L9Tb.FTTfM2SYm8XEoIEbQGeOhfypVJ9sNf.', 'xxx', 'ad23123@1231', '2532414132', 'xxxx', 'xx', 'ACTIVE', 1, '2026-03-15 01:39:19', '2026-03-15 01:39:19');

-- ----------------------------
-- Table structure for large_scenic_area
-- ----------------------------
DROP TABLE IF EXISTS `large_scenic_area`;
CREATE TABLE `large_scenic_area`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '大景区名称',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '大景区专属描述',
  `location` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '地理位置',
  `image_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '大景区图片 URL',
  `opening_hours` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '开放时间',
  `price` decimal(10, 2) NOT NULL COMMENT '价格',
  `tags` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '标签（逗号分隔）',
  `is_public_facility` tinyint(0) NOT NULL DEFAULT 0,
  `created_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
  `updated_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_is_public_facility`(`is_public_facility`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of large_scenic_area
-- ----------------------------
INSERT INTO `large_scenic_area` VALUES (1, '嵩阳景区', '嵩阳书院始建于北魏太和八年（公元484年），当时是佛教活动场所，名为嵩阳寺。隋唐年间成为道教活动场所，唐高宗和武则天曾两次以这里为行宫。北宋时期成为著名的教育场所，名儒范仲淹、程颐、程颢、司马光等人都曾在此讲学，司马光的历史巨著《资治通鉴》有一部分就是在书院完成的。这些名儒的讲学活动，不仅使嵩阳书院成为北宋四大书院之首，而且也使嵩阳书院成为宋代理学的发源地之一。', '景区东北方', '/images/4f377437-6347-4790-a609-069a64170bca_2-1499592162278.jpg', '8:00-20:00', 32.00, '人文风光', 0, '2026-03-16 04:50:54', '2026-03-16 05:46:59');
INSERT INTO `large_scenic_area` VALUES (4, '书证沟景区', '树正沟为九寨沟主沟，是九寨沟秀丽风景的大门，在九寨沟呈“Y”字形分布的三条沟谷中处于下支]。共有各种湖泊（海子）40余个，约占九寨沟景区全部湖泊的40%，40多个湖泊，犹如40多面晶莹的宝镜，顺沟叠延五、六公里。水光潋滟，碧波荡漾，鸟雀鸣唱，芦苇摇曳。沟内主要景点包括盆景滩、芦苇海、火花海、卧龙海、树正群海、树正瀑布、老虎海、犀牛海、双龙海及诺日朗瀑布等 [4] [8] [12]。其中，火花海在2017年地震中堤坝崩塌，后经生态修复恢复了湖泊景观；树正群海由大小19块海子组成，森林、湖泊、小瀑布相错相连', '景区西北方', '/images/2dfd36d9-88cd-44f7-b8e8-e56bdb81c2df_R-C.jpg', '8:00-20:00', 43.00, '自然美景', 0, '2026-03-16 05:45:06', '2026-03-16 05:45:06');

-- ----------------------------
-- Table structure for non_login_employee
-- ----------------------------
DROP TABLE IF EXISTS `non_login_employee`;
CREATE TABLE `non_login_employee`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `real_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `email` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `phone` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `department` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `position` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `status` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
  `updated_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_status`(`status`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of non_login_employee
-- ----------------------------

-- ----------------------------
-- Table structure for permission
-- ----------------------------
DROP TABLE IF EXISTS `permission`;
CREATE TABLE `permission`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '权能编码',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '权能名称',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '权能详细描述',
  `module` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '所属模块',
  `created_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
  `updated_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_permission_code`(`code`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 21 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of permission
-- ----------------------------
INSERT INTO `permission` VALUES (1, 'user:view', '查看用户', '查看系统用户信息', '用户管理', '2026-03-14 20:05:54', '2026-03-14 20:05:54');
INSERT INTO `permission` VALUES (2, 'user:create', '创建用户', '创建新用户', '用户管理', '2026-03-14 20:05:54', '2026-03-14 20:05:54');
INSERT INTO `permission` VALUES (3, 'user:update', '修改用户', '修改用户信息', '用户管理', '2026-03-14 20:05:54', '2026-03-14 20:05:54');
INSERT INTO `permission` VALUES (4, 'user:delete', '删除用户', '删除用户', '用户管理', '2026-03-14 20:05:54', '2026-03-14 20:05:54');
INSERT INTO `permission` VALUES (5, 'employee:view', '查看员工', '查看员工信息', '员工管理', '2026-03-14 20:05:54', '2026-03-14 20:05:54');
INSERT INTO `permission` VALUES (6, 'employee:create', '创建员工', '创建新员工', '员工管理', '2026-03-14 20:05:54', '2026-03-14 20:05:54');
INSERT INTO `permission` VALUES (7, 'employee:update', '修改员工', '修改员工信息', '员工管理', '2026-03-14 20:05:54', '2026-03-14 20:05:54');
INSERT INTO `permission` VALUES (8, 'employee:delete', '删除员工', '删除员工', '员工管理', '2026-03-14 20:05:54', '2026-03-14 20:05:54');
INSERT INTO `permission` VALUES (9, 'product:view', '查看商品', '查看商品信息', '商品管理', '2026-03-14 20:05:54', '2026-03-14 20:05:54');
INSERT INTO `permission` VALUES (10, 'product:create', '创建商品', '创建新商品', '商品管理', '2026-03-14 20:05:54', '2026-03-14 20:05:54');
INSERT INTO `permission` VALUES (11, 'product:update', '修改商品', '修改商品信息', '商品管理', '2026-03-14 20:05:54', '2026-03-14 20:05:54');
INSERT INTO `permission` VALUES (12, 'product:delete', '删除商品', '删除商品', '商品管理', '2026-03-14 20:05:54', '2026-03-14 20:05:54');
INSERT INTO `permission` VALUES (13, 'scenic:view', '查看景区', '查看景区信息', '景区管理', '2026-03-14 20:05:54', '2026-03-14 20:05:54');
INSERT INTO `permission` VALUES (14, 'scenic:create', '创建景区', '创建新景区', '景区管理', '2026-03-14 20:05:54', '2026-03-14 20:05:54');
INSERT INTO `permission` VALUES (15, 'scenic:update', '修改景区', '修改景区信息', '景区管理', '2026-03-14 20:05:54', '2026-03-14 20:05:54');
INSERT INTO `permission` VALUES (16, 'scenic:delete', '删除景区', '删除景区', '景区管理', '2026-03-14 20:05:54', '2026-03-14 20:05:54');
INSERT INTO `permission` VALUES (17, 'permission:view', '查看权限', '查看权限信息', '权限管理', '2026-03-14 20:05:54', '2026-03-14 20:05:54');
INSERT INTO `permission` VALUES (18, 'permission:create', '创建权限', '创建新权限', '权限管理', '2026-03-14 20:05:54', '2026-03-14 20:05:54');
INSERT INTO `permission` VALUES (19, 'permission:update', '修改权限', '修改权限信息', '权限管理', '2026-03-14 20:05:54', '2026-03-14 20:05:54');
INSERT INTO `permission` VALUES (20, 'permission:delete', '删除权限', '删除权限', '权限管理', '2026-03-14 20:05:54', '2026-03-14 20:05:54');

-- ----------------------------
-- Table structure for permissions
-- ----------------------------
DROP TABLE IF EXISTS `permissions`;
CREATE TABLE `permissions`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `level` int(0) NOT NULL,
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `UKn5aybip77714anlj8quyebgyk`(`level`) USING BTREE,
  UNIQUE INDEX `UKpnvtwliis6p05pn6i3ndjrqt2`(`name`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of permissions
-- ----------------------------

-- ----------------------------
-- Table structure for product
-- ----------------------------
DROP TABLE IF EXISTS `product`;
CREATE TABLE `product`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '商品名称',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '商品描述',
  `price` decimal(10, 2) NOT NULL COMMENT '商品价格',
  `image_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '商品图片 URL',
  `status` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
  `updated_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_status`(`status`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of product
-- ----------------------------
INSERT INTO `product` VALUES (1, '1111', '11', 75.00, '', 'ON_SALE', '2026-03-19 08:14:30', '2026-03-19 09:14:53');

-- ----------------------------
-- Table structure for product_large_scenic_area
-- ----------------------------
DROP TABLE IF EXISTS `product_large_scenic_area`;
CREATE TABLE `product_large_scenic_area`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `product_id` bigint(0) NOT NULL COMMENT '商品 ID',
  `large_scenic_area_id` bigint(0) NOT NULL COMMENT '大景区 ID',
  `created_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_product_area`(`product_id`, `large_scenic_area_id`) USING BTREE,
  INDEX `idx_product_id`(`product_id`) USING BTREE,
  INDEX `idx_large_scenic_area_id`(`large_scenic_area_id`) USING BTREE,
  CONSTRAINT `fk_product_area_large_area` FOREIGN KEY (`large_scenic_area_id`) REFERENCES `large_scenic_area` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_product_area_product` FOREIGN KEY (`product_id`) REFERENCES `product` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of product_large_scenic_area
-- ----------------------------
INSERT INTO `product_large_scenic_area` VALUES (1, 1, 1, '2026-03-19 17:14:52');
INSERT INTO `product_large_scenic_area` VALUES (2, 1, 4, '2026-03-19 17:14:52');

-- ----------------------------
-- Table structure for role
-- ----------------------------
DROP TABLE IF EXISTS `role`;
CREATE TABLE `role`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '职位名称',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '职位描述',
  `created_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
  `updated_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_role_name`(`name`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of role
-- ----------------------------
INSERT INTO `role` VALUES (1, '系统管理员', '负责系统整体管理，拥有所有权限', '2026-03-14 10:46:12', '2026-03-14 10:46:12');
INSERT INTO `role` VALUES (2, '用户管理员', '负责用户管理相关操作', '2026-03-14 10:46:12', '2026-03-14 10:46:12');
INSERT INTO `role` VALUES (3, '员工管理员', '负责员工管理相关操作', '2026-03-14 10:46:12', '2026-03-14 10:46:12');
INSERT INTO `role` VALUES (4, '商品管理员', '负责商品管理相关操作', '2026-03-14 10:46:12', '2026-03-14 10:46:12');
INSERT INTO `role` VALUES (5, '景区管理员', '负责景区管理相关操作', '2026-03-14 10:46:12', '2026-03-14 10:46:12');

-- ----------------------------
-- Table structure for role_permission
-- ----------------------------
DROP TABLE IF EXISTS `role_permission`;
CREATE TABLE `role_permission`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `role_id` bigint(0) NOT NULL COMMENT '角色ID',
  `permission_id` bigint(0) NOT NULL COMMENT '权限ID',
  `created_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_role_permission`(`role_id`, `permission_id`) USING BTREE,
  INDEX `fk_role_permission_permission`(`permission_id`) USING BTREE,
  CONSTRAINT `fk_role_permission_permission` FOREIGN KEY (`permission_id`) REFERENCES `permission` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_role_permission_role` FOREIGN KEY (`role_id`) REFERENCES `role` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB AUTO_INCREMENT = 37 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of role_permission
-- ----------------------------
INSERT INTO `role_permission` VALUES (1, 1, 1, '2026-03-14 20:05:54');
INSERT INTO `role_permission` VALUES (2, 1, 2, '2026-03-14 20:05:54');
INSERT INTO `role_permission` VALUES (3, 1, 3, '2026-03-14 20:05:54');
INSERT INTO `role_permission` VALUES (4, 1, 4, '2026-03-14 20:05:54');
INSERT INTO `role_permission` VALUES (5, 1, 5, '2026-03-14 20:05:54');
INSERT INTO `role_permission` VALUES (6, 1, 6, '2026-03-14 20:05:54');
INSERT INTO `role_permission` VALUES (7, 1, 7, '2026-03-14 20:05:54');
INSERT INTO `role_permission` VALUES (8, 1, 8, '2026-03-14 20:05:54');
INSERT INTO `role_permission` VALUES (9, 1, 9, '2026-03-14 20:05:54');
INSERT INTO `role_permission` VALUES (10, 1, 10, '2026-03-14 20:05:54');
INSERT INTO `role_permission` VALUES (11, 1, 11, '2026-03-14 20:05:54');
INSERT INTO `role_permission` VALUES (12, 1, 12, '2026-03-14 20:05:54');
INSERT INTO `role_permission` VALUES (13, 1, 13, '2026-03-14 20:05:54');
INSERT INTO `role_permission` VALUES (14, 1, 14, '2026-03-14 20:05:54');
INSERT INTO `role_permission` VALUES (15, 1, 15, '2026-03-14 20:05:54');
INSERT INTO `role_permission` VALUES (16, 1, 16, '2026-03-14 20:05:54');
INSERT INTO `role_permission` VALUES (17, 1, 17, '2026-03-14 20:05:54');
INSERT INTO `role_permission` VALUES (18, 1, 18, '2026-03-14 20:05:54');
INSERT INTO `role_permission` VALUES (19, 1, 19, '2026-03-14 20:05:55');
INSERT INTO `role_permission` VALUES (20, 1, 20, '2026-03-14 20:05:55');
INSERT INTO `role_permission` VALUES (21, 2, 1, '2026-03-14 20:05:55');
INSERT INTO `role_permission` VALUES (22, 2, 2, '2026-03-14 20:05:55');
INSERT INTO `role_permission` VALUES (23, 2, 3, '2026-03-14 20:05:55');
INSERT INTO `role_permission` VALUES (24, 2, 4, '2026-03-14 20:05:55');
INSERT INTO `role_permission` VALUES (25, 3, 5, '2026-03-14 20:05:55');
INSERT INTO `role_permission` VALUES (26, 3, 6, '2026-03-14 20:05:55');
INSERT INTO `role_permission` VALUES (27, 3, 7, '2026-03-14 20:05:55');
INSERT INTO `role_permission` VALUES (28, 3, 8, '2026-03-14 20:05:55');
INSERT INTO `role_permission` VALUES (29, 4, 9, '2026-03-14 20:05:55');
INSERT INTO `role_permission` VALUES (30, 4, 10, '2026-03-14 20:05:55');
INSERT INTO `role_permission` VALUES (31, 4, 11, '2026-03-14 20:05:55');
INSERT INTO `role_permission` VALUES (32, 4, 12, '2026-03-14 20:05:55');
INSERT INTO `role_permission` VALUES (33, 5, 13, '2026-03-14 20:05:55');
INSERT INTO `role_permission` VALUES (34, 5, 14, '2026-03-14 20:05:55');
INSERT INTO `role_permission` VALUES (35, 5, 15, '2026-03-14 20:05:55');
INSERT INTO `role_permission` VALUES (36, 5, 16, '2026-03-14 20:05:55');

-- ----------------------------
-- Table structure for scenic_area_edge
-- ----------------------------
DROP TABLE IF EXISTS `scenic_area_edge`;
CREATE TABLE `scenic_area_edge`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `distance` decimal(10, 2) NOT NULL,
  `duration` int(0) NOT NULL,
  `from_area_id` bigint(0) NOT NULL,
  `to_area_id` bigint(0) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of scenic_area_edge
-- ----------------------------
INSERT INTO `scenic_area_edge` VALUES (1, '2026-03-16 05:53:31.000804', 2000.00, 16, 2, 1, '2026-03-16 05:53:31.000804', '公路');
INSERT INTO `scenic_area_edge` VALUES (2, '2026-03-16 05:53:55.380334', 4000.00, 30, 2, 3, '2026-03-16 05:53:55.380334', '公路');
INSERT INTO `scenic_area_edge` VALUES (3, '2026-03-16 05:54:18.309587', 1423.00, 19, 2, 4, '2026-03-16 05:54:18.309587', '盘山公路');
INSERT INTO `scenic_area_edge` VALUES (4, '2026-03-16 05:55:14.054955', 3231.00, 30, 1, 4, '2026-03-16 05:55:14.054955', '公路');
INSERT INTO `scenic_area_edge` VALUES (5, '2026-03-16 05:55:36.303502', 531.00, 10, 4, 3, '2026-03-16 05:55:36.303502', '索道');

-- ----------------------------
-- Table structure for scenic_edge
-- ----------------------------
DROP TABLE IF EXISTS `scenic_edge`;
CREATE TABLE `scenic_edge`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `start_area_id` bigint(0) NOT NULL COMMENT '起始大景区 ID',
  `end_area_id` bigint(0) NOT NULL COMMENT '结束大景区 ID',
  `distance` double NOT NULL COMMENT '距离（米）',
  `time_cost` int(0) NOT NULL COMMENT '时间成本（分钟）',
  `path_description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '路径描述',
  `created_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
  `updated_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_start_area_id`(`start_area_id`) USING BTREE,
  INDEX `idx_end_area_id`(`end_area_id`) USING BTREE,
  CONSTRAINT `fk_edge_end_area` FOREIGN KEY (`end_area_id`) REFERENCES `large_scenic_area` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_edge_start_area` FOREIGN KEY (`start_area_id`) REFERENCES `large_scenic_area` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of scenic_edge
-- ----------------------------

-- ----------------------------
-- Table structure for shopping_cart
-- ----------------------------
DROP TABLE IF EXISTS `shopping_cart`;
CREATE TABLE `shopping_cart`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(0) NOT NULL,
  `product_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `price` decimal(10, 2) NOT NULL,
  `features` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
  `created_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
  `updated_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_id`(`user_id`) USING BTREE,
  CONSTRAINT `fk_cart_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of shopping_cart
-- ----------------------------
INSERT INTO `shopping_cart` VALUES (1, 4, '1111', 75.00, '11', '2026-03-19 11:32:14', '2026-03-19 11:32:14');

-- ----------------------------
-- Table structure for small_scenic_spot
-- ----------------------------
DROP TABLE IF EXISTS `small_scenic_spot`;
CREATE TABLE `small_scenic_spot`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `large_area_id` bigint(0) NOT NULL COMMENT '所属大景区 ID',
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '小景点名称',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '小景点描述',
  `image_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '小景点图片 URL',
  `visiting_duration` int(0) NULL DEFAULT 60 COMMENT '建议游览时长（分钟）',
  `tags` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '标签（逗号分隔）',
  `created_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
  `updated_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_large_area_id`(`large_area_id`) USING BTREE,
  CONSTRAINT `fk_small_spot_large_area` FOREIGN KEY (`large_area_id`) REFERENCES `large_scenic_area` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of small_scenic_spot
-- ----------------------------

-- ----------------------------
-- Table structure for users
-- ----------------------------
DROP TABLE IF EXISTS `users`;
CREATE TABLE `users`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `username` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `email` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `role` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'USER',
  `is_admin` tinyint(0) NOT NULL DEFAULT 0 COMMENT '0: 普通用户, 1: 超级管理员, 2: 景区管理员, 3: 订单管理员',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_username`(`username`) USING BTREE,
  UNIQUE INDEX `uk_email`(`email`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of users
-- ----------------------------
INSERT INTO `users` VALUES (1, 'admin', '$2a$10$4e3C03374E1D4F8C8B7A2B3C4D5E6F7G8H9I0J1K2L3M4N5O6P7Q8R9S0T', 'admin@example.com', 'ADMIN', 1);
INSERT INTO `users` VALUES (3, 'zhangsan', '$2a$10$LvRbbJ64AmnN6PbiudVpTeZbdbyVvfj2vH8vottTVP2Kz1/.kBekK', '123132412946@qq.com', 'USER', 0);
INSERT INTO `users` VALUES (4, 'shi', '$2a$10$IBx7GfzuRxT25cD/qKxpoehFnlj0/KGQNeevGs/hnYOMT4i/ckpmy', '12312312@qq.com', 'USER', 0);

SET FOREIGN_KEY_CHECKS = 1;
