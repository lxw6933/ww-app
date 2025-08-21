package com.ww.app.common.utils;

import com.alibaba.fastjson.JSON;
import com.ww.app.common.common.Result;
import com.ww.app.common.constant.Constant;
import org.springframework.http.MediaType;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Objects;

/**
 * @author ww
 * @create 2024-09-20- 17:22
 * @description:
 */
public class HttpContextUtils {

    private HttpContextUtils() {}

    public static HttpServletRequest getHttpServletRequest() {
        return ((ServletRequestAttributes) Objects.requireNonNull(RequestContextHolder.getRequestAttributes())).getRequest();
    }

    public static String getAppReqIp() {
        return HttpContextUtils.getHttpServletRequest().getHeader(Constant.USER_REAL_IP);
    }

    public static String getProjectPath(){
        HttpServletRequest request = getHttpServletRequest();
        return request.getScheme() + "://" + request.getServerName() + ":"
                + request.getServerPort() + request.getContextPath() + "/";
    }

    public static void write(HttpServletResponse response, Result<Object> result) {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_OK);
        response.setCharacterEncoding("UTF-8");
        try(PrintWriter writer = response.getWriter()) {
            writer.write(JSON.toJSONString(result));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
