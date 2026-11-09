-- 说明：这是 note 单体的建表 + 种子数据脚本。
-- 直接执行即可创建/切换到 CipherWS 库；脚本会 DROP 并重建表，请先备份存量数据。
CREATE DATABASE IF NOT EXISTS `CipherWS`
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
USE `CipherWS`;
/*
 Navicat Premium Data Transfer

 Source Server         : localhost_3306
 Source Server Type    : MySQL
 Source Server Version : 80046
 Source Host           : localhost:3306
 Source Schema         : cipherws

 Target Server Type    : MySQL
 Target Server Version : 80046
 File Encoding         : 65001

 Date: 09/11/2026 10:56:39
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for note
-- ----------------------------
DROP TABLE IF EXISTS `note`;
CREATE TABLE `note`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '笔记 ID',
  `user_id` bigint NOT NULL COMMENT '所属用户',
  `folder_id` bigint NULL DEFAULT NULL COMMENT '所属文件夹，NULL 表示未分类',
  `title` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '无标题笔记' COMMENT '标题',
  `content` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '正文，富文本/纯文本',
  `status` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'private' COMMENT '状态：draft 草稿 / private 私有已发布',
  `deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否在回收站',
  `flagged` tinyint(1) NOT NULL DEFAULT 0 COMMENT '管理员是否标记违规',
  `deleted_at` datetime NULL DEFAULT NULL COMMENT '进入回收站的时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_note_user_deleted_updated`(`user_id`, `deleted`, `updated_at`) USING BTREE,
  INDEX `idx_note_flagged`(`flagged`) USING BTREE,
  INDEX `idx_note_folder`(`user_id`, `folder_id`, `deleted`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 85 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '笔记表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of note
-- ----------------------------
INSERT INTO `note` VALUES (1, 5, NULL, 'JAVA', '这份笔记是一份从基础语法到高级特性的 Java 学习路线式知识清单。要点如下：\n\n- **基础语法**：变量声明与赋值、算术运算符（含自增自减前置/后置区别）、比较与逻辑运算符（含短路）、三元运算符。\n- **流程控制**：if/switch 选择结构（含 case 穿透、default）；for/while/do-while 循环及 break、continue、嵌套循环。\n- **数组**：声明创建、静态/动态初始化、索引访问、遍历、二维数组。\n- **方法**：定义调用、参数返回值、重载、递归；局部变量与成员变量的区分。\n- **数据类型**：8 种基本类型；以 String 为代表的引用类型及其常用操作（创建、拼接、截取、查找、替换、大小写转换、去空格、内容比较）。\n- **面向对象**：类与对象、构造方法、this、封装、继承与方法重写、super、多态与类型转换、instanceof、抽象类/方法、接口（默认/静态方法、常量、多实现）、final 与 static（含代码块执行顺序）、包与导包、四种权限修饰符、四种内部类。\n- **异常处理**：try-catch-finally、throw/throws、自定义异常、运行时与编译时异常及常见异常类型。\n- **常用类与工具**：包装类与自动装箱拆箱、Math、Random、日期时间基础。\n- **集合**：ArrayList、LinkedList、HashSet、HashMap（含 entrySet 遍历）、泛型与通配符、增强 for、可变参数、枚举、注解。\n- **Object 类与设计模式**：equals、hashCode、toString、getClass、克隆（浅/深拷贝）；单例（懒汉/饿汉）、简单工厂。\n- **IO 流**：字节流、字符流、File 操作、文件读写复制、缓冲流、转换流、序列化与反序列化。\n- **多线程**：Thread/Runnable 两种创建方式、生命周期、sleep/join、线程安全（同步代码块/方法、Lock）、死锁、wait/notify、并发三大特性、volatile。\n- **反射**：获取 Class 对象、创建实例、调用方法、获取字段。\n- **函数式编程**：Lambda、函数式接口、方法引用、Stream（filter/map/collect/reduce、排序、中间与终端操作）、Optional。\n- **网络编程**：Socket、ServerSocket、客户端/服务端、TCP/UDP、IP、端口、域名、URL。\n- **正则表达式**：Pattern、Matcher、字符串匹配/替换/分割。', 'draft', 0, 0, NULL, '2026-10-25 21:15:34', '2026-10-25 23:32:52');
INSERT INTO `note` VALUES (56, 5, NULL, 'python', 'int', 'draft', 0, 0, NULL, '2026-10-25 22:39:54', '2026-10-25 22:40:06');
INSERT INTO `note` VALUES (57, 5, NULL, 'c', 'main', 'draft', 0, 0, NULL, '2026-10-25 22:40:06', '2026-10-25 22:41:11');

-- ----------------------------
-- Table structure for note_admin_operation_log
-- ----------------------------
DROP TABLE IF EXISTS `note_admin_operation_log`;
CREATE TABLE `note_admin_operation_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `admin_id` bigint NOT NULL,
  `type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `detail` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '',
  `ip` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_alog_admin_created`(`admin_id`, `created_at`) USING BTREE,
  INDEX `idx_alog_type`(`type`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '管理员操作日志表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of note_admin_operation_log
-- ----------------------------
INSERT INTO `note_admin_operation_log` VALUES (1, 1, '修改 AI 配置', '模型 qwen-plus -> deepseek-v4.1-flash，最大字数 1200 -> 1500，密钥已更新', '10.6.36.47', '2026-10-25 22:18:44');

-- ----------------------------
-- Table structure for note_ai_config
-- ----------------------------
DROP TABLE IF EXISTS `note_ai_config`;
CREATE TABLE `note_ai_config`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `endpoint` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'OpenAI 兼容的 chat/completions 地址',
  `api_key` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '接口密钥，禁止回传给前端',
  `model` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'qwen-plus',
  `max_chars` int NOT NULL DEFAULT 1200 COMMENT '单次总结最大字数',
  `updated_by` bigint NULL DEFAULT NULL COMMENT '最后修改的管理员',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'AI 配置表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of note_ai_config
-- ----------------------------
INSERT INTO `note_ai_config` VALUES (1, 'https://api.deepseek.com/chat/completions', 'sk-72f2c5cd8bc44003b8779c39efce44fa', 'deepseek-chat', 1500, 1, '2026-10-25 22:20:38');

-- ----------------------------
-- Table structure for note_ai_summary_record
-- ----------------------------
DROP TABLE IF EXISTS `note_ai_summary_record`;
CREATE TABLE `note_ai_summary_record`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `note_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `source_snippet` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '提交总结的原文片段',
  `summary` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'AI 返回的摘要',
  `quota_cost` int NOT NULL DEFAULT 1 COMMENT '本次消耗额度',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_ai_user_created`(`user_id`, `created_at`) USING BTREE,
  INDEX `idx_ai_note`(`note_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 13 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'AI 总结记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of note_ai_summary_record
-- ----------------------------
INSERT INTO `note_ai_summary_record` VALUES (10, 1, 5, '变量声明赋值运算符算术运算符加号减号乘号除号取模自增自减前置后置比较运算符等于不等大于小于大于等于小于等于逻辑运算符与或非短路与短路或三元运算符选择结构if语句ifelse语句嵌套ifswitch语句case穿透default分支循环结构for循环while循环dowhile循环break语句continue语句嵌套循环数组声明创建初始化静态初始化动态初始化数组长度索引访问遍历数组二维数组方法定义调用参数返回值重载递归局部变量成员变量基本数据类型byte short int long float double char boolean引用数据类型String类字符串创建拼接长度获取字符截取子串查找替换转换大小写去除空格比较内容相等忽略大小写面向对象类对象实例化构造方法无参构造有参构造this关键字封装private修饰符getter方法setter方法继承extends子类父类方法重写super关键字多态向上转型向下转型instanceof抽象类abstract抽象方法接口interface实现implements默认方法静态方法常量多接口实现final关键字修饰变量修饰方法修饰类st', '**主旨：** 这份笔记系统梳理了Java编程语言从基础语法到高级特性的核心知识点，涵盖变量、运算符、流程控制、数组、面向对象、异常、集合、IO、多线程、反射、Lambda、网络编程等内容，是一份Java学习路线式的知识清单。\n\n**要点：**\n\n1. **基础语法**：包括变量声明与赋值、算术运算符（含自增自减的前置/后置区别）、比较运算符、逻辑运算符（含短路与/短路或）、三元运算符。\n\n2. **流程控制**：选择结构（if、if-else、嵌套if、switch及case穿透、default分支）；循环结构（for、while、do-while、break、continue、嵌套循环）。\n\n3. **数组**：声明、创建、静态/动态初始化、长度与索引访问、遍历、二维数组。\n\n4. **方法**：定义、调用、参数、返回值、重载、递归；区分局部变量与成员变量。\n\n5. **数据类型**：8种基本数据类型；引用类型以String为代表，涉及字符串创建、拼接、长度、截取、查找、替换、大小写转换、去空格、内容比较及忽略大小写比较。\n\n6. **面向对象**：类与对象、实例化、构造方法（无参/有参）、this关键字、封装（private、getter/setter）、继承（extends、方法重写、super）、多态（向上/向下转型、instanceof）、抽象类与抽象方法、接口（implements、默认方法、静态方法、常量、多接口实现）、final与static关键字（静态变量/方法/代码块、代码块执行顺序）、包与导包、四种权限修饰符、四种内部类。\n\n7. **异常处理**：try-catch-finally、throw/throws、自定义异常、运行时与编译时异常、常见异常（空指针、数组越界、类型转换、算术、字符串转数字）。\n\n8. **常用类与工具**：包装类及自动装箱拆箱、Math类、Random类、日期时间基础使用。\n\n9. **集合**：ArrayList（增删改查、遍历）、LinkedList、HashSet、HashMap（put/get/containsKey、entrySet遍历）、泛型（类/方法/接口、通配符）、增强for循环、可变参数、枚举、注解。\n\n10. **Object类与设计模式**：equals、hashCode、toString、getClass、克隆（浅拷贝/深拷贝）；单例模式（懒汉式/饿汉式）、工厂模式（简单工厂）。\n\n11. **IO流**：字节流、字符流、File类操作、文件读写与复制、缓冲流、转换流、序列化与反序列化。\n\n12. **多线程**：两种创建方式（继承Thread、实现Runnable）、线程生命周期、启动、sleep、join、线程安全（同步代码块/方法、Lock）、死锁、线程通信（wait/notify）、并发三大特性（原子性、可见性、有序性）、volatile关键字。\n\n13. **反射**：获取Class对象、创建实例、调用方法、获取字段。\n\n14. **函数式编程**：Lambda表达式、函数式接口、方法引用、Stream流（filter/map/collect/reduce、排序、中间操作与终端操作）、Optional类。\n\n15. **网络编程**：Socket、ServerSocket、客户端/服务端、TCP/UDP协议、IP、端口、域名、URL。\n\n16. **正则表达式**：Pattern、Matcher、字符串匹配/替换/分割。', 1, '2026-10-25 22:21:57');
INSERT INTO `note_ai_summary_record` VALUES (12, 1, 5, '**主旨： 这份笔记系统梳理了Java编程语言从基础语法到高级特性的核心知识点，涵盖变量、运算符、流程控制、数组、面向对象、异常、集合、IO、多线程、反射、Lambda、网络编程等内容，是一份Java学习路线式的知识清单。**\n\n**\n**\n\n**要点：**\n\n**\n**\n\n**基础语法：包括变量声明与赋值、算术运算符（含自增自减的前置/后置区别）、比较运算符、逻辑运算符（含短路与/短路或）、三元运算符。**\n\n**流程控制：选择结构（if、if-else、嵌套if、switch及case穿透、default分支）；循环结构（for、while、do-while、break、continue、嵌套循环）。**\n\n**数组：声明、创建、静态/动态初始化、长度与索引访问、遍历、二维数组。**\n\n**方法：定义、调用、参数、返回值、重载、递归；区分局部变量与成员变量。**\n\n**数据类型：8种基本数据类型；引用类型以String为代表，涉及字符串创建、拼接、长度、截取、查找、替换、大小写转换、去空格、内容比较及忽略大小写比较。**\n\n**面向对象：类与对象、实例化、构造方法（无参/有参）、t', '这份笔记是一份从基础语法到高级特性的 Java 学习路线式知识清单。要点如下：\n\n- **基础语法**：变量声明与赋值、算术运算符（含自增自减前置/后置区别）、比较与逻辑运算符（含短路）、三元运算符。\n- **流程控制**：if/switch 选择结构（含 case 穿透、default）；for/while/do-while 循环及 break、continue、嵌套循环。\n- **数组**：声明创建、静态/动态初始化、索引访问、遍历、二维数组。\n- **方法**：定义调用、参数返回值、重载、递归；局部变量与成员变量的区分。\n- **数据类型**：8 种基本类型；以 String 为代表的引用类型及其常用操作（创建、拼接、截取、查找、替换、大小写转换、去空格、内容比较）。\n- **面向对象**：类与对象、构造方法、this、封装、继承与方法重写、super、多态与类型转换、instanceof、抽象类/方法、接口（默认/静态方法、常量、多实现）、final 与 static（含代码块执行顺序）、包与导包、四种权限修饰符、四种内部类。\n- **异常处理**：try-catch-finally、throw/throws、自定义异常、运行时与编译时异常及常见异常类型。\n- **常用类与工具**：包装类与自动装箱拆箱、Math、Random、日期时间基础。\n- **集合**：ArrayList、LinkedList、HashSet、HashMap（含 entrySet 遍历）、泛型与通配符、增强 for、可变参数、枚举、注解。\n- **Object 类与设计模式**：equals、hashCode、toString、getClass、克隆（浅/深拷贝）；单例（懒汉/饿汉）、简单工厂。\n- **IO 流**：字节流、字符流、File 操作、文件读写复制、缓冲流、转换流、序列化与反序列化。\n- **多线程**：Thread/Runnable 两种创建方式、生命周期、sleep/join、线程安全（同步代码块/方法、Lock）、死锁、wait/notify、并发三大特性、volatile。\n- **反射**：获取 Class 对象、创建实例、调用方法、获取字段。\n- **函数式编程**：Lambda、函数式接口、方法引用、Stream（filter/map/collect/reduce、排序、中间与终端操作）、Optional。\n- **网络编程**：Socket、ServerSocket、客户端/服务端、TCP/UDP、IP、端口、域名、URL。\n- **正则表达式**：Pattern、Matcher、字符串匹配/替换/分割。', 1, '2026-10-25 22:26:23');

-- ----------------------------
-- Table structure for note_folder
-- ----------------------------
DROP TABLE IF EXISTS `note_folder`;
CREATE TABLE `note_folder`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '文件夹 ID',
  `user_id` bigint NOT NULL COMMENT '所属用户',
  `name` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '文件夹名，最多 20 字符',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_folder_user_name`(`user_id`, `name`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 19 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '笔记文件夹表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of note_folder
-- ----------------------------

-- ----------------------------
-- Table structure for note_system_announcement
-- ----------------------------
DROP TABLE IF EXISTS `note_system_announcement`;
CREATE TABLE `note_system_announcement`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `admin_id` bigint NOT NULL COMMENT '发布的管理员',
  `enabled` tinyint(1) NOT NULL DEFAULT 1,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_ann_enabled_created`(`enabled`, `created_at`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '系统公告表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of note_system_announcement
-- ----------------------------
INSERT INTO `note_system_announcement` VALUES (1, '笔记系统上线', '支持笔记编写、标签分类、回收站与 AI 智能总结。AI 总结每次消耗 1 次额度，额度可在个人资料页查看。', 1, 1, '2026-10-19 19:28:31', '2026-10-19 19:28:31');

-- ----------------------------
-- Table structure for note_tag
-- ----------------------------
DROP TABLE IF EXISTS `note_tag`;
CREATE TABLE `note_tag`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '所属用户',
  `name` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '标签名，最多 12 字符',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_tag_user_name`(`user_id`, `name`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 17 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '标签表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of note_tag
-- ----------------------------
INSERT INTO `note_tag` VALUES (1, 5, 'JAVA', '2026-10-25 22:41:04');

-- ----------------------------
-- Table structure for note_tag_rel
-- ----------------------------
DROP TABLE IF EXISTS `note_tag_rel`;
CREATE TABLE `note_tag_rel`  (
  `note_id` bigint NOT NULL,
  `tag_id` bigint NOT NULL,
  PRIMARY KEY (`note_id`, `tag_id`) USING BTREE,
  INDEX `idx_rel_tag`(`tag_id`) USING BTREE,
  CONSTRAINT `fk_rel_note` FOREIGN KEY (`note_id`) REFERENCES `note` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_rel_tag` FOREIGN KEY (`tag_id`) REFERENCES `note_tag` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '笔记标签关联表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of note_tag_rel
-- ----------------------------
INSERT INTO `note_tag_rel` VALUES (57, 1);

-- ----------------------------
-- Table structure for note_user
-- ----------------------------
DROP TABLE IF EXISTS `note_user`;
CREATE TABLE `note_user`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户 ID',
  `username` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '显示名，由邮箱前缀生成，不要求唯一',
  `password` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'BCrypt 密码摘要，禁止明文',
  `role` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'user' COMMENT '角色：user / admin',
  `enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用，0 禁用后不能登录',
  `email` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '登录账号，全库唯一',
  `nickname` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '昵称',
  `avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '头像地址',
  `bio` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '个人简介',
  `ai_quota` int NOT NULL DEFAULT 5 COMMENT '剩余 AI 总结次数',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_email`(`email`) USING BTREE,
  INDEX `idx_user_username`(`username`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 9 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of note_user
-- ----------------------------
INSERT INTO `note_user` VALUES (1, 'admin', '$2a$10$92FF7YqFlVJUz8/v.orl5.YwxrH0vpsyezysBm1PpmjDBNhu8w4RK', 'admin', 1, 'admin@cipherws.icu', '站点管理员', '', '负责用户审核、笔记巡检与 AI 参数配置。', 100, '2026-10-19 19:28:31', '2026-10-25 22:21:11');
INSERT INTO `note_user` VALUES (2, 'demo', '$2a$10$Pcp3wsX/qPs3jiPDYVECc.ynQNPebioTtl5Z7rOZY6nvNsSSIlSd6', 'user', 1, 'demo@cipherws.icu', '池禾', '/uploads/avatar/2-896091cc435e443a.png', '毕业设计中，用这个笔记本记技术笔记和灵感。', 8, '2026-10-19 19:28:31', '2026-10-20 00:00:53');
INSERT INTO `note_user` VALUES (5, '2435182981', '$2a$10$VTRgaFZkZnBNfhcYa5ZcIO81lkqud5IP79Wa6iG4S5z1/hL7CElou', 'user', 1, '2435182981@qq.com', '刘振宇', '/uploads/avatar/5-5020ec15b2984d18.png', '', 2, '2026-10-21 18:57:13', '2026-10-25 22:36:05');

-- ----------------------------
-- Table structure for note_user_operation_log
-- ----------------------------
DROP TABLE IF EXISTS `note_user_operation_log`;
CREATE TABLE `note_user_operation_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '操作类型',
  `detail` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '操作详情',
  `ip` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '来源 IP',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_ulog_user_created`(`user_id`, `created_at`) USING BTREE,
  INDEX `idx_ulog_type`(`type`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 117 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户操作日志表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of note_user_operation_log
-- ----------------------------
INSERT INTO `note_user_operation_log` VALUES (1, 2, '登录', '账号登录成功', '', '2026-10-19 21:30:23');
INSERT INTO `note_user_operation_log` VALUES (2, 2, '登录', '账号登录成功', '', '2026-10-19 21:31:22');
INSERT INTO `note_user_operation_log` VALUES (3, 2, '登录', '账号登录成功', '', '2026-10-19 21:31:51');
INSERT INTO `note_user_operation_log` VALUES (4, 2, '登录', '账号登录成功', '', '2026-10-19 21:32:17');
INSERT INTO `note_user_operation_log` VALUES (5, 2, '登录', '账号登录成功', '', '2026-10-19 22:48:04');
INSERT INTO `note_user_operation_log` VALUES (6, 2, '登录', '账号登录成功', '', '2026-10-20 00:00:45');
INSERT INTO `note_user_operation_log` VALUES (7, 2, '登录', '账号登录成功', '', '2026-10-20 00:00:53');
INSERT INTO `note_user_operation_log` VALUES (8, 2, '更换头像', '上传了新头像', '', '2026-10-20 00:00:53');
INSERT INTO `note_user_operation_log` VALUES (9, 3, '登录', '邮箱登录成功', '', '2026-10-20 23:38:32');
INSERT INTO `note_user_operation_log` VALUES (10, 2, '登录', '邮箱登录成功', '', '2026-10-20 23:38:32');
INSERT INTO `note_user_operation_log` VALUES (11, 2, '登录', '邮箱登录成功', '', '2026-10-20 23:40:03');
INSERT INTO `note_user_operation_log` VALUES (12, 4, '登录', '邮箱登录成功', '', '2026-10-20 23:41:55');
INSERT INTO `note_user_operation_log` VALUES (13, 5, '登录', '邮箱登录成功', '', '2026-10-21 18:57:24');
INSERT INTO `note_user_operation_log` VALUES (14, 5, '更换头像', '上传了新头像', '', '2026-10-21 18:57:54');
INSERT INTO `note_user_operation_log` VALUES (15, 1, '登录', '邮箱登录成功', '', '2026-10-21 19:01:08');
INSERT INTO `note_user_operation_log` VALUES (16, 5, '登录', '邮箱登录成功', '', '2026-10-21 19:03:26');
INSERT INTO `note_user_operation_log` VALUES (17, 2, '登录', '邮箱登录成功', '', '2026-10-21 19:11:23');
INSERT INTO `note_user_operation_log` VALUES (18, 6, '登录', '邮箱登录成功', '', '2026-10-21 19:11:43');
INSERT INTO `note_user_operation_log` VALUES (19, 6, '登录', '邮箱登录成功', '', '2026-10-21 19:12:16');
INSERT INTO `note_user_operation_log` VALUES (20, 8, '登录', '邮箱登录成功', '', '2026-10-21 19:14:43');
INSERT INTO `note_user_operation_log` VALUES (21, 9, '登录', '邮箱登录成功', '', '2026-10-21 19:16:34');
INSERT INTO `note_user_operation_log` VALUES (22, 9, '验证邮箱', '通过验证码验证了当前邮箱 s***@test.local', '', '2026-10-21 19:17:39');
INSERT INTO `note_user_operation_log` VALUES (23, 9, '登录', '邮箱登录成功', '', '2026-10-21 19:18:21');
INSERT INTO `note_user_operation_log` VALUES (24, 9, '登录', '邮箱登录成功', '', '2026-10-21 19:19:57');
INSERT INTO `note_user_operation_log` VALUES (25, 9, '验证邮箱', '通过验证码验证了当前邮箱 s***@test.local', '', '2026-10-21 19:19:59');
INSERT INTO `note_user_operation_log` VALUES (26, 9, '更换绑定邮箱', '邮箱由 s***@test.local 更换为 s***@test.local', '', '2026-10-21 19:20:00');
INSERT INTO `note_user_operation_log` VALUES (27, 9, '登录', '邮箱登录成功', '', '2026-10-21 19:20:39');
INSERT INTO `note_user_operation_log` VALUES (28, 9, '登录', '邮箱登录成功', '', '2026-10-21 19:22:19');
INSERT INTO `note_user_operation_log` VALUES (29, 9, '登录', '邮箱登录成功', '', '2026-10-21 19:24:31');
INSERT INTO `note_user_operation_log` VALUES (30, 9, '登录', '邮箱登录成功', '', '2026-10-21 19:24:51');
INSERT INTO `note_user_operation_log` VALUES (31, 9, '修改密码', '通过邮箱验证码修改了密码', '', '2026-10-21 19:27:01');
INSERT INTO `note_user_operation_log` VALUES (32, 9, '登录', '邮箱登录成功', '', '2026-10-21 19:27:04');
INSERT INTO `note_user_operation_log` VALUES (33, 5, '登录', '邮箱登录成功', '', '2026-10-21 19:43:46');
INSERT INTO `note_user_operation_log` VALUES (34, 2, '登录', '邮箱登录成功', '', '2026-10-21 19:47:00');
INSERT INTO `note_user_operation_log` VALUES (35, 2, '登录', '邮箱登录成功', '', '2026-10-21 19:47:26');
INSERT INTO `note_user_operation_log` VALUES (36, 2, '登录', '邮箱登录成功', '', '2026-10-21 19:50:53');
INSERT INTO `note_user_operation_log` VALUES (37, 2, '登录', '邮箱登录成功', '', '2026-10-21 19:52:07');
INSERT INTO `note_user_operation_log` VALUES (38, 5, '验证邮箱', '通过验证码验证了当前邮箱 2***@qq.com', '', '2026-10-21 19:54:30');
INSERT INTO `note_user_operation_log` VALUES (39, 5, '更换绑定邮箱', '邮箱由 2***@qq.com 更换为 1***@qq.com', '', '2026-10-21 19:55:02');
INSERT INTO `note_user_operation_log` VALUES (40, 5, '修改密码', '通过邮箱验证码修改了密码', '', '2026-10-21 19:56:14');
INSERT INTO `note_user_operation_log` VALUES (41, 5, '登录', '邮箱登录成功', '', '2026-10-21 19:56:28');
INSERT INTO `note_user_operation_log` VALUES (42, 5, '登录', '邮箱登录成功', '', '2026-10-21 19:57:46');
INSERT INTO `note_user_operation_log` VALUES (43, 5, '登录', '邮箱登录成功', '', '2026-10-25 21:06:48');
INSERT INTO `note_user_operation_log` VALUES (44, 5, '验证邮箱', '通过验证码验证了当前邮箱 1***@qq.com', '', '2026-10-25 21:07:11');
INSERT INTO `note_user_operation_log` VALUES (45, 5, '更换绑定邮箱', '邮箱由 1***@qq.com 更换为 2***@qq.com', '', '2026-10-25 21:07:29');
INSERT INTO `note_user_operation_log` VALUES (46, 2, '登录', '邮箱登录成功', '', '2026-10-25 21:29:22');
INSERT INTO `note_user_operation_log` VALUES (47, 2, '登录', '邮箱登录成功', '', '2026-10-25 21:30:02');
INSERT INTO `note_user_operation_log` VALUES (48, 2, '登录', '邮箱登录成功', '', '2026-10-25 21:30:27');
INSERT INTO `note_user_operation_log` VALUES (49, 2, '登录', '邮箱登录成功', '', '2026-10-25 21:37:05');
INSERT INTO `note_user_operation_log` VALUES (50, 2, '登录', '邮箱登录成功', '', '2026-10-25 21:37:16');
INSERT INTO `note_user_operation_log` VALUES (51, 2, '登录', '邮箱登录成功', '', '2026-10-25 21:37:56');
INSERT INTO `note_user_operation_log` VALUES (52, 2, '登录', '邮箱登录成功', '', '2026-10-25 21:45:39');
INSERT INTO `note_user_operation_log` VALUES (53, 2, '登录', '邮箱登录成功', '', '2026-10-25 21:46:08');
INSERT INTO `note_user_operation_log` VALUES (54, 2, '登录', '邮箱登录成功', '', '2026-10-25 21:46:36');
INSERT INTO `note_user_operation_log` VALUES (55, 2, '登录', '邮箱登录成功', '', '2026-10-25 21:46:50');
INSERT INTO `note_user_operation_log` VALUES (56, 2, '登录', '邮箱登录成功', '', '2026-10-25 21:47:33');
INSERT INTO `note_user_operation_log` VALUES (57, 2, '登录', '邮箱登录成功', '', '2026-10-25 21:48:00');
INSERT INTO `note_user_operation_log` VALUES (58, 2, '登录', '邮箱登录成功', '', '2026-10-25 21:48:13');
INSERT INTO `note_user_operation_log` VALUES (59, 2, '登录', '邮箱登录成功', '', '2026-10-25 21:48:32');
INSERT INTO `note_user_operation_log` VALUES (60, 2, '登录', '邮箱登录成功', '', '2026-10-25 21:48:43');
INSERT INTO `note_user_operation_log` VALUES (61, 2, '登录', '邮箱登录成功', '', '2026-10-25 21:49:03');
INSERT INTO `note_user_operation_log` VALUES (62, 2, '登录', '邮箱登录成功', '', '2026-10-25 21:49:20');
INSERT INTO `note_user_operation_log` VALUES (63, 2, '登录', '邮箱登录成功', '', '2026-10-25 21:49:48');
INSERT INTO `note_user_operation_log` VALUES (64, 2, '登录', '邮箱登录成功', '', '2026-10-25 21:50:03');
INSERT INTO `note_user_operation_log` VALUES (65, 2, '登录', '邮箱登录成功', '', '2026-10-25 21:50:27');
INSERT INTO `note_user_operation_log` VALUES (66, 2, '登录', '邮箱登录成功', '', '2026-10-25 21:50:53');
INSERT INTO `note_user_operation_log` VALUES (67, 2, '登录', '邮箱登录成功', '', '2026-10-25 21:51:11');
INSERT INTO `note_user_operation_log` VALUES (68, 2, '登录', '邮箱登录成功', '', '2026-10-25 21:51:32');
INSERT INTO `note_user_operation_log` VALUES (69, 2, '登录', '邮箱登录成功', '', '2026-10-25 21:52:00');
INSERT INTO `note_user_operation_log` VALUES (70, 2, '登录', '邮箱登录成功', '', '2026-10-25 21:52:15');
INSERT INTO `note_user_operation_log` VALUES (71, 2, '登录', '邮箱登录成功', '', '2026-10-25 21:52:37');
INSERT INTO `note_user_operation_log` VALUES (72, 2, '登录', '邮箱登录成功', '', '2026-10-25 21:52:48');
INSERT INTO `note_user_operation_log` VALUES (73, 2, '登录', '邮箱登录成功', '', '2026-10-25 21:53:11');
INSERT INTO `note_user_operation_log` VALUES (74, 2, '登录', '邮箱登录成功', '', '2026-10-25 21:53:40');
INSERT INTO `note_user_operation_log` VALUES (75, 2, '登录', '邮箱登录成功', '', '2026-10-25 21:53:59');
INSERT INTO `note_user_operation_log` VALUES (76, 2, '登录', '邮箱登录成功', '', '2026-10-25 21:54:22');
INSERT INTO `note_user_operation_log` VALUES (77, 2, '登录', '邮箱登录成功', '', '2026-10-25 21:54:38');
INSERT INTO `note_user_operation_log` VALUES (78, 2, '登录', '邮箱登录成功', '', '2026-10-25 21:54:55');
INSERT INTO `note_user_operation_log` VALUES (79, 2, '登录', '邮箱登录成功', '', '2026-10-25 21:55:03');
INSERT INTO `note_user_operation_log` VALUES (80, 2, '登录', '邮箱登录成功', '', '2026-10-25 21:55:13');
INSERT INTO `note_user_operation_log` VALUES (81, 2, '登录', '邮箱登录成功', '', '2026-10-25 22:01:23');
INSERT INTO `note_user_operation_log` VALUES (82, 2, '登录', '邮箱登录成功', '', '2026-10-25 22:01:51');
INSERT INTO `note_user_operation_log` VALUES (83, 1, '登录', '邮箱登录成功', '', '2026-10-25 22:17:01');
INSERT INTO `note_user_operation_log` VALUES (84, 5, '登录', '邮箱登录成功', '', '2026-10-25 22:19:04');
INSERT INTO `note_user_operation_log` VALUES (85, 1, '登录', '邮箱登录成功', '', '2026-10-25 22:20:44');
INSERT INTO `note_user_operation_log` VALUES (86, 1, 'AI 总结', '对《AI ????》调用 AI 总结，消耗 1 次额度', '', '2026-10-25 22:20:45');
INSERT INTO `note_user_operation_log` VALUES (87, 1, '登录', '邮箱登录成功', '', '2026-10-25 22:21:02');
INSERT INTO `note_user_operation_log` VALUES (88, 1, 'AI 总结', '对《AI 中文联调》调用 AI 总结，消耗 1 次额度', '', '2026-10-25 22:21:03');
INSERT INTO `note_user_operation_log` VALUES (89, 5, 'AI 总结', '对《JAVA》调用 AI 总结，消耗 1 次额度', '', '2026-10-25 22:21:57');
INSERT INTO `note_user_operation_log` VALUES (90, 2, '登录', '邮箱登录成功', '', '2026-10-25 22:24:23');
INSERT INTO `note_user_operation_log` VALUES (91, 5, 'AI 总结', '对《JAVA》调用 AI 总结，消耗 1 次额度', '', '2026-10-25 22:26:23');
INSERT INTO `note_user_operation_log` VALUES (92, 2, '登录', '邮箱登录成功', '', '2026-10-25 22:30:44');
INSERT INTO `note_user_operation_log` VALUES (93, 2, '登录', '邮箱登录成功', '', '2026-10-25 22:31:06');
INSERT INTO `note_user_operation_log` VALUES (94, 2, '登录', '邮箱登录成功', '', '2026-10-25 22:31:14');
INSERT INTO `note_user_operation_log` VALUES (95, 2, '登录', '邮箱登录成功', '', '2026-10-25 22:34:50');
INSERT INTO `note_user_operation_log` VALUES (96, 5, 'AI 总结', '对《无标题笔记》调用 AI 总结，消耗 1 次额度', '', '2026-10-25 22:36:05');
INSERT INTO `note_user_operation_log` VALUES (97, 2, '登录', '邮箱登录成功', '', '2026-10-25 22:37:44');
INSERT INTO `note_user_operation_log` VALUES (98, 2, '登录', '邮箱登录成功', '', '2026-10-25 23:44:59');
INSERT INTO `note_user_operation_log` VALUES (99, 2, '登录', '邮箱登录成功', '', '2026-10-25 23:45:21');
INSERT INTO `note_user_operation_log` VALUES (100, 2, '登录', '邮箱登录成功', '', '2026-10-25 23:45:56');
INSERT INTO `note_user_operation_log` VALUES (101, 2, '登录', '邮箱登录成功', '', '2026-10-25 23:46:21');
INSERT INTO `note_user_operation_log` VALUES (102, 2, '登录', '邮箱登录成功', '', '2026-10-25 23:46:56');
INSERT INTO `note_user_operation_log` VALUES (103, 2, '登录', '邮箱登录成功', '', '2026-10-25 23:47:07');
INSERT INTO `note_user_operation_log` VALUES (104, 2, '登录', '邮箱登录成功', '', '2026-10-25 23:51:54');
INSERT INTO `note_user_operation_log` VALUES (105, 2, '登录', '邮箱登录成功', '', '2026-10-25 23:54:57');
INSERT INTO `note_user_operation_log` VALUES (106, 2, '登录', '邮箱登录成功', '', '2026-10-25 23:55:07');
INSERT INTO `note_user_operation_log` VALUES (107, 5, '登录', '邮箱登录成功', '', '2026-10-26 12:31:51');
INSERT INTO `note_user_operation_log` VALUES (108, 2, '登录', '邮箱登录成功', '', '2026-10-26 13:56:01');
INSERT INTO `note_user_operation_log` VALUES (109, 2, '登录', '邮箱登录成功', '', '2026-10-26 19:11:01');
INSERT INTO `note_user_operation_log` VALUES (110, 2, '登录', '邮箱登录成功', '', '2026-10-26 19:11:13');
INSERT INTO `note_user_operation_log` VALUES (111, 2, '登录', '邮箱登录成功', '', '2026-10-26 19:11:21');
INSERT INTO `note_user_operation_log` VALUES (112, 2, '登录', '邮箱登录成功', '', '2026-10-26 19:11:48');
INSERT INTO `note_user_operation_log` VALUES (113, 2, '登录', '邮箱登录成功', '', '2026-10-26 19:12:39');
INSERT INTO `note_user_operation_log` VALUES (114, 2, '登录', '邮箱登录成功', '', '2026-10-26 19:13:17');
INSERT INTO `note_user_operation_log` VALUES (115, 2, '登录', '邮箱登录成功', '', '2026-10-26 21:08:01');
INSERT INTO `note_user_operation_log` VALUES (116, 2, '登录', '邮箱登录成功', '', '2026-10-26 21:08:22');
INSERT INTO `note_user_operation_log` VALUES (117, 2, '登录', '邮箱登录成功', '', '2026-11-04 20:51:14');

SET FOREIGN_KEY_CHECKS = 1;
