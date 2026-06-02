package com.Zx1nggg.FAMS.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 启用 Spring 定时任务调度（用于 IoT 数据模拟器和小时聚合器）
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
