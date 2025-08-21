package com.ww.mall.monitor.core.aop;

import cn.hutool.core.util.StrUtil;
import com.ww.app.common.common.ClientUser;
import com.ww.app.common.context.AuthorizationContext;
import com.ww.app.common.utils.HttpContextUtils;
import com.ww.app.common.utils.TracerUtils;
import com.ww.mall.monitor.core.annotation.BizTrace;
import lombok.extern.slf4j.Slf4j;
import org.apache.skywalking.apm.toolkit.trace.ActiveSpan;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.Method;
import java.util.Arrays;

import static com.ww.mall.monitor.constant.SkywalkingConstant.*;

@Slf4j
@Aspect
public class BizTraceAspect {

    @Around("@annotation(com.ww.mall.monitor.core.annotation.BizTrace)")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Method method = signature.getMethod();
        BizTrace ann = method.getAnnotation(BizTrace.class);
        if (ann == null) {
            ann = pjp.getTarget().getClass().getAnnotation(BizTrace.class);
        }

        String operation = (ann != null && !ann.operation().isEmpty())
                ? ann.operation() : method.getDeclaringClass().getSimpleName() + StrUtil.COLON + method.getName();

        // 主动创建本地埋点
        ActiveSpan.tag(BIZ_NAME, operation);
        ActiveSpan.tag(REQ_IP, HttpContextUtils.getAppReqIp());
        ActiveSpan.tag(BIZ_TRACE_ID, TracerUtils.getTraceId());
        if (ann != null && ann.includeArgs()) {
            try {
                ActiveSpan.tag(BIZ_PARAMS, truncate(Arrays.toString(pjp.getArgs())));
            } catch (Throwable ignore) { }
        }

        // 用户上下文
        if (ann != null && ann.includeUser()) {
            try {
                ClientUser clientUser = AuthorizationContext.getClientUser(false);
                if (clientUser != null) {
                    ActiveSpan.tag(REQ_USER_ID, String.valueOf(clientUser.getId()));
                    if (clientUser.getChannelId() != null) {
                        ActiveSpan.tag(BIZ_CHANNEL_ID, String.valueOf(clientUser.getChannelId()));
                    }
                }
            } catch (Throwable ignore) { }
        }

        try {
            Object ret = pjp.proceed();
            if (ann != null && ann.includeReturn() && ret != null) {
                try {
                    ActiveSpan.tag(BIZ_RETURN_OBJ, truncate(String.valueOf(ret)));
                } catch (Throwable ignore) { }
            }
            return ret;
        } catch (Throwable e) {
            ActiveSpan.error(e);
            throw e;
        }
    }

    private String truncate(String s) {
        if (s == null) return null;
        int max = 1024;
        return s.length() > max ? s.substring(0, max) + "..." : s;
    }

}


