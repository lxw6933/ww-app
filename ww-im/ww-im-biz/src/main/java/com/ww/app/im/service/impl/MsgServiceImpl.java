package com.ww.app.im.service.impl;

import cn.hutool.extra.spring.SpringUtil;
import com.ww.app.common.exception.ApiException;
import com.ww.app.im.common.ImMsgBody;
import com.ww.app.im.handler.MsgHandler;
import com.ww.app.im.service.MsgService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author ww
 * @create 2024-12-25 21:26
 * @description: 处理业务消息
 */
@Slf4j
@Service
public class MsgServiceImpl implements MsgService, InitializingBean {

    private final List<MsgHandler> msgHandlers = new ArrayList<>();

    @Override
    public void handleImMsg(ImMsgBody imMsgBody) {
        MsgHandler msgHandler = getMsgHandlerAdapter(imMsgBody);
        msgHandler.handle(imMsgBody);
    }

    private MsgHandler getMsgHandlerAdapter(ImMsgBody imMsgBody) {
        for (MsgHandler handler : this.msgHandlers) {
            if (handler.supports(imMsgBody)) {
                return handler;
            }
        }
        throw new ApiException("未找到业务消息处理器");
    }

    @Override
    public void afterPropertiesSet() {
        Map<String, MsgHandler> msgHandlerMap = SpringUtil.getBeansOfType(MsgHandler.class);
        if (!msgHandlerMap.isEmpty()) {
            msgHandlers.addAll(msgHandlerMap.values());
        }
    }
}
