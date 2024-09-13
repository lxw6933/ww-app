package com.ww.mall.admin.view.query;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ww.mall.admin.entity.SysRole;
import com.ww.mall.common.common.MallPage;
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
public class SysRolePageQuery extends MallPage {

    private String name;

    public QueryWrapper<SysRole> getQueryWrapper() {
        QueryWrapper<SysRole> queryWrapper = new QueryWrapper<>();
        if (StringUtils.isNotEmpty(this.name)) {
            queryWrapper.like("name", this.name);
        }
        return queryWrapper;
    }

}
