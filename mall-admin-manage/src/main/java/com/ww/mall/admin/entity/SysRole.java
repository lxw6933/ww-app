package com.ww.mall.admin.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ww.mall.mybatis.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author ww
 * @create 2024-05-20- 09:44
 * @description:
 */
@Data
@TableName("sys_role")
@EqualsAndHashCode(callSuper = true)
public class SysRole extends BaseEntity {

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

}
