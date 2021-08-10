package com.ww.mall.mvc.view.form.admin;

import com.ww.mall.common.valid.group.UpdateGroup;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 后台角色表 - Form
 *
 * @author ww
 * @date 2021-06-26 11:36:58
 */
@Data
public class SysRoleForm {

    /**
     * 角色ID
     */
    @NotNull(groups = {UpdateGroup.class}, message = "id不能为空")
    private Long id;

    /**
     * 角色名称
     */
    @NotEmpty(message = "roleName不能为空")
    @Length(max = 50, message = "roleName字符长度不能大于50")
    private String roleName;

    /**
     * 角色标识码 格式：ADMIN
     */
    @NotEmpty(message = "roleNo不能为空")
    @Length(max = 50, message = "roleNo字符长度不能大于50")
    private String roleNo;

    /**
     * 角色描述
     */
    @Length(max = 100, message = "roleDesc字符长度不能大于100")
    private String roleDesc;

    /**
     * 排序（默认10）
     */
    private Integer roleSort;

    /**
     * 权限id集合
     */
    private List<Long> permissionIds;

}
