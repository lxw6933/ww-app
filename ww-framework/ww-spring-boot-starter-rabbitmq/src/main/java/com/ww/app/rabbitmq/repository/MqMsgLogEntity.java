package com.ww.app.rabbitmq.repository;

import com.ww.app.rabbitmq.common.BaseMqLog;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * @description:
 * @author: ww
 * @create: 2023/7/15 23:42
 **/
@Data
@Document("t_mq_msg_log")
@EqualsAndHashCode(callSuper = true)
public class MqMsgLogEntity extends BaseMqLog {

    /**
     * 是否删除（0：否，1：是）
     */
    private Boolean isDel;

    public MqMsgLogEntity() {}

}
