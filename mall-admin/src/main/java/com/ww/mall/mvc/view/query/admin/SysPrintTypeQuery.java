package com.ww.mall.mvc.view.query.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ww.mall.mvc.entity.SysPrintTypeEntity;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

/**
 * @description:
 * @author: ww
 * @create: 2021-05-17 13:50
 */
@Data
public class SysPrintTypeQuery {

    /**
     * ID
     */
    private Long id;

    /**
     * 类型名称
     */
    private String name;

    /**
     * 所属平台（0：平台，1：租户）
     */
    private Integer systemType;


    public QueryWrapper<SysPrintTypeEntity> getQueryWrapper() {
        QueryWrapper<SysPrintTypeEntity> wrapper = new QueryWrapper<>();
        if (this.id != null) {
            wrapper.eq("`id`", this.id);
        }
        if (StringUtils.isNotBlank(this.name)) {
            wrapper.like("`name`", this.name);
        }
        if (this.systemType != null) {
            wrapper.eq("`system_type`", this.systemType);
        }
        return wrapper;
    }

}
