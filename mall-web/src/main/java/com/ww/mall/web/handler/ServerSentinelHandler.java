package com.ww.mall.web.handler;

import com.alibaba.csp.sentinel.adapter.spring.webmvc.callback.BlockExceptionHandler;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.authority.AuthorityException;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeException;
import com.alibaba.csp.sentinel.slots.block.flow.FlowException;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowException;
import com.alibaba.csp.sentinel.slots.system.SystemBlockException;
import com.alibaba.fastjson.JSON;
import com.alibaba.nacos.common.http.param.MediaType;
import com.ww.mall.common.common.Result;
import com.ww.mall.common.enums.GlobalResCodeConstants;
import lombok.extern.slf4j.Slf4j;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;

import static com.ww.mall.common.common.Result.error;

/**
 * @description: 所有服务限流回调
 * @author: ww
 * @create: 2023/7/16 10:19
 **/
@Slf4j
public class ServerSentinelHandler implements BlockExceptionHandler {

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, BlockException e) throws Exception {
        Result<Object> result;
        if (e instanceof FlowException) {
            log.error("服务请求：[{}]接口限流：{}", request.getRequestURL(), e.getMessage());
            result = error(GlobalResCodeConstants.FLOW_EXCEPTION);
        } else if (e instanceof DegradeException) {
            log.error("服务请求：[{}]接口降级：{}", request.getRequestURL(), e.getMessage());
            result = error(GlobalResCodeConstants.DEGRADE_EXCEPTION);
        } else if (e instanceof ParamFlowException) {
            log.error("服务请求：[{}]参数限流：{}", request.getRequestURL(), e.getMessage());
            result = error(GlobalResCodeConstants.PARMA_FLOW_EXCEPTION);
        } else if (e instanceof SystemBlockException) {
            log.error("服务请求：[{}]系统限流：{}", request.getRequestURL(), e.getMessage());
            result = error(GlobalResCodeConstants.SYSTEM_BLOCK_EXCEPTION);
        } else if (e instanceof AuthorityException) {
            log.error("服务请求：[{}]权限控制：{}", request.getRequestURL(), e.getMessage());
            result = error(GlobalResCodeConstants.AUTH_LIMIT_EXCEPTION);
        } else {
            log.error("服务请求：[{}]未知异常：{}", request.getRequestURL(), e.getMessage());
            result = error(GlobalResCodeConstants.UNKNOWN);
        }
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON);
        response.getWriter().write(JSON.toJSONString(result));
    }
}
