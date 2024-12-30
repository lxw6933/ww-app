package com.ww.app.admin.view.query;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ww.app.admin.entity.SysRole;
import com.ww.app.common.common.AppPage;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.StringUtils;

/**
 * @author ww
 * @create 2024-05-21- 14:47
 * @description:
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SysRolePageQuery extends AppPage {

    private String name;

    private Boolean status;

    public QueryWrapper<SysRole> getQueryWrapper() {
        QueryWrapper<SysRole> queryWrapper = new QueryWrapper<>();
        if (StringUtils.isNotEmpty(this.name)) {
            queryWrapper.like("name", this.name);
        }
        if (status != null) {
            queryWrapper.eq("status", this.status);
        }
        return queryWrapper;
    }

}
