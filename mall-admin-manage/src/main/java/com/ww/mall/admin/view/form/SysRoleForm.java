package com.ww.mall.admin.view.form;

import com.mzt.logapi.starter.annotation.DiffLogField;
import com.ww.mall.admin.framework.operatelog.MenuParseFunction;
import com.ww.mall.common.valid.group.UpdateGroup;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;

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
    @DiffLogField(name = "角色名称")
    private String name;

    /**
     * 用户昵称
     */
    @DiffLogField(name = "角色标识")
    private String roleNo;

    /**
     * 备注
     */
    @DiffLogField(name = "角色备注")
    private String remark;

    /**
     * 权限id集合
     */
    @DiffLogField(name = "角色权限", function = MenuParseFunction.NAME)
    private List<Long> permissionIds;

}
