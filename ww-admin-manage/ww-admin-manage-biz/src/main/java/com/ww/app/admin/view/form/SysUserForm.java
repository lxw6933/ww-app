package com.ww.app.admin.view.form;

import com.mzt.logapi.starter.annotation.DiffLogField;
import com.ww.app.admin.operatelog.RoleParseFunction;
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
@Schema(description = "新增修改用户表单")
public class SysUserForm {

    /**
     * 用户ID
     */
    @NotNull(groups = {UpdateGroup.class}, message = "id不能为空")
    @Schema(description = "用户id")
    private Long id;

    /**
     * 用户账号
     */
    @DiffLogField(name = "用户账号")
    @Schema(description = "用户账号")
    private String username;

    /**
     * 用户昵称
     */
    @DiffLogField(name = "用户昵称")
    @Schema(description = "用户昵称")
    private String realName;

    /**
     * 头像
     */
    @DiffLogField(name = "用户头像")
    @Schema(description = "用户头像")
    private String avatar;

    /**
     * 邮箱
     */
    @DiffLogField(name = "用户邮箱")
    @Schema(description = "用户邮箱")
    private String email;

    /**
     * 电话号码
     */
    @DiffLogField(name = "用户手机号")
    @Schema(description = "用户手机号")
    private String phone;

    /**
     * 备注
     */
    @DiffLogField(name = "备注")
    @Schema(description = "用户备注")
    private String remark;

    /**
     * 角色id集合
     */
    @DiffLogField(name = "用户角色", function = RoleParseFunction.NAME)
    @Schema(description = "用户角色id集合")
    private List<Long> roleIds;

}
