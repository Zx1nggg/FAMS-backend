package com.Zx1nggg.FAMS.common.aspect;

import com.Zx1nggg.FAMS.common.annotation.Log;
import com.Zx1nggg.FAMS.modules.log.entity.OperLog;
import com.Zx1nggg.FAMS.modules.log.service.IOperLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

@Slf4j
@Aspect
@Component
public class LogAspect {

    @Autowired
    private IOperLogService operLogService;

    @Around("@annotation(opLog)")
    public Object around(ProceedingJoinPoint joinPoint, Log opLog) throws Throwable {
        OperLog record = new OperLog();
        record.setTitle(opLog.title());
        record.setBusinessType((byte) opLog.businessType());
        record.setOperTime(LocalDateTime.now());

        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                record.setOperUrl(request.getRequestURI());
                record.setOperIp(getClientIp(request));
                Object userId = request.getAttribute("currentUserId");
                if (userId != null) {
                    record.setOperName(userId.toString());
                }
            }

            Object result = joinPoint.proceed();

            record.setStatus((byte) 1);
            operLogService.save(record);
            return result;
        } catch (Throwable e) {
            record.setStatus((byte) 0);
            record.setErrorMsg(truncate(e.getMessage(), 500));
            operLogService.save(record);
            throw e;
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    private String truncate(String msg, int maxLen) {
        if (msg == null) return null;
        return msg.length() > maxLen ? msg.substring(0, maxLen) : msg;
    }
}
