package com.ww.mall.admin.view.form;

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
public class SysUserForm {

    /**
     * 用户ID
     */
    @NotNull(groups = {UpdateGroup.class}, message = "id不能为空")
    private Long id;

    /**
     * 用户账号
     */
    private String username;

    /**
     * 用户昵称
     */
    private String nickname;

    /**
     * 头像
     */
    private String headPicture;

    /**
     * 性别
     */
    private Integer sex;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 电话号码
     */
    private String phone;

    /**
     * 备注
     */
    private String remark;

    /**
     * 角色id集合
     */
    private List<Long> roleIds;

}
