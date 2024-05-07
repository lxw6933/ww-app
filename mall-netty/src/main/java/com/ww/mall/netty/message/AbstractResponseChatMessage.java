package com.ww.mall.netty.message;

import com.ww.mall.netty.message.chat.MallChatMessage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * @author ww
 * @create 2024-05-07 19:48
 * @description:
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public abstract class AbstractResponseChatMessage extends MallChatMessage {
    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 结果消息
     */
    private String reason;

}
