package com.ww.mall.utils;

import org.springframework.http.MediaType;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Objects;

/**
 * @description:
 * @author: ww
 * @create: 2021-05-13 09:05
 */
public class HttpContextUtils {

    private HttpContextUtils() {}

    public static HttpServletRequest getHttpServletRequest() {
        return ((ServletRequestAttributes) Objects.requireNonNull(RequestContextHolder.getRequestAttributes())).getRequest();
    }

    /**
     * 获取项目上下文路径
     * @return path
     */
    public static String getProjectPath(){
        HttpServletRequest request = getHttpServletRequest();
        // http://localhost:8080/
        return request.getScheme() + "://" + request.getServerName() + ":"
                + request.getServerPort() + request.getContextPath() + "/";
    }

    /**
     * 向前端返回json
     * @param response response
     * @param json R
     */
    public static void backJson(HttpServletResponse response, R json) {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_OK);
        response.setCharacterEncoding("UTF-8");
        try {
            response.getWriter().write(json.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
