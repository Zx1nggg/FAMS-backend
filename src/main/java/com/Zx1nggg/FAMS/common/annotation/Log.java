package com.Zx1nggg.FAMS.common.annotation;

import java.lang.annotation.*;

/**
 * 操作日志注解 — 标注在 Controller 方法上即可自动记录操作日志
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Log {

    /** 模块标题，如：养殖场管理 */
    String title() default "";

    /** 业务类型：1=新增, 2=修改, 3=删除, 4=导出 */
    int businessType() default 0;
}
