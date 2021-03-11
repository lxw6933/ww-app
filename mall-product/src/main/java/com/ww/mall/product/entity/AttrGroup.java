package com.ww.mall.product.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
* @author ww
* @since 2021-03-10
*/
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("pms_attr_group")
public class AttrGroup implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
    * 分组id
    */
    @TableId(value = "attr_group_id", type = IdType.AUTO)
    private Long attrGroupId;

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
    private String descript;

    /**
    * 组图标
    */
    private String icon;

    /**
    * 所属分类id
    */
    private Long catelogId;


}