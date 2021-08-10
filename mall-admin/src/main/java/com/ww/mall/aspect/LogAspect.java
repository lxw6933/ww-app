package com.ww.mall.aspect;

import com.alibaba.fastjson.JSON;
import com.ww.mall.common.utils.JsonUtils;
import com.ww.mall.utils.HttpContextUtils;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.text.SimpleDateFormat;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * @description: sql打印日誌
 * @author: ww
 * @create: 2021-05-17 14:02
 */
@Slf4j
@Aspect
@Component
public class LogAspect {

    @Pointcut("execution(public * com.ww.mall.mvc.controller..*.*(..))")
    public void executionService() {
        // Do nothing because of pointcut expression.
    }

    @Around("executionService()")
    public Object doAround(ProceedingJoinPoint pjp) throws Throwable {
        HttpServletRequest request = HttpContextUtils.getHttpServletRequest();
        Enumeration<String> headerNames = request.getHeaderNames();
        Map<String, Object> headerMap = new HashMap<>(10);
        do {
            String header = headerNames.nextElement();
            headerMap.put(header, request.getHeader(header));
        } while (headerNames.hasMoreElements());
        long start = System.currentTimeMillis();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        log.info("***************************start " + sdf.format(start) + " *************************************************");
        String method = request.getMethod();
        String uri = request.getRequestURI();
        log.info("\n" +
                        "请求地址  >>>  {}\n" +
                        "请求方法  >>>  {}\n" +
                        "请求参数  >>>  {}\n" +
                        "请求来源  >>>  {}\n" +
                        "内容类型  >>>  {}\n" +
                        "请求头部  >>>  {}\n",
                uri,
                method,
                JsonUtils.toJson(request.getParameterMap()),
                /*paraString,*/
                request.getRemoteAddr(),
                request.getContentType(),
                JSON.toJSONString(headerMap));
        Object result = pjp.proceed();
        long end = System.currentTimeMillis();
        log.info("\n" +
                "请求结束" + uri + " " + sdf.format(end) + "耗时 " + (end - start) + "ms" + "\n" +
                JSON.toJSONString(result, true));
        log.info("***************************end   " + sdf.format(end) + "耗时 " + (end - start) + "ms");
        return result;
    }

}

