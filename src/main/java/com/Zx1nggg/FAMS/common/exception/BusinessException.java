package com.Zx1nggg.FAMS.common.exception;

import lombok.Getter;
// 业务异常类
@Getter
public class BusinessException extends RuntimeException{
    private  Integer code;
    public BusinessException(Integer code, String message){
        super(message);
        this.code = code;
    }

    public BusinessException(String message) {
        super(message);
        this.code = 500;
    }

}
