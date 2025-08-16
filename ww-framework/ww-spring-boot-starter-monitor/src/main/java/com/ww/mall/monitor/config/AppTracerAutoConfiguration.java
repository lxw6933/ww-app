package com.ww.mall.monitor.config;

import com.ww.app.common.enums.WebFilterOrderEnum;
import com.ww.mall.monitor.core.aop.BizTraceAspect;
import com.ww.mall.monitor.core.filter.TraceFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;

/**
 * @author ww
 * @create 2025-08-14 23:07
 * @description:
 */
@AutoConfiguration
public class AppTracerAutoConfiguration {

    @Bean
    public BizTraceAspect bizTracingAop() {
        return new BizTraceAspect();
    }

    @Bean
    public FilterRegistrationBean<TraceFilter> traceFilter() {
        FilterRegistrationBean<TraceFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new TraceFilter());
        registrationBean.setOrder(WebFilterOrderEnum.TRACE_FILTER);
        return registrationBean;
    }

}
