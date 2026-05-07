package com.Zx1nggg.FAMS.common.exception;

import com.Zx1nggg.FAMS.common.api.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    // 拦截自定义的异常
    @ExceptionHandler(BusinessException.class)
    public Result<String> handleBusinessException(BusinessException e) {
        // 使用 log.warn 记录业务逻辑级别的错误，而不是控制台乱喷红字
        log.warn("业务异常：[{}] {}", e.getCode(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    // 拦截所有其他异常
    @ExceptionHandler(Exception.class)
    public Result<String> handleException(Exception e) {
        // 真正的系统崩溃，使用 log.error 记录完整的堆栈信息到日志文件中
        log.error("服务器未知异常：", e);
        return Result.error(500, "服务器异常，请稍后重试");
    }
}
