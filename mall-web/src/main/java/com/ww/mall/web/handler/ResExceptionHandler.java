package com.ww.mall.web.handler;

import com.ww.mall.common.common.Result;
import com.ww.mall.common.enums.CodeEnum;
import com.ww.mall.common.exception.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * @author ww
 * @create 2023-07-15- 14:10
 * @description: 全局异常处理器
 */
@Slf4j
@RestControllerAdvice
public class ResExceptionHandler {

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<Object> handleMissingServletRequestParameterException(MissingServletRequestParameterException e) {
        log.error("缺少请求参数", e);
        return new Result<>(CodeEnum.PARAM_ERROR.getCode(), "缺少请求参数");
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Object> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.error("参数解析失败", e);
        return new Result<>(CodeEnum.PARAM_ERROR.getCode(), "参数解析失败");
    }

    @ResponseStatus(HttpStatus.OK)
    @ExceptionHandler(ApiException.class)
    public Result<Object> handleApiException(ApiException e) {
        log.error("服务器异常", e);
        if (StringUtils.isNotEmpty(e.getCode())) {
            return new Result<>(e.getCode(), e.getMessage());
        }
        return new Result<>(CodeEnum.FAIL.getCode(), e.getMessage());
    }

    @ResponseStatus(HttpStatus.OK)
    @ExceptionHandler(value = RuntimeException.class)
    public Result<Object> handlerRuntimeException(RuntimeException e) {
        log.error("RuntimeException", e);
        // 自定义异常特殊处理
        return new Result<>(CodeEnum.SYSTEM_ERROR.getCode(), CodeEnum.SYSTEM_ERROR.getMessage());
    }

    @ResponseStatus(HttpStatus.OK)
    @ExceptionHandler(value = Exception.class)
    public Result<Object> defaultErrorHandler(Exception e) {
        log.error("系统异常", e);
        return new Result<>(CodeEnum.SYSTEM_ERROR.getCode(), CodeEnum.SYSTEM_ERROR.getMessage());
    }

}
