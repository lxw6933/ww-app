package com.ww.mall.consumer.server.member;

import cn.hutool.extra.spring.SpringUtil;
import com.ww.mall.common.common.Result;
import com.ww.mall.common.exception.ApiException;
import com.ww.mall.rabbitmq.template.MsgConsumerTemplate;
import com.ww.mall.web.feign.MemberFeignService;
import com.ww.mall.web.view.bo.AddMemberIntegralBO;
import lombok.extern.slf4j.Slf4j;

/**
 * @description:
 * @author: ww
 * @create: 2023/7/22 23:13
 **/
@Slf4j
public class MemberRegisterMsgConsumerTemplate extends MsgConsumerTemplate<Long> {
    private final MemberFeignService memberFeignService = SpringUtil.getBean(MemberFeignService.class);

    @Override
    public boolean serverHandler(Long msg) {
        AddMemberIntegralBO addMemberIntegralBO = new AddMemberIntegralBO();
        addMemberIntegralBO.setMemberId(msg);
        addMemberIntegralBO.setIntegralType(true);
        addMemberIntegralBO.setIntegralNum(100);
        Result<Boolean> booleanResult = memberFeignService.addMemberIntegral(addMemberIntegralBO);
        if (Boolean.TRUE.equals(booleanResult.isSuccess())) {
            if (Boolean.TRUE.equals(booleanResult.getValue())) {
                log.info("【新用户注册】添加积分成功");
                return true;
            } else {
                log.warn("【新用户注册】添加积分失败");
                return false;
            }
        } else {
            // 远程调用失败
            log.error("【新用户注册】消费者远程调用失败：{}", booleanResult.getMessage());
            throw new ApiException("消费者远程调用失败");
        }
    }
}
