package com.ww.mall.mvc.view.query.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ww.mall.mvc.entity.SysDictTypeEntity;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

/**
 * @description:
 * @author: ww
 * @create: 2021-05-18 17:46
 */
@Data
public class SysDictTypeQuery {

    /**
     * id
     */
    private Long id;

    /**
     * 字典名称
     */
    private String name;

    /**
     * 字典类型
     */
    private String type;

    /**
     * 状态（0：停用，1：正常）
     */
    private Boolean status;

    /**
     * 备注
     */
    private String remarks;

    public QueryWrapper<SysDictTypeEntity> getQueryWrapper() {
        QueryWrapper<SysDictTypeEntity> wrapper = new QueryWrapper<>();
        if (this.id != null) {
            wrapper.eq("`id`", this.id);
        }
        if (StringUtils.isNotBlank(this.name)) {
            wrapper.like("`name`", this.name);
        }
        if (StringUtils.isNotBlank(this.type)) {
            wrapper.like("`type`", this.type);
        }
        if (this.status != null) {
            wrapper.eq("`status`", this.status);
        }
        if (StringUtils.isNotBlank(this.remarks)) {
            wrapper.like("`remarks`", this.remarks);
        }
        return wrapper;
    }

}

