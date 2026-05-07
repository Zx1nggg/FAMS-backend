package com.Zx1nggg.FAMS;

import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.OutputFile;
import com.baomidou.mybatisplus.generator.engine.FreemarkerTemplateEngine;

import java.util.Collections;

/**
 * 智渔 FAMS - MyBatis-Plus 一键代码生成器
 * 运行这个类的 main 方法，会自动读取数据库并生成 CRUD 代码。
 */
public class CodeGenerator {

    private static final String DB_URL = "jdbc:mysql://localhost:3306/fams?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&nullCatalogMeansCurrent=true";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "wumengyun520.";

    public static void main(String[] args) {

        // 1. 获取当前项目的根目录
        String projectPath = System.getProperty("user.dir");
        // 2. 核心基础包名 (请根据实际情况修改)
        String parentPackage = "com.Zx1nggg.FAMS";

        // 3. 先统一生成到一个 "modules/temp" 临时目录
        String moduleName = "modules.temp";

        FastAutoGenerator.create(DB_URL, DB_USER, DB_PASS)
                // 全局配置
                .globalConfig(builder -> {
                    builder.author("Zx1nggg") // 设置作者
                            .enableSwagger() // Swagger 接口文档
                            .outputDir(projectPath + "/src/main/java"); // 指定输出目录
                })
                // 包配置
                .packageConfig(builder -> {
                    builder.parent(parentPackage) // 设置父包名
                            .moduleName(moduleName) // 设置子包名
                            // Mapper XML 存放路径
                            .pathInfo(Collections.singletonMap(OutputFile.xml, projectPath + "/src/main/resources/mapper/" + moduleName));
                })
                // 策略配置 (核心)
                .strategyConfig(builder -> {
                    builder.addInclude(
                                    // 在这里填入生成代码的表名。这里我把系统核心表先写上，你可以一次性全写，或者分批生成
                                    "sys_user", "sys_role", "sys_menu", "sys_user_role", "sys_role_menu",
                                    "t_farm", "t_pond", "t_seedling_dict", "t_purchase_batch",
                                    "t_batch_growth_log", "t_pond_feed_log", "sys_alarm_record"
                                    ,"t_supplier", "t_iot_sensor_data", "t_sop_template", "t_pond_task", "t_harvest_record"
                            )
                            .addTablePrefix("sys_", "t_") // 生成实体类时，去掉表前缀 (如 sys_user -> User)

                            // 实体类策略
                            .entityBuilder()
                            .enableLombok() // 开启 lombok 模型
                            .enableTableFieldAnnotation() // 开启字段注解

                            // 控制层策略
                            .controllerBuilder()
                            .enableRestStyle() // 开启 @RestController 风格

                            // Mapper 层策略
                            .mapperBuilder()
                            .enableBaseResultMap() // 生成基础的 resultMap
                            .enableBaseColumnList(); // 生成基础的字段列表
                })
                // 使用 Freemarker 模板引擎
                .templateEngine(new FreemarkerTemplateEngine())
                .execute();

        System.out.println("FAMS 系统代码生成完毕！请去 src/main/java/.../modules/temp 下查看！");
    }
}
