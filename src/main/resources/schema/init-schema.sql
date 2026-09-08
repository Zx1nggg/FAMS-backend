-- FAMS 当前空库初始化（MySQL 8.0+），2026-09-08
-- 仅在专门新建的空数据库执行。禁止导入已有业务数据库。
-- 来源：历史 dump 的纯表结构与仓库增量；不包含用户、密码或业务演示数据。

CREATE TABLE `sys_dict_data` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `dict_type` varchar(100) NOT NULL COMMENT '字典类型 (如: sys_disease_type 病害类型)',
  `dict_label` varchar(100) NOT NULL COMMENT '字典标签 (如: 肠炎病, 白斑综合征)',
  `dict_value` varchar(100) NOT NULL COMMENT '字典键值 (如: enteritis, wss)',
  `css_class` varchar(100) DEFAULT NULL COMMENT '前端ElementPlus的样式属性 (如: danger, warning)',
  `status` tinyint DEFAULT '1' COMMENT '状态: 1正常, 0停用',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='通用字典数据表';

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

CREATE TABLE `sys_role` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `role_name` varchar(50) NOT NULL COMMENT '角色名称 (如: 养殖户, 监管人员)',
  `role_key` varchar(50) NOT NULL COMMENT '角色权限字符串 (如: role_farmer, role_regulator)',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色信息表';

