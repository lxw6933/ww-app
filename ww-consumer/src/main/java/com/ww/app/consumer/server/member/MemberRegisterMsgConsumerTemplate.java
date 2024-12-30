package com.ww.app.consumer.server.member;

import cn.hutool.extra.spring.SpringUtil;
import com.ww.app.common.common.Result;
import com.ww.app.member.member.rpc.MemberApi;
import com.ww.app.member.member.bo.AddMemberIntegralBO;
import com.ww.app.rabbitmq.template.MsgConsumerTemplate;
import lombok.extern.slf4j.Slf4j;

/**
 * @description:
 * @author: ww
 * @create: 2023/7/22 23:13
 **/
@Slf4j
public class MemberRegisterMsgConsumerTemplate extends MsgConsumerTemplate<Long> {
    private final MemberApi memberApi = SpringUtil.getBean(MemberApi.class);

    @Override
    public boolean serverHandler(Long msg) {
        AddMemberIntegralBO addMemberIntegralBO = new AddMemberIntegralBO();
        addMemberIntegralBO.setMemberId(msg);
        addMemberIntegralBO.setIntegralType(true);
        addMemberIntegralBO.setIntegralNum(100);
        Result<Boolean> booleanResult = memberApi.addMemberIntegral(addMemberIntegralBO);
        booleanResult.checkError();
        if (Boolean.TRUE.equals(booleanResult.getData())) {
            log.info("[新用户注册]添加积分成功");
            return true;
        } else {
            log.warn("[新用户注册]添加积分失败");
            return false;
        }
    }
}
