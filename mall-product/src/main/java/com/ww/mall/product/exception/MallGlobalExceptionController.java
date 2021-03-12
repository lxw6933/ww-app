package com.ww.mall.product.exception;

import com.ww.mall.common.constant.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Author:         ww
 * Datetime:       2021\3\12 0012
 * Description:    全局异常处理
 */
@Slf4j
@RestControllerAdvice(basePackages = "com.ww.mall.product.controller")
public class MallGlobalExceptionController {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public R ExceptionHandle(MethodArgumentNotValidException e) {
        log.error("数据校验出现问题{}，异常类型：{}", e.getMessage(), e.getClass());
        BindingResult bindingResult = e.getBindingResult();
        Map<String, String> errorMap = new HashMap<>();
        bindingResult.getFieldErrors().forEach((fieldError) -> {
            errorMap.put(fieldError.getField(), fieldError.getDefaultMessage());
        });
        return R.error(400, "数据校验失败").put("data", errorMap);
    }

    @ExceptionHandler(value = Throwable.class)
    public R handleException(Throwable throwable) {
        log.error("未知错误：", throwable);
        return R.error(500, "系统未知异常");
    }

}
