package com.ww.mall.admin.view.query;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ww.mall.admin.entity.SysUser;
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
public class SysUserPageQuery extends MallPage {

    private String username;

    private Boolean status;

    private Boolean valid;

    public QueryWrapper<SysUser> getQueryWrapper() {
        QueryWrapper<SysUser> queryWrapper = new QueryWrapper<>();
        if (StringUtils.isNotEmpty(this.username)) {
            queryWrapper.like("username", this.username);
        }
        if (this.valid != null) {
            queryWrapper.eq("valid", this.valid);
        }
        if (this.status != null) {
            queryWrapper.eq("status", this.status);
        }
        return queryWrapper;
    }

}
