-- MySQL dump 10.13  Distrib 8.0.19, for Win64 (x86_64)
--
-- Host: localhost    Database: fams
-- ------------------------------------------------------
-- Server version	8.4.6

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `sys_alarm_record`
--

DROP TABLE IF EXISTS `sys_alarm_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_alarm_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `farm_id` bigint NOT NULL COMMENT '接收告警的养殖场ID',
  `alarm_level` tinyint DEFAULT NULL COMMENT '告警级别: 1提示, 2警告, 3严重 (如: 溶氧极低)',
  `alarm_type` varchar(50) DEFAULT NULL COMMENT '告警类型: IOT_TEMP(水温超标), IOT_DO(缺氧), BIOLOGY(突发死苗)',
  `alarm_content` varchar(500) NOT NULL COMMENT '告警详情描述 (如: 1号池溶氧量低至2.5mg/L，请立即开启增氧设备！)',
  `is_handled` tinyint DEFAULT '0' COMMENT '处理状态: 0未处理, 1已处理',
  `handle_time` datetime DEFAULT NULL COMMENT '养殖户点击"已知晓/已处理"的时间',
  `create_time` datetime DEFAULT NULL COMMENT '告警发生时间',
  PRIMARY KEY (`id`),
  KEY `idx_farm_status` (`farm_id`,`is_handled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统异常告警与处理记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_alarm_record`
--

LOCK TABLES `sys_alarm_record` WRITE;
/*!40000 ALTER TABLE `sys_alarm_record` DISABLE KEYS */;
/*!40000 ALTER TABLE `sys_alarm_record` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_dict_data`
--

DROP TABLE IF EXISTS `sys_dict_data`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_dict_data` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `dict_type` varchar(100) NOT NULL COMMENT '字典类型 (如: sys_disease_type 病害类型)',
  `dict_label` varchar(100) NOT NULL COMMENT '字典标签 (如: 肠炎病, 白斑综合征)',
  `dict_value` varchar(100) NOT NULL COMMENT '字典键值 (如: enteritis, wss)',
  `css_class` varchar(100) DEFAULT NULL COMMENT '前端ElementPlus的样式属性 (如: danger, warning)',
  `status` tinyint DEFAULT '1' COMMENT '状态: 1正常, 0停用',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='通用字典数据表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_dict_data`
--

