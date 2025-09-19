package com.ww.mall.product.service.sku;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ww.app.common.exception.ApiException;
import com.ww.mall.product.controller.admin.sku.req.ProductSkuBO;
import com.ww.mall.product.convert.sku.ProductSkuConvert;
import com.ww.mall.product.dao.sku.ProductSkuMapper;
import com.ww.mall.product.dto.sku.ProductSkuUpdateStockReqDTO;
import com.ww.mall.product.entity.property.ProductProperty;
import com.ww.mall.product.entity.property.ProductPropertyValue;
import com.ww.mall.product.entity.sku.ProductSku;
import com.ww.mall.product.service.property.ProductPropertyService;
import com.ww.mall.product.service.property.ProductPropertyValueService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

import static com.ww.app.common.utils.CollectionUtils.convertMap;
import static com.ww.app.common.utils.CollectionUtils.convertSet;
import static com.ww.mall.product.enums.ErrorCodeConstants.*;

/**
 * @author ww
 * @create 2025-09-09 16:05
 * @description:
 */
@Slf4j
@Service
public class ProductSkuServiceImpl extends ServiceImpl<ProductSkuMapper, ProductSku> implements ProductSkuService {

    @Resource
    private ProductSkuMapper productSkuMapper;

    @Resource
    private ProductSkuService productSkuService;

    @Resource
    private ProductPropertyService productPropertyService;

