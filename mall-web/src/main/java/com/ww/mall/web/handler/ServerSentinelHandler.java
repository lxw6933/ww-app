package com.ww.mall.web.handler;

import com.alibaba.csp.sentinel.adapter.spring.webmvc.callback.BlockExceptionHandler;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.fastjson.JSON;
import com.alibaba.nacos.common.http.param.MediaType;
import com.ww.mall.common.common.Result;
import com.ww.mall.common.enums.CodeEnum;
import lombok.extern.slf4j.Slf4j;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;

/**
 * @description: 所有服务限流回调
 * @author: ww
 * @create: 2023/7/16 10:19
 **/
@Slf4j
public class ServerSentinelHandler implements BlockExceptionHandler {

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, BlockException e) throws Exception {
        log.error("服务请求：【{}】限流异常回调,异常原因：{}", request.getRequestURL(), e.getMessage());
        Result<Object> result = new Result<>(CodeEnum.LIMIT_ERROR.getCode(), CodeEnum.LIMIT_ERROR.getMessage());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON);
        response.getWriter().write(JSON.toJSONString(result));
    }
}
