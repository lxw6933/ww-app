package com.ww.app.admin.view.form;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.mzt.logapi.starter.annotation.DiffLogField;
import com.ww.app.admin.operatelog.MenuParseFunction;
import com.ww.app.common.valid.group.UpdateGroup;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * @author ww
 * @create 2024-05-21- 13:32
 * @description:
 */
@Data
@Schema(description = "新增修改角色表单")
public class SysRoleForm {

    /**
     * 用户ID
     */
    @NotNull(groups = {UpdateGroup.class}, message = "id不能为空")
    @Schema(description = "角色id")
    private Long id;

    /**
     * 用户账号
     */
    @DiffLogField(name = "角色名称")
    @Schema(description = "角色名称")
    private String name;

    /**
     * 用户昵称
     */
    @DiffLogField(name = "角色标识")
    @Schema(description = "角色标识")
    private String roleNo;

    @DiffLogField(name = "角色状态")
    @Schema(description = "角色状态")
    private Boolean status;

    /**
     * 备注
     */
    @DiffLogField(name = "角色备注")
    @Schema(description = "角色备注")
    private String remark;

    /**
     * 权限id集合
     */
    @DiffLogField(name = "角色权限", function = MenuParseFunction.NAME)
    @Schema(description = "角色权限")
    @JsonAlias({"permissionIds", "permissions"})
    private List<Long> permissionIds;

}
