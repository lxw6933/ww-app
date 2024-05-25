package com.ww.mall.open.domain.client.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ww.mall.web.cmmon.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author ww
 * @create 2024-05-25 15:22
 * @description:
 */
@Data
@TableName("open_business_client_info")
@EqualsAndHashCode(callSuper = true)
public class BusinessClientInfo extends BaseEntity {
}
