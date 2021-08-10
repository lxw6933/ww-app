package com.ww.mall.mvc.view.query.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ww.mall.mvc.entity.SysRoleEntity;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

/**
 * 后台角色表 - Query
 *
 * @author ww
 * @date 2021-06-26 11:36:58
 */
@Data
public class SysRoleQuery {

    /**
     * 角色名称
     */
    private String roleName;


    public QueryWrapper<SysRoleEntity> getQueryWrapper() {
        QueryWrapper<SysRoleEntity> wrapper = new QueryWrapper<>();
        if (StringUtils.isNotBlank(this.roleName)) {
            wrapper.like("`role_name`", this.roleName);
        }
        return wrapper;
    }

}