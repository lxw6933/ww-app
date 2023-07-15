package com.ww.mall.coupon.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * @author ww
 * @create 2023-07-15- 09:52
 * @description: 实体基类
 */
@Data
public class BaseEntity implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Version
    private Long version;

    @TableField(fill = FieldFill.INSERT)
    private Long creatorId;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updaterId;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

}
