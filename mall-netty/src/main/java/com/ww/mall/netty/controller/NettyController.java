package com.ww.mall.netty.controller;

import com.ww.mall.netty.config.ClientConfig;
import com.ww.mall.netty.message.chat.req.LoginRequestMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * @author ww
 * @create 2024-05-09- 09:05
 * @description:
 */
@RestController
public class NettyController {

    @Resource
    private ClientConfig clientConfig;

    @GetMapping("/send")
    public String send() {
        LoginRequestMessage message = new LoginRequestMessage();
        message.setUsername("zahngsan");
        message.setPassword("123");
//        MessageBase.Message message = new MessageBase.Message()
//                .toBuilder().setCmd(MessageBase.Message.CommandType.NORMAL)
//                .setContent("hello server")
//                .setRequestId(UUID.randomUUID().toString()).build();
        clientConfig.sendMsg(message);
        return "send ok";
    }

}
