package com.ww.app.security.filter;

import com.ww.app.common.common.AdminUser;
import com.ww.app.common.common.BaseUser;
import com.ww.app.common.common.ClientUser;
import com.ww.app.common.constant.Constant;
import com.ww.app.common.enums.UserType;
import com.ww.app.common.exception.ApiException;
import com.ww.app.security.component.AuthorityStore;
import com.ww.app.common.context.AuthorizationContext;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

import static com.ww.app.common.enums.GlobalResCodeConstants.ILLEGAL_REQUEST;

/**
 * @author ww
 * @create 2024-09-20- 17:34
 * @description: token校验过滤器
 */
@Slf4j
@Component
public class TokenAuthenticationAuthFilter extends OncePerRequestFilter {

    @Autowired(required = false)
    private AuthorityStore authorityStore;

    @Override
    protected void doFilterInternal(HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws ServletException, IOException {
        String token = request.getHeader(Constant.USER_TOKEN_KEY);
        if (StringUtils.isNotEmpty(token)) {
            String userType = request.getHeader(Constant.USER_TYPE);
            if (UserType.ADMIN.name().equals(userType)) {
                AdminUser adminUser = AuthorizationContext.getAdminUser();
                Authentication authentication = buildAuthentication(request, adminUser);
                // 告诉security此用户jwt已经认证过了
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } else if (UserType.CLIENT.name().equals(userType)) {
                ClientUser clientUser = AuthorizationContext.getClientUser();
                Authentication authentication = buildAuthentication(request, clientUser);
                // 告诉security此用户jwt已经认证过了
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } else {
                throw new ApiException(ILLEGAL_REQUEST);
            }
        }
        // 不携带token或者token校验正常放行
        filterChain.doFilter(request, response);
    }

    private Authentication buildAuthentication(HttpServletRequest request, BaseUser baseUser) {
        List<GrantedAuthority> grantedAuthorities = authorityStore == null ? null : authorityStore.loadCurrentUserAuthorities(baseUser.getId());
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(baseUser, null, grantedAuthorities);
        authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        return authenticationToken;
    }
}
