package com.ww.app.seckill.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document("t_secKill_order")
public class SecKillOrder {

    @Id
    private String id;

    private Long userId;

    private String skuCode;

    private Integer orderType;

    private String orderNo;

    private String createTime;
}
