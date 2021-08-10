package com.ww.mall.mvc.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.ww.mall.mvc.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @description: 字典
 * @author: ww
 * @create: 2021-05-18 16:53
 */
@Data
@TableName("sys_dict")
@EqualsAndHashCode(callSuper = true)
public class SysDictEntity extends BaseEntity {
    private static final long serialVersionUID = 1L;

    @TableId
    private Long id;
    /**
     * 字典名称
     */
    private String label;
    /**
     * 字典类型
     */
    private String dictType;
    /**
     * 字典码
     */
    private String code;
    /**
     * 排序
     */
    private Integer sort;
    /**
     * 状态（0：停用，1：正常）
     */
    private Boolean status;
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

