package com.ww.mall.config.security.filter;

import com.ww.mall.config.security.service.MyUserDetailsService;
import com.ww.mall.utils.HttpContextUtils;
import com.ww.mall.utils.JwtTokenUtils;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.MalformedJwtException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.annotation.Resource;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @description: jwt过滤器
 * @author: ww
 * @create: 2021/6/26 上午7:32
 **/
@Component
public class JwtTokenFilter extends OncePerRequestFilter {

    @Resource
    private MyUserDetailsService myUserDetailsService;
    @Resource
    private JwtTokenUtils jwtTokenUtils;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        System.out.println("执行token过滤器");
        String token = request.getHeader(jwtTokenUtils.getHeader());
        String username = null;
        // 判断请求头是否携带token，携带token进行验证
        if (!StringUtils.isEmpty(token)) {
            try {
                username = jwtTokenUtils.getUsernameFromToken(token);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
                logger.warn("获取 JWT Token 信息异常");
                HttpContextUtils.backJson(response, R.error("获取 JWT Token 信息异常"));
            } catch (ExpiredJwtException e) {
                e.printStackTrace();
                logger.warn("JWT Token 已过期");
                HttpContextUtils.backJson(response, R.error("JWT Token 已过期"));
                return;
            } catch (MalformedJwtException e) {
                e.printStackTrace();
                logger.warn("获取 JWT Token 信息错误：");
                HttpContextUtils.backJson(response, R.error("获取 JWT Token 信息错误"));
                return;
            } catch (JwtException e) {
                e.printStackTrace();
                HttpContextUtils.backJson(response, R.error("JWT 未知异常"));
                return;
            } catch (Exception e) {
                e.printStackTrace();
                HttpContextUtils.backJson(response, R.error(e.getMessage()));
                return;
            }
        }
        // 验证token中携带的信息是否正确
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = this.myUserDetailsService.loadUserByUsername(username);
            // 验证token的有效性
            try {
                if (jwtTokenUtils.validateToken(token, userDetails)) {
                    // 给使用此jwt的用户进行授权(将userDetails 赋值给 principal，避免重复查询数据库)
                    UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                    usernamePasswordAuthenticationToken
                            .setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    // 告诉security此用户jwt已经认证过了
                    SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
                }
            } catch (Exception e) {
                logger.warn(e.getMessage());
                HttpContextUtils.backJson(response, R.error(e.getMessage()));
                return;
            }
        }
        // 不携带token或者token校验正常放行
        filterChain.doFilter(request, response);
    }

}
