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

 Date: 23/03/2026 12:51:42
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
INSERT INTO `employee` VALUES (1, 'admin2', '$2a$10$Ar./7IEJYtWyZX.QpVcAkem5.NlpucW6t7RAGqcD67YUqh5m7qoA6', '系统管理员2', 'admin2@example.com', '13800138001', '管理部', '系统管理员', 'ACTIVE', 1, '2026-03-14 08:12:48', '2026-03-22 16:52:53');
INSERT INTO `employee` VALUES (2, 'admin', '$2a$10$FXVg36gPT90LMBXZl/PSDOOyvJptcnWq/xw3GgG9JyVBwYdrweLwG', 'xxx', 'ad23123@1231', '2532414132', 'xxxx', 'xx', 'ACTIVE', 1, '2026-03-15 01:39:19', '2026-03-22 16:51:31');

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
  `intensity_level` int(0) NOT NULL,
  `crowd_level` int(0) NOT NULL,
  `family_friendly_score` decimal(4, 2) NOT NULL COMMENT '亲子友好分 0-5',
  `elderly_friendly_score` decimal(4, 2) NOT NULL COMMENT '老人友好分 0-5',
  `nature_score` decimal(4, 2) NOT NULL COMMENT '自然风光分 0-5',
  `culture_score` decimal(4, 2) NOT NULL COMMENT '人文历史分 0-5',
  `photography_score` decimal(4, 2) NOT NULL COMMENT '拍照观景分 0-5',
  `leisure_score` decimal(4, 2) NOT NULL COMMENT '休闲轻松分 0-5',
  `food_convenience_score` decimal(4, 2) NOT NULL COMMENT '餐饮便利分 0-5',
  `restroom_convenience_score` decimal(4, 2) NOT NULL COMMENT '卫生间便利分 0-5',
  `popularity_score` decimal(4, 2) NOT NULL COMMENT '热门程度 0-5',
  `created_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
  `updated_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_is_area_type`(`is_area_type`) USING BTREE,
  INDEX `idx_large_area_name`(`name`) USING BTREE,
  INDEX `idx_large_area_nature`(`nature_score`) USING BTREE,
  INDEX `idx_large_area_culture`(`culture_score`) USING BTREE,
  INDEX `idx_large_area_elderly`(`elderly_friendly_score`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 9 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of large_scenic_area
-- ----------------------------
INSERT INTO `large_scenic_area` VALUES (1, '开封站', '开封市主要铁路到达节点，可作为游客进入市区游览的常见起点。', '河南省开封市禹王台区', '/images/system/kaifeng-railway-station.jpg', '00:00-23:59', 30, 0.00, '交通枢纽,到达点,换乘', 1, 1, 3, 3.50, 4.20, 0.50, 1.20, 2.50, 3.50, 4.20, 4.20, 4.00, '2026-03-23 13:20:00', '2026-03-23 13:20:00');
INSERT INTO `large_scenic_area` VALUES (2, '清明上河园', '以《清明上河图》为蓝本打造的大型宋文化主题景区，适合沉浸式游览、看演出和夜游。', '河南省开封市龙亭区龙亭西路5号', '/images/system/qingming-shangheyuan.jpg', '09:00-22:00', 240, 120.00, '宋文化,夜游,演艺,沉浸式,拍照', 0, 3, 4, 4.60, 3.90, 2.80, 4.90, 4.80, 4.10, 4.30, 4.20, 4.90, '2026-03-23 13:20:00', '2026-03-23 13:20:00');
INSERT INTO `large_scenic_area` VALUES (3, '龙亭景区', '开封代表性历史景区之一，适合观景、赏花和了解古都格局。', '河南省开封市龙亭区中山路北段', '/images/system/longting.jpg', '08:00-18:30', 120, 60.00, '历史,古都,观景,赏花,拍照', 0, 2, 3, 4.20, 4.20, 3.20, 4.60, 4.70, 4.00, 3.70, 3.80, 4.60, '2026-03-23 13:20:00', '2026-03-23 13:20:00');
INSERT INTO `large_scenic_area` VALUES (4, '开封府', '以包公文化和府衙文化展示见长，适合历史体验与演艺观赏。', '河南省开封市鼓楼区包公东湖北岸', '/images/system/kaifengfu.jpg', '07:30-18:30', 100, 65.00, '包公文化,府衙,历史,演艺', 0, 2, 3, 4.00, 4.10, 1.20, 4.90, 4.10, 3.80, 3.90, 3.70, 4.50, '2026-03-23 13:20:00', '2026-03-23 13:20:00');
INSERT INTO `large_scenic_area` VALUES (5, '大相国寺', '开封重要佛教文化景点，节奏相对平缓，适合人文参观与静态游览。', '河南省开封市鼓楼区自由路西段36号', '/images/system/daxiangguosi.jpg', '08:00-17:30', 80, 40.00, '寺庙,人文,静态游览,老人友好', 0, 1, 2, 3.80, 4.80, 1.00, 4.80, 3.60, 4.50, 3.20, 3.50, 4.20, '2026-03-23 13:20:00', '2026-03-23 13:20:00');
INSERT INTO `large_scenic_area` VALUES (6, '铁塔景区', '以千年铁塔和园林环境闻名，适合散步、拍照和轻松游览。', '河南省开封市顺河回族区北门大街210号', '/images/system/iron-pagoda.jpg', '08:00-18:00', 90, 40.00, '古塔,园林,散步,拍照', 0, 1, 2, 4.10, 4.70, 2.40, 4.30, 4.60, 4.50, 3.40, 3.90, 4.40, '2026-03-23 13:20:00', '2026-03-23 13:20:00');
INSERT INTO `large_scenic_area` VALUES (7, '万岁山武侠城', '以武侠演艺和互动体验见长，适合看表演、亲子出游和沉浸式体验。', '河南省开封市龙亭区东京大道中段', '/images/system/wansuishan.jpg', '08:00-18:00', 220, 100.00, '武侠,演艺,亲子,沉浸式,夜场', 0, 3, 4, 4.70, 3.60, 2.30, 4.20, 4.90, 4.00, 4.10, 4.00, 4.80, '2026-03-23 13:20:00', '2026-03-23 13:20:00');
INSERT INTO `large_scenic_area` VALUES (8, '鼓楼夜市', '开封热门餐饮与夜间休闲聚集点，适合作为晚间收尾地点。', '河南省开封市鼓楼区鼓楼广场周边', '/images/system/gulou-night-market.jpg', '18:00-23:30', 90, 0.00, '夜市,美食,休闲,晚间', 1, 1, 4, 4.50, 4.00, 0.80, 2.50, 4.20, 4.80, 4.90, 3.60, 4.70, '2026-03-23 13:20:00', '2026-03-23 13:20:00');

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
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

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
) ENGINE = InnoDB AUTO_INCREMENT = 14 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

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
  `distance` decimal(10, 2) NOT NULL,
  `duration` int(0) NOT NULL,
  `from_area_id` bigint(0) NOT NULL,
  `to_area_id` bigint(0) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `cost_amount` decimal(10, 2) NOT NULL COMMENT '该段路线额外花费',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_scenic_area_edge_pair_mode`(`from_area_id`, `to_area_id`) USING BTREE,
  INDEX `idx_scenic_area_edge_from`(`from_area_id`) USING BTREE,
  INDEX `idx_scenic_area_edge_to`(`to_area_id`) USING BTREE,
  CONSTRAINT `fk_scenic_area_edge_from_area` FOREIGN KEY (`from_area_id`) REFERENCES `large_scenic_area` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_scenic_area_edge_to_area` FOREIGN KEY (`to_area_id`) REFERENCES `large_scenic_area` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB AUTO_INCREMENT = 14 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of scenic_area_edge
-- ----------------------------
INSERT INTO `scenic_area_edge` VALUES (1, 6500.00, 25, 1, 2, '2026-03-23 13:25:00.000000', '2026-03-23 13:25:00.000000', '开封站前往清明上河园，适合公交或打车', 12.00);
INSERT INTO `scenic_area_edge` VALUES (2, 7100.00, 28, 1, 3, '2026-03-23 13:25:00.000000', '2026-03-23 13:25:00.000000', '开封站前往龙亭景区，适合公交或打车', 12.00);
INSERT INTO `scenic_area_edge` VALUES (3, 4800.00, 18, 1, 4, '2026-03-23 13:25:00.000000', '2026-03-23 13:25:00.000000', '开封站前往开封府，适合作为首站', 10.00);
INSERT INTO `scenic_area_edge` VALUES (4, 1200.00, 8, 2, 3, '2026-03-23 13:25:00.000000', '2026-03-23 13:25:00.000000', '清明上河园与龙亭景区相邻，可步行或短途接驳', 0.00);
INSERT INTO `scenic_area_edge` VALUES (5, 2800.00, 12, 2, 4, '2026-03-23 13:25:00.000000', '2026-03-23 13:25:00.000000', '清明上河园到开封府，路程较顺', 6.00);
INSERT INTO `scenic_area_edge` VALUES (6, 2500.00, 10, 3, 7, '2026-03-23 13:25:00.000000', '2026-03-23 13:25:00.000000', '龙亭景区到万岁山武侠城，适合连线游玩', 5.00);
INSERT INTO `scenic_area_edge` VALUES (7, 3400.00, 15, 3, 4, '2026-03-23 13:25:00.000000', '2026-03-23 13:25:00.000000', '龙亭景区到开封府', 6.00);
INSERT INTO `scenic_area_edge` VALUES (8, 1800.00, 8, 4, 5, '2026-03-23 13:25:00.000000', '2026-03-23 13:25:00.000000', '开封府到大相国寺，适合半日文化线', 0.00);
INSERT INTO `scenic_area_edge` VALUES (9, 3500.00, 12, 5, 6, '2026-03-23 13:25:00.000000', '2026-03-23 13:25:00.000000', '大相国寺到铁塔景区，适合继续东线游览', 6.00);
INSERT INTO `scenic_area_edge` VALUES (10, 4200.00, 15, 4, 6, '2026-03-23 13:25:00.000000', '2026-03-23 13:25:00.000000', '开封府到铁塔景区', 8.00);
INSERT INTO `scenic_area_edge` VALUES (11, 1500.00, 8, 4, 8, '2026-03-23 13:25:00.000000', '2026-03-23 13:25:00.000000', '开封府到鼓楼夜市，适合作为晚间收尾', 0.00);
INSERT INTO `scenic_area_edge` VALUES (12, 2200.00, 10, 5, 8, '2026-03-23 13:25:00.000000', '2026-03-23 13:25:00.000000', '大相国寺到鼓楼夜市，步行或短途接驳均可', 0.00);
INSERT INTO `scenic_area_edge` VALUES (13, 3900.00, 15, 7, 2, '2026-03-23 13:25:00.000000', '2026-03-23 13:25:00.000000', '万岁山武侠城返回清明上河园，适合晚场联动', 8.00);

-- ----------------------------
-- Table structure for scenic_edge
-- ----------------------------
DROP TABLE IF EXISTS `scenic_edge`;
CREATE TABLE `scenic_edge`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `large_area_id` bigint(0) NOT NULL COMMENT '所属大景区 ID',
  `start_spot_id` bigint(0) NOT NULL COMMENT '起始小景点 ID',
  `end_spot_id` bigint(0) NOT NULL COMMENT '结束小景点 ID',
  `distance` double NOT NULL COMMENT '距离（米）',
  `time_cost` int(0) NOT NULL COMMENT '时间成本（分钟）',
  `path_description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '路径描述',
  `transport_mode` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'WALK' COMMENT '交通方式: WALK/SHUTTLE/CABLEWAY',
  `intensity_level` int(0) NOT NULL DEFAULT 2 COMMENT '强度等级 1-5',
  `scenic_score` decimal(4, 2) NOT NULL COMMENT '沿途风景分 0-5',
  `comfort_score` decimal(4, 2) NOT NULL COMMENT '舒适度分 0-5',
  `elderly_friendly_score` decimal(4, 2) NOT NULL COMMENT '老人友好分 0-5',
  `created_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
  `updated_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_spot_pair_mode`(`start_spot_id`, `end_spot_id`, `transport_mode`) USING BTREE,
  INDEX `idx_large_area_id`(`large_area_id`) USING BTREE,
  INDEX `idx_start_spot_id`(`start_spot_id`) USING BTREE,
  INDEX `idx_end_spot_id`(`end_spot_id`) USING BTREE,
  CONSTRAINT `fk_edge_end_spot` FOREIGN KEY (`end_spot_id`) REFERENCES `small_scenic_spot` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_edge_large_area` FOREIGN KEY (`large_area_id`) REFERENCES `large_scenic_area` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_edge_start_spot` FOREIGN KEY (`start_spot_id`) REFERENCES `small_scenic_spot` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB AUTO_INCREMENT = 9 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of scenic_edge
-- ----------------------------
INSERT INTO `scenic_edge` VALUES (1, 2, 1, 2, 320.00, 6, '从迎宾门步行至虹桥主景区', 'WALK', 1, 4.20, 4.10, 4.00, '2026-03-23 13:30:00', '2026-03-23 13:30:00');
INSERT INTO `scenic_edge` VALUES (2, 2, 2, 3, 450.00, 8, '从虹桥前往东京码头观景区', 'WALK', 1, 4.60, 4.00, 3.80, '2026-03-23 13:30:00', '2026-03-23 13:30:00');
INSERT INTO `scenic_edge` VALUES (3, 3, 4, 5, 500.00, 10, '从龙亭大殿步行至杨家湖观景带', 'WALK', 1, 4.50, 4.20, 4.20, '2026-03-23 13:30:00', '2026-03-23 13:30:00');
INSERT INTO `scenic_edge` VALUES (4, 4, 6, 7, 280.00, 5, '从府衙正厅步行至包公断案演艺区', 'WALK', 1, 4.20, 4.00, 4.00, '2026-03-23 13:30:00', '2026-03-23 13:30:00');
INSERT INTO `scenic_edge` VALUES (5, 5, 8, 9, 260.00, 5, '从大雄宝殿步行至千手千眼观音殿', 'WALK', 1, 4.10, 4.30, 4.60, '2026-03-23 13:30:00', '2026-03-23 13:30:00');
INSERT INTO `scenic_edge` VALUES (6, 6, 10, 11, 380.00, 7, '从铁塔主景点前往塔影步道', 'WALK', 1, 4.40, 4.50, 4.50, '2026-03-23 13:30:00', '2026-03-23 13:30:00');
INSERT INTO `scenic_edge` VALUES (7, 7, 12, 13, 650.00, 12, '从武侠城主入口前往三打祝家庄演艺场', 'WALK', 2, 4.80, 3.80, 3.50, '2026-03-23 13:30:00', '2026-03-23 13:30:00');
INSERT INTO `scenic_edge` VALUES (8, 7, 13, 14, 420.00, 8, '从演艺场步行至打铁花观演区', 'WALK', 1, 4.90, 3.90, 3.40, '2026-03-23 13:30:00', '2026-03-23 13:30:00');

-- ----------------------------
-- Table structure for shopping_cart
-- ----------------------------
DROP TABLE IF EXISTS `shopping_cart`;
CREATE TABLE `shopping_cart`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(0) NOT NULL,
  `item_type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `item_id` bigint(0) NULL DEFAULT NULL COMMENT '对应套餐或景区的ID',
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
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of shopping_cart
-- ----------------------------
INSERT INTO `shopping_cart` VALUES (1, 4, 'PRODUCT', 1, '1111', 75.00, '', '11', 1, '2026-03-19 11:32:14', '2026-03-22 16:27:27');

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
  `intensity_level` int(0) NOT NULL,
  `queue_level` int(0) NOT NULL,
  `family_friendly_score` decimal(4, 2) NOT NULL COMMENT '亲子友好分 0-5',
  `elderly_friendly_score` decimal(4, 2) NOT NULL COMMENT '老人友好分 0-5',
  `nature_score` decimal(4, 2) NOT NULL COMMENT '自然风光分 0-5',
  `culture_score` decimal(4, 2) NOT NULL COMMENT '人文历史分 0-5',
  `photography_score` decimal(4, 2) NOT NULL COMMENT '拍照观景分 0-5',
  `rest_convenience_score` decimal(4, 2) NOT NULL COMMENT '休息便利分 0-5',
  `created_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
  `updated_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_large_area_id`(`large_area_id`) USING BTREE,
  INDEX `idx_is_spot_type`(`is_spot_type`) USING BTREE,
  CONSTRAINT `fk_small_spot_large_area` FOREIGN KEY (`large_area_id`) REFERENCES `large_scenic_area` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB AUTO_INCREMENT = 15 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of small_scenic_spot
-- ----------------------------
INSERT INTO `small_scenic_spot` VALUES (1, 2, '迎宾门', 1, '清明上河园主要入园节点，适合作为游览起点。', '/images/system/qingming-gate.jpg', 20, '入口,服务,导览', 1, 2, 4.20, 4.30, 0.80, 2.00, 3.20, 4.60, '2026-03-23 13:35:00', '2026-03-23 13:35:00');
INSERT INTO `small_scenic_spot` VALUES (2, 2, '虹桥', 0, '园内代表性地标，适合拍照和观察宋风街景。', '/images/system/qingming-hongqiao.jpg', 40, '地标,拍照,宋风', 1, 3, 4.30, 3.90, 1.20, 4.20, 4.90, 3.80, '2026-03-23 13:35:00', '2026-03-23 13:35:00');
INSERT INTO `small_scenic_spot` VALUES (3, 2, '东京码头', 0, '适合观景和感受水岸氛围，也是夜游较受欢迎的点位。', '/images/system/qingming-dock.jpg', 45, '观景,夜游,水岸', 1, 2, 4.20, 3.80, 1.50, 4.00, 4.70, 3.90, '2026-03-23 13:35:00', '2026-03-23 13:35:00');
INSERT INTO `small_scenic_spot` VALUES (4, 3, '龙亭大殿', 0, '龙亭景区核心建筑，适合观景和了解古都历史。', '/images/system/longting-hall.jpg', 35, '历史,核心景点,观景', 1, 2, 4.00, 4.20, 1.00, 4.70, 4.60, 3.80, '2026-03-23 13:35:00', '2026-03-23 13:35:00');
INSERT INTO `small_scenic_spot` VALUES (5, 3, '杨家湖观景带', 0, '适合散步、拍照和轻松游览。', '/images/system/yangjiahu.jpg', 40, '湖景,散步,拍照', 1, 1, 4.10, 4.50, 2.00, 3.40, 4.70, 4.20, '2026-03-23 13:35:00', '2026-03-23 13:35:00');
INSERT INTO `small_scenic_spot` VALUES (6, 4, '府衙正厅', 0, '开封府核心参观点，适合了解衙署格局。', '/images/system/kaifengfu-mainhall.jpg', 30, '府衙,历史,文化', 1, 2, 3.80, 4.10, 0.80, 4.80, 4.00, 3.60, '2026-03-23 13:35:00', '2026-03-23 13:35:00');
INSERT INTO `small_scenic_spot` VALUES (7, 4, '包公断案演艺区', 0, '适合观看互动演艺，沉浸感较强。', '/images/system/kaifengfu-performance.jpg', 45, '演艺,互动,包公', 2, 3, 4.30, 3.70, 0.60, 4.70, 4.30, 3.20, '2026-03-23 13:35:00', '2026-03-23 13:35:00');
INSERT INTO `small_scenic_spot` VALUES (8, 5, '大雄宝殿', 0, '大相国寺主要礼佛与参观区域。', '/images/system/daxiangguosi-mainhall.jpg', 25, '寺庙,礼佛,人文', 1, 1, 3.60, 4.70, 0.80, 4.80, 3.50, 3.80, '2026-03-23 13:35:00', '2026-03-23 13:35:00');
INSERT INTO `small_scenic_spot` VALUES (9, 5, '千手千眼观音殿', 0, '人文特征突出，适合安静参观。', '/images/system/daxiangguosi-guanyin.jpg', 25, '佛教文化,静态参观', 1, 1, 3.50, 4.80, 0.60, 4.90, 3.40, 3.90, '2026-03-23 13:35:00', '2026-03-23 13:35:00');
INSERT INTO `small_scenic_spot` VALUES (10, 6, '铁塔主景点', 0, '景区核心打卡点，适合观塔与拍照。', '/images/system/iron-pagoda-main.jpg', 35, '古塔,打卡,拍照', 1, 2, 4.00, 4.60, 1.20, 4.40, 4.80, 4.10, '2026-03-23 13:35:00', '2026-03-23 13:35:00');
INSERT INTO `small_scenic_spot` VALUES (11, 6, '塔影步道', 0, '适合慢走和拍照，整体节奏轻松。', '/images/system/iron-pagoda-path.jpg', 30, '散步,拍照,轻松', 1, 1, 4.10, 4.70, 1.80, 3.60, 4.60, 4.50, '2026-03-23 13:35:00', '2026-03-23 13:35:00');
INSERT INTO `small_scenic_spot` VALUES (12, 7, '武侠城主入口', 1, '万岁山武侠城入园与导览节点。', '/images/system/wansuishan-gate.jpg', 20, '入口,导览,服务', 1, 2, 4.30, 3.80, 0.50, 2.20, 3.50, 4.30, '2026-03-23 13:35:00', '2026-03-23 13:35:00');
INSERT INTO `small_scenic_spot` VALUES (13, 7, '三打祝家庄演艺场', 0, '热门实景演艺区域，适合沉浸式观看表演。', '/images/system/wansuishan-performance.jpg', 60, '演艺,武侠,热门', 2, 4, 4.70, 3.40, 0.70, 4.10, 4.90, 3.20, '2026-03-23 13:35:00', '2026-03-23 13:35:00');
INSERT INTO `small_scenic_spot` VALUES (14, 7, '打铁花观演区', 0, '夜间氛围突出，适合拍照和收尾观演。', '/images/system/wansuishan-fireworks.jpg', 45, '夜场,拍照,演艺', 1, 4, 4.50, 3.30, 0.60, 3.80, 5.00, 3.10, '2026-03-23 13:35:00', '2026-03-23 13:35:00');

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
INSERT INTO `users` VALUES (3, 'zhangsan', '$2a$10$LvRbbJ64AmnN6PbiudVpTeZbdbyVvfj2vH8vottTVP2Kz1/.kBekK', '123132412946@qq.com', 'USER', 0);
INSERT INTO `users` VALUES (4, 'shi', '$2a$10$IBx7GfzuRxT25cD/qKxpoehFnlj0/KGQNeevGs/hnYOMT4i/ckpmy', '12312312@qq.com', 'USER', 0);

SET FOREIGN_KEY_CHECKS = 1;
