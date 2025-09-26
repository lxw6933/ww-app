package com.ww.app.member.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ww.app.mybatis.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author ww
 * @create 2025-09-26 13:52
 * @description:
 */
@Data
@TableName("member_address")
@EqualsAndHashCode(callSuper = true)
public class MemberAddress extends BaseEntity {

    /**
     * 用户编号
     */
    private Long userId;

    /**
     * 收件人名称
     */
    private String name;

    /**
     * 手机号
     */
    private String mobile;

    /**
     * 地区编号
     */
    private Long areaId;

    /**
     * 收件详细地址
     */
    private String detailAddress;

    /**
     * 是否默认
     * <p>
     * true - 默认收件地址
     */
    private Boolean defaultStatus;

}
