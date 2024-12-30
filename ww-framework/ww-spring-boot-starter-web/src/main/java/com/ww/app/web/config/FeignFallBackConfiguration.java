package com.ww.app.web.config;

import com.ww.app.admin.user.fallback.AdminUserApiFallBack;
import com.ww.app.member.member.fallback.MemberApiFallBack;
import com.ww.app.third.sms.fallback.SmsApiFallBack;
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
    public SmsApiFallBack smsApiFallBack() {
        return new SmsApiFallBack();
    }

}
