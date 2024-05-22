package com.ww.mall.admin.view.form;

import com.ww.mall.common.enums.SysPlatformType;
import com.ww.mall.common.valid.group.UpdateGroup;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * @author ww
 * @create 2024-05-21- 13:32
 * @description:
 */
@Data
public class SysRoleForm {

    /**
     * 用户ID
     */
    @NotNull(groups = {UpdateGroup.class}, message = "id不能为空")
    private Long id;

    /**
     * 用户账号
     */
    private String name;

    /**
     * 用户昵称
     */
    private String roleNo;

    /**
     * 备注
     */
    private String remark;

    /**
     * 平台
     */
    private SysPlatformType platform;

}
