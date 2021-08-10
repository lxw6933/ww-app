package com.ww.mall.resolver;

import com.ww.mall.annotation.LoginUser;
import com.ww.mall.common.common.R;
import com.ww.mall.common.constant.TokenKeyConstant;
import com.ww.mall.mvc.entity.User;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * @description: 有@LoginUser注解的方法参数，注入当前登录用户
 * @author: ww
 * @create: 2021-05-18 14:45
 */
@Deprecated
@Component
public class CurrentUserMethodArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterType().isAssignableFrom(User.class)
                && parameter.hasParameterAnnotation(LoginUser.class);
    }

    @Override
    public Object resolveArgument(MethodParameter methodParameter, ModelAndViewContainer modelAndViewContainer, NativeWebRequest request, WebDataBinderFactory webDataBinderFactory) throws Exception {
        User user = (User) request.getAttribute(TokenKeyConstant.APP_KEY, RequestAttributes.SCOPE_REQUEST);
        if (user == null) {
            return R.unlogin();
        }
        return user;
    }
}
