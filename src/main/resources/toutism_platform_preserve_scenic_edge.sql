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
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '大景区/地点名称',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '大景区或非景区地点描述',
  `location` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '地理位置',
  `image_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '大景区/地点图片 URL',
  `opening_hours` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '开放时间',
  `recommended_visit_duration` int(0) NOT NULL DEFAULT 120 COMMENT '建议游览时长（分钟）',
  `price` decimal(10, 2) NOT NULL COMMENT '价格',
  `tags` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '标签（逗号分隔）',
  `is_area_type` int(0) NOT NULL DEFAULT 0 COMMENT '地点类型：0-景区，1-非景区地点（酒店、火车站、高速收费站等）',
  `intensity_level` tinyint(0) NOT NULL DEFAULT 2 COMMENT '体力消耗等级：1低 2中 3高 4很高',
  `crowd_level` tinyint(0) NOT NULL DEFAULT 2 COMMENT '拥挤程度：1很少 2较少 3较多 4很多',
  `family_friendly_score` decimal(4, 2) NOT NULL DEFAULT 0.00 COMMENT '亲子友好分 0-5',
  `elderly_friendly_score` decimal(4, 2) NOT NULL DEFAULT 0.00 COMMENT '老人友好分 0-5',
  `nature_score` decimal(4, 2) NOT NULL DEFAULT 0.00 COMMENT '自然风光分 0-5',
  `culture_score` decimal(4, 2) NOT NULL DEFAULT 0.00 COMMENT '人文历史分 0-5',
  `photography_score` decimal(4, 2) NOT NULL DEFAULT 0.00 COMMENT '拍照观景分 0-5',
  `leisure_score` decimal(4, 2) NOT NULL DEFAULT 0.00 COMMENT '休闲轻松分 0-5',
  `food_convenience_score` decimal(4, 2) NOT NULL DEFAULT 0.00 COMMENT '餐饮便利分 0-5',
  `restroom_convenience_score` decimal(4, 2) NOT NULL DEFAULT 0.00 COMMENT '卫生间便利分 0-5',
  `popularity_score` decimal(4, 2) NOT NULL DEFAULT 0.00 COMMENT '热门程度 0-5',
  `created_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
  `updated_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_is_area_type`(`is_area_type`) USING BTREE,
  INDEX `idx_large_area_name`(`name`) USING BTREE,
  INDEX `idx_large_area_nature`(`nature_score`) USING BTREE,
  INDEX `idx_large_area_culture`(`culture_score`) USING BTREE,
  INDEX `idx_large_area_elderly`(`elderly_friendly_score`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of large_scenic_area
-- ----------------------------
INSERT INTO `large_scenic_area`
(`id`, `name`, `description`, `location`, `image_url`, `opening_hours`, `recommended_visit_duration`, `price`, `tags`, `is_area_type`,
 `intensity_level`, `crowd_level`, `family_friendly_score`, `elderly_friendly_score`, `nature_score`, `culture_score`,
 `photography_score`, `leisure_score`, `food_convenience_score`, `restroom_convenience_score`, `popularity_score`, `created_at`, `updated_at`)
VALUES
(1, '嵩阳景区', '嵩阳书院始建于北魏太和八年（公元484年），当时是佛教活动场所，名为嵩阳寺。隋唐年间成为道教活动场所，唐高宗和武则天曾两次以这里为行宫。北宋时期成为著名的教育场所，名儒范仲淹、程颐、程颢、司马光等人都曾在此讲学，司马光的历史巨著《资治通鉴》有一部分就是在书院完成的。这些名儒的讲学活动，不仅使嵩阳书院成为北宋四大书院之首，而且也使嵩阳书院成为宋代理学的发源地之一。', '景区东北方', '/images/4f377437-6347-4790-a609-069a64170bca_2-1499592162278.jpg', '8:00-20:00', 180, 32.00, '人文风光,书院,历史,拍照', 0, 2, 2, 3.80, 4.20, 2.20, 4.90, 4.30, 3.60, 3.20, 4.00, 4.50, '2026-03-16 04:50:54', '2026-03-16 05:46:59'),
(2, '东大门', '景区公共大门，游客进入收费景区的常用入口，可换乘、问询和短暂停留。', '景区东入口', '/images/system/default-gate-east.jpg', '00:00-23:59', 20, 0.00, '公共大门,免费入口,换乘,问询', 1, 1, 2, 4.20, 4.50, 1.00, 1.00, 2.00, 4.00, 3.50, 4.60, 4.00, '2026-03-16 05:00:00', '2026-03-16 05:00:00'),
(3, '游客中心', '游客中心提供咨询、购票、休息、餐饮和卫生间等配套服务，可作为中转点。', '景区中心服务区', '/images/system/default-visitor-center.jpg', '8:00-20:00', 30, 0.00, '游客中心,服务,餐饮,卫生间,休息', 1, 1, 3, 4.50, 4.70, 1.20, 1.20, 1.80, 4.80, 4.80, 5.00, 4.40, '2026-03-16 05:10:00', '2026-03-16 05:10:00'),
(4, '书证沟景区', '树正沟为九寨沟主沟，是九寨沟秀丽风景的大门，在九寨沟呈“Y”字形分布的三条沟谷中处于下支。共有各种湖泊（海子）40余个，约占九寨沟景区全部湖泊的40%，40多个湖泊犹如40多面晶莹的宝镜，顺沟叠延五、六公里。', '景区西北方', '/images/2dfd36d9-88cd-44f7-b8e8-e56bdb81c2df_R-C.jpg', '8:00-20:00', 240, 43.00, '自然美景,湖泊,瀑布,观景,拍照', 0, 3, 2, 4.00, 3.50, 4.90, 2.00, 4.80, 4.10, 2.60, 3.20, 4.70, '2026-03-16 05:45:06', '2026-03-16 05:45:06');

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
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `cost_amount` decimal(10, 2) NOT NULL DEFAULT 0.00 COMMENT '该段路线额外花费',
  `transport_mode` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'WALK' COMMENT 'WALK/ROAD/SHUTTLE/CABLEWAY',
  `intensity_level` tinyint(0) NOT NULL DEFAULT 2 COMMENT '体力消耗等级：1低 2中 3高 4很高',
  `scenic_score` decimal(4, 2) NOT NULL DEFAULT 0.00 COMMENT '沿途风景分 0-5',
  `comfort_score` decimal(4, 2) NOT NULL DEFAULT 0.00 COMMENT '舒适度分 0-5',
  `elderly_friendly_score` decimal(4, 2) NOT NULL DEFAULT 0.00 COMMENT '老人友好分 0-5',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_scenic_area_edge_pair_mode`(`from_area_id`, `to_area_id`, `transport_mode`) USING BTREE,
  INDEX `idx_scenic_area_edge_from`(`from_area_id`) USING BTREE,
  INDEX `idx_scenic_area_edge_to`(`to_area_id`) USING BTREE,
  CONSTRAINT `fk_scenic_area_edge_from_area` FOREIGN KEY (`from_area_id`) REFERENCES `large_scenic_area` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_scenic_area_edge_to_area` FOREIGN KEY (`to_area_id`) REFERENCES `large_scenic_area` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of scenic_area_edge
-- ----------------------------
INSERT INTO `scenic_area_edge`
(`id`, `created_at`, `distance`, `duration`, `from_area_id`, `to_area_id`, `updated_at`, `description`,
 `cost_amount`, `transport_mode`, `intensity_level`, `scenic_score`, `comfort_score`, `elderly_friendly_score`)
VALUES
(1, '2026-03-16 05:53:31.000804', 2000.00, 16, 2, 1, '2026-03-16 05:53:31.000804', '公路', 0.00, 'ROAD', 2, 2.50, 3.80, 4.20),
(2, '2026-03-16 05:53:55.380334', 4000.00, 30, 2, 3, '2026-03-16 05:53:55.380334', '公路', 0.00, 'ROAD', 1, 1.20, 4.50, 4.80),
(3, '2026-03-16 05:54:18.309587', 1423.00, 19, 2, 4, '2026-03-16 05:54:18.309587', '盘山公路', 0.00, 'ROAD', 3, 4.50, 2.80, 2.60),
(4, '2026-03-16 05:55:14.054955', 3231.00, 30, 1, 4, '2026-03-16 05:55:14.054955', '公路', 0.00, 'ROAD', 2, 3.80, 3.50, 3.20),
(5, '2026-03-16 05:55:36.303502', 531.00, 10, 4, 3, '2026-03-16 05:55:36.303502', '索道', 20.00, 'CABLEWAY', 1, 4.90, 4.80, 4.50);

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
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of scenic_edge
-- ----------------------------
INSERT INTO `scenic_edge`
(`id`, `start_area_id`, `end_area_id`, `distance`, `time_cost`, `path_description`, `created_at`, `updated_at`)
VALUES
(1, 2, 1, 2000.00, 16, '公路', '2026-03-16 05:53:31', '2026-03-16 05:53:31'),
(2, 2, 3, 4000.00, 30, '公路', '2026-03-16 05:53:55', '2026-03-16 05:53:55'),
(3, 2, 4, 1423.00, 19, '盘山公路', '2026-03-16 05:54:18', '2026-03-16 05:54:18'),
(4, 1, 4, 3231.00, 30, '公路', '2026-03-16 05:55:14', '2026-03-16 05:55:14'),
(5, 4, 3, 531.00, 10, '索道', '2026-03-16 05:55:36', '2026-03-16 05:55:36');


-- ----------------------------
-- Table structure for shopping_cart
-- ----------------------------
DROP TABLE IF EXISTS `shopping_cart`;
CREATE TABLE `shopping_cart`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(0) NOT NULL,
  `item_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'PRODUCT' COMMENT 'PRODUCT表示套餐, SCENIC_AREA表示单个景区',
  `item_id` bigint(0) NULL COMMENT '对应套餐或景区的ID',
  `item_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '商品或景区名称',
  `price` decimal(10, 2) NOT NULL,
  `image_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '图片URL',
  `features` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '描述',
  `quantity` int(0) NOT NULL DEFAULT 1 COMMENT '数量',
  `created_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
  `updated_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_id`(`user_id`) USING BTREE,
  INDEX `idx_item_type`(`item_type`) USING BTREE,
  CONSTRAINT `fk_cart_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of shopping_cart
-- ----------------------------
INSERT INTO `shopping_cart` VALUES (1, 4, 'PRODUCT', 1, '1111', 75.00, '', '11', 1, '2026-03-19 11:32:14', '2026-03-19 11:32:14');

-- ----------------------------
-- Table structure for small_scenic_spot
-- ----------------------------
DROP TABLE IF EXISTS `small_scenic_spot`;
CREATE TABLE `small_scenic_spot`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `large_area_id` bigint(0) NOT NULL COMMENT '所属大景区 ID',
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '小景区/点位名称',
  `is_spot_type` int(0) NOT NULL DEFAULT 0 COMMENT '小景区类型：0-景点，1-公共设施',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '小景区/点位描述',
  `image_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '小景区/点位图片 URL',
  `visiting_duration` int(0) NULL DEFAULT 60 COMMENT '建议游览时长（分钟）',
  `tags` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '标签（逗号分隔）',
  `intensity_level` tinyint(0) NOT NULL DEFAULT 2 COMMENT '体力消耗等级：1低 2中 3高 4很高',
  `queue_level` tinyint(0) NOT NULL DEFAULT 1 COMMENT '排队程度：1低 2中 3高 4很高',
  `family_friendly_score` decimal(4, 2) NOT NULL DEFAULT 0.00 COMMENT '亲子友好分 0-5',
  `elderly_friendly_score` decimal(4, 2) NOT NULL DEFAULT 0.00 COMMENT '老人友好分 0-5',
  `nature_score` decimal(4, 2) NOT NULL DEFAULT 0.00 COMMENT '自然风光分 0-5',
  `culture_score` decimal(4, 2) NOT NULL DEFAULT 0.00 COMMENT '人文历史分 0-5',
  `photography_score` decimal(4, 2) NOT NULL DEFAULT 0.00 COMMENT '拍照观景分 0-5',
  `rest_convenience_score` decimal(4, 2) NOT NULL DEFAULT 0.00 COMMENT '休息便利分 0-5',
  `created_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
  `updated_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_large_area_id`(`large_area_id`) USING BTREE,
  INDEX `idx_is_spot_type`(`is_spot_type`) USING BTREE,
  CONSTRAINT `fk_small_spot_large_area` FOREIGN KEY (`large_area_id`) REFERENCES `large_scenic_area` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of small_scenic_spot
-- ----------------------------
INSERT INTO `small_scenic_spot`
(`id`, `large_area_id`, `name`, `is_spot_type`, `description`, `image_url`, `visiting_duration`, `tags`,
 `intensity_level`, `queue_level`, `family_friendly_score`, `elderly_friendly_score`, `nature_score`,
 `culture_score`, `photography_score`, `rest_convenience_score`, `created_at`, `updated_at`)
VALUES
(1, 1, '嵩阳书院', 0, '适合人文参观与历史学习，是嵩阳景区核心游览点。', '/images/system/songyang-academy.jpg', 90, '书院,历史,文化,拍照', 1, 2, 3.60, 4.50, 1.50, 5.00, 4.20, 3.80, '2026-03-16 06:00:00', '2026-03-16 06:00:00'),
(2, 1, '将军柏', 0, '古树景观点，步行压力小，适合轻松游览。', '/images/system/general-cypress.jpg', 30, '古树,拍照,轻松', 1, 1, 3.50, 4.60, 2.50, 3.50, 4.00, 3.60, '2026-03-16 06:01:00', '2026-03-16 06:01:00'),
(3, 2, '东大门检票口', 1, '公共大门检票与问询区域，免费通行。', '/images/system/east-gate-check.jpg', 10, '入口,免费,问询', 1, 2, 4.00, 4.50, 0.50, 0.50, 1.00, 4.20, '2026-03-16 06:02:00', '2026-03-16 06:02:00'),
(4, 3, '游客中心休息区', 1, '游客中心休息区，可就餐和短暂停留。', '/images/system/visitor-rest.jpg', 20, '休息,餐饮,卫生间', 1, 1, 4.80, 4.90, 0.50, 0.50, 1.00, 5.00, '2026-03-16 06:03:00', '2026-03-16 06:03:00'),
(5, 4, '树正瀑布', 0, '自然景观代表点，适合拍照打卡。', '/images/system/shuzheng-waterfall.jpg', 50, '瀑布,自然,拍照', 2, 2, 4.20, 3.40, 4.90, 1.20, 4.90, 3.20, '2026-03-16 06:04:00', '2026-03-16 06:04:00'),
(6, 4, '火花海', 0, '观景和摄影体验较强，自然风光突出。', '/images/system/spark-lake.jpg', 60, '湖泊,观景,摄影', 2, 2, 4.10, 3.30, 5.00, 1.00, 5.00, 3.10, '2026-03-16 06:05:00', '2026-03-16 06:05:00');

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

-- ----------------------------
-- Table structure for route_plan_record
-- ----------------------------
DROP TABLE IF EXISTS `route_plan_record`;
CREATE TABLE `route_plan_record`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(0) NULL DEFAULT NULL COMMENT '用户ID，可为空（游客模式）',
  `start_area_id` bigint(0) NULL DEFAULT NULL COMMENT '起点ID',
  `end_area_id` bigint(0) NULL DEFAULT NULL COMMENT '终点ID',
  `original_query` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户原始提问',
  `extracted_preferences_json` json NULL COMMENT 'AI从问句中抽取出的偏好权重JSON',
  `candidate_area_ids_json` json NULL COMMENT '候选地点ID列表JSON',
  `route_result_json` json NULL COMMENT '最终路线结果JSON',
  `created_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_route_plan_user_id`(`user_id`) USING BTREE,
  INDEX `idx_route_plan_start_area_id`(`start_area_id`) USING BTREE,
  INDEX `idx_route_plan_end_area_id`(`end_area_id`) USING BTREE,
  CONSTRAINT `fk_route_plan_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_route_plan_start_area` FOREIGN KEY (`start_area_id`) REFERENCES `large_scenic_area` (`id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_route_plan_end_area` FOREIGN KEY (`end_area_id`) REFERENCES `large_scenic_area` (`id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of route_plan_record
-- ----------------------------

SET FOREIGN_KEY_CHECKS = 1;
