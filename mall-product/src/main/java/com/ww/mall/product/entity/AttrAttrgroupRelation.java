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
@TableName("pms_attr_attrgroup_relation")
public class AttrAttrgroupRelation implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
    * id
    */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
    * 属性id
    */
    private Long attrId;

    /**
    * 属性分组id
    */
    private Long attrGroupId;

    /**
    * 属性组内排序
    */
    private Integer attrSort;


}