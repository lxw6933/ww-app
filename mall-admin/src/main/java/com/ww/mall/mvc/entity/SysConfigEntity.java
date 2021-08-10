package com.ww.mall.mvc.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

/**
 * @description: 系统配置表
 * @author: ww
 * @create: 2021-05-18 19:05
 */
@Data
@TableName("sys_config")
public class SysConfigEntity {

    @TableId
    private Long id;

    /**
     * key
     */
    private String paramKey;

    /**
     * value
     */
    private String paramValue;
    
    /**
     * 状态 0：隐藏 1：显示
     */
    private Boolean status;

    /**
     * 备注
     */
    private String remark;

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

