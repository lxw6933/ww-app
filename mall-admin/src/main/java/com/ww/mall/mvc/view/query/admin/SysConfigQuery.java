package com.ww.mall.mvc.view.query.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ww.mall.mvc.entity.SysConfigEntity;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

/**
 * @description:
 * @author: ww
 * @create: 2021-05-18 16:59
 */
@Data
public class SysConfigQuery {

    /**
     * id
     */
    private Long id;

    /**
     * key
     */
    private String paramKey;

    /**
     * 状态 0：隐藏 1：显示
     */
    private Boolean status;

    public QueryWrapper<SysConfigEntity> getQueryWrapper() {
        QueryWrapper<SysConfigEntity> wrapper = new QueryWrapper<>();
        if (this.id != null) {
            wrapper.eq("`id`", this.id);
        }
        if (StringUtils.isNotBlank(this.paramKey)) {
            wrapper.like("`paramKey`", this.paramKey);
        }
        if (this.status != null) {
            wrapper.eq("`status`", this.status);
        }
        return wrapper;
    }

}
