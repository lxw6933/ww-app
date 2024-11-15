package com.ww.mall.web.config;

import com.ww.mall.admin.user.fallback.AdminUserApiFallBack;
import com.ww.mall.member.member.fallback.MemberApiFallBack;
import com.ww.mall.web.feign.inner.ThirdServerFeignServiceFallBack;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @description:
 * @author: ww
 * @create: 2023/7/22 14:58
 **/
@Configuration
public class FeignFallBackConfiguration {

    @Bean
    public AdminUserApiFallBack adminUserApiFallBack() {
        return new AdminUserApiFallBack();
    }

    @Bean
    public MemberApiFallBack memberApiFallBack() {
        return new MemberApiFallBack();
    }

    @Bean
    public ThirdServerFeignServiceFallBack thirdServerFeignServiceFallBack() {
        return new ThirdServerFeignServiceFallBack();
    }

}
