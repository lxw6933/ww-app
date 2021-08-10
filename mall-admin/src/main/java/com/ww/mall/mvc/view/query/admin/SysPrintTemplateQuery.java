package com.ww.mall.mvc.view.query.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ww.mall.mvc.entity.SysPrintTemplateEntity;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

/**
 * @description:
 * @author: ww
 * @create: 2021-05-17 13:36
 */
@Data
public class SysPrintTemplateQuery {

    /**
     * 库位ID
     */
    private Long id;

    /**
     * 中心端ID
     */
    private Long centerId;

    /**
     * 模板名称
     */
    private String name;

    /**
     * 模板类型，关联sys_print_type
     */
    private Long typeId;

    /**
     * 模板内容
     */
    private String content;

    /**
     * 是否系统默认（0：否，1：是）
     */
    private Boolean isDefault;

    /**
     * 是否删除（0：否，1：是）
     */
    private Boolean isDel;

    public QueryWrapper<SysPrintTemplateEntity> getQueryWrapper() {
        QueryWrapper<SysPrintTemplateEntity> wrapper = new QueryWrapper<>();
        if (this.id != null) {
            wrapper.eq("`id`", this.id);
        }
        if (this.centerId != null) {
            wrapper.eq("`center_id`", this.centerId);
        }
        if (StringUtils.isNotBlank(this.name)) {
            wrapper.like("`name`", this.name);
        }
        if (this.typeId != null) {
            wrapper.eq("`type_id`", this.typeId);
        }
        if (StringUtils.isNotBlank(this.content)) {
            wrapper.like("`content`", this.content);
        }
        if (this.isDefault != null) {
            wrapper.eq("`is_default`", this.isDefault);
        }
        if (this.isDel != null) {
            wrapper.eq("`is_del`", this.isDel);
        }
        return wrapper;
    }


}
