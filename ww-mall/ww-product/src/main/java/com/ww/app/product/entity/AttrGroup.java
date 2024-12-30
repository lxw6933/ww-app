package com.ww.app.product.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * @author ww
 * @create 2023-07-29- 10:48
 * @description:
 */
@Data
@TableName("t_attr_group")
public class AttrGroup implements Serializable {

    /**
     * 分组id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 组名
     */
    private String attrGroupName;

    /**
     * 排序
     */
    private Integer sort;

    /**
     * 描述
     */
    private String descInfo;

    /**
     * 组图标
     */
    private String icon;

    /**
     * 所属分类id
     */
    private Long categoryId;

}
