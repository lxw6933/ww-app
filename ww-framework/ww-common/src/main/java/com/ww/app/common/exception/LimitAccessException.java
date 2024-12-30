package com.ww.app.common.exception;

/**
 * @description: 限流异常
 * @author: ww
 * @create: 2021/5/21 下午7:37
 **/
public class LimitAccessException extends RuntimeException {

    private static final long serialVersionUID = -3608667856397125671L;

    public LimitAccessException(String message){
        super(message);
    }

}
