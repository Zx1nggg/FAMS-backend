package com.Zx1nggg.FAMS.modules.log.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 系统操作审计日志
 * </p>
 *
 * @author Zx1nggg
 * @since 2026-05-12
 */
@Getter
@Setter
@ToString
@TableName("sys_oper_log")
@Schema(name = "OperLog", description = "系统操作审计日志")
public class OperLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 模块标题 (如: 投放登记)
     */
    @TableField("title")
    @Schema(description = "模块标题 (如: 投放登记)")
    private String title;

    /**
     * 业务类型 (1新增, 2修改, 3删除, 4导出)
     */
    @TableField("business_type")
    @Schema(description = "业务类型 (1新增, 2修改, 3删除, 4导出)")
    private Byte businessType;

    /**
     * 操作人员
     */
    @TableField("oper_name")
    @Schema(description = "操作人员")
    private String operName;

    /**
     * 主机IP地址
     */
    @TableField("oper_ip")
    @Schema(description = "主机IP地址")
    private String operIp;

    /**
     * 请求URL
     */
    @TableField("oper_url")
    @Schema(description = "请求URL")
    private String operUrl;

    /**
     * 操作状态 (1正常, 0异常)
     */
    @TableField("status")
    @Schema(description = "操作状态 (1正常, 0异常)")
    private Byte status;

    /**
     * 错误消息 (如果是异常，记录Exception堆栈)
     */
    @TableField("error_msg")
    @Schema(description = "错误消息 (如果是异常，记录Exception堆栈)")
    private String errorMsg;

    /**
     * 操作时间
     */
    @TableField("oper_time")
    @Schema(description = "操作时间")
    private LocalDateTime operTime;
}
