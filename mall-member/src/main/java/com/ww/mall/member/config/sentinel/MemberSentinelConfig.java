package com.ww.mall.member.config.sentinel;

import org.springframework.context.annotation.Configuration;

/**
 * @description: 全局限流返回格式
 * @author: ww
 * @create: 2021/7/7 下午6:40
 **/
@Configuration
public class MemberSentinelConfig {

    public MemberSentinelConfig() {
//        WenCall
//        WebCallbackManager.setUrlBlockHandler(new UrlBlockHandler() {
//            @Override
//            public void blocked(HttpServletRequest request, HttpServletResponse response, BlockException e) throws IOException {
//                response.setCharacterEncoding("UTF-8");
//                response.setContentType("application/json");
//                response.getWriter().write(R.error("请求人数过多，请稍后再试！！！").toString());
//            }
//        });
    }


}