LOCK TABLES `sys_dict_data` WRITE;
/*!40000 ALTER TABLE `sys_dict_data` DISABLE KEYS */;
/*!40000 ALTER TABLE `sys_dict_data` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_menu`
--

DROP TABLE IF EXISTS `sys_menu`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_menu` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `parent_id` bigint DEFAULT '0' COMMENT '父菜单ID',
  `menu_name` varchar(50) NOT NULL COMMENT '菜单名称',
  `path` varchar(200) DEFAULT NULL COMMENT '路由地址',
  `component` varchar(255) DEFAULT NULL COMMENT 'Vue组件路径',
  `perms` varchar(100) DEFAULT NULL COMMENT '权限标识 (如: base:pond:add)',
  `menu_type` char(1) DEFAULT NULL COMMENT '菜单类型: M目录, C菜单, F按钮',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统菜单与按钮权限表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_menu`
--

LOCK TABLES `sys_menu` WRITE;
/*!40000 ALTER TABLE `sys_menu` DISABLE KEYS */;
/*!40000 ALTER TABLE `sys_menu` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_oper_log`
--

DROP TABLE IF EXISTS `sys_oper_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_oper_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(50) DEFAULT NULL COMMENT '模块标题 (如: 投放登记)',
  `business_type` tinyint DEFAULT NULL COMMENT '业务类型 (1新增, 2修改, 3删除, 4导出)',
  `oper_name` varchar(50) DEFAULT NULL COMMENT '操作人员',
  `oper_ip` varchar(50) DEFAULT NULL COMMENT '主机IP地址',
  `oper_url` varchar(255) DEFAULT NULL COMMENT '请求URL',
  `status` tinyint DEFAULT NULL COMMENT '操作状态 (1正常, 0异常)',
  `error_msg` varchar(2000) DEFAULT NULL COMMENT '错误消息 (如果是异常，记录Exception堆栈)',
  `oper_time` datetime DEFAULT NULL COMMENT '操作时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统操作审计日志';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_oper_log`
--

LOCK TABLES `sys_oper_log` WRITE;
/*!40000 ALTER TABLE `sys_oper_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `sys_oper_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_role`
--

DROP TABLE IF EXISTS `sys_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_role` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `role_name` varchar(50) NOT NULL COMMENT '角色名称 (如: 养殖户, 监管人员)',
  `role_key` varchar(50) NOT NULL COMMENT '角色权限字符串 (如: role_farmer, role_regulator)',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色信息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_role`
--

LOCK TABLES `sys_role` WRITE;
/*!40000 ALTER TABLE `sys_role` DISABLE KEYS */;
/*!40000 ALTER TABLE `sys_role` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_role_menu`
--

DROP TABLE IF EXISTS `sys_role_menu`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_role_menu` (
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `menu_id` bigint NOT NULL COMMENT '菜单ID',
  PRIMARY KEY (`role_id`,`menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色菜单关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_role_menu`
--

LOCK TABLES `sys_role_menu` WRITE;
/*!40000 ALTER TABLE `sys_role_menu` DISABLE KEYS */;
/*!40000 ALTER TABLE `sys_role_menu` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_user`
--

DROP TABLE IF EXISTS `sys_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_user` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(50) NOT NULL COMMENT '登录账号',
  `password` varchar(100) NOT NULL COMMENT '密码(BCrypt加密)',
  `real_name` varchar(50) DEFAULT NULL COMMENT '真实姓名/负责人姓名',
  `phone` varchar(20) DEFAULT NULL COMMENT '联系电话',
  `user_type` varchar(20) DEFAULT NULL COMMENT '用户类型: ADMIN(管理员), REGULATOR(监管方), FARMER(养殖户)',
  `farm_id` bigint DEFAULT NULL COMMENT '所属养殖场ID (如果是养殖户，则关联t_farm；管理员和监管方为空)',
  `status` tinyint DEFAULT '1' COMMENT '帐号状态: 1正常, 0停用',
  PRIMARY KEY (`id`),
  UNIQUE KEY `username` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统用户信息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_user`
--

LOCK TABLES `sys_user` WRITE;
/*!40000 ALTER TABLE `sys_user` DISABLE KEYS */;
INSERT INTO `sys_user` VALUES (1,'admin','$2a$10$WtwOPdQ2uFN3O5G73vfpWuWUmpaSF/Erf3C9McqbFlU8k/hVmPM3W','系统管理员(总干事)','13800000001','ADMIN',NULL,1),(2,'farmer','$2a$10$cot1R8kdIlHQX7mPQRrQ2Ohn/qJyTyDCRqEUGhOiFMytpqwHFP7lO','陈老农(基地负责人)','13800000002','FARMER',1,1),(3,'leader','$2a$10$k169nlnA08koJxPoAUzOK.7HKtPHnTNP997MEnlV7eXtYt1c450wm','海洋学院研究员','13800000003','REGULATOR',NULL,1);
/*!40000 ALTER TABLE `sys_user` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_user_role`
--

DROP TABLE IF EXISTS `sys_user_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_user_role` (
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  PRIMARY KEY (`user_id`,`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户角色关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_user_role`
--

LOCK TABLES `sys_user_role` WRITE;
/*!40000 ALTER TABLE `sys_user_role` DISABLE KEYS */;
/*!40000 ALTER TABLE `sys_user_role` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_batch_growth_log`
--

DROP TABLE IF EXISTS `t_batch_growth_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_batch_growth_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `batch_no` varchar(64) NOT NULL COMMENT '针对哪个批次',
  `pond_id` bigint NOT NULL COMMENT '发生在哪口池塘',
  `log_date` date NOT NULL,
  `avg_length` decimal(8,2) DEFAULT NULL COMMENT '抽测均长(cm)',
  `avg_weight` decimal(8,2) DEFAULT NULL COMMENT '抽测均重(g)',
  `routine_death_count` int DEFAULT '0' COMMENT '日常合理损耗(尾) - 算入正常死亡率',
  `abnormal_death_count` int DEFAULT '0' COMMENT '异常突发死亡数(尾) - 必须填原因',
  `abnormal_reason` varchar(255) DEFAULT NULL COMMENT '异常死亡原因 (如: 用药不当, 停电缺氧)',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='批次生物生长与死亡抽测记录';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_batch_growth_log`
--

LOCK TABLES `t_batch_growth_log` WRITE;
/*!40000 ALTER TABLE `t_batch_growth_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `t_batch_growth_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_farm`
--

DROP TABLE IF EXISTS `t_farm`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_farm` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `farm_name` varchar(100) NOT NULL COMMENT '养殖场名称 (如: 顺德一区基地)',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_farm`
--

LOCK TABLES `t_farm` WRITE;
/*!40000 ALTER TABLE `t_farm` DISABLE KEYS */;
/*!40000 ALTER TABLE `t_farm` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_harvest_record`
--

DROP TABLE IF EXISTS `t_harvest_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_harvest_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `batch_no` varchar(64) NOT NULL COMMENT '大结局主线：哪个批次出塘了 (一对一关系)',
  `pond_id` bigint NOT NULL COMMENT '从哪个池塘捞出的',
  `harvest_date` date NOT NULL COMMENT '实际出塘结算日期',
  `predicted_weight_kg` decimal(10,2) DEFAULT NULL COMMENT '算法预测产量(kg) (供后期做算法精确度误差分析)',
  `actual_total_weight_kg` decimal(10,2) NOT NULL COMMENT '实际过磅总产量(kg)',
  `actual_avg_weight_g` decimal(8,2) DEFAULT NULL COMMENT '最终出池抽测均重(g/尾)',
  `buyer_name` varchar(100) NOT NULL COMMENT '收购方/去向 (满足国家《销售记录》合规要求)',
  `trace_qr_code_url` varchar(255) DEFAULT NULL COMMENT '系统自动生成的C端溯源H5页面链接 (前端转成二维码展示)',
  `trace_query_count` int DEFAULT '0' COMMENT '消费者扫码查询次数 (防伪被恶意盗刷的校验手段)',
  `operator_id` bigint NOT NULL COMMENT '经手人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `batch_no` (`batch_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='出塘结算与消费者防伪溯源凭证表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_harvest_record`
--

LOCK TABLES `t_harvest_record` WRITE;
/*!40000 ALTER TABLE `t_harvest_record` DISABLE KEYS */;
/*!40000 ALTER TABLE `t_harvest_record` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_iot_sensor_data`
--

DROP TABLE IF EXISTS `t_iot_sensor_data`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_iot_sensor_data` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `pond_id` bigint NOT NULL COMMENT '监测的是哪口池塘',
  `device_sn` varchar(50) NOT NULL COMMENT '硬件设备SN码',
  `water_temp` decimal(5,2) DEFAULT NULL COMMENT '实时水温(°C)',
  `dissolved_oxygen` decimal(5,2) DEFAULT NULL COMMENT '溶氧量(mg/L)',
  `ph_value` decimal(4,2) DEFAULT NULL COMMENT 'pH值',
  `collect_time` datetime NOT NULL COMMENT '传感器采集时间',
  PRIMARY KEY (`id`),
  KEY `idx_pond_time` (`pond_id`,`collect_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='物联网水质传感器实时流水表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_iot_sensor_data`
--

LOCK TABLES `t_iot_sensor_data` WRITE;
/*!40000 ALTER TABLE `t_iot_sensor_data` DISABLE KEYS */;
/*!40000 ALTER TABLE `t_iot_sensor_data` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_pond`
--

DROP TABLE IF EXISTS `t_pond`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_pond` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `farm_id` bigint NOT NULL COMMENT '所属养殖场 (Where锁定)',
  `pond_name` varchar(50) NOT NULL COMMENT '池塘编号 (如: 1号高位池)',
  `area_mu` decimal(8,2) DEFAULT NULL COMMENT '面积(亩)',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_pond`
--

LOCK TABLES `t_pond` WRITE;
/*!40000 ALTER TABLE `t_pond` DISABLE KEYS */;
/*!40000 ALTER TABLE `t_pond` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_pond_feed_log`
--

DROP TABLE IF EXISTS `t_pond_feed_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_pond_feed_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `pond_id` bigint NOT NULL COMMENT '针对哪个池塘',
  `log_date` date NOT NULL COMMENT '操作日期',
  `feed_brand` varchar(50) DEFAULT NULL COMMENT '饲料品牌',
  `feed_amount` decimal(8,2) DEFAULT NULL COMMENT '投饵量(kg)',
  `water_change_status` varchar(50) DEFAULT NULL COMMENT '换水状态 (如: 换水30%)',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='池塘环境与投喂作业日志(无视批次)';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_pond_feed_log`
--

LOCK TABLES `t_pond_feed_log` WRITE;
/*!40000 ALTER TABLE `t_pond_feed_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `t_pond_feed_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_pond_task`
--

DROP TABLE IF EXISTS `t_pond_task`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_pond_task` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `pond_id` bigint NOT NULL COMMENT '任务关联的具体池塘ID',
  `batch_no` varchar(64) NOT NULL COMMENT '任务关联的具体批次号 (防混养干扰)',
  `task_type` varchar(30) DEFAULT NULL COMMENT '任务类型 (继承自模板)',
  `task_desc` varchar(200) NOT NULL COMMENT '任务说明 (继承自模板)',
  `scheduled_date` date NOT NULL COMMENT '计划执行日期 (核心！由系统根据: 下塘日期 + day_offset 自动算出来)',
  `status` tinyint DEFAULT '0' COMMENT '任务状态机: 0-待执行, 1-已打卡完成, 2-已逾期未做 (红色警告)',
  `finish_time` datetime DEFAULT NULL COMMENT '养殖户实际点击"打卡"的时间',
  `operator_id` bigint DEFAULT NULL COMMENT '执行此任务的养殖户ID',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_pond_date_status` (`pond_id`,`scheduled_date`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='基于SOP引擎自动生成的池塘每日待办任务表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_pond_task`
--

LOCK TABLES `t_pond_task` WRITE;
/*!40000 ALTER TABLE `t_pond_task` DISABLE KEYS */;
/*!40000 ALTER TABLE `t_pond_task` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_purchase_batch`
--

DROP TABLE IF EXISTS `t_purchase_batch`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_purchase_batch` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `farm_id` bigint NOT NULL COMMENT '确立 Where 属性: 进了哪个场',
  `batch_no` varchar(64) NOT NULL COMMENT 'What: 唯一批次号',
  `supplier_id` bigint NOT NULL COMMENT '供应商',
  `purchase_unit` varchar(20) NOT NULL COMMENT '采购包装单位 (如: 袋, 箱)',
  `unit_qty` int NOT NULL COMMENT '包装件数 (如: 50袋)',
  `density_per_unit` int NOT NULL COMMENT '每包装预估密度 (如: 2000尾/袋)',
  `estimated_total_qty` int NOT NULL COMMENT '系统换算总尾数 (件数 * 密度)',
  `batch_status` tinyint DEFAULT '0' COMMENT 'Why 状态机: 0-待检疫, 1-已检疫入库, 2-养殖中, 3-已出库结算',
  `quarantine_cert_no` varchar(100) DEFAULT NULL COMMENT '检疫证号',
  `purchase_date` date NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `batch_no` (`batch_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_purchase_batch`
--

LOCK TABLES `t_purchase_batch` WRITE;
/*!40000 ALTER TABLE `t_purchase_batch` DISABLE KEYS */;
/*!40000 ALTER TABLE `t_purchase_batch` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_seedling_dict`
--

DROP TABLE IF EXISTS `t_seedling_dict`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_seedling_dict` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `category_name` varchar(50) NOT NULL COMMENT '品种名称 (如: 南美白对虾)',
  `growth_cycle_days` int DEFAULT NULL COMMENT '标准养殖周期(天)',
  `allowable_mortality_rate` decimal(5,2) DEFAULT NULL COMMENT '自然容许死亡率(%) - 超过此值视为异常',
  `min_temp` decimal(5,2) DEFAULT NULL COMMENT '最低水温',
  `min_do` decimal(5,2) DEFAULT NULL COMMENT '最低溶氧量(mg/L)',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='苗种分类字典';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_seedling_dict`
--

LOCK TABLES `t_seedling_dict` WRITE;
/*!40000 ALTER TABLE `t_seedling_dict` DISABLE KEYS */;
/*!40000 ALTER TABLE `t_seedling_dict` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_sop_template`
--

DROP TABLE IF EXISTS `t_sop_template`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_sop_template` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `category_id` bigint NOT NULL COMMENT '适用苗种字典ID (关联 t_seedling_dict，如: 南美白对虾)',
  `stage_name` varchar(50) NOT NULL COMMENT '养殖阶段 (如: 苗期、标粗期、育肥期)',
  `day_offset` int NOT NULL COMMENT '时间偏移量 (定义规则：如下塘后第 15 天执行)',
  `task_type` varchar(30) DEFAULT NULL COMMENT '任务类型 (枚举值: DISINFECT-消毒, TEST-抽测, WATER-换水, FEED-特殊投喂)',
  `task_desc` varchar(200) NOT NULL COMMENT '标准操作指南/作业要求 (如: 使用聚维酮碘全池泼洒消毒)',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_category` (`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='标准化养殖 SOP 规则模板表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_sop_template`
--

LOCK TABLES `t_sop_template` WRITE;
/*!40000 ALTER TABLE `t_sop_template` DISABLE KEYS */;
/*!40000 ALTER TABLE `t_sop_template` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_supplier`
--

DROP TABLE IF EXISTS `t_supplier`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_supplier` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `supplier_name` varchar(100) NOT NULL COMMENT '供应商名称 (如: 湛江海联水产种苗基地)',
  `contact_person` varchar(50) DEFAULT NULL COMMENT '联系人',
  `contact_phone` varchar(20) DEFAULT NULL COMMENT '联系电话',
  `qualification_code` varchar(100) DEFAULT NULL COMMENT '水产苗种生产许可证号',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='苗种供应商/培育基地档案表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_supplier`
--

LOCK TABLES `t_supplier` WRITE;
/*!40000 ALTER TABLE `t_supplier` DISABLE KEYS */;
/*!40000 ALTER TABLE `t_supplier` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping routines for database 'fams'
--
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-05-12 16:08:12
