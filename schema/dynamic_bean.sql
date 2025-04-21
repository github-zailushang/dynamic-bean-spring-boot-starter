/*
 Navicat Premium Data Transfer

 Source Server         : master
 Source Server Type    : MySQL
 Source Server Version : 80041
 Source Host           : 
 Source Schema         : dynamic_bean

 Target Server Type    : MySQL
 Target Server Version : 80041
 File Encoding         : 65001

 Date: 17/04/2025 20:58:32
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for refresh_bean
-- ----------------------------
DROP TABLE IF EXISTS `refresh_bean`;
CREATE TABLE `refresh_bean`  (
  `id` int(0) NOT NULL AUTO_INCREMENT,
  `bean_name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `lambda_script` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `description` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of refresh_bean
-- ----------------------------
INSERT INTO `refresh_bean` VALUES (1, 'runnable-task', 'return { param -> println \"Runnable running ...\" } as shop.zailushang.spring.boot.framework.SAM', '任务型接口示例：无参无返回值');
INSERT INTO `refresh_bean` VALUES (2, 'consumer-task', 'return { param -> println \"Hello $param\" } as shop.zailushang.spring.boot.framework.SAM', '消费型接口示例：单参无返回值');
INSERT INTO `refresh_bean` VALUES (3, 'supplier-task', 'return { param -> \"zailushang\"} as shop.zailushang.spring.boot.framework.SAM', '供给型接口示例：无参带返回值');
INSERT INTO `refresh_bean` VALUES (4, 'function-task', 'return { param -> param.replace(\"PHP\",\"Java\") } as shop.zailushang.spring.boot.framework.SAM', '函数型接口示例：单参带返回值（任意）');
INSERT INTO `refresh_bean` VALUES (5, 'predicate-task', 'return { param -> \"gay\" == param } as shop.zailushang.spring.boot.framework.SAM', '断言型接口示例：单参带返回值（Boolean）');
INSERT INTO `refresh_bean` VALUES (6, 'run-4-act', 'import javax.sql.DataSource;return { param -> println act.getBean(DataSource.class) } as shop.zailushang.spring.boot.framework.SAM', '使用内置对象 act 查找依赖示例');
INSERT INTO `refresh_bean` VALUES (7, 'run-4-itl', 'return { param -> println \"itl.get() = ${itl.get()}, in groovy.\"; itl.remove(); } as shop.zailushang.spring.boot.framework.SAM', '使用内置对象 itl 获取线程变量示例');

SET FOREIGN_KEY_CHECKS = 1;
