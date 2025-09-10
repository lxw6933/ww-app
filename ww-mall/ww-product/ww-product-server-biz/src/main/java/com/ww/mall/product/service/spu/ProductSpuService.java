package com.ww.mall.product.service.spu;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ww.app.common.common.AppPageResult;
import com.ww.mall.product.entity.spu.ProductSpu;
import com.ww.mall.product.view.bo.ProductSpuBO;
import com.ww.mall.product.view.bo.ProductSpuStatusBO;
import com.ww.mall.product.view.query.ProductSpuPageQuery;
import com.ww.mall.product.view.vo.ProductSpuPageAdminVO;

/**
 * @author ww
 * @create 2025-09-09 16:04
 * @description:
 */
public interface ProductSpuService extends IService<ProductSpu> {

    /**
     * 商品分页列表【后台】
     *
     * @param productSpuPageQuery 查询条件
     * @return AppPageResult<ProductSpuAdminVO>
     */
    AppPageResult<ProductSpuPageAdminVO> page(ProductSpuPageQuery productSpuPageQuery);

    /**
     * 添加商品
     *
     * @param productSpuBO 添加信息
     * @return boolean
     */
    boolean add(ProductSpuBO productSpuBO);

    /**
     * 编辑商品
     *
     * @param productSpuBO 更新信息
     * @return boolean
     */
    boolean update(ProductSpuBO productSpuBO);

    /**
     * 获得商品 SPU
     *
     * @param id 编号
     * @return 商品 SPU
     */
    ProductSpu get(Long id);

    /**
     * 更新 SPU 状态
     *
     * @param productSpuStatusBO 更新请求
     */
    void updateSpuStatus(ProductSpuStatusBO productSpuStatusBO);

    /**
     * 通过分类 categoryId 查询 SPU 个数
     *
     * @param categoryId 分类 categoryId
     * @return SPU 数量
     */
    Long getSpuCountByCategoryId(Long categoryId);

    /**
     * 通过品牌 brandId 查询 SPU 个数
     *
     * @param brandId 分类 brandId
     * @return SPU 数量
     */
    Long getSpuCountByBrandId(Long brandId);

    /**
     * 更新商品 SPU 销量
     *
     * @param id        商品 SPU 编号
     * @param incrCount 增加的数量
     */
    void updateSalesCount(Long id, int incrCount);

    /**
     * 更新商品 SPU 浏览量
     *
     * @param id        商品 SPU 编号
     * @param incrCount 增加的数量
     */
    void updateBrowseCount(Long id, int incrCount);

}
