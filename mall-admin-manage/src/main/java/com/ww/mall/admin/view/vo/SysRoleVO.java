package com.ww.mall.admin.view.vo;

import com.ww.mall.common.enums.SysPlatformType;
import lombok.Data;

/**
 * @author ww
 * @create 2024-05-21- 14:19
 * @description:
 */
@Data
public class SysRoleVO {

    private Long id;

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
