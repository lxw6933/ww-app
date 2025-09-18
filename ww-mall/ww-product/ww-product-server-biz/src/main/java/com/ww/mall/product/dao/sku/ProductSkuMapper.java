package com.ww.mall.product.dao.sku;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ww.mall.product.entity.sku.ProductSku;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * @author ww
 * @create 2025-09-09 16:01
 * @description:
 */
@Mapper
public interface ProductSkuMapper extends BaseMapper<ProductSku> {

    @Select("SELECT * FROM product_sku WHERE id = #{id}")
    ProductSku selectByIdIncludeDeleted(@Param("id") Long id);

    @Update("UPDATE product_sku SET stock = stock + #{num} WHERE id = #{id}")
    void incrStock(@Param("id") Long id, @Param("num") Integer num);

    @Update("UPDATE product_sku SET stock = stock - #{num} WHERE id = #{id} and stock >= #{num}")
    int decrStock(@Param("id") Long id, @Param("num") Integer num);

}
