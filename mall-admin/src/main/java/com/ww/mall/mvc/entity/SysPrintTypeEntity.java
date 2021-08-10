package com.ww.mall.mvc.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.ww.mall.mvc.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @description: 打印模板類型
 * @author: ww
 * @create: 2021-05-17 13:40
 */
@Data
@TableName("sys_print_type")
@EqualsAndHashCode(callSuper = true)
public class SysPrintTypeEntity extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    @TableId
    private String id;

    /**
     * 类型名称
     */
    private String name;

    /**
     * 所属平台（0：平台，1：租户）
     */
    private Integer systemType;

    /**
     * Json字符串，例如[{"key":"${name}","value":"姓名"}]
     */
    private String keyValue;

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
