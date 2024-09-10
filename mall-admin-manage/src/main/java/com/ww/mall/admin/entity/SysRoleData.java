package com.ww.mall.admin.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * @author ww
 * @create 2024-05-20- 16:14
 * @description:
 */
@Data
@TableName("sys_role_data")
public class SysRoleData {

    /**
     * 角色id
     */
    private Long roleId;

    /**
     * 权限数据id【渠道id，商家id】
     */
    private Long dataId;

}
