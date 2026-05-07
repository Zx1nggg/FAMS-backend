package com.Zx1nggg.FAMS;

import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.OutputFile;
import com.baomidou.mybatisplus.generator.engine.FreemarkerTemplateEngine;

import java.util.Collections;

/**
 * 智渔 FAMS - MyBatis-Plus 代码生成器 (Spring Boot 3.x)
 */
public class CodeGenerator {

    private static final String DB_URL = "jdbc:mysql://localhost:3306/fams?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&nullCatalogMeansCurrent=true";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "wumengyun520.";

    public static void main(String[] args) {

        String projectPath = System.getProperty("user.dir");
        String parentPackage = "com.Zx1nggg.FAMS";
        String moduleName = "modules.temp";

        FastAutoGenerator.create(DB_URL, DB_USER, DB_PASS)
                .globalConfig(builder -> {
                    builder.author("Zx1nggg")
                            .enableSpringdoc()
                            .outputDir(projectPath + "/src/main/java");
                })
                .packageConfig(builder -> {
                    builder.parent(parentPackage)
                            .moduleName(moduleName)
                            .pathInfo(Collections.singletonMap(OutputFile.xml, projectPath + "/src/main/resources/mapper/" + moduleName));
                })
                .strategyConfig(builder -> {
                    builder.addInclude(
                                    "sys_user", "sys_role", "sys_menu", "sys_user_role", "sys_role_menu",
                                    "t_farm", "t_pond", "t_seedling_dict", "t_purchase_batch",
                                    "t_batch_growth_log", "t_pond_feed_log", "sys_alarm_record",
                                    "t_supplier", "t_iot_sensor_data", "t_sop_template", "t_pond_task", "t_harvest_record"
                            )
                            .addTablePrefix("sys_", "t_")
                            .entityBuilder()
                            .enableLombok()
                            .enableTableFieldAnnotation()
                            .controllerBuilder()
                            .enableRestStyle()
                            .mapperBuilder()
                            .enableBaseResultMap()
                            .enableBaseColumnList();
                })
                .templateEngine(new FreemarkerTemplateEngine())
                .execute();

        System.out.println("FAMS 系统代码生成完毕！注解已适配 OpenAPI 3 / Springdoc。");
    }
}