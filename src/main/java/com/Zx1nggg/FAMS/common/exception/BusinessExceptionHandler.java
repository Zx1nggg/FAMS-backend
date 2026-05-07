package com.Zx1nggg.FAMS.common.exception;

import lombok.Getter;
// 业务异常类
@Getter
public class BusinessExceptionHandler extends RuntimeException{
    private  Integer code;
    public BusinessExceptionHandler(Integer code,String message){
        super(message);
        this.code = code;
    }

}
