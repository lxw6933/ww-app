package com.ww.mall.mvc.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.ww.mall.mvc.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @description: 打印模板
 * @author: ww
 * @create: 2021-05-17 13:21
 */
@Data
@TableName("sys_print_template")
@EqualsAndHashCode(callSuper = true)
public class SysPrintTemplateEntity extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 库位ID
     */
    @TableId
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
    private String typeId;

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

    /**
     * 乐观锁
     */
    @Version
    private Integer version;
    /**
     * 逻辑删除 0：已删除；1：正常
     */
    @TableLogic
    private Integer deleted;

}
