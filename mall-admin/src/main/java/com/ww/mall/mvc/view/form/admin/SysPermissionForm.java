package com.ww.mall.mvc.view.form.admin;

import com.ww.mall.common.valid.group.UpdateGroup;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

/**
 * 后台权限表 - Form
 *
 * @author ww
 * @date 2021-06-26 11:36:58
 */
@Data
public class SysPermissionForm {

    /**
     * 权限ID
     */
    @NotNull(groups = {UpdateGroup.class}, message = "id不能为空")
    private Long id;

    /**
     * 父级权限 关联 sys_permission
     */
    private Long parentId;

    /**
     * 权限名称
     */
    @NotEmpty(message = "name不能为空")
    @Length(max = 50, message = "name字符长度不能大于50")
    private String name;

    /**
     * 权限类型（0：目录；1：菜单；2：权限）
     */
    @NotNull(message = "type不能为空")
    private Integer type;

    /**
     * 权限标识码 格式：xx:xx
     */
    @Length(max = 100, message = "code字符长度不能大于100")
    private String code;

    /**
     * 权限url
     */
    @Length(max = 255, message = "url字符长度不能大于255")
    private String url;

    /**
     * 权限api(多个url用,分隔)
     */
    @Length(max = 65535, message = "permUrl字符长度不能大于65535")
    private String permUrl;

    /**
     * 菜单图标
     */
    @Length(max = 100, message = "icon字符长度不能大于100")
    private String icon;

    /**
     * 排序(默认10)
     */
    private Integer sort;

}
