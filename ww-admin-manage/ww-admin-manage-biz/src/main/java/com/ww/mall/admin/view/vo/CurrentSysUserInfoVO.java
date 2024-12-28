package com.ww.mall.admin.view.vo;

import cn.hutool.core.lang.tree.Tree;
import lombok.Data;

import java.util.List;

/**
 * @author ww
 * @create 2024-09-12 21:12
 * @description:
 */
@Data
public class CurrentSysUserInfoVO {
    /**
     * 用户id
     */
    private Long userId;

    /**
     * 用户名（账号名称）
     */
    private String username;

    /**
     * 用户昵称（姓名）
     */
    private String realName;

    /**
     * 头像
     */
    private String avatar;

    /**
     * 用户权限集合
     */
    private List<Tree<Long>> menuTreeList;

    /**
     * 用户权限标识集合
     */
    private List<String> permissions;
}
