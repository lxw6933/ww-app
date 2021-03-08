package com.ww.mall.coupon.exception;

import com.ww.mall.common.constant.R;
import com.ww.mall.common.exception.MallCodeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Author:         ww
 * Datetime:       2021\3\8 0008
 * Description:    异常统一处理
 */
@Slf4j
@RestControllerAdvice(basePackages = "com.ww.mall.coupon.controller")
public class MallExceptionControllerAdvice {

    /**
     * 处理未知类型异常
     */
    @ExceptionHandler(value = Exception.class)
    public R handleException(Exception e){
        log.error("出现异常：{}，异常类型：{}",e.getMessage(),e.getClass());
        return R.error(MallCodeEnum.UNKNOWN_EXCEPTION.getCode(), MallCodeEnum.UNKNOWN_EXCEPTION.getMsg()).put("data",e.getStackTrace());
    }


}
