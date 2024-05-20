package com.ww.mall.admin.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ww.mall.admin.enums.SysPlatformType;
import com.ww.mall.web.cmmon.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author ww
 * @create 2024-05-20- 09:44
 * @description:
 */
@Data
@TableName("sys_role")
@EqualsAndHashCode(callSuper = true)
public class SysRole extends BaseEntity {

    /**
     * 平台  角色类型
     */
    private SysPlatformType platform;

    /**
     * 角色名称
     */
    private String name;

    /**
     * 角色编号
     */
    private String roleNo;

    /**
     * 备注
     */
    private String remark;

    /**
     * 状态
     */
    private Boolean status;

    /**
     * 是否有效
     */
    private Boolean valid;

    /**
     * 平台id
     */
    private Long platformId;

}
