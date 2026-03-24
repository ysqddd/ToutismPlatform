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

 Date: 24/03/2026 14:35:29
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
  `price` decimal(10, 2) NOT NULL COMMENT '价格：景区表示门票或票价参考，饭店/餐饮点表示平均人均消费，免费公共节点可为 0',
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
) ENGINE = InnoDB AUTO_INCREMENT = 21 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of large_scenic_area
-- ----------------------------
INSERT INTO `large_scenic_area` VALUES (1, '开封站', '开封市主要铁路到达节点，可作为游客进入市区游览的常见起点。', '河南省开封市禹王台区', '/images/kaifeng-railway-station.jpg', '00:00-23:59', 30, 0.00, '交通枢纽,到达点,换乘', 1, 1, 3, 3.50, 4.20, 0.50, 1.20, 2.50, 3.50, 4.20, 4.20, 4.00, '2026-03-23 13:20:00', '2026-03-23 13:20:00');
INSERT INTO `large_scenic_area` VALUES (2, '清明上河园', '以《清明上河图》为蓝本打造的大型宋文化主题景区，适合沉浸式游览、看演出和夜游。', '河南省开封市龙亭区龙亭西路5号', '/images/qingming-shangheyuan.jpg', '09:00-22:00', 240, 120.00, '宋文化,夜游,演艺,沉浸式,拍照', 0, 3, 4, 4.60, 3.90, 2.80, 4.90, 4.80, 4.10, 4.30, 4.20, 4.90, '2026-03-23 13:20:00', '2026-03-23 13:20:00');
INSERT INTO `large_scenic_area` VALUES (3, '龙亭景区', '开封代表性历史景区之一，适合观景、赏花和了解古都格局。', '河南省开封市龙亭区中山路北段', '/images/longting.jpg', '08:00-18:30', 120, 60.00, '历史,古都,观景,赏花,拍照', 0, 2, 3, 4.20, 4.20, 3.20, 4.60, 4.70, 4.00, 3.70, 3.80, 4.60, '2026-03-23 13:20:00', '2026-03-23 13:20:00');
INSERT INTO `large_scenic_area` VALUES (4, '开封府', '以包公文化和府衙文化展示见长，适合历史体验与演艺观赏。', '河南省开封市鼓楼区包公东湖北岸', '/images/kaifengfu.jpg', '07:30-18:30', 100, 65.00, '包公文化,府衙,历史,演艺', 0, 2, 3, 4.00, 4.10, 1.20, 4.90, 4.10, 3.80, 3.90, 3.70, 4.50, '2026-03-23 13:20:00', '2026-03-23 13:20:00');
INSERT INTO `large_scenic_area` VALUES (5, '大相国寺', '开封重要佛教文化景点，节奏相对平缓，适合人文参观与静态游览。', '河南省开封市鼓楼区自由路西段36号', '/images/daxiangguosi.jpg', '08:00-17:30', 80, 40.00, '寺庙,人文,静态游览,老人友好', 0, 1, 2, 3.80, 4.80, 1.00, 4.80, 3.60, 4.50, 3.20, 3.50, 4.20, '2026-03-23 13:20:00', '2026-03-23 13:20:00');
INSERT INTO `large_scenic_area` VALUES (6, '铁塔景区', '以千年铁塔和园林环境闻名，适合散步、拍照和轻松游览。', '河南省开封市顺河回族区北门大街210号', '/images/iron-pagoda.jpg', '08:00-18:00', 90, 40.00, '古塔,园林,散步,拍照', 0, 1, 2, 4.10, 4.70, 2.40, 4.30, 4.60, 4.50, 3.40, 3.90, 4.40, '2026-03-23 13:20:00', '2026-03-23 13:20:00');
INSERT INTO `large_scenic_area` VALUES (7, '万岁山武侠城', '以武侠演艺和互动体验见长，适合看表演、亲子出游和沉浸式体验。', '河南省开封市龙亭区东京大道中段', '/images/wansuishan.jpg', '08:00-18:00', 220, 100.00, '武侠,演艺,亲子,沉浸式,夜场', 0, 3, 4, 4.70, 3.60, 2.30, 4.20, 4.90, 4.00, 4.10, 4.00, 4.80, '2026-03-23 13:20:00', '2026-03-23 13:20:00');
INSERT INTO `large_scenic_area` VALUES (8, '鼓楼夜市', '开封热门餐饮与夜间休闲聚集点，适合作为晚间收尾地点。', '河南省开封市鼓楼区鼓楼广场周边', '/images/gulou-night-market.jpg', '18:00-23:30', 90, 0.00, '夜市,美食,休闲,晚间', 1, 1, 4, 4.50, 4.00, 0.80, 2.50, 4.20, 4.80, 4.90, 3.60, 4.70, '2026-03-23 13:20:00', '2026-03-23 13:20:00');
INSERT INTO `large_scenic_area` VALUES (9, '包公祠', '以纪念包拯为主题的人文景点，适合与开封府组成包公文化线。', '河南省开封市鼓楼区向阳路1号', '/images/baogongci.jpg', '08:00-19:00', 70, 30.00, '包公文化,祠堂,历史,人文', 0, 1, 3, 3.90, 4.30, 0.80, 4.90, 4.10, 4.20, 3.70, 3.70, 4.50, '2026-03-24 10:00:00', '2026-03-24 10:00:00');
INSERT INTO `large_scenic_area` VALUES (10, '天波杨府', '以杨家将文化为主题，兼具园林观赏、历史故事和演艺体验。', '河南省开封市龙亭区龙亭北路14号', '/images/tianboyangfu.jpg', '08:00-18:00', 90, 60.00, '杨家将,历史,园林,演艺,拍照', 0, 2, 3, 4.20, 4.00, 1.50, 4.70, 4.50, 4.00, 3.60, 3.80, 4.40, '2026-03-24 10:00:00', '2026-03-24 10:00:00');
INSERT INTO `large_scenic_area` VALUES (11, '中国翰园碑林', '集碑刻、书法、园林于一体的人文景区，适合文化游和拍照散步。', '河南省开封市龙亭区龙亭北路15号', '/images/hanyuan-beilin.jpg', '08:00-18:00', 100, 40.00, '碑林,书法,园林,人文,拍照', 0, 1, 2, 3.80, 4.40, 2.20, 4.80, 4.70, 4.50, 3.50, 3.80, 4.30, '2026-03-24 10:00:00', '2026-03-24 10:00:00');
INSERT INTO `large_scenic_area` VALUES (12, '开封博物馆', '开封市重要综合性博物馆，适合系统了解开封历史与宋文化。', '河南省开封市龙亭区郑开大道第六大街', '/images/kaifeng-museum.jpg', '09:00-17:00', 120, 0.00, '博物馆,宋文化,历史,研学,室内', 0, 1, 2, 4.50, 4.70, 0.50, 5.00, 4.10, 4.60, 3.20, 4.60, 4.70, '2026-03-24 10:00:00', '2026-03-24 10:00:00');
INSERT INTO `large_scenic_area` VALUES (13, '禹王台公园', '兼具古迹、园林和休闲属性的历史公园，节奏舒缓，适合轻松游览。', '河南省开封市禹王台区繁塔西街东段', '/images/yuwangtai-park.jpg', '08:00-18:00', 90, 30.00, '历史公园,园林,散步,休闲,古迹', 0, 1, 2, 4.00, 4.60, 2.20, 4.20, 4.20, 4.70, 3.60, 4.10, 4.00, '2026-03-24 10:00:00', '2026-03-24 10:00:00');
INSERT INTO `large_scenic_area` VALUES (14, '繁塔', '北宋古塔遗存，是开封现存年代很早的重要地面古建筑之一，适合历史向和古建向游客。', '河南省开封市禹王台区繁塔西街30号', '/images/fanta.jpg', '08:30-17:30', 60, 25.00, '古塔,北宋,古建,文物,历史', 0, 1, 2, 3.40, 4.20, 1.00, 4.90, 4.50, 4.00, 2.80, 3.40, 4.10, '2026-03-24 10:00:00', '2026-03-24 10:00:00');
INSERT INTO `large_scenic_area` VALUES (15, '开封第一楼（寺后街店）', '开封代表性的灌汤包老字号之一，以小笼灌汤包、桶子鸡和传统豫菜闻名，适合作为老城游览中的正餐或特色小吃补给点。', '河南省开封市鼓楼区寺后街8号', '/images/kaifeng-diyilou.jpg', '10:00-21:00', 75, 68.00, '老字号,灌汤包,豫菜,开封美食,正餐,小吃', 1, 1, 4, 4.60, 4.50, 0.20, 4.70, 4.10, 4.60, 5.00, 4.40, 4.90, '2026-03-24 18:30:00', '2026-03-24 18:30:00');
INSERT INTO `large_scenic_area` VALUES (16, '又一新饭店（寺后街店）', '开封知名老字号豫菜饭店，适合品尝套四宝、鲤鱼焙面、灌汤包等经典开封菜，可作为鼓楼一带游玩的聚餐补给点。', '河南省开封市鼓楼区寺后街23号', '/images/kaifeng-youyixin.jpg', '11:00-21:30', 90, 90.00, '老字号,豫菜,开封菜,聚餐,灌汤包,鲤鱼焙面', 1, 1, 3, 4.60, 4.40, 0.20, 4.80, 4.00, 4.70, 5.00, 4.30, 4.80, '2026-03-24 18:30:00', '2026-03-24 18:30:00');
INSERT INTO `large_scenic_area` VALUES (17, '马豫兴桶子鸡（丁角街店）', '开封著名老字号熟食与传统风味店，以桶子鸡等回族风味食品闻名，适合作为特色熟食打卡与轻餐补给点。', '河南省开封市鼓楼区丁角街62号', '/images/mayuxing-tongziji.jpg', '08:30-20:30', 45, 45.00, '老字号,桶子鸡,熟食,开封特产,回族风味,小吃', 1, 1, 3, 4.10, 4.20, 0.10, 4.50, 3.60, 4.00, 4.80, 3.60, 4.60, '2026-03-24 18:30:00', '2026-03-24 18:30:00');
INSERT INTO `large_scenic_area` VALUES (18, '黄家老店（大梁路店）', '开封本地知名灌汤包与豫菜饭店之一，性价比较高，适合家庭游客和想体验本地传统味道的游客。', '河南省开封市龙亭区大梁路18号', '/images/huangjia-laodian.jpg', '10:00-21:30', 75, 65.00, '灌汤包,老店,豫菜,家庭聚餐,开封美食,本地口碑', 1, 1, 4, 4.40, 4.50, 0.20, 4.40, 4.00, 4.50, 5.00, 4.20, 4.70, '2026-03-24 18:30:00', '2026-03-24 18:30:00');
INSERT INTO `large_scenic_area` VALUES (19, '一品楼灌汤包子（延庆观店）', '开封游客和本地食客都较熟悉的灌汤包店，适合在鼓楼、书店街、延庆观一线游览时顺路用餐。', '河南省开封市鼓楼区观前街15号延庆观向西50米路南', '/images/yipinlou-guntangbao.jpg', '09:30-21:00', 60, 55.00, '灌汤包,开封小吃,正餐,鼓楼商圈,本地美食', 1, 1, 4, 4.20, 4.30, 0.10, 4.20, 3.90, 4.40, 4.90, 4.10, 4.50, '2026-03-24 18:30:00', '2026-03-24 18:30:00');
INSERT INTO `large_scenic_area` VALUES (20, '邢家锅贴老店（金明广场店）', '开封较有代表性的锅贴与传统小吃店之一，适合把锅贴、杏仁茶等作为轻松休息时的特色补给。', '河南省开封市龙亭区大梁路合旺苑1号楼', '/images/xingjia-guotie.jpg', '08:00-22:00', 50, 38.00, '锅贴,开封小吃,非遗风味,轻餐,杏仁茶,本地推荐', 1, 1, 3, 4.10, 4.20, 0.10, 4.00, 3.80, 4.40, 4.80, 3.90, 4.40, '2026-03-24 18:30:00', '2026-03-24 18:30:00');

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
) ENGINE = InnoDB AUTO_INCREMENT = 8 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of product
-- ----------------------------
INSERT INTO `product` VALUES (1, '1111', '11', 75.00, '', 'ON_SALE', '2026-03-19 08:14:30', '2026-03-19 09:14:53');
INSERT INTO `product` VALUES (2, '开封经典初游一日套餐', '推荐路线：开封站 → 清明上河园 → 开封府 → 鼓楼夜市。适合第一次到开封的游客，一天内兼顾宋文化沉浸体验、府衙历史体验和夜市美食收尾。', 159.00, '/images/package-classic-one-day.jpg', 'ON_SALE', '2026-03-24 11:30:00', '2026-03-24 11:30:00');
INSERT INTO `product` VALUES (3, '宋都文化深度体验套餐', '推荐路线：龙亭景区 → 天波杨府 → 中国翰园碑林 → 开封博物馆。适合喜欢历史、园林、书法、博物馆内容的用户，整体偏文化深度游。', 129.00, '/images/package-song-culture.jpg', 'ON_SALE', '2026-03-24 11:30:00', '2026-03-24 11:30:00');
INSERT INTO `product` VALUES (4, '包公文化半日精品套餐', '推荐路线：开封府 → 包公祠 → 大相国寺。适合时间较紧、偏好人文历史和包公文化主题的游客，半天到一天均可安排。', 99.00, '/images/package-baogong-halfday.jpg', 'ON_SALE', '2026-03-24 11:30:00', '2026-03-24 11:30:00');
INSERT INTO `product` VALUES (5, '古迹园林休闲漫游套餐', '推荐路线：铁塔景区 → 禹王台公园 → 繁塔 → 鼓楼夜市。适合喜欢古塔、园林和轻松慢游的游客，节奏舒缓，适合拍照与散步。', 89.00, '/images/package-heritage-leisure.jpg', 'ON_SALE', '2026-03-24 11:30:00', '2026-03-24 11:30:00');
INSERT INTO `product` VALUES (6, '亲子演艺欢乐畅玩套餐', '推荐路线：清明上河园 → 万岁山武侠城 → 鼓楼夜市。适合亲子和年轻游客，主打沉浸式演艺、互动体验和夜间休闲。', 188.00, '/images/package-family-show.jpg', 'ON_SALE', '2026-03-24 11:30:00', '2026-03-24 11:30:00');
INSERT INTO `product` VALUES (7, '开封深度两日精选套餐', '推荐路线建议：第一天游览清明上河园、龙亭景区、开封府、鼓楼夜市；第二天游览大相国寺、铁塔景区、包公祠、天波杨府、中国翰园碑林、开封博物馆。适合希望一次性较完整体验开封核心文化景点的游客。', 299.00, '/images/package-two-day-deep.jpg', 'ON_SALE', '2026-03-24 11:30:00', '2026-03-24 11:30:00');

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
) ENGINE = InnoDB AUTO_INCREMENT = 31 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of product_large_scenic_area
-- ----------------------------
INSERT INTO `product_large_scenic_area` VALUES (1, 1, 1, '2026-03-19 17:14:52');
INSERT INTO `product_large_scenic_area` VALUES (2, 1, 4, '2026-03-19 17:14:52');
INSERT INTO `product_large_scenic_area` VALUES (3, 2, 1, '2026-03-24 11:35:00');
INSERT INTO `product_large_scenic_area` VALUES (4, 2, 2, '2026-03-24 11:35:00');
INSERT INTO `product_large_scenic_area` VALUES (5, 2, 4, '2026-03-24 11:35:00');
INSERT INTO `product_large_scenic_area` VALUES (6, 2, 8, '2026-03-24 11:35:00');
INSERT INTO `product_large_scenic_area` VALUES (7, 3, 3, '2026-03-24 11:35:00');
INSERT INTO `product_large_scenic_area` VALUES (8, 3, 10, '2026-03-24 11:35:00');
INSERT INTO `product_large_scenic_area` VALUES (9, 3, 11, '2026-03-24 11:35:00');
INSERT INTO `product_large_scenic_area` VALUES (10, 3, 12, '2026-03-24 11:35:00');
INSERT INTO `product_large_scenic_area` VALUES (11, 4, 4, '2026-03-24 11:35:00');
INSERT INTO `product_large_scenic_area` VALUES (12, 4, 9, '2026-03-24 11:35:00');
INSERT INTO `product_large_scenic_area` VALUES (13, 4, 5, '2026-03-24 11:35:00');
INSERT INTO `product_large_scenic_area` VALUES (14, 5, 6, '2026-03-24 11:35:00');
INSERT INTO `product_large_scenic_area` VALUES (15, 5, 13, '2026-03-24 11:35:00');
INSERT INTO `product_large_scenic_area` VALUES (16, 5, 14, '2026-03-24 11:35:00');
INSERT INTO `product_large_scenic_area` VALUES (17, 5, 8, '2026-03-24 11:35:00');
INSERT INTO `product_large_scenic_area` VALUES (18, 6, 2, '2026-03-24 11:35:00');
INSERT INTO `product_large_scenic_area` VALUES (19, 6, 7, '2026-03-24 11:35:00');
INSERT INTO `product_large_scenic_area` VALUES (20, 6, 8, '2026-03-24 11:35:00');
INSERT INTO `product_large_scenic_area` VALUES (21, 7, 2, '2026-03-24 11:35:00');
INSERT INTO `product_large_scenic_area` VALUES (22, 7, 3, '2026-03-24 11:35:00');
INSERT INTO `product_large_scenic_area` VALUES (23, 7, 4, '2026-03-24 11:35:00');
INSERT INTO `product_large_scenic_area` VALUES (24, 7, 5, '2026-03-24 11:35:00');
INSERT INTO `product_large_scenic_area` VALUES (25, 7, 6, '2026-03-24 11:35:00');
INSERT INTO `product_large_scenic_area` VALUES (26, 7, 8, '2026-03-24 11:35:00');
INSERT INTO `product_large_scenic_area` VALUES (27, 7, 9, '2026-03-24 11:35:00');
INSERT INTO `product_large_scenic_area` VALUES (28, 7, 10, '2026-03-24 11:35:00');
INSERT INTO `product_large_scenic_area` VALUES (29, 7, 11, '2026-03-24 11:35:00');
INSERT INTO `product_large_scenic_area` VALUES (30, 7, 12, '2026-03-24 11:35:00');

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
  CONSTRAINT `fk_route_plan_end_area` FOREIGN KEY (`end_area_id`) REFERENCES `large_scenic_area` (`id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_route_plan_start_area` FOREIGN KEY (`start_area_id`) REFERENCES `large_scenic_area` (`id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_route_plan_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of route_plan_record
-- ----------------------------

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
) ENGINE = InnoDB AUTO_INCREMENT = 64 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

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
INSERT INTO `scenic_area_edge` VALUES (14, 900.00, 6, 4, 9, '2026-03-24 10:05:00.000000', '2026-03-24 10:05:00.000000', '开封府到包公祠，可步行串联包公文化线', 0.00);
INSERT INTO `scenic_area_edge` VALUES (15, 900.00, 6, 9, 4, '2026-03-24 10:05:00.000000', '2026-03-24 10:05:00.000000', '包公祠返回开封府，适合反向串联', 0.00);
INSERT INTO `scenic_area_edge` VALUES (16, 1600.00, 8, 9, 5, '2026-03-24 10:05:00.000000', '2026-03-24 10:05:00.000000', '包公祠到大相国寺，适合人文连线', 0.00);
INSERT INTO `scenic_area_edge` VALUES (17, 1600.00, 8, 5, 9, '2026-03-24 10:05:00.000000', '2026-03-24 10:05:00.000000', '大相国寺到包公祠，适合补充包公文化点', 0.00);
INSERT INTO `scenic_area_edge` VALUES (18, 700.00, 5, 3, 10, '2026-03-24 10:05:00.000000', '2026-03-24 10:05:00.000000', '龙亭景区到天波杨府，适合步行连游', 0.00);
INSERT INTO `scenic_area_edge` VALUES (19, 700.00, 5, 10, 3, '2026-03-24 10:05:00.000000', '2026-03-24 10:05:00.000000', '天波杨府返回龙亭景区，动线顺畅', 0.00);
INSERT INTO `scenic_area_edge` VALUES (20, 1100.00, 7, 10, 11, '2026-03-24 10:05:00.000000', '2026-03-24 10:05:00.000000', '天波杨府到中国翰园碑林，可继续文化园林线', 0.00);
INSERT INTO `scenic_area_edge` VALUES (21, 1100.00, 7, 11, 10, '2026-03-24 10:05:00.000000', '2026-03-24 10:05:00.000000', '中国翰园碑林返回天波杨府', 0.00);
INSERT INTO `scenic_area_edge` VALUES (22, 1000.00, 7, 3, 11, '2026-03-24 10:05:00.000000', '2026-03-24 10:05:00.000000', '龙亭景区到中国翰园碑林，适合步行', 0.00);
INSERT INTO `scenic_area_edge` VALUES (23, 1000.00, 7, 11, 3, '2026-03-24 10:05:00.000000', '2026-03-24 10:05:00.000000', '中国翰园碑林返回龙亭景区', 0.00);
INSERT INTO `scenic_area_edge` VALUES (24, 1500.00, 8, 11, 2, '2026-03-24 10:05:00.000000', '2026-03-24 10:05:00.000000', '中国翰园碑林到清明上河园，可作为北线延展', 0.00);
INSERT INTO `scenic_area_edge` VALUES (25, 1500.00, 8, 2, 11, '2026-03-24 10:05:00.000000', '2026-03-24 10:05:00.000000', '清明上河园到中国翰园碑林，适合补充书法园林点', 0.00);
INSERT INTO `scenic_area_edge` VALUES (26, 9000.00, 22, 11, 12, '2026-03-24 10:05:00.000000', '2026-03-24 10:05:00.000000', '中国翰园碑林前往开封博物馆，建议打车或公交', 15.00);
INSERT INTO `scenic_area_edge` VALUES (27, 9000.00, 22, 12, 11, '2026-03-24 10:05:00.000000', '2026-03-24 10:05:00.000000', '开封博物馆返回中国翰园碑林', 15.00);
INSERT INTO `scenic_area_edge` VALUES (28, 8500.00, 24, 12, 13, '2026-03-24 10:05:00.000000', '2026-03-24 10:05:00.000000', '开封博物馆到禹王台公园，跨区游览建议打车', 15.00);
INSERT INTO `scenic_area_edge` VALUES (29, 8500.00, 24, 13, 12, '2026-03-24 10:05:00.000000', '2026-03-24 10:05:00.000000', '禹王台公园到开封博物馆', 15.00);
INSERT INTO `scenic_area_edge` VALUES (30, 2100.00, 10, 13, 14, '2026-03-24 10:05:00.000000', '2026-03-24 10:05:00.000000', '禹王台公园到繁塔，适合历史古建线', 0.00);
INSERT INTO `scenic_area_edge` VALUES (31, 2100.00, 10, 14, 13, '2026-03-24 10:05:00.000000', '2026-03-24 10:05:00.000000', '繁塔返回禹王台公园', 0.00);
INSERT INTO `scenic_area_edge` VALUES (32, 1700.00, 8, 1, 13, '2026-03-24 10:05:00.000000', '2026-03-24 10:05:00.000000', '开封站到禹王台公园，适合作为东南片区起点', 0.00);
INSERT INTO `scenic_area_edge` VALUES (33, 1700.00, 8, 13, 1, '2026-03-24 10:05:00.000000', '2026-03-24 10:05:00.000000', '禹王台公园返回开封站', 0.00);
INSERT INTO `scenic_area_edge` VALUES (34, 2200.00, 10, 1, 14, '2026-03-24 10:05:00.000000', '2026-03-24 10:05:00.000000', '开封站到繁塔，适合古迹线首站', 0.00);
INSERT INTO `scenic_area_edge` VALUES (35, 2200.00, 10, 14, 1, '2026-03-24 10:05:00.000000', '2026-03-24 10:05:00.000000', '繁塔返回开封站', 0.00);
INSERT INTO `scenic_area_edge` VALUES (36, 1800.00, 9, 10, 7, '2026-03-24 10:05:00.000000', '2026-03-24 10:05:00.000000', '天波杨府到万岁山武侠城，适合亲子或演艺联动', 0.00);
INSERT INTO `scenic_area_edge` VALUES (37, 1800.00, 9, 7, 10, '2026-03-24 10:05:00.000000', '2026-03-24 10:05:00.000000', '万岁山武侠城到天波杨府', 0.00);
INSERT INTO `scenic_area_edge` VALUES (38, 1700.00, 8, 9, 8, '2026-03-24 10:05:00.000000', '2026-03-24 10:05:00.000000', '包公祠到鼓楼夜市，适合晚间收尾', 0.00);
INSERT INTO `scenic_area_edge` VALUES (39, 1700.00, 8, 8, 9, '2026-03-24 10:05:00.000000', '2026-03-24 10:05:00.000000', '鼓楼夜市到包公祠，适合反向串联', 0.00);
INSERT INTO `scenic_area_edge` VALUES (40, 600.00, 4, 15, 8, '2026-03-24 18:45:00.000000', '2026-03-24 18:45:00.000000', '开封第一楼（寺后街店）到鼓楼夜市，适合饭后继续夜游或补给', 0.00);
INSERT INTO `scenic_area_edge` VALUES (41, 600.00, 4, 8, 15, '2026-03-24 18:45:00.000000', '2026-03-24 18:45:00.000000', '鼓楼夜市到开封第一楼（寺后街店），适合作为老城美食补给点', 0.00);
INSERT INTO `scenic_area_edge` VALUES (42, 1300.00, 7, 15, 4, '2026-03-24 18:45:00.000000', '2026-03-24 18:45:00.000000', '开封第一楼（寺后街店）到开封府，可串联老城人文与美食路线', 0.00);
INSERT INTO `scenic_area_edge` VALUES (43, 1300.00, 7, 4, 15, '2026-03-24 18:45:00.000000', '2026-03-24 18:45:00.000000', '开封府到开封第一楼（寺后街店），适合游览后就近用餐', 0.00);
INSERT INTO `scenic_area_edge` VALUES (44, 650.00, 4, 16, 8, '2026-03-24 18:45:00.000000', '2026-03-24 18:45:00.000000', '又一新饭店（寺后街店）到鼓楼夜市，适合老城晚间连续游玩', 0.00);
INSERT INTO `scenic_area_edge` VALUES (45, 650.00, 4, 8, 16, '2026-03-24 18:45:00.000000', '2026-03-24 18:45:00.000000', '鼓楼夜市到又一新饭店（寺后街店），适合作为聚餐补给点', 0.00);
INSERT INTO `scenic_area_edge` VALUES (46, 1200.00, 6, 16, 5, '2026-03-24 18:45:00.000000', '2026-03-24 18:45:00.000000', '又一新饭店（寺后街店）到大相国寺，适合人文游后用餐', 0.00);
INSERT INTO `scenic_area_edge` VALUES (47, 1200.00, 6, 5, 16, '2026-03-24 18:45:00.000000', '2026-03-24 18:45:00.000000', '大相国寺到又一新饭店（寺后街店），适合午餐或晚餐衔接', 0.00);
INSERT INTO `scenic_area_edge` VALUES (48, 900.00, 5, 17, 8, '2026-03-24 18:45:00.000000', '2026-03-24 18:45:00.000000', '马豫兴桶子鸡（丁角街店）到鼓楼夜市，适合作为特色熟食补给点', 0.00);
INSERT INTO `scenic_area_edge` VALUES (49, 900.00, 5, 8, 17, '2026-03-24 18:45:00.000000', '2026-03-24 18:45:00.000000', '鼓楼夜市到马豫兴桶子鸡（丁角街店），适合顺路打卡本地老字号', 0.00);
INSERT INTO `scenic_area_edge` VALUES (50, 1000.00, 6, 17, 5, '2026-03-24 18:45:00.000000', '2026-03-24 18:45:00.000000', '马豫兴桶子鸡（丁角街店）到大相国寺，适合老城文化与小吃连线', 0.00);
INSERT INTO `scenic_area_edge` VALUES (51, 1000.00, 6, 5, 17, '2026-03-24 18:45:00.000000', '2026-03-24 18:45:00.000000', '大相国寺到马豫兴桶子鸡（丁角街店），适合游览后补给', 0.00);
INSERT INTO `scenic_area_edge` VALUES (52, 2300.00, 10, 18, 3, '2026-03-24 18:45:00.000000', '2026-03-24 18:45:00.000000', '黄家老店（大梁路店）到龙亭景区，适合景区游与正餐衔接', 5.00);
INSERT INTO `scenic_area_edge` VALUES (53, 2300.00, 10, 3, 18, '2026-03-24 18:45:00.000000', '2026-03-24 18:45:00.000000', '龙亭景区到黄家老店（大梁路店），适合作为用餐补给点', 5.00);
INSERT INTO `scenic_area_edge` VALUES (54, 2600.00, 12, 18, 2, '2026-03-24 18:45:00.000000', '2026-03-24 18:45:00.000000', '黄家老店（大梁路店）到清明上河园，适合演艺游后就近用餐', 6.00);
INSERT INTO `scenic_area_edge` VALUES (55, 2600.00, 12, 2, 18, '2026-03-24 18:45:00.000000', '2026-03-24 18:45:00.000000', '清明上河园到黄家老店（大梁路店），适合午晚餐衔接', 6.00);
INSERT INTO `scenic_area_edge` VALUES (56, 500.00, 3, 19, 8, '2026-03-24 18:45:00.000000', '2026-03-24 18:45:00.000000', '一品楼灌汤包子（延庆观店）到鼓楼夜市，适合老城商圈顺路用餐', 0.00);
INSERT INTO `scenic_area_edge` VALUES (57, 500.00, 3, 8, 19, '2026-03-24 18:45:00.000000', '2026-03-24 18:45:00.000000', '鼓楼夜市到一品楼灌汤包子（延庆观店），适合补充开封传统小吃', 0.00);
INSERT INTO `scenic_area_edge` VALUES (58, 1400.00, 7, 19, 4, '2026-03-24 18:45:00.000000', '2026-03-24 18:45:00.000000', '一品楼灌汤包子（延庆观店）到开封府，适合府衙文化线中途补给', 0.00);
INSERT INTO `scenic_area_edge` VALUES (59, 1400.00, 7, 4, 19, '2026-03-24 18:45:00.000000', '2026-03-24 18:45:00.000000', '开封府到一品楼灌汤包子（延庆观店），适合作为餐饮补给点', 0.00);
INSERT INTO `scenic_area_edge` VALUES (60, 3200.00, 12, 20, 2, '2026-03-24 18:45:00.000000', '2026-03-24 18:45:00.000000', '邢家锅贴老店（金明广场店）到清明上河园，适合西北片区轻餐补给', 6.00);
INSERT INTO `scenic_area_edge` VALUES (61, 3200.00, 12, 2, 20, '2026-03-24 18:45:00.000000', '2026-03-24 18:45:00.000000', '清明上河园到邢家锅贴老店（金明广场店），适合作为出园后用餐点', 6.00);
INSERT INTO `scenic_area_edge` VALUES (62, 3600.00, 14, 20, 3, '2026-03-24 18:45:00.000000', '2026-03-24 18:45:00.000000', '邢家锅贴老店（金明广场店）到龙亭景区，可串联轻餐与观景游', 8.00);
INSERT INTO `scenic_area_edge` VALUES (63, 3600.00, 14, 3, 20, '2026-03-24 18:45:00.000000', '2026-03-24 18:45:00.000000', '龙亭景区到邢家锅贴老店（金明广场店），适合游览后补给', 8.00);

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
) ENGINE = InnoDB AUTO_INCREMENT = 21 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of scenic_edge
-- ----------------------------
INSERT INTO `scenic_edge` VALUES (1, 2, 1, 2, 320, 6, '从迎宾门步行至虹桥主景区', 'WALK', 1, 4.20, 4.10, 4.00, '2026-03-23 13:30:00', '2026-03-23 13:30:00');
INSERT INTO `scenic_edge` VALUES (2, 2, 2, 3, 450, 8, '从虹桥前往东京码头观景区', 'WALK', 1, 4.60, 4.00, 3.80, '2026-03-23 13:30:00', '2026-03-23 13:30:00');
INSERT INTO `scenic_edge` VALUES (3, 3, 4, 5, 500, 10, '从龙亭大殿步行至杨家湖观景带', 'WALK', 1, 4.50, 4.20, 4.20, '2026-03-23 13:30:00', '2026-03-23 13:30:00');
INSERT INTO `scenic_edge` VALUES (4, 4, 6, 7, 280, 5, '从府衙正厅步行至包公断案演艺区', 'WALK', 1, 4.20, 4.00, 4.00, '2026-03-23 13:30:00', '2026-03-23 13:30:00');
INSERT INTO `scenic_edge` VALUES (5, 5, 8, 9, 260, 5, '从大雄宝殿步行至千手千眼观音殿', 'WALK', 1, 4.10, 4.30, 4.60, '2026-03-23 13:30:00', '2026-03-23 13:30:00');
INSERT INTO `scenic_edge` VALUES (6, 6, 10, 11, 380, 7, '从铁塔主景点前往塔影步道', 'WALK', 1, 4.40, 4.50, 4.50, '2026-03-23 13:30:00', '2026-03-23 13:30:00');
INSERT INTO `scenic_edge` VALUES (7, 7, 12, 13, 650, 12, '从中门前往三打祝家庄演艺场', 'WALK', 2, 4.80, 3.80, 3.50, '2026-03-23 13:30:00', '2026-03-24 20:20:00');
INSERT INTO `scenic_edge` VALUES (8, 7, 13, 14, 420, 8, '从演艺场步行至打铁花观演区', 'WALK', 1, 4.90, 3.90, 3.40, '2026-03-23 13:30:00', '2026-03-23 13:30:00');
INSERT INTO `scenic_edge` VALUES (9, 9, 15, 16, 180, 4, '从包公祠山门步行至包拯纪念主殿', 'WALK', 1, 3.80, 4.20, 4.10, '2026-03-24 10:15:00', '2026-03-24 10:15:00');
INSERT INTO `scenic_edge` VALUES (10, 9, 16, 15, 180, 4, '从包拯纪念主殿返回包公祠山门', 'WALK', 1, 3.80, 4.20, 4.10, '2026-03-24 10:15:00', '2026-03-24 10:15:00');
INSERT INTO `scenic_edge` VALUES (11, 10, 17, 18, 320, 6, '从天波楼步行至杨家将演武场', 'WALK', 1, 4.10, 4.00, 3.80, '2026-03-24 10:15:00', '2026-03-24 10:15:00');
INSERT INTO `scenic_edge` VALUES (12, 10, 18, 17, 320, 6, '从杨家将演武场返回天波楼', 'WALK', 1, 4.10, 4.00, 3.80, '2026-03-24 10:15:00', '2026-03-24 10:15:00');
INSERT INTO `scenic_edge` VALUES (13, 11, 19, 20, 420, 8, '从碑廊主展区步行至翰园湖景区', 'WALK', 1, 4.50, 4.30, 4.40, '2026-03-24 10:15:00', '2026-03-24 10:15:00');
INSERT INTO `scenic_edge` VALUES (14, 11, 20, 19, 420, 8, '从翰园湖景区返回碑廊主展区', 'WALK', 1, 4.50, 4.30, 4.40, '2026-03-24 10:15:00', '2026-03-24 10:15:00');
INSERT INTO `scenic_edge` VALUES (15, 12, 21, 22, 260, 5, '从基本陈列展厅步行至北宋东京城专题展区', 'WALK', 1, 3.90, 4.70, 4.80, '2026-03-24 10:15:00', '2026-03-24 10:15:00');
INSERT INTO `scenic_edge` VALUES (16, 12, 22, 21, 260, 5, '从北宋东京城专题展区返回基本陈列展厅', 'WALK', 1, 3.90, 4.70, 4.80, '2026-03-24 10:15:00', '2026-03-24 10:15:00');
INSERT INTO `scenic_edge` VALUES (17, 13, 23, 24, 380, 7, '从御碑亭步行至古吹台园林步道', 'WALK', 1, 4.20, 4.60, 4.70, '2026-03-24 10:15:00', '2026-03-24 10:15:00');
INSERT INTO `scenic_edge` VALUES (18, 13, 24, 23, 380, 7, '从古吹台园林步道返回御碑亭', 'WALK', 1, 4.20, 4.60, 4.70, '2026-03-24 10:15:00', '2026-03-24 10:15:00');
INSERT INTO `scenic_edge` VALUES (19, 14, 25, 26, 160, 4, '从繁塔塔身观赏区步行至佛砖碑刻区', 'WALK', 1, 4.30, 4.00, 4.10, '2026-03-24 10:15:00', '2026-03-24 10:15:00');
INSERT INTO `scenic_edge` VALUES (20, 14, 26, 25, 160, 4, '从佛砖碑刻区返回繁塔塔身观赏区', 'WALK', 1, 4.30, 4.00, 4.10, '2026-03-24 10:15:00', '2026-03-24 10:15:00');
INSERT INTO `scenic_edge` VALUES (21, 2, 27, 2, 380, 7, '从金水门步行至虹桥主景区', 'WALK', 1, 4.20, 4.10, 4.20, '2026-03-24 20:20:00', '2026-03-24 20:20:00');
INSERT INTO `scenic_edge` VALUES (22, 2, 28, 2, 420, 8, '从端门步行至虹桥主景区', 'WALK', 1, 4.10, 4.00, 4.00, '2026-03-24 20:20:00', '2026-03-24 20:20:00');
INSERT INTO `scenic_edge` VALUES (23, 2, 29, 2, 460, 8, '从丹凤门步行至虹桥主景区', 'WALK', 1, 4.10, 4.00, 4.00, '2026-03-24 20:20:00', '2026-03-24 20:20:00');
INSERT INTO `scenic_edge` VALUES (24, 2, 30, 3, 260, 5, '从通津门步行至东京码头观景区', 'WALK', 1, 4.30, 4.20, 4.20, '2026-03-24 20:20:00', '2026-03-24 20:20:00');
INSERT INTO `scenic_edge` VALUES (25, 3, 31, 4, 180, 4, '从午门步行至龙亭大殿', 'WALK', 1, 4.40, 4.20, 4.40, '2026-03-24 20:20:00', '2026-03-24 20:20:00');
INSERT INTO `scenic_edge` VALUES (26, 3, 32, 5, 220, 5, '从北门步行至杨家湖观景带', 'WALK', 1, 4.20, 4.30, 4.50, '2026-03-24 20:20:00', '2026-03-24 20:20:00');
INSERT INTO `scenic_edge` VALUES (27, 3, 33, 5, 260, 5, '从东便门步行至杨家湖观景带', 'WALK', 1, 4.10, 4.20, 4.40, '2026-03-24 20:20:00', '2026-03-24 20:20:00');
INSERT INTO `scenic_edge` VALUES (28, 4, 34, 6, 120, 3, '从府门步行至府衙正厅', 'WALK', 1, 4.00, 4.20, 4.50, '2026-03-24 20:20:00', '2026-03-24 20:20:00');
INSERT INTO `scenic_edge` VALUES (29, 5, 35, 8, 90, 2, '从山门步行至大雄宝殿', 'WALK', 1, 3.90, 4.40, 4.80, '2026-03-24 20:20:00', '2026-03-24 20:20:00');
INSERT INTO `scenic_edge` VALUES (30, 7, 36, 13, 520, 10, '从南门步行至三打祝家庄演艺场', 'WALK', 2, 4.20, 3.90, 3.80, '2026-03-24 20:20:00', '2026-03-24 20:20:00');
INSERT INTO `scenic_edge` VALUES (31, 7, 37, 13, 560, 10, '从北大门步行至三打祝家庄演艺场', 'WALK', 2, 4.30, 3.80, 3.70, '2026-03-24 20:20:00', '2026-03-24 20:20:00');
INSERT INTO `scenic_edge` VALUES (32, 7, 38, 14, 480, 9, '从东大门步行至打铁花观演区', 'WALK', 2, 4.40, 3.80, 3.60, '2026-03-24 20:20:00', '2026-03-24 20:20:00');
INSERT INTO `scenic_edge` VALUES (33, 11, 39, 19, 150, 4, '从南大门步行至碑廊主展区', 'WALK', 1, 4.10, 4.40, 4.50, '2026-03-24 20:20:00', '2026-03-24 20:20:00');
INSERT INTO `scenic_edge` VALUES (34, 11, 40, 20, 180, 4, '从北大门步行至翰园湖景区', 'WALK', 1, 4.20, 4.50, 4.60, '2026-03-24 20:20:00', '2026-03-24 20:20:00');
INSERT INTO `scenic_edge` VALUES (35, 13, 41, 23, 200, 4, '从东门步行至御碑亭', 'WALK', 1, 4.00, 4.40, 4.50, '2026-03-24 20:20:00', '2026-03-24 20:20:00');
INSERT INTO `scenic_edge` VALUES (36, 13, 42, 24, 180, 4, '从西门步行至古吹台园林步道', 'WALK', 1, 4.00, 4.50, 4.60, '2026-03-24 20:20:00', '2026-03-24 20:20:00');

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
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of shopping_cart
-- ----------------------------
INSERT INTO `shopping_cart` VALUES (2, 4, 'PRODUCT', 2, '开封经典初游一日套餐', 159.00, '/images/package-classic-one-day.jpg', '推荐路线：开封站 → 清明上河园 → 开封府 → 鼓楼夜市。适合第一次到开封的游客，一天内兼顾宋文化沉浸体验、府衙历史体验和夜市美食收尾。', 1, '2026-03-24 04:15:15', '2026-03-24 04:15:15');

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
) ENGINE = InnoDB AUTO_INCREMENT = 27 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of small_scenic_spot
-- ----------------------------
INSERT INTO `small_scenic_spot` VALUES (1, 2, '迎宾门', 1, '清明上河园东大门，开园迎宾表演所在入口，位于龙亭湖区域中心位置。', '/images/qingming-gate.jpg', 20, '东门,正门,入口,服务,导览', 1, 2, 4.20, 4.30, 0.80, 2.00, 3.20, 4.60, '2026-03-23 13:35:00', '2026-03-24 18:30:00');
INSERT INTO `small_scenic_spot` VALUES (2, 2, '虹桥', 0, '园内代表性地标，适合拍照和观察宋风街景。', '/images/qingming-hongqiao.jpg', 40, '地标,拍照,宋风', 1, 3, 4.30, 3.90, 1.20, 4.20, 4.90, 3.80, '2026-03-23 13:35:00', '2026-03-23 13:35:00');
INSERT INTO `small_scenic_spot` VALUES (3, 2, '东京码头', 0, '适合观景和感受水岸氛围，也是夜游较受欢迎的点位。', '/images/qingming-dock.jpg', 45, '观景,夜游,水岸', 1, 2, 4.20, 3.80, 1.50, 4.00, 4.70, 3.90, '2026-03-23 13:35:00', '2026-03-23 13:35:00');
INSERT INTO `small_scenic_spot` VALUES (4, 3, '龙亭大殿', 0, '龙亭景区核心建筑，适合观景和了解古都历史。', '/images/longting-hall.jpg', 35, '历史,核心景点,观景', 1, 2, 4.00, 4.20, 1.00, 4.70, 4.60, 3.80, '2026-03-23 13:35:00', '2026-03-23 13:35:00');
INSERT INTO `small_scenic_spot` VALUES (5, 3, '杨家湖观景带', 0, '适合散步、拍照和轻松游览。', '/images/yangjiahu.jpg', 40, '湖景,散步,拍照', 1, 1, 4.10, 4.50, 2.00, 3.40, 4.70, 4.20, '2026-03-23 13:35:00', '2026-03-23 13:35:00');
INSERT INTO `small_scenic_spot` VALUES (6, 4, '府衙正厅', 0, '开封府核心参观点，适合了解衙署格局。', '/images/kaifengfu-mainhall.jpg', 30, '府衙,历史,文化', 1, 2, 3.80, 4.10, 0.80, 4.80, 4.00, 3.60, '2026-03-23 13:35:00', '2026-03-23 13:35:00');
INSERT INTO `small_scenic_spot` VALUES (7, 4, '包公断案演艺区', 0, '适合观看互动演艺，沉浸感较强。', '/images/kaifengfu-performance.jpg', 45, '演艺,互动,包公', 2, 3, 4.30, 3.70, 0.60, 4.70, 4.30, 3.20, '2026-03-23 13:35:00', '2026-03-23 13:35:00');
INSERT INTO `small_scenic_spot` VALUES (8, 5, '大雄宝殿', 0, '大相国寺主要礼佛与参观区域。', '/images/daxiangguosi-mainhall.jpg', 25, '寺庙,礼佛,人文', 1, 1, 3.60, 4.70, 0.80, 4.80, 3.50, 3.80, '2026-03-23 13:35:00', '2026-03-23 13:35:00');
INSERT INTO `small_scenic_spot` VALUES (9, 5, '千手千眼观音殿', 0, '人文特征突出，适合安静参观。', '/images/daxiangguosi-guanyin.jpg', 25, '佛教文化,静态参观', 1, 1, 3.50, 4.80, 0.60, 4.90, 3.40, 3.90, '2026-03-23 13:35:00', '2026-03-23 13:35:00');
INSERT INTO `small_scenic_spot` VALUES (10, 6, '铁塔主景点', 0, '景区核心打卡点，适合观塔与拍照。', '/images/iron-pagoda-main.jpg', 35, '古塔,打卡,拍照', 1, 2, 4.00, 4.60, 1.20, 4.40, 4.80, 4.10, '2026-03-23 13:35:00', '2026-03-23 13:35:00');
INSERT INTO `small_scenic_spot` VALUES (11, 6, '塔影步道', 0, '适合慢走和拍照，整体节奏轻松。', '/images/iron-pagoda-path.jpg', 30, '散步,拍照,轻松', 1, 1, 4.10, 4.70, 1.80, 3.60, 4.60, 4.50, '2026-03-23 13:35:00', '2026-03-23 13:35:00');
INSERT INTO `small_scenic_spot` VALUES (12, 7, '中门', 1, '万岁山武侠城常用入园口之一，公开游玩攻略常建议从中门入园。', '/images/wansuishan-gate.jpg', 20, '中门,入口,导览,服务', 1, 2, 4.30, 3.80, 0.50, 2.20, 3.50, 4.30, '2026-03-23 13:35:00', '2026-03-24 18:30:00');
INSERT INTO `small_scenic_spot` VALUES (13, 7, '三打祝家庄演艺场', 0, '热门实景演艺区域，适合沉浸式观看表演。', '/images/wansuishan-performance.jpg', 60, '演艺,武侠,热门', 2, 4, 4.70, 3.40, 0.70, 4.10, 4.90, 3.20, '2026-03-23 13:35:00', '2026-03-23 13:35:00');
INSERT INTO `small_scenic_spot` VALUES (14, 7, '打铁花观演区', 0, '夜间氛围突出，适合拍照和收尾观演。', '/images/wansuishan-fireworks.jpg', 45, '夜场,拍照,演艺', 1, 4, 4.50, 3.30, 0.60, 3.80, 5.00, 3.10, '2026-03-23 13:35:00', '2026-03-23 13:35:00');
INSERT INTO `small_scenic_spot` VALUES (15, 9, '包公祠山门', 1, '包公祠主要入园与导览节点。', '/images/baogongci-gate.jpg', 15, '入口,导览,服务', 1, 1, 3.80, 4.20, 0.50, 2.50, 3.20, 4.40, '2026-03-24 10:10:00', '2026-03-24 10:10:00');
INSERT INTO `small_scenic_spot` VALUES (16, 9, '包拯纪念主殿', 0, '包公祠核心参观点，适合了解包拯生平和清官文化。', '/images/baogongci-mainhall.jpg', 35, '包公,主殿,历史,人文', 1, 2, 3.90, 4.30, 0.60, 4.90, 4.10, 3.80, '2026-03-24 10:10:00', '2026-03-24 10:10:00');
INSERT INTO `small_scenic_spot` VALUES (17, 10, '天波楼', 0, '天波杨府标志性建筑，适合观景和打卡。', '/images/tianboyangfu-tower.jpg', 30, '地标,杨家将,拍照', 1, 2, 4.10, 3.90, 1.00, 4.60, 4.70, 3.60, '2026-03-24 10:10:00', '2026-03-24 10:10:00');
INSERT INTO `small_scenic_spot` VALUES (18, 10, '杨家将演武场', 0, '适合观看主题演艺或了解杨家将文化。', '/images/tianboyangfu-stage.jpg', 40, '演艺,杨家将,互动', 2, 3, 4.30, 3.70, 0.70, 4.70, 4.40, 3.20, '2026-03-24 10:10:00', '2026-03-24 10:10:00');
INSERT INTO `small_scenic_spot` VALUES (19, 11, '碑廊主展区', 0, '中国翰园碑林核心碑刻展示区，适合书法文化参观。', '/images/hanyuan-gallery.jpg', 45, '碑刻,书法,文化', 1, 1, 3.60, 4.30, 1.20, 4.90, 4.30, 3.80, '2026-03-24 10:10:00', '2026-03-24 10:10:00');
INSERT INTO `small_scenic_spot` VALUES (20, 11, '翰园湖景区', 0, '适合散步、拍照和放松。', '/images/hanyuan-lake.jpg', 35, '湖景,散步,拍照', 1, 1, 3.90, 4.50, 2.80, 3.60, 4.80, 4.40, '2026-03-24 10:10:00', '2026-03-24 10:10:00');
INSERT INTO `small_scenic_spot` VALUES (21, 12, '基本陈列展厅', 0, '系统展示开封历史发展脉络的主要展厅。', '/images/museum-history-hall.jpg', 60, '展厅,历史,宋文化', 1, 2, 4.50, 4.70, 0.40, 5.00, 4.00, 4.60, '2026-03-24 10:10:00', '2026-03-24 10:10:00');
INSERT INTO `small_scenic_spot` VALUES (22, 12, '北宋东京城专题展区', 0, '适合深入了解东京城格局、考古与宋都文化。', '/images/museum-song-zone.jpg', 45, '宋都,考古,专题展', 1, 2, 4.40, 4.70, 0.30, 5.00, 4.10, 4.50, '2026-03-24 10:10:00', '2026-03-24 10:10:00');
INSERT INTO `small_scenic_spot` VALUES (23, 13, '御碑亭', 0, '禹王台公园内较具代表性的历史点位。', '/images/yuwangtai-stele.jpg', 20, '碑亭,古迹,历史', 1, 1, 3.70, 4.50, 1.00, 4.40, 4.00, 4.20, '2026-03-24 10:10:00', '2026-03-24 10:10:00');
INSERT INTO `small_scenic_spot` VALUES (24, 13, '古吹台园林步道', 0, '适合慢走休闲、感受公园景观氛围。', '/images/yuwangtai-path.jpg', 35, '园林,散步,休闲', 1, 1, 4.00, 4.70, 2.60, 3.80, 4.30, 4.80, '2026-03-24 10:10:00', '2026-03-24 10:10:00');
INSERT INTO `small_scenic_spot` VALUES (25, 14, '繁塔塔身观赏区', 0, '繁塔主体观赏区域，适合古建拍照与打卡。', '/images/fanta-main.jpg', 25, '古塔,古建,打卡', 1, 1, 3.30, 4.10, 0.80, 4.80, 4.70, 3.20, '2026-03-24 10:10:00', '2026-03-24 10:10:00');
INSERT INTO `small_scenic_spot` VALUES (26, 14, '佛砖碑刻区', 0, '可观察繁塔佛砖与碑刻细节，适合历史爱好者。', '/images/fanta-brick.jpg', 25, '佛砖,碑刻,文物', 1, 1, 3.20, 4.20, 0.60, 4.90, 4.30, 3.10, '2026-03-24 10:10:00', '2026-03-24 10:10:00');
INSERT INTO `small_scenic_spot` VALUES (27, 2, '金水门', 1, '清明上河园西门，靠近内顺城路一侧，公开停车与换票信息常以该门为标识。', NULL, 15, '西门,入口,服务,导览', 1, 1, 4.00, 4.20, 0.60, 3.60, 3.80, 4.30, '2026-03-24 18:30:00', '2026-03-24 18:30:00');
INSERT INTO `small_scenic_spot` VALUES (28, 2, '端门', 1, '清明上河园常用入园口之一，公开交通与停车信息中长期作为可导航入口出现。', NULL, 15, '入口,服务,导览', 1, 1, 4.00, 4.20, 0.60, 3.80, 3.80, 4.20, '2026-03-24 18:30:00', '2026-03-24 18:30:00');
INSERT INTO `small_scenic_spot` VALUES (29, 2, '丹凤门', 1, '清明上河园公开停车信息涉及的入口之一，与端门同属北侧片区。', NULL, 15, '入口,服务,导览', 1, 1, 4.00, 4.20, 0.60, 3.90, 3.90, 4.20, '2026-03-24 18:30:00', '2026-03-24 18:30:00');
INSERT INTO `small_scenic_spot` VALUES (30, 2, '通津门', 1, '清明上河园五个公开入口之一，位于御河岸边，属于水路通道。', NULL, 10, '水门,入口,御河,导览', 1, 1, 3.80, 4.00, 1.20, 4.20, 4.20, 4.00, '2026-03-24 18:30:00', '2026-03-24 18:30:00');
INSERT INTO `small_scenic_spot` VALUES (31, 3, '午门', 1, '龙亭景区南大门，位于景区中轴线南端。', NULL, 15, '南门,午门,入口,导览', 1, 1, 4.00, 4.30, 0.60, 4.60, 4.20, 4.20, '2026-03-24 18:30:00', '2026-03-24 18:30:00');
INSERT INTO `small_scenic_spot` VALUES (32, 3, '北门', 1, '龙亭景区北门，属于景区公开手绘地图中的出入口之一。', NULL, 15, '北门,入口,导览', 1, 1, 4.00, 4.30, 0.80, 4.20, 4.10, 4.20, '2026-03-24 18:30:00', '2026-03-24 18:30:00');
INSERT INTO `small_scenic_spot` VALUES (33, 3, '东便门', 1, '龙亭景区东便门，属于景区公开手绘地图中的出入口之一。', NULL, 15, '东门,便门,入口,导览', 1, 1, 4.00, 4.30, 0.80, 4.20, 4.20, 4.10, '2026-03-24 18:30:00', '2026-03-24 18:30:00');
INSERT INTO `small_scenic_spot` VALUES (34, 4, '府门', 1, '开封府正门，游客通常由此进入景区开始游览。', NULL, 15, '正门,府门,入口,导览', 1, 1, 4.00, 4.20, 0.40, 4.80, 4.20, 4.10, '2026-03-24 18:30:00', '2026-03-24 18:30:00');
INSERT INTO `small_scenic_spot` VALUES (35, 5, '山门', 1, '大相国寺山门，属于寺院传统主入口。', NULL, 15, '山门,入口,寺庙,导览', 1, 1, 3.90, 4.50, 0.40, 4.90, 3.80, 4.10, '2026-03-24 18:30:00', '2026-03-24 18:30:00');
INSERT INTO `small_scenic_spot` VALUES (36, 7, '南门', 1, '万岁山武侠城公开入口之一。', NULL, 15, '南门,入口,导览', 1, 2, 4.20, 4.10, 0.50, 3.80, 3.90, 4.20, '2026-03-24 18:30:00', '2026-03-24 18:30:00');
INSERT INTO `small_scenic_spot` VALUES (37, 7, '北大门', 1, '万岁山武侠城公开入口之一。', NULL, 15, '北门,北大门,入口,导览', 1, 2, 4.20, 4.10, 0.50, 3.80, 4.00, 4.20, '2026-03-24 18:30:00', '2026-03-24 18:30:00');
INSERT INTO `small_scenic_spot` VALUES (38, 7, '东大门', 1, '万岁山武侠城公开入口之一。', NULL, 15, '东门,东大门,入口,导览', 1, 2, 4.20, 4.10, 0.50, 3.80, 4.00, 4.10, '2026-03-24 18:30:00', '2026-03-24 18:30:00');
INSERT INTO `small_scenic_spot` VALUES (39, 11, '南大门', 1, '中国翰园碑林公开游玩路线中的主要起点之一。', NULL, 15, '南门,南大门,入口,导览', 1, 1, 4.00, 4.40, 0.80, 4.70, 4.30, 4.20, '2026-03-24 18:30:00', '2026-03-24 18:30:00');
INSERT INTO `small_scenic_spot` VALUES (40, 11, '北大门', 1, '中国翰园碑林公开游玩路线中的出口/入口之一。', NULL, 15, '北门,北大门,入口,导览', 1, 1, 4.00, 4.40, 0.80, 4.60, 4.20, 4.20, '2026-03-24 18:30:00', '2026-03-24 18:30:00');
INSERT INTO `small_scenic_spot` VALUES (41, 13, '东门', 1, '禹王台公园公开开园公告中明确提到的入口。', NULL, 15, '东门,入口,导览', 1, 1, 4.10, 4.50, 1.00, 4.30, 4.10, 4.40, '2026-03-24 18:30:00', '2026-03-24 18:30:00');
INSERT INTO `small_scenic_spot` VALUES (42, 13, '西门', 1, '禹王台公园公开开园公告中明确提到的入口。', NULL, 15, '西门,入口,导览', 1, 1, 4.10, 4.50, 1.00, 4.30, 4.10, 4.40, '2026-03-24 18:30:00', '2026-03-24 18:30:00');

-- ----------------------------
-- Table structure for users
-- ----------------------------
DROP TABLE IF EXISTS `users`;
CREATE TABLE `users`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `username` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `email` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_username`(`username`) USING BTREE,
  UNIQUE INDEX `uk_email`(`email`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of users
-- ----------------------------
INSERT INTO `users` VALUES (3, 'zhangsan', '$2a$10$LvRbbJ64AmnN6PbiudVpTeZbdbyVvfj2vH8vottTVP2Kz1/.kBekK', '123132412946@qq.com');
INSERT INTO `users` VALUES (4, 'shi', '$2a$10$IBx7GfzuRxT25cD/qKxpoehFnlj0/KGQNeevGs/hnYOMT4i/ckpmy', '12312312@qq.com');

SET FOREIGN_KEY_CHECKS = 1;
