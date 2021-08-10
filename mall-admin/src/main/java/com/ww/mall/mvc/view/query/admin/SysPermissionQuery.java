package com.ww.mall.mvc.view.query.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ww.mall.mvc.entity.SysPermissionEntity;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

/**
 * 后台权限表 - Query
 *
 * @author ww
 * @date 2021-06-26 11:36:58
 */
@Data
public class SysPermissionQuery {

    /**
     * 权限ID
     */
    private Long id;

    /**
     * 父级权限 关联 sys_permission
     */
    private Long parentId;

    /**
     * 权限名称
     */
    private String name;

    /**
     * 权限类型（0：目录；1：菜单；2：权限）
     */
    private Integer type;

    /**
     * 权限url
     */
    private String url;

    /**
     * 权限api(多个url用,分隔)
     */
    private String permUrl;


    public QueryWrapper<SysPermissionEntity> getQueryWrapper() {
        QueryWrapper<SysPermissionEntity> wrapper = new QueryWrapper<>();
        if (this.id != null) {
            wrapper.eq("`id`", this.id);
        }
        if (this.parentId != null) {
            wrapper.eq("`parent_id`", this.parentId);
        }
        if (StringUtils.isNotBlank(this.name)) {
            wrapper.like("`name`", this.name);
        }
        if (this.type != null) {
            wrapper.eq("`type`", this.type);
        }
        if (StringUtils.isNotBlank(this.url)) {
            wrapper.like("`url`", this.url);
        }
        if (StringUtils.isNotBlank(this.permUrl)) {
            wrapper.like("`perm_url`", this.permUrl);
        }
        return wrapper;
    }

}