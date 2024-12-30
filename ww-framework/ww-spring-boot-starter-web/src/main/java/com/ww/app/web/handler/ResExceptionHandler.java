package com.ww.app.web.handler;

import com.ww.app.common.common.Result;
import com.ww.app.common.enums.GlobalResCodeConstants;
import com.ww.app.common.exception.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

import static com.ww.app.common.common.Result.error;
import static com.ww.app.common.common.Result.success;

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
        return success(GlobalResCodeConstants.BAD_REQUEST);
    }

    @ResponseStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public Result<Object> handleHttpMediaTypeNotSupportedException(HttpMediaTypeNotSupportedException e) {
        log.error("不支持当前媒体类型", e);
        return success(GlobalResCodeConstants.NOT_SUPPORTED_MEDIA);
    }

    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public Result<Object> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        log.error("不支持当前请求方法", e);
        return success(GlobalResCodeConstants.METHOD_NOT_ALLOWED);
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(NoHandlerFoundException.class)
    public Result<Object> noHandlerFoundException(NoHandlerFoundException e) {
        log.error("请求url不存在", e);
        return success(GlobalResCodeConstants.NOT_FOUND);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Object> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.error("参数解析失败", e);
        return success(GlobalResCodeConstants.BAD_REQUEST);
    }

    @ResponseStatus(HttpStatus.OK)
    @ExceptionHandler(ApiException.class)
    public Result<Object> handleApiException(ApiException e) {
        log.error("业务接口异常: {}", e.getMessage());
        return error(e);
    }

    @ResponseStatus(HttpStatus.OK)
    @ExceptionHandler(value = RuntimeException.class)
    public Result<Object> handlerRuntimeException(RuntimeException e) {
        log.error("RuntimeException", e);
        // 自定义异常特殊处理
        return error(GlobalResCodeConstants.SYSTEM_ERROR);
    }

    @ResponseStatus(HttpStatus.OK)
    @ExceptionHandler(value = Exception.class)
    public Result<Object> defaultErrorHandler(Exception e) {
        log.error("系统异常", e);
        return error(GlobalResCodeConstants.SYSTEM_ERROR);
    }

}