CREATE TABLE `sys_role_menu` (
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `menu_id` bigint NOT NULL COMMENT '菜单ID',
  PRIMARY KEY (`role_id`,`menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色菜单关联表';

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统用户信息表';

CREATE TABLE `sys_user_role` (
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  PRIMARY KEY (`user_id`,`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户角色关联表';

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

CREATE TABLE `t_farm` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `farm_name` varchar(100) NOT NULL COMMENT '养殖场名称 (如: 顺德一区基地)',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

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

CREATE TABLE `t_pond` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `farm_id` bigint NOT NULL COMMENT '所属养殖场 (Where锁定)',
  `pond_name` varchar(50) NOT NULL COMMENT '池塘编号 (如: 1号高位池)',
  `area_mu` decimal(8,2) DEFAULT NULL COMMENT '面积(亩)',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `t_pond_feed_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `pond_id` bigint NOT NULL COMMENT '针对哪个池塘',
  `log_date` date NOT NULL COMMENT '操作日期',
  `feed_brand` varchar(50) DEFAULT NULL COMMENT '饲料品牌',
  `feed_amount` decimal(8,2) DEFAULT NULL COMMENT '投饵量(kg)',
  `water_change_status` varchar(50) DEFAULT NULL COMMENT '换水状态 (如: 换水30%)',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='池塘环境与投喂作业日志(无视批次)';

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
  `quarantine_reviewer_id` bigint DEFAULT NULL COMMENT '检疫审核监管人员ID',
  `quarantine_reviewed_at` datetime DEFAULT NULL COMMENT '检疫审核通过时间',
  `purchase_date` date NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `batch_no` (`batch_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `t_seedling_dict` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `category_name` varchar(50) NOT NULL COMMENT '品种名称 (如: 南美白对虾)',
  `growth_cycle_days` int DEFAULT NULL COMMENT '标准养殖周期(天)',
  `allowable_mortality_rate` decimal(5,2) DEFAULT NULL COMMENT '自然容许死亡率(%) - 超过此值视为异常',
  `min_temp` decimal(5,2) DEFAULT NULL COMMENT '最低水温',
  `min_do` decimal(5,2) DEFAULT NULL COMMENT '最低溶氧量(mg/L)',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='苗种分类字典';

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

CREATE TABLE `t_supplier` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `supplier_name` varchar(100) NOT NULL COMMENT '供应商名称 (如: 湛江海联水产种苗基地)',
  `contact_person` varchar(50) DEFAULT NULL COMMENT '联系人',
  `contact_phone` varchar(20) DEFAULT NULL COMMENT '联系电话',
  `qualification_code` varchar(100) DEFAULT NULL COMMENT '水产苗种生产许可证号',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='苗种供应商/培育基地档案表';

CREATE TABLE `t_supplier_seedling` (
  `supplier_id` BIGINT NOT NULL COMMENT '供应商ID',
  `seedling_id` BIGINT NOT NULL COMMENT '苗种公共目录ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`supplier_id`, `seedling_id`),
  KEY `idx_supplier_seedling_seedling` (`seedling_id`),
  CONSTRAINT `fk_supplier_seedling_supplier`
    FOREIGN KEY (`supplier_id`) REFERENCES `t_supplier` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_supplier_seedling_seedling`
    FOREIGN KEY (`seedling_id`) REFERENCES `t_seedling_dict` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='供应商经核准可供应的苗种品类';

-- 来源：migration-add-is-deleted.sql
ALTER TABLE t_farm
    ADD COLUMN is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-正常 1-已删除';

ALTER TABLE t_pond
    ADD COLUMN is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-正常 1-已删除';

-- 来源：migration-add-stocking.sql
CREATE TABLE `t_stocking` (
  `id`              bigint NOT NULL AUTO_INCREMENT,
  `batch_id`        bigint NOT NULL              COMMENT '批次ID (FK → t_purchase_batch.id)',
  `pond_id`         bigint NOT NULL              COMMENT '池塘ID (FK → t_pond.id)',
  `stocked_units`   int NOT NULL                 COMMENT '投放件数（袋/箱数）',
  `stocked_qty`     int NOT NULL                 COMMENT '系统换算尾数 = stocked_units × 批次.density_per_unit',
  `stocked_weight`  decimal(10,2) DEFAULT NULL   COMMENT '投放总重(kg)，可选',
  `stocking_date`   date NOT NULL                COMMENT '投放日期',
  `remark`          varchar(255) DEFAULT NULL    COMMENT '备注',
  `create_time`     datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time`     datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted`      tinyint NOT NULL DEFAULT 0   COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_pond_id` (`pond_id`),
  KEY `idx_batch_id` (`batch_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='投放登记表：批次与池塘的多对多关联';

-- 来源：migration-add-patrol-log.sql
CREATE TABLE `t_patrol_log` (
  `id`              bigint NOT NULL AUTO_INCREMENT,
  `pond_id`         bigint NOT NULL              COMMENT '巡塘的池塘ID',
  `batch_no`        varchar(64) DEFAULT NULL     COMMENT '关联批次号（混合养殖时指定，可选）',
  `patrol_time`     datetime NOT NULL            COMMENT '巡塘时间（一日多巡的核心字段）',
  `weather`         varchar(20) DEFAULT NULL     COMMENT '天气（晴/阴/雨）',
  `water_temp`      decimal(4,1) DEFAULT NULL    COMMENT '水温感官估算(°C)',
  `water_color`     varchar(30) DEFAULT NULL     COMMENT '水色感官（翠绿/黄绿/浑浊/发红）',
  `operator_id`     bigint DEFAULT NULL          COMMENT '巡塘人ID',
  `remark`          varchar(500) DEFAULT NULL    COMMENT '巡塘综合备注',
  `create_time`     datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time`     datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted`      tinyint NOT NULL DEFAULT 0   COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_pond_date` (`pond_id`, `patrol_time`),
  KEY `idx_operator` (`operator_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='日常巡塘台账主表：每次巡塘一条记录';

ALTER TABLE t_pond_feed_log
    ADD COLUMN patrol_log_id bigint DEFAULT NULL COMMENT '关联巡塘记录ID' AFTER id;

ALTER TABLE t_batch_growth_log
    ADD COLUMN patrol_log_id bigint DEFAULT NULL COMMENT '关联巡塘记录ID' AFTER id;

-- 来源：migration-add-seedling-id.sql
ALTER TABLE t_purchase_batch
    ADD COLUMN seedling_id bigint NOT NULL COMMENT '苗种品种，关联t_seedling_dict.id' AFTER supplier_id;

-- 来源：migration-add-cost-fields.sql
ALTER TABLE t_purchase_batch
    ADD COLUMN unit_price    DECIMAL(10, 2) DEFAULT NULL COMMENT '单价(元/件)' AFTER density_per_unit,
    ADD COLUMN total_amount  DECIMAL(10, 2) DEFAULT NULL COMMENT '总金额(元) = unitQty × unitPrice' AFTER unit_price;

ALTER TABLE t_pond_feed_log

    ADD COLUMN feed_unit_price     DECIMAL(10, 2) DEFAULT NULL COMMENT '饲料单价(元/kg)' AFTER water_change_status,
    ADD COLUMN feed_total_amount   DECIMAL(10, 2) DEFAULT NULL COMMENT '本次投喂金额(元) = feedAmount × feedUnitPrice' AFTER feed_unit_price,

    ADD COLUMN medicine_name       VARCHAR(100)   DEFAULT NULL COMMENT '药品名称' AFTER feed_total_amount,
    ADD COLUMN medicine_dosage     DECIMAL(10, 2) DEFAULT NULL COMMENT '用量' AFTER medicine_name,
    ADD COLUMN medicine_unit       VARCHAR(20)    DEFAULT NULL COMMENT '用量单位(ml/g/袋)' AFTER medicine_dosage,
    ADD COLUMN medicine_amount     DECIMAL(10, 2) DEFAULT NULL COMMENT '本次药费(元)' AFTER medicine_unit;

-- 来源：migration-add-user-profile.sql
ALTER TABLE sys_user
  ADD COLUMN avatar  VARCHAR(255) DEFAULT NULL COMMENT '头像路径（相对路径，如 uploads/avatar/1_xxx.jpg）',
  ADD COLUMN email   VARCHAR(100) DEFAULT NULL COMMENT '电子邮箱',
  ADD COLUMN gender  TINYINT      DEFAULT 0  COMMENT '性别: 0未设置 1男 2女',
  ADD COLUMN address VARCHAR(200) DEFAULT NULL COMMENT '地址 / 所在地';

-- 来源：migration-add-registration.sql
CREATE TABLE `sys_registration_application` (
  `id`                 BIGINT        NOT NULL AUTO_INCREMENT,
  `username`           VARCHAR(50)   NOT NULL                   COMMENT '前端展示昵称',
  `password`           VARCHAR(100)  NOT NULL                   COMMENT 'BCrypt加密密码',
  `real_name`          VARCHAR(50)   NOT NULL                   COMMENT '负责人/法人真实姓名',
  `phone`              VARCHAR(20)   DEFAULT NULL               COMMENT '联系电话',
  `email`              VARCHAR(100)  DEFAULT NULL               COMMENT '电子邮箱',
  `farm_name`          VARCHAR(100)  NOT NULL                   COMMENT '申请入驻的养殖场名称',
  `farm_province`      VARCHAR(50)   DEFAULT NULL               COMMENT '养殖场所属省份',
  `farm_city`          VARCHAR(50)   DEFAULT NULL               COMMENT '养殖场所属城市',
  `farm_address`       VARCHAR(200)  DEFAULT NULL               COMMENT '养殖场详细地址',
  `application_reason` TEXT          DEFAULT NULL               COMMENT '入驻申请理由/补充说明',
  `status`             TINYINT       DEFAULT 0                  COMMENT '审批状态: 0=待审批, 1=已通过, 2=已拒绝',
  `reviewer_id`        BIGINT        DEFAULT NULL               COMMENT '审批人ID（管理员）',
  `review_comment`     VARCHAR(500)  DEFAULT NULL               COMMENT '审批意见/拒绝原因',
  `reviewed_at`        DATETIME      DEFAULT NULL               COMMENT '审批时间',
  `created_at`         DATETIME      DEFAULT CURRENT_TIMESTAMP  COMMENT '申请提交时间',
  `updated_at`         DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_status` (`status`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='入驻申请表';

-- 来源：migration-phone-login.sql
ALTER TABLE sys_user ADD UNIQUE INDEX idx_phone (phone);

-- 来源：migration-username-to-nickname.sql
ALTER TABLE sys_user DROP INDEX username;

ALTER TABLE sys_user MODIFY COLUMN username VARCHAR(50) NOT NULL COMMENT '网名/昵称';

ALTER TABLE sys_registration_application DROP INDEX uk_username;

ALTER TABLE sys_registration_application MODIFY COLUMN username VARCHAR(50) NOT NULL COMMENT '前端展示昵称';

-- 来源：migration-add-harvest-settlement.sql
ALTER TABLE t_harvest_record
    ADD COLUMN unit_price         DECIMAL(10,2) DEFAULT NULL   COMMENT '出塘单价(元/kg)',
    ADD COLUMN total_revenue      DECIMAL(12,2) DEFAULT NULL   COMMENT '总收入(元) = actual_total_weight_kg × unit_price',
    ADD COLUMN seedling_cost      DECIMAL(12,2) DEFAULT NULL   COMMENT '苗种成本(元)',
    ADD COLUMN feed_cost          DECIMAL(12,2) DEFAULT NULL   COMMENT '饲料成本(元)',
    ADD COLUMN medicine_cost      DECIMAL(12,2) DEFAULT NULL   COMMENT '药品成本(元)',
    ADD COLUMN other_cost         DECIMAL(12,2) DEFAULT NULL   COMMENT '其他成本(元)',
    ADD COLUMN total_cost         DECIMAL(12,2) DEFAULT NULL   COMMENT '总成本(元) = 四项成本之和',
    ADD COLUMN net_profit         DECIMAL(12,2) DEFAULT NULL   COMMENT '净利润(元) = total_revenue - total_cost',
    ADD COLUMN settlement_status  TINYINT     DEFAULT 0        COMMENT '结算状态: 0=未结算 1=已结算',
    ADD COLUMN remark             VARCHAR(500) DEFAULT NULL    COMMENT '备注',
    ADD COLUMN is_deleted         TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0=正常 1=已删除',
    ADD COLUMN update_time        DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';

-- 来源：migration-add-farm-geo.sql
ALTER TABLE t_farm
    ADD COLUMN longitude  DECIMAL(10, 7) COMMENT '经度',
    ADD COLUMN latitude   DECIMAL(10, 7) COMMENT '纬度',
    ADD COLUMN address    VARCHAR(255)   COMMENT '详细地址';

CREATE INDEX idx_farm_geo ON t_farm (longitude, latitude);

-- 来源：migration-20260906-auth-version.sql
ALTER TABLE sys_user ADD COLUMN auth_version BIGINT NOT NULL DEFAULT 0 COMMENT '账号认证版本，安全状态变化时递增';

-- 来源：migration-20260906-inspections.sql
CREATE TABLE t_inspection_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    farm_id BIGINT NOT NULL,
    pond_id BIGINT NULL,
    inspection_date DATE NOT NULL,
    inspection_type VARCHAR(50) NOT NULL,
    inspection_item VARCHAR(200),
    result VARCHAR(20) NOT NULL,
    inspector_name VARCHAR(50),
    inspector_id BIGINT NOT NULL,
    description TEXT,
    unqualified_reason VARCHAR(500),
    attachment_urls TEXT,
    rectify_status VARCHAR(20) NOT NULL DEFAULT 'none',
    rectify_deadline DATE,
    rectify_remark VARCHAR(500),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT NOT NULL DEFAULT 0,
    INDEX idx_inspection_farm (farm_id),
    INDEX idx_inspection_date (inspection_date),
    INDEX idx_inspection_rectify (rectify_status, is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='监管方线下抽检档案';

CREATE TABLE sys_alarm_rule (
    id BIGINT NOT NULL AUTO_INCREMENT,
    rule_code VARCHAR(80) NOT NULL COMMENT '规则唯一编码',
    rule_name VARCHAR(150) NOT NULL COMMENT '规则名称',
    source_type VARCHAR(30) NOT NULL COMMENT 'IOT/BIOLOGY/QUARANTINE/TRANSPORT/SYSTEM',
    alarm_code VARCHAR(50) NOT NULL COMMENT '触发后的告警编码',
    metric_code VARCHAR(50) NULL COMMENT '监测指标编码，复杂业务规则可为空',
    threshold_operator VARCHAR(8) NULL COMMENT 'LT/LE/GT/GE/EQ/BETWEEN',
    threshold_value DECIMAL(14,4) NULL COMMENT '阈值或区间下限',
    threshold_value_high DECIMAL(14,4) NULL COMMENT '区间上限',
    metric_unit VARCHAR(20) NULL COMMENT '指标单位',
    severity TINYINT NOT NULL COMMENT '1提示、2警告、3严重',
    scope_type VARCHAR(20) NOT NULL DEFAULT 'GLOBAL' COMMENT 'GLOBAL/FARM/SPECIES',
    farm_id BIGINT NULL COMMENT '指定养殖场',
    seedling_id BIGINT NULL COMMENT '指定苗种品种',
    cooldown_minutes INT NOT NULL DEFAULT 30 COMMENT '重复触发冷却分钟数',
    rule_config JSON NULL COMMENT '检疫、调运等复杂规则配置',
    enabled TINYINT NOT NULL DEFAULT 1 COMMENT '0停用、1启用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_alarm_rule_code (rule_code),
    KEY idx_alarm_rule_enabled_source (enabled, source_type),
    KEY idx_alarm_rule_scope (scope_type, farm_id, seedling_id),
    CONSTRAINT chk_alarm_rule_source CHECK (source_type IN ('IOT','BIOLOGY','QUARANTINE','TRANSPORT','SYSTEM')),
    CONSTRAINT chk_alarm_rule_severity CHECK (severity IN (1,2,3)),
    CONSTRAINT chk_alarm_rule_scope CHECK (scope_type IN ('GLOBAL','FARM','SPECIES')),
    CONSTRAINT chk_alarm_rule_operator CHECK (
        threshold_operator IS NULL OR threshold_operator IN ('LT','LE','GT','GE','EQ','BETWEEN')
    ),
    CONSTRAINT chk_alarm_rule_enabled CHECK (enabled IN (0,1)),
    CONSTRAINT chk_alarm_rule_cooldown CHECK (cooldown_minutes >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='告警触发规则';

CREATE TABLE sys_alarm_record (
    id BIGINT NOT NULL AUTO_INCREMENT,
    farm_id BIGINT NOT NULL COMMENT '所属养殖场ID',
    pond_id BIGINT NULL COMMENT '所属池塘ID，养殖场级告警可为空',
    rule_id BIGINT NULL COMMENT '触发规则ID',
    alarm_code VARCHAR(50) NOT NULL COMMENT '告警编码',
    title VARCHAR(150) NOT NULL COMMENT '告警标题',
    message VARCHAR(500) NOT NULL COMMENT '告警详细描述',
    source_type VARCHAR(30) NOT NULL COMMENT 'IOT/BIOLOGY/QUARANTINE/TRANSPORT/SYSTEM',
    source_id BIGINT NULL COMMENT '传感器数据或业务记录ID',
    severity TINYINT NOT NULL COMMENT '1提示、2警告、3严重',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '0待确认、1已确认、2处理中、3已解决、4已关闭',
    metric_code VARCHAR(50) NULL COMMENT '触发指标编码',
    trigger_value DECIMAL(14,4) NULL COMMENT '最近一次触发实际值',
    threshold_operator VARCHAR(8) NULL COMMENT 'LT/LE/GT/GE/EQ/BETWEEN',
    threshold_value DECIMAL(14,4) NULL COMMENT '触发阈值或区间下限',
    threshold_value_high DECIMAL(14,4) NULL COMMENT '区间上限',
    metric_unit VARCHAR(20) NULL COMMENT '指标单位',
    dedup_key VARCHAR(160) NOT NULL COMMENT '如 farm:pond:alarmCode:metric',
    active_dedup_key VARCHAR(160) GENERATED ALWAYS AS (
        CASE WHEN status IN (0,1,2) THEN dedup_key ELSE NULL END
    ) STORED COMMENT '活动告警唯一键',
    occurrence_count INT NOT NULL DEFAULT 1 COMMENT '累计触发次数',
    first_occurred_at DATETIME NOT NULL COMMENT '首次触发时间',
    last_occurred_at DATETIME NOT NULL COMMENT '最近触发时间',
    acknowledged_by BIGINT NULL COMMENT '确认人ID',
    acknowledged_at DATETIME NULL COMMENT '确认时间',
    resolved_by BIGINT NULL COMMENT '解决人ID',
    resolved_at DATETIME NULL COMMENT '解决时间',
    resolution_remark VARCHAR(500) NULL COMMENT '解决说明',
    recovered_at DATETIME NULL COMMENT '监测指标自动恢复时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_alarm_active_dedup (active_dedup_key),
    KEY idx_alarm_farm_status_severity_time (farm_id, status, severity, last_occurred_at),
    KEY idx_alarm_pond_status_time (pond_id, status, last_occurred_at),
    KEY idx_alarm_code_status_time (alarm_code, status, last_occurred_at),
    KEY idx_alarm_source (source_type, source_id),
    KEY idx_alarm_rule_time (rule_id, last_occurred_at),
    CONSTRAINT fk_alarm_record_rule FOREIGN KEY (rule_id) REFERENCES sys_alarm_rule(id) ON DELETE SET NULL,
    CONSTRAINT chk_alarm_source CHECK (source_type IN ('IOT','BIOLOGY','QUARANTINE','TRANSPORT','SYSTEM')),
    CONSTRAINT chk_alarm_severity CHECK (severity IN (1,2,3)),
    CONSTRAINT chk_alarm_status CHECK (status IN (0,1,2,3,4)),
    CONSTRAINT chk_alarm_occurrence CHECK (occurrence_count >= 1),
    CONSTRAINT chk_alarm_operator CHECK (
        threshold_operator IS NULL OR threshold_operator IN ('LT','LE','GT','GE','EQ','BETWEEN')
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='告警事件主表';

CREATE TABLE sys_alarm_action_log (
    id BIGINT NOT NULL AUTO_INCREMENT,
    alarm_id BIGINT NOT NULL COMMENT '告警事件ID',
    action_type VARCHAR(30) NOT NULL COMMENT 'ACKNOWLEDGE/START_PROCESS/RESOLVE/CLOSE/REOPEN/AUTO_RECOVER',
    from_status TINYINT NULL COMMENT '变更前状态',
    to_status TINYINT NOT NULL COMMENT '变更后状态',
    operator_id BIGINT NULL COMMENT '操作人ID，系统操作可为空',
    action_remark VARCHAR(500) NULL COMMENT '操作或处理说明',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_alarm_action_alarm_time (alarm_id, created_at),
    KEY idx_alarm_action_operator_time (operator_id, created_at),
    CONSTRAINT fk_alarm_action_record FOREIGN KEY (alarm_id) REFERENCES sys_alarm_record(id) ON DELETE CASCADE,
    CONSTRAINT chk_alarm_action_from_status CHECK (from_status IS NULL OR from_status IN (0,1,2,3,4)),
    CONSTRAINT chk_alarm_action_to_status CHECK (to_status IN (0,1,2,3,4)),
    CONSTRAINT chk_alarm_action_type CHECK (
        action_type IN ('ACKNOWLEDGE','START_PROCESS','RESOLVE','CLOSE','REOPEN','AUTO_RECOVER')
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='告警处理审计流水';

-- 默认告警规则（配置，不是业务数据）
INSERT INTO sys_alarm_rule
    (rule_code, rule_name, source_type, alarm_code, metric_code,
     threshold_operator, threshold_value, metric_unit, severity, cooldown_minutes)
VALUES
    ('IOT_DO_LOW_DEFAULT','溶解氧过低','IOT','IOT_DO_LOW','dissolved_oxygen','LT',3.5000,'mg/L',3,30),
    ('IOT_TEMP_HIGH_DEFAULT','水温过高','IOT','IOT_TEMP_HIGH','water_temp','GT',35.0000,'℃',2,30),
    ('IOT_PH_LOW_DEFAULT','pH过低','IOT','IOT_PH_LOW','ph_value','LT',6.5000,'pH',2,30),
    ('IOT_PH_HIGH_DEFAULT','pH过高','IOT','IOT_PH_HIGH','ph_value','GT',9.0000,'pH',2,30);

ALTER TABLE t_harvest_record ADD COLUMN active_record TINYINT GENERATED ALWAYS AS (CASE WHEN is_deleted=0 THEN 1 ELSE NULL END) STORED;
ALTER TABLE t_harvest_record ADD UNIQUE KEY uk_harvest_batch_pond_active(batch_no, pond_id, active_record);
ALTER TABLE t_harvest_record DROP INDEX batch_no;

-- 仅为后续农场级联删除记录批次；旧删除数据维持 NULL，不自动推断恢复范围。
ALTER TABLE t_farm ADD COLUMN delete_batch VARCHAR(36) NULL COMMENT '本次级联删除批次';
ALTER TABLE t_pond ADD COLUMN delete_batch VARCHAR(36) NULL COMMENT '随农场级联删除的批次，单独删除时为空';

ALTER TABLE sys_registration_application
    ADD COLUMN pending_phone VARCHAR(20) GENERATED ALWAYS AS (CASE WHEN status=0 THEN phone ELSE NULL END) STORED,
    ADD UNIQUE KEY uk_registration_pending_phone(pending_phone);