    @Resource
    private ProductPropertyValueService productPropertyValueService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createSkuList(Long spuId, List<ProductSkuBO> skus) {
        List<ProductSku> skuList = new ArrayList<>();
        skus.forEach(sku -> {
            ProductSku productSku = ProductSkuConvert.INSTANCE.convert(sku);
            productSku.setSpuId(spuId);
            skuList.add(productSku);
        });
        productSkuService.saveBatch(skuList);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSkuList(Long spuId, List<ProductSkuBO> skus) {
        List<ProductSku> spuSkus = getSkuListBySpuId(spuId);
        // 构建属性与 SKU 的映射关系
        // key：propertyValueId有序集合连接符
        // value：skuId
        Map<String, Long> existsSkuMap = convertMap(spuSkus, this::buildPropertyKey, ProductSku::getId);

        // 拆分三个集合，新插入的、需要更新的、需要删除的
        List<ProductSku> insertSkus = new ArrayList<>();
        List<ProductSku> updateSkus = new ArrayList<>();

        List<ProductSku> allUpdateSkus = new ArrayList<>();
        skus.forEach(sku -> {
            ProductSku productSku = ProductSkuConvert.INSTANCE.convert(sku);
            productSku.setSpuId(spuId);
            allUpdateSkus.add(productSku);
        });
        allUpdateSkus.forEach(sku -> {
            String propertiesKey = this.buildPropertyKey(sku);
            // 1、找得到的，进行更新
            Long existsSkuId = existsSkuMap.remove(propertiesKey);
            if (existsSkuId != null) {
                sku.setId(existsSkuId);
                updateSkus.add(sku);
                return;
            }
            // 2、找不到，进行插入
            sku.setSpuId(spuId);
            insertSkus.add(sku);
        });

        // 执行最终的批量操作
        if (CollUtil.isNotEmpty(insertSkus)) {
            productSkuService.saveBatch(insertSkus);
        }
        if (CollUtil.isNotEmpty(updateSkus)) {
            updateSkus.forEach(sku -> productSkuService.updateById(sku));
        }
        if (CollUtil.isNotEmpty(existsSkuMap)) {
            productSkuService.removeBatchByIds(existsSkuMap.values());
        }
    }

    @Override
    public ProductSku get(Long id) {
        return this.getById(id);
    }

    @Override
    public ProductSku getSku(Long id, boolean includeDeleted) {
        if (includeDeleted) {
            return productSkuMapper.selectByIdIncludeDeleted(id);
        }
        return get(id);
    }

    @Override
    public void validateSkuList(List<ProductSkuBO> skus, Boolean specType) {
        // 0、校验skus是否为空
        if (CollUtil.isEmpty(skus)) {
            throw new ApiException(SKU_NOT_EXISTS);
        }
        // 单规格
        if (ObjectUtil.equal(specType, Boolean.FALSE)) {
            ProductSkuBO skuBO = skus.get(0);
            List<ProductSku.Property> properties = new ArrayList<>();
            ProductSku.Property property = new ProductSku.Property();
            property.setPropertyId(ProductProperty.ID_DEFAULT);
            property.setPropertyName(ProductProperty.NAME_DEFAULT);
            property.setValueId(ProductPropertyValue.ID_DEFAULT);
            property.setValueName(ProductPropertyValue.NAME_DEFAULT);
            properties.add(property);
            skuBO.setProperties(properties);
            return;
        }

        // 1、校验属性项存在
        Set<Long> propertyIds = skus.stream()
                // 筛选
                .filter(p -> p.getProperties() != null)
                // 遍历多个 Property 属性
                .flatMap(p -> p.getProperties().stream())
                // 将每个 Property 转换成对应的 propertyId
                .map(ProductSku.Property::getPropertyId)
                .collect(Collectors.toSet());
        List<ProductProperty> propertyList = productPropertyService.listByIds(propertyIds);
        if (propertyList.size() != propertyIds.size()) {
            throw new ApiException(PROPERTY_NOT_EXISTS);
        }

        // 2. 校验，一个 SKU 下，没有重复的属性。校验方式是，遍历每个 SKU ，看看是否有重复的属性 propertyId
        List<ProductPropertyValue> productPropertyValues = productPropertyValueService.list(new LambdaQueryWrapper<ProductPropertyValue>()
                .in(ProductPropertyValue::getPropertyId, propertyIds)
        );
        Map<Long, ProductPropertyValue> propertyValueMap = convertMap(productPropertyValues, ProductPropertyValue::getId);
        skus.forEach(sku -> {
            Set<Long> skuPropertyIds = convertSet(sku.getProperties(), propertyItem -> propertyValueMap.get(propertyItem.getValueId()).getPropertyId());
            if (skuPropertyIds.size() != sku.getProperties().size()) {
                throw new ApiException(SKU_PROPERTIES_DUPLICATED);
            }
        });

        // 3. 再校验，每个 Sku 的属性值的数量，是一致的。
        int attrValueIdsSize = skus.get(0).getProperties().size();
        for (int i = 1; i < skus.size(); i++) {
            if (attrValueIdsSize != skus.get(i).getProperties().size()) {
                throw new ApiException(SPU_ATTR_NUMBERS_MUST_BE_EQUALS);
            }
        }

        // 4. 最后校验，每个 Sku 之间不是重复的
        // 每个元素，都是一个 Sku 的 attrValueId 集合。这样，通过最外层的 Set ，判断是否有重复的.
        Set<Set<Long>> skuAttrValues = new HashSet<>();
        for (ProductSkuBO sku : skus) {
            // 添加失败，说明重复
            if (!skuAttrValues.add(convertSet(sku.getProperties(), ProductSku.Property::getValueId))) {
                throw new ApiException(SPU_SKU_NOT_DUPLICATE);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSkuStock(ProductSkuUpdateStockReqDTO updateStockReqDTO) {
        updateStockReqDTO.getItems().forEach(item -> {
            if (item.getIncrCount() > 0) {
                productSkuMapper.incrStock(item.getId(), item.getIncrCount());
            } else if (item.getIncrCount() < 0) {
                int decrStockResult = productSkuMapper.decrStock(item.getId(), - item.getIncrCount());
                if (decrStockResult == 0) {
                    throw new ApiException(SKU_STOCK_NOT_ENOUGH);
                }
            }
        });
    }

    @Override
    public List<ProductSku> getSkuListBySpuId(Long spuId) {
        return this.list(new LambdaQueryWrapper<ProductSku>().eq(ProductSku::getSpuId, spuId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateSkuProperty(Long propertyId, String propertyName) {
        // 获取所有的 sku
        List<ProductSku> skuDOList = this.list();
        // 处理后需要更新的 sku
        List<ProductSku> updateSkus = new ArrayList<>();
        if (CollUtil.isEmpty(skuDOList)) {
            return 0;
        }
        skuDOList.stream()
                .filter(sku -> sku.getProperties() != null)
                .forEach(sku -> sku.getProperties().forEach(property -> {
                    if (property.getPropertyId().equals(propertyId)) {
                        property.setPropertyName(propertyName);
                        updateSkus.add(sku);
                    }
                }));
        if (CollUtil.isEmpty(updateSkus)) {
            return 0;
        }

        productSkuService.updateBatchById(updateSkus);
        return updateSkus.size();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateSkuPropertyValue(Long propertyValueId, String propertyValueName) {
        // 获取所有的 sku
        List<ProductSku> skuDOList = this.list();
        // 处理后需要更新的 sku
        List<ProductSku> updateSkus = new ArrayList<>();
        if (CollUtil.isEmpty(skuDOList)) {
            return 0;
        }
        skuDOList.stream()
                .filter(sku -> sku.getProperties() != null)
                .forEach(sku -> sku.getProperties().forEach(property -> {
                    if (property.getValueId().equals(propertyValueId)) {
                        property.setValueName(propertyValueName);
                        updateSkus.add(sku);
                    }
                }));
        if (CollUtil.isEmpty(updateSkus)) {
            return 0;
        }

        productSkuService.updateBatchById(updateSkus);
        return updateSkus.size();
    }

    public String buildPropertyKey(ProductSku sku) {
        if (CollUtil.isEmpty(sku.getProperties())) {
            return StrUtil.EMPTY;
        }
        List<ProductSku.Property> properties = new ArrayList<>(sku.getProperties());
        properties.sort(Comparator.comparing(ProductSku.Property::getValueId));
        return properties.stream().map(p -> String.valueOf(p.getValueId())).collect(Collectors.joining());
    }

}
