package com.Zx1nggg.FAMS.common.exception;

import com.Zx1nggg.FAMS.common.api.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler({org.springframework.web.servlet.resource.NoResourceFoundException.class,
            org.springframework.web.servlet.NoHandlerFoundException.class})
    public Result<String> handleNotFound(Exception e) { return Result.error(404, "接口或资源不存在"); }
    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    public Result<String> handleMethodNotAllowed(Exception e) { return Result.error(405, "请求方法不支持"); }
    @ExceptionHandler({org.springframework.dao.DuplicateKeyException.class,
            org.springframework.dao.ConcurrencyFailureException.class})
    public Result<String> handleConflict(Exception e) {
        return Result.error(409, "记录重复或已被其他操作修改，请刷新后重试");
    }
    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public Result<String> handleAccessDenied(Exception e) {
        return Result.error(403, "权限不足，拒绝访问");
    }

    @ExceptionHandler({org.springframework.web.bind.MethodArgumentNotValidException.class,
            org.springframework.http.converter.HttpMessageNotReadableException.class,
            org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class,
            org.springframework.web.bind.MissingServletRequestParameterException.class,
            jakarta.validation.ConstraintViolationException.class})
    public Result<String> handleInvalidInput(Exception e) {
        return Result.error(400, "请求参数不合法，请检查必填项、类型和取值范围");
    }

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
        // JDBC 异常可能包含 SQL 参数（密码/个人资料），不直接输出异常消息或堆栈。
        log.error("服务器异常，类型：{}", e.getClass().getSimpleName(),e);
        return Result.error(500, "服务器异常，请稍后重试");
    }
}
