package com.ww.mall.product.service.sku;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ww.mall.product.dto.sku.ProductSkuUpdateStockReqDTO;
import com.ww.mall.product.entity.sku.ProductSku;
import com.ww.mall.product.controller.admin.sku.req.ProductSkuBO;

import java.util.List;

/**
 * @author ww
 * @create 2025-09-09 16:04
 * @description:
 */
public interface ProductSkuService extends IService<ProductSku> {

    /**
     * 批量创建 SKU
     *
     * @param spuId 商品 SPU 编号
     * @param skus  SKU 对象集合
     */
    void createSkuList(Long spuId, List<ProductSkuBO> skus);

    /**
     * 根据 SPU 编号，批量更新它的 SKU 信息
     *
     * @param spuId SPU 编码
     * @param skus  SKU 的集合
     */
    void updateSkuList(Long spuId, List<ProductSkuBO> skus);

    /**
     * 对 sku 的组合的属性等进行合法性校验
     *
     * @param skus sku组合的集合
     */
    void validateSkuList(List<ProductSkuBO> skus, Boolean specType);

    /**
     * 更新 SKU 库存（增量）
     * <p>
     * 如果更新的库存不足，会抛出异常
     *
     * @param updateStockReqDTO 更行请求
     */
    void updateSkuStock(ProductSkuUpdateStockReqDTO updateStockReqDTO);

    /**
     * 获得商品 SKU 集合
     *
     * @param spuId spu 编号
     * @return 商品sku 集合
     */
    List<ProductSku> getSkuListBySpuId(Long spuId);

    /**
     * 更新 sku 属性
     *
     * @param propertyId   属性 id
     * @param propertyName 属性名
     * @return int 影响的行数
     */
    int updateSkuProperty(Long propertyId, String propertyName);

    /**
     * 更新 sku 属性值
     *
     * @param propertyValueId   属性值 id
     * @param propertyValueName 属性值名字
     * @return int 影响的行数
     */
    int updateSkuPropertyValue(Long propertyValueId, String propertyValueName);

}
