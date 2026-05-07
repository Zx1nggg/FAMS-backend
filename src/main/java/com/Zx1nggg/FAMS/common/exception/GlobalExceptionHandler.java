package com.Zx1nggg.FAMS.common.exception;

import com.Zx1nggg.FAMS.common.api.Result;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    // 拦截自定义的异常
    @ExceptionHandler(BusinessExceptionHandler.class)
    public Result<String> handleBusinessException(BusinessExceptionHandler e){
        System.err.println("业务异常："+e.getMessage());
        return Result.error(e.getCode(),e.getMessage());
    }
    // 拦截未知异常
    @ExceptionHandler(Exception.class)
    public Result<String> handleException(Exception e){
        e.printStackTrace(); // 打印异常信息,完整的错误栈信息
        return Result.error(500,"服务器异常，请稍后重试");
    }
}
