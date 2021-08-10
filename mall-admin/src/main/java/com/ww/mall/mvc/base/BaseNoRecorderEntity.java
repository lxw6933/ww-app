package com.ww.mall.mvc.base;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * @description: 基础类（无操作者）
 * @author: ww
 * @create: 2021-04-16 09:14
 */
@Data
public class BaseNoRecorderEntity implements Serializable {

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    /** 修改时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

}
