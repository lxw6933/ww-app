package com.ww.mall.product.dao.spu;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ww.mall.product.entity.spu.ProductSpu;
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
public interface ProductSpuMapper extends BaseMapper<ProductSpu> {

    @Select("SELECT * FROM product_spu WHERE id = #{id}")
    ProductSpu selectByIdIncludeDeleted(@Param("id") Long id);

    @Update("UPDATE product_spu SET salesCount = salesCount + #{num} WHERE id = #{id}")
    void updateSalesCount(@Param("id") Long id, @Param("num") Integer num);

    @Update("UPDATE product_spu SET browseCount = browseCount + #{num} WHERE id = #{id}")
    void updateBrowseCount(@Param("id") Long id, @Param("num") Integer num);

}
