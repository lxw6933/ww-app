package com.ww.mall.product.service.spu;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ww.mall.product.controller.admin.sku.req.ProductSkuBO;
import com.ww.mall.product.controller.admin.spu.req.ProductSpuBO;
import com.ww.mall.product.controller.admin.spu.req.ProductSpuStatusBO;
import com.ww.mall.product.controller.app.spu.res.AppProductSpuDetailVO;
import com.ww.mall.product.entity.brand.ProductBrand;
import com.ww.mall.product.entity.category.ProductCategory;
import com.ww.mall.product.entity.property.ProductProperty;
import com.ww.mall.product.entity.property.ProductPropertyValue;
import com.ww.mall.product.entity.sku.ProductSku;
import com.ww.mall.product.entity.spu.ProductSpu;
import com.ww.mall.product.enums.SpuStatus;
import com.ww.mall.product.enums.SpuType;
import com.ww.mall.product.service.brand.ProductBrandService;
import com.ww.mall.product.service.category.ProductCategoryService;
import com.ww.mall.product.service.property.ProductPropertyService;
import com.ww.mall.product.service.property.ProductPropertyValueService;
import com.ww.mall.product.service.sku.ProductSkuService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 商品SPU服务测试类
 *
 * @author ww
 * @create 2025-01-XX
 */
@Slf4j
@SpringBootTest
@Transactional
@Rollback
class ProductSpuServiceTest {

    @Resource
    private ProductSpuService productSpuService;

    @Resource
    private ProductSkuService productSkuService;

    @Resource
    private ProductCategoryService productCategoryService;

    @Resource
    private ProductBrandService productBrandService;

    @Resource
    private ProductPropertyService productPropertyService;

    @Resource
    private ProductPropertyValueService productPropertyValueService;

    /**
     * 测试单SKU商品新增 - 手机
     */
    @Test
    @DisplayName("测试单SKU商品新增 - 智能手机")
    void testAddSingleSkuProduct_Phone() {
        log.info("========== 开始测试单SKU商品新增 - 智能手机 ==========");

        // 1. 从数据库获取真实的分类数据 - 智能手机分类（三级分类）
        ProductCategory category = productCategoryService.getOne(
                new LambdaQueryWrapper<ProductCategory>()
                        .eq(ProductCategory::getName, "智能手机")
                        .last("LIMIT 1")
        );
        assertNotNull(category, "未找到智能手机分类");
        log.info("获取到分类: {}", category.getName());

        // 2. 从数据库获取真实的品牌数据 - 苹果
        ProductBrand brand = productBrandService.getOne(
                new LambdaQueryWrapper<ProductBrand>()
                        .eq(ProductBrand::getName, "苹果")
                        .last("LIMIT 1")
        );
        assertNotNull(brand, "未找到苹果品牌");
        log.info("获取到品牌: {}", brand.getName());

        // 3. 构建单SKU商品
        ProductSpuBO spuBO = buildSingleSkuSpuBO(category.getId(), brand.getId(),
                "iPhone 15 Pro Max", "iPhone15ProMax",
                "苹果最新旗舰手机，搭载A17 Pro芯片",
                "https://example.com/images/iphone15promax.jpg",
                Arrays.asList(
                        "https://example.com/images/iphone15promax1.jpg",
                        "https://example.com/images/iphone15promax2.jpg",
                        "https://example.com/images/iphone15promax3.jpg"
                ));

        // 4. 构建单SKU
        ProductSkuBO skuBO = new ProductSkuBO();
        skuBO.setName("iPhone 15 Pro Max 256GB 深空黑色");
        skuBO.setPrice(999900L); // 9999元，单位：分
        skuBO.setMarketPrice(1099900L); // 10999元
        skuBO.setCostPrice(800000L); // 8000元
        skuBO.setStock(100);
        skuBO.setImg("https://example.com/images/iphone15promax_sku.jpg");
        skuBO.setBarCode("1234567890123");
        // 单规格不需要设置properties，系统会自动设置为默认值

        spuBO.setSkus(Collections.singletonList(skuBO));

        // 5. 执行新增
        boolean result = productSpuService.add(spuBO);
        assertTrue(result, "商品新增失败");

        // 6. 验证数据
        ProductSpu savedSpu = productSpuService.getOne(
                new LambdaQueryWrapper<ProductSpu>()
                        .eq(ProductSpu::getName, spuBO.getName())
                        .last("LIMIT 1")
        );
        assertNotNull(savedSpu, "商品保存失败");
        assertEquals(spuBO.getName(), savedSpu.getName());
        assertEquals(Boolean.FALSE, savedSpu.getSpecType(), "应该是单规格");
        assertEquals(999900L, savedSpu.getMinPrice(), "最低价格应该是999900分");
        assertEquals(SpuStatus.DOWN, savedSpu.getStatus(), "新商品默认应该是下架状态");

        // 验证SKU
        List<ProductSku> skus = productSkuService.getSkuListBySpuId(savedSpu.getId());
        assertEquals(1, skus.size(), "单规格商品应该只有1个SKU");
        ProductSku savedSku = skus.get(0);
        assertEquals(skuBO.getPrice(), savedSku.getPrice());
        assertEquals(skuBO.getStock(), savedSku.getStock());

        log.info("========== 单SKU商品新增测试通过 ==========");
    }

    /**
     * 测试多SKU商品新增 - 手机（颜色+存储）
     */
    @Test
    @DisplayName("测试多SKU商品新增 - 手机（颜色+存储）")
    void testAddMultiSkuProduct_Phone() {
        log.info("========== 开始测试多SKU商品新增 - 手机（颜色+存储） ==========");

        // 1. 从数据库获取真实的分类数据 - 智能手机
        ProductCategory category = productCategoryService.getOne(
                new LambdaQueryWrapper<ProductCategory>()
                        .eq(ProductCategory::getName, "智能手机")
                        .last("LIMIT 1")
        );
        assertNotNull(category, "未找到智能手机分类");

        // 2. 从数据库获取真实的品牌数据 - 华为
        ProductBrand brand = productBrandService.getOne(
                new LambdaQueryWrapper<ProductBrand>()
                        .eq(ProductBrand::getName, "华为")
                        .last("LIMIT 1")
        );
        assertNotNull(brand, "未找到华为品牌");

        // 3. 从数据库获取真实的属性数据 - 颜色和存储
        ProductProperty colorProperty = productPropertyService.getOne(
                new LambdaQueryWrapper<ProductProperty>()
                        .eq(ProductProperty::getName, "颜色")
                        .last("LIMIT 1")
        );
        assertNotNull(colorProperty, "未找到颜色属性");

        ProductProperty storageProperty = productPropertyService.getOne(
                new LambdaQueryWrapper<ProductProperty>()
                        .eq(ProductProperty::getName, "存储")
                        .last("LIMIT 1")
        );
        assertNotNull(storageProperty, "未找到存储属性");

        // 4. 从数据库获取真实的属性值数据
        // 颜色：黑色、白色、银色
        List<ProductPropertyValue> colorValues = productPropertyValueService.list(
                new LambdaQueryWrapper<ProductPropertyValue>()
                        .eq(ProductPropertyValue::getPropertyId, colorProperty.getId())
                        .in(ProductPropertyValue::getName, Arrays.asList("黑色", "白色", "银色"))
        );
        assertEquals(3, colorValues.size(), "应该找到3个颜色属性值");

        // 存储：128GB、256GB、512GB
        List<ProductPropertyValue> storageValues = productPropertyValueService.list(
                new LambdaQueryWrapper<ProductPropertyValue>()
                        .eq(ProductPropertyValue::getPropertyId, storageProperty.getId())
                        .in(ProductPropertyValue::getName, Arrays.asList("128GB", "256GB", "512GB"))
        );
        assertEquals(3, storageValues.size(), "应该找到3个存储属性值");

        // 5. 构建多SKU商品
        ProductSpuBO spuBO = buildMultiSkuSpuBO(category.getId(), brand.getId(),
                "华为Mate 60 Pro", "Mate60Pro",
                "华为旗舰手机，支持5G网络",
                "https://example.com/images/mate60pro.jpg",
                Arrays.asList(
                        "https://example.com/images/mate60pro1.jpg",
                        "https://example.com/images/mate60pro2.jpg"
                ));

        // 6. 构建多SKU（3种颜色 × 3种存储 = 9个SKU）
        List<ProductSkuBO> skuList = new ArrayList<>();
        long basePrice = 599900L; // 5999元基础价
        int stockBase = 50;

        for (ProductPropertyValue colorValue : colorValues) {
            for (ProductPropertyValue storageValue : storageValues) {
                ProductSkuBO skuBO = new ProductSkuBO();
                skuBO.setName(String.format("华为Mate 60 Pro %s %s", colorValue.getName(), storageValue.getName()));

                // 根据存储容量调整价格
                long price = basePrice;
                if ("256GB".equals(storageValue.getName())) {
                    price += 50000L; // +500元
                } else if ("512GB".equals(storageValue.getName())) {
                    price += 100000L; // +1000元
                }

                skuBO.setPrice(price);
                skuBO.setMarketPrice(price + 20000L); // 市场价+200元
                skuBO.setCostPrice(price - 100000L); // 成本价-1000元
                skuBO.setStock(stockBase);
                skuBO.setImg(String.format("https://example.com/images/mate60pro_%s_%s.jpg",
                        colorValue.getName(), storageValue.getName()));
                skuBO.setBarCode(String.format("HUAWEI%s%s", colorValue.getId(), storageValue.getId()));

                // 设置属性
                List<ProductSku.Property> properties = Arrays.asList(
                        new ProductSku.Property(colorProperty.getId(), colorProperty.getName(),
                                colorValue.getId(), colorValue.getName()),
                        new ProductSku.Property(storageProperty.getId(), storageProperty.getName(),
                                storageValue.getId(), storageValue.getName())
                );
                skuBO.setProperties(properties);

                skuList.add(skuBO);
            }
        }

        spuBO.setSkus(skuList);

        // 7. 执行新增
        boolean result = productSpuService.add(spuBO);
        assertTrue(result, "商品新增失败");

        // 8. 验证数据
        ProductSpu savedSpu = productSpuService.getOne(
                new LambdaQueryWrapper<ProductSpu>()
                        .eq(ProductSpu::getName, spuBO.getName())
                        .last("LIMIT 1")
        );
        assertNotNull(savedSpu, "商品保存失败");
        assertEquals(Boolean.TRUE, savedSpu.getSpecType(), "应该是多规格");
        assertEquals(basePrice, savedSpu.getMinPrice(), "最低价格应该是基础价");

        // 验证SKU数量
        List<ProductSku> savedSkus = productSkuService.getSkuListBySpuId(savedSpu.getId());
        assertEquals(9, savedSkus.size(), "应该创建9个SKU（3颜色×3存储）");

        log.info("========== 多SKU商品新增测试通过，共创建{}个SKU ==========", savedSkus.size());
    }

    /**
     * 测试多SKU商品新增 - 服装（颜色+尺码）
     */
    @Test
    @DisplayName("测试多SKU商品新增 - 服装（颜色+尺码）")
    void testAddMultiSkuProduct_Clothing() {
        log.info("========== 开始测试多SKU商品新增 - 服装（颜色+尺码） ==========");

        // 1. 从数据库获取真实的分类数据 - T恤（三级分类）
        ProductCategory category = productCategoryService.getOne(
                new LambdaQueryWrapper<ProductCategory>()
                        .eq(ProductCategory::getName, "T恤")
                        .last("LIMIT 1")
        );
        assertNotNull(category, "未找到T恤分类");

        // 2. 从数据库获取真实的品牌数据 - 耐克
        ProductBrand brand = productBrandService.getOne(
                new LambdaQueryWrapper<ProductBrand>()
                        .eq(ProductBrand::getName, "耐克")
                        .last("LIMIT 1")
        );
        assertNotNull(brand, "未找到耐克品牌");

        // 3. 从数据库获取真实的属性数据 - 颜色和尺码
        ProductProperty colorProperty = productPropertyService.getOne(
                new LambdaQueryWrapper<ProductProperty>()
                        .eq(ProductProperty::getName, "颜色")
                        .last("LIMIT 1")
        );
        assertNotNull(colorProperty, "未找到颜色属性");

        ProductProperty sizeProperty = productPropertyService.getOne(
                new LambdaQueryWrapper<ProductProperty>()
                        .eq(ProductProperty::getName, "尺寸")
                        .last("LIMIT 1")
        );
        assertNotNull(sizeProperty, "未找到尺寸属性");

        // 4. 从数据库获取真实的属性值数据
        // 颜色：黑色、白色、红色
        List<ProductPropertyValue> colorValues = productPropertyValueService.list(
                new LambdaQueryWrapper<ProductPropertyValue>()
                        .eq(ProductPropertyValue::getPropertyId, colorProperty.getId())
                        .in(ProductPropertyValue::getName, Arrays.asList("黑色", "白色", "红色"))
        );
        assertEquals(3, colorValues.size(), "应该找到3个颜色属性值");

        // 尺码：S、M、L、XL
        List<ProductPropertyValue> sizeValues = productPropertyValueService.list(
                new LambdaQueryWrapper<ProductPropertyValue>()
                        .eq(ProductPropertyValue::getPropertyId, sizeProperty.getId())
                        .in(ProductPropertyValue::getName, Arrays.asList("S", "M", "L", "XL"))
        );
        assertEquals(4, sizeValues.size(), "应该找到4个尺码属性值");

        // 5. 构建多SKU商品
        ProductSpuBO spuBO = buildMultiSkuSpuBO(category.getId(), brand.getId(),
                "Nike Dri-FIT运动T恤", "NikeDriFIT",
                "Nike经典运动T恤，采用Dri-FIT技术，快速排汗",
                "https://example.com/images/nike_tshirt.jpg",
                Arrays.asList(
                        "https://example.com/images/nike_tshirt1.jpg",
                        "https://example.com/images/nike_tshirt2.jpg"
                ));

        // 6. 构建多SKU（3种颜色 × 4种尺码 = 12个SKU）
        List<ProductSkuBO> skuList = new ArrayList<>();
        long basePrice = 19900L; // 199元
        int stockBase = 30;

        for (ProductPropertyValue colorValue : colorValues) {
            for (ProductPropertyValue sizeValue : sizeValues) {
                ProductSkuBO skuBO = new ProductSkuBO();
                skuBO.setName(String.format("Nike Dri-FIT运动T恤 %s %s", colorValue.getName(), sizeValue.getName()));
                skuBO.setPrice(basePrice);
                skuBO.setMarketPrice(basePrice + 5000L); // 市场价+50元
                skuBO.setCostPrice(basePrice - 5000L); // 成本价-50元
                skuBO.setStock(stockBase);
                skuBO.setImg(String.format("https://example.com/images/nike_tshirt_%s_%s.jpg",
                        colorValue.getName(), sizeValue.getName()));
                skuBO.setBarCode(String.format("NIKE%s%s", colorValue.getId(), sizeValue.getId()));

                // 设置属性
                List<ProductSku.Property> properties = Arrays.asList(
                        new ProductSku.Property(colorProperty.getId(), colorProperty.getName(),
                                colorValue.getId(), colorValue.getName()),
                        new ProductSku.Property(sizeProperty.getId(), sizeProperty.getName(),
                                sizeValue.getId(), sizeValue.getName())
                );
                skuBO.setProperties(properties);

                skuList.add(skuBO);
            }
        }

        spuBO.setSkus(skuList);

        // 7. 执行新增
        boolean result = productSpuService.add(spuBO);
        assertTrue(result, "商品新增失败");

        // 8. 验证数据
        ProductSpu savedSpu = productSpuService.getOne(
                new LambdaQueryWrapper<ProductSpu>()
                        .eq(ProductSpu::getName, spuBO.getName())
                        .last("LIMIT 1")
        );
        assertNotNull(savedSpu, "商品保存失败");
        assertEquals(Boolean.TRUE, savedSpu.getSpecType(), "应该是多规格");

        // 验证SKU数量
        List<ProductSku> savedSkus = productSkuService.getSkuListBySpuId(savedSpu.getId());
        assertEquals(12, savedSkus.size(), "应该创建12个SKU（3颜色×4尺码）");

        log.info("========== 多SKU服装商品新增测试通过，共创建{}个SKU ==========", savedSkus.size());
    }

    /**
     * 测试商品查询
     */
    @Test
    @DisplayName("测试商品查询")
    void testGetProduct() {
        log.info("========== 开始测试商品查询 ==========");

        // 先创建一个商品
        ProductCategory category = productCategoryService.getOne(
                new LambdaQueryWrapper<ProductCategory>()
                        .eq(ProductCategory::getName, "智能手机")
                        .last("LIMIT 1")
        );
        ProductBrand brand = productBrandService.getOne(
                new LambdaQueryWrapper<ProductBrand>()
                        .eq(ProductBrand::getName, "小米")
                        .last("LIMIT 1")
        );

        ProductSpuBO spuBO = buildSingleSkuSpuBO(category.getId(), brand.getId(),
                "小米14", "Xiaomi14", "小米最新旗舰手机",
                "https://example.com/images/xiaomi14.jpg", null);

        ProductSkuBO skuBO = new ProductSkuBO();
        skuBO.setName("小米14 256GB");
        skuBO.setPrice(399900L);
        skuBO.setStock(200);
        skuBO.setImg("https://example.com/images/xiaomi14_sku.jpg");
        spuBO.setSkus(Collections.singletonList(skuBO));

        productSpuService.add(spuBO);

        // 查询商品
        ProductSpu savedSpu = productSpuService.getOne(
                new LambdaQueryWrapper<ProductSpu>()
                        .eq(ProductSpu::getName, "小米14")
                        .last("LIMIT 1")
        );
        assertNotNull(savedSpu, "商品查询失败");

        ProductSpu queriedSpu = productSpuService.get(savedSpu.getId());
        assertNotNull(queriedSpu, "通过ID查询商品失败");
        assertEquals("小米14", queriedSpu.getName());

        // 查询SKU
        List<ProductSku> skus = productSkuService.getSkuListBySpuId(queriedSpu.getId());
        assertEquals(1, skus.size());
        assertEquals(399900L, skus.get(0).getPrice());

        log.info("========== 商品查询测试通过 ==========");
    }

    @Test
    @DisplayName("测试商品详情")
    void testProductDetail() {
        AppProductSpuDetailVO spu = productSpuService.detail(4L);
        printSpuDetail(spu);
    }

    /**
     * 测试商品状态更新
     */
    @Test
    @DisplayName("测试商品状态更新")
    void testUpdateProductStatus() {
        log.info("========== 开始测试商品状态更新 ==========");

        // 先创建一个商品
        ProductCategory category = productCategoryService.getOne(
                new LambdaQueryWrapper<ProductCategory>()
                        .eq(ProductCategory::getName, "智能手机")
                        .last("LIMIT 1")
        );
        ProductBrand brand = productBrandService.getOne(
                new LambdaQueryWrapper<ProductBrand>()
                        .eq(ProductBrand::getName, "OPPO")
                        .last("LIMIT 1")
        );

        ProductSpuBO spuBO = buildSingleSkuSpuBO(category.getId(), brand.getId(),
                "OPPO Find X7", "OPPOFindX7", "OPPO旗舰手机",
                "https://example.com/images/oppo_findx7.jpg", null);

        ProductSkuBO skuBO = new ProductSkuBO();
        skuBO.setName("OPPO Find X7 256GB");
        skuBO.setPrice(499900L);
        skuBO.setStock(150);
        skuBO.setImg("https://example.com/images/oppo_findx7_sku.jpg");
        spuBO.setSkus(Collections.singletonList(skuBO));

        productSpuService.add(spuBO);

        ProductSpu savedSpu = productSpuService.getOne(
                new LambdaQueryWrapper<ProductSpu>()
                        .eq(ProductSpu::getName, "OPPO Find X7")
                        .last("LIMIT 1")
        );

        // 更新状态为上架
        ProductSpuStatusBO statusBO = new ProductSpuStatusBO();
        statusBO.setId(savedSpu.getId());
        statusBO.setStatus(SpuStatus.UP);
        productSpuService.updateSpuStatus(statusBO);

        // 验证状态
        ProductSpu updatedSpu = productSpuService.get(savedSpu.getId());
        assertEquals(SpuStatus.UP, updatedSpu.getStatus(), "商品状态应该更新为上架");

        // 更新状态为冻结
        statusBO.setStatus(SpuStatus.FREEZE);
        productSpuService.updateSpuStatus(statusBO);

        updatedSpu = productSpuService.get(savedSpu.getId());
        assertEquals(SpuStatus.FREEZE, updatedSpu.getStatus(), "商品状态应该更新为冻结");

        log.info("========== 商品状态更新测试通过 ==========");
    }

    /**
     * 测试商品更新
     */
    @Test
    @DisplayName("测试商品更新")
    void testUpdateProduct() {
        log.info("========== 开始测试商品更新 ==========");

        // 先创建一个商品
        ProductCategory category = productCategoryService.getOne(
                new LambdaQueryWrapper<ProductCategory>()
                        .eq(ProductCategory::getName, "智能手机")
                        .last("LIMIT 1")
        );
        ProductBrand brand = productBrandService.getOne(
                new LambdaQueryWrapper<ProductBrand>()
                        .eq(ProductBrand::getName, "vivo")
                        .last("LIMIT 1")
        );

        ProductSpuBO spuBO = buildSingleSkuSpuBO(category.getId(), brand.getId(),
                "vivo X100", "VivoX100", "vivo旗舰手机",
                "https://example.com/images/vivo_x100.jpg", null);

        ProductSkuBO skuBO = new ProductSkuBO();
        skuBO.setName("vivo X100 256GB");
        skuBO.setPrice(399900L);
        skuBO.setStock(100);
        skuBO.setImg("https://example.com/images/vivo_x100_sku.jpg");
        spuBO.setSkus(Collections.singletonList(skuBO));

        productSpuService.add(spuBO);

        ProductSpu savedSpu = productSpuService.getOne(
                new LambdaQueryWrapper<ProductSpu>()
                        .eq(ProductSpu::getName, "vivo X100")
                        .last("LIMIT 1")
        );

        // 更新商品信息
        ProductSpuBO updateBO = buildSingleSkuSpuBO(category.getId(), brand.getId(),
                "vivo X100 Pro", "VivoX100Pro", "vivo旗舰手机Pro版本",
                "https://example.com/images/vivo_x100_pro.jpg", null);

        updateBO.setId(savedSpu.getId());

        ProductSkuBO updateSkuBO = new ProductSkuBO();
        updateSkuBO.setName("vivo X100 Pro 512GB");
        updateSkuBO.setPrice(499900L);
        updateSkuBO.setStock(150);
        updateSkuBO.setImg("https://example.com/images/vivo_x100_pro_sku.jpg");
        updateBO.setSkus(Collections.singletonList(updateSkuBO));

        productSpuService.update(updateBO);

        // 验证更新
        ProductSpu updatedSpu = productSpuService.get(savedSpu.getId());
        assertEquals("vivo X100 Pro", updatedSpu.getName(), "商品名称应该更新");
        assertEquals("vivo旗舰手机Pro版本", updatedSpu.getIntroduction(), "商品简介应该更新");

        List<ProductSku> skus = productSkuService.getSkuListBySpuId(savedSpu.getId());
        assertEquals(1, skus.size());
        assertEquals(499900L, skus.get(0).getPrice(), "SKU价格应该更新");
        assertEquals(150, skus.get(0).getStock(), "SKU库存应该更新");

        log.info("========== 商品更新测试通过 ==========");
    }

    /**
     * 测试通过分类查询商品数量
     */
    @Test
    @DisplayName("测试通过分类查询商品数量")
    void testGetSpuCountByCategoryId() {
        log.info("========== 开始测试通过分类查询商品数量 ==========");

        // 获取分类
        ProductCategory category = productCategoryService.getOne(
                new LambdaQueryWrapper<ProductCategory>()
                        .eq(ProductCategory::getName, "智能手机")
                        .last("LIMIT 1")
        );
        assertNotNull(category, "未找到智能手机分类");

        // 获取品牌
        ProductBrand brand = productBrandService.getOne(
                new LambdaQueryWrapper<ProductBrand>()
                        .eq(ProductBrand::getName, "三星")
                        .last("LIMIT 1")
        );
        assertNotNull(brand, "未找到三星品牌");

        // 创建3个商品
        for (int i = 1; i <= 3; i++) {
            ProductSpuBO spuBO = buildSingleSkuSpuBO(category.getId(), brand.getId(),
                    "三星Galaxy S" + (20 + i), "SamsungGalaxyS" + (20 + i),
                    "三星Galaxy S系列手机",
                    "https://example.com/images/samsung_s" + (20 + i) + ".jpg", null);

            ProductSkuBO skuBO = new ProductSkuBO();
            skuBO.setName("三星Galaxy S" + (20 + i) + " 256GB");
            skuBO.setPrice((500000L + i * 10000L));
            skuBO.setStock(100);
            skuBO.setImg("https://example.com/images/samsung_s" + (20 + i) + "_sku.jpg");
            spuBO.setSkus(Collections.singletonList(skuBO));

            productSpuService.add(spuBO);
        }

        // 查询该分类下的商品数量
        Long count = productSpuService.getSpuCountByCategoryId(category.getId());
        assertTrue(count >= 3, "该分类下应该至少有3个商品");

        log.info("========== 通过分类查询商品数量测试通过，数量: {} ==========", count);
    }

    /**
     * 测试通过品牌查询商品数量
     */
    @Test
    @DisplayName("测试通过品牌查询商品数量")
    void testGetSpuCountByBrandId() {
        log.info("========== 开始测试通过品牌查询商品数量 ==========");

        // 获取分类
        ProductCategory category = productCategoryService.getOne(
                new LambdaQueryWrapper<ProductCategory>()
                        .eq(ProductCategory::getName, "智能手机")
                        .last("LIMIT 1")
        );
        assertNotNull(category, "未找到智能手机分类");

        // 获取品牌
        ProductBrand brand = productBrandService.getOne(
                new LambdaQueryWrapper<ProductBrand>()
                        .eq(ProductBrand::getName, "索尼")
                        .last("LIMIT 1")
        );
        assertNotNull(brand, "未找到索尼品牌");

        // 创建2个商品
        for (int i = 1; i <= 2; i++) {
            ProductSpuBO spuBO = buildSingleSkuSpuBO(category.getId(), brand.getId(),
                    "索尼Xperia " + i, "SonyXperia" + i,
                    "索尼Xperia系列手机",
                    "https://example.com/images/sony_xperia" + i + ".jpg", null);

            ProductSkuBO skuBO = new ProductSkuBO();
            skuBO.setName("索尼Xperia " + i + " 256GB");
            skuBO.setPrice(600000L);
            skuBO.setStock(50);
            skuBO.setImg("https://example.com/images/sony_xperia" + i + "_sku.jpg");
            spuBO.setSkus(Collections.singletonList(skuBO));

            productSpuService.add(spuBO);
        }

        // 查询该品牌下的商品数量
        Long count = productSpuService.getSpuCountByBrandId(brand.getId());
        assertTrue(count >= 2, "该品牌下应该至少有2个商品");

        log.info("========== 通过品牌查询商品数量测试通过，数量: {} ==========", count);
    }

    /**
     * 测试多SKU商品新增 - 笔记本电脑（内存+存储+处理器）
     */
    @Test
    @DisplayName("测试多SKU商品新增 - 笔记本电脑（内存+存储+处理器）")
    void testAddMultiSkuProduct_Laptop() {
        log.info("========== 开始测试多SKU商品新增 - 笔记本电脑（内存+存储+处理器） ==========");

        // 1. 从数据库获取真实的分类数据 - 笔记本电脑
        ProductCategory category = productCategoryService.getOne(
                new LambdaQueryWrapper<ProductCategory>()
                        .eq(ProductCategory::getName, "笔记本电脑")
                        .last("LIMIT 1")
        );
        assertNotNull(category, "未找到笔记本电脑分类");

        // 2. 从数据库获取真实的品牌数据 - 联想
        ProductBrand brand = productBrandService.getOne(
                new LambdaQueryWrapper<ProductBrand>()
                        .eq(ProductBrand::getName, "联想")
                        .last("LIMIT 1")
        );
        assertNotNull(brand, "未找到联想品牌");

        // 3. 从数据库获取真实的属性数据
        ProductProperty memoryProperty = productPropertyService.getOne(
                new LambdaQueryWrapper<ProductProperty>()
                        .eq(ProductProperty::getName, "内存")
                        .last("LIMIT 1")
        );
        assertNotNull(memoryProperty, "未找到内存属性");

        ProductProperty storageProperty = productPropertyService.getOne(
                new LambdaQueryWrapper<ProductProperty>()
                        .eq(ProductProperty::getName, "存储")
                        .last("LIMIT 1")
        );
        assertNotNull(storageProperty, "未找到存储属性");

        ProductProperty processorProperty = productPropertyService.getOne(
                new LambdaQueryWrapper<ProductProperty>()
                        .eq(ProductProperty::getName, "处理器")
                        .last("LIMIT 1")
        );
        assertNotNull(processorProperty, "未找到处理器属性");

        // 4. 从数据库获取真实的属性值数据
        // 内存：8GB、16GB
        List<ProductPropertyValue> memoryValues = productPropertyValueService.list(
                new LambdaQueryWrapper<ProductPropertyValue>()
                        .eq(ProductPropertyValue::getPropertyId, memoryProperty.getId())
                        .in(ProductPropertyValue::getName, Arrays.asList("8GB", "16GB"))
        );
        assertEquals(2, memoryValues.size(), "应该找到2个内存属性值");

        // 存储：256GB、512GB
        List<ProductPropertyValue> storageValues = productPropertyValueService.list(
                new LambdaQueryWrapper<ProductPropertyValue>()
                        .eq(ProductPropertyValue::getPropertyId, storageProperty.getId())
                        .in(ProductPropertyValue::getName, Arrays.asList("256GB", "512GB"))
        );
        assertEquals(2, storageValues.size(), "应该找到2个存储属性值");

        // 处理器：Intel i5、Intel i7
        List<ProductPropertyValue> processorValues = productPropertyValueService.list(
                new LambdaQueryWrapper<ProductPropertyValue>()
                        .eq(ProductPropertyValue::getPropertyId, processorProperty.getId())
                        .in(ProductPropertyValue::getName, Arrays.asList("Intel i5", "Intel i7"))
        );
        assertFalse(processorValues.isEmpty(), "应该至少找到1个处理器属性值");

        // 5. 构建多SKU商品
        ProductSpuBO spuBO = buildMultiSkuSpuBO(category.getId(), brand.getId(),
                "联想ThinkPad X1 Carbon", "ThinkPadX1Carbon",
                "联想ThinkPad X1 Carbon商务笔记本电脑",
                "https://example.com/images/thinkpad_x1.jpg",
                Arrays.asList(
                        "https://example.com/images/thinkpad_x1_1.jpg",
                        "https://example.com/images/thinkpad_x1_2.jpg"
                ));

        // 6. 构建多SKU（2内存 × 2存储 × 2处理器 = 8个SKU，如果处理器只有1个则是4个）
        List<ProductSkuBO> skuList = new ArrayList<>();
        long basePrice = 899900L; // 8999元基础价
        int stockBase = 20;

        for (ProductPropertyValue memoryValue : memoryValues) {
            for (ProductPropertyValue storageValue : storageValues) {
                for (ProductPropertyValue processorValue : processorValues) {
                    ProductSkuBO skuBO = new ProductSkuBO();
                    skuBO.setName(String.format("联想ThinkPad X1 Carbon %s %s %s",
                            processorValue.getName(), memoryValue.getName(), storageValue.getName()));

                    // 根据配置调整价格
                    long price = basePrice;
                    if ("16GB".equals(memoryValue.getName())) {
                        price += 50000L; // +500元
                    }
                    if ("512GB".equals(storageValue.getName())) {
                        price += 30000L; // +300元
                    }
                    if ("Intel i7".equals(processorValue.getName())) {
                        price += 100000L; // +1000元
                    }

                    skuBO.setPrice(price);
                    skuBO.setMarketPrice(price + 50000L);
                    skuBO.setCostPrice(price - 200000L);
                    skuBO.setStock(stockBase);
                    skuBO.setImg(String.format("https://example.com/images/thinkpad_x1_%s_%s_%s.jpg",
                            processorValue.getName(), memoryValue.getName(), storageValue.getName()));
                    skuBO.setBarCode(String.format("THINKPAD%s%s%s",
                            processorValue.getId(), memoryValue.getId(), storageValue.getId()));

                    // 设置属性
                    List<ProductSku.Property> properties = Arrays.asList(
                            new ProductSku.Property(processorProperty.getId(), processorProperty.getName(),
                                    processorValue.getId(), processorValue.getName()),
                            new ProductSku.Property(memoryProperty.getId(), memoryProperty.getName(),
                                    memoryValue.getId(), memoryValue.getName()),
                            new ProductSku.Property(storageProperty.getId(), storageProperty.getName(),
                                    storageValue.getId(), storageValue.getName())
                    );
                    skuBO.setProperties(properties);

                    skuList.add(skuBO);
                }
            }
        }

        spuBO.setSkus(skuList);

        // 7. 执行新增
        boolean result = productSpuService.add(spuBO);
        assertTrue(result, "商品新增失败");

        // 8. 验证数据
        ProductSpu savedSpu = productSpuService.getOne(
                new LambdaQueryWrapper<ProductSpu>()
                        .eq(ProductSpu::getName, spuBO.getName())
                        .last("LIMIT 1")
        );
        assertNotNull(savedSpu, "商品保存失败");
        assertEquals(Boolean.TRUE, savedSpu.getSpecType(), "应该是多规格");

        // 验证SKU数量
        List<ProductSku> savedSkus = productSkuService.getSkuListBySpuId(savedSpu.getId());
        int expectedSkuCount = memoryValues.size() * storageValues.size() * processorValues.size();
        assertEquals(expectedSkuCount, savedSkus.size(),
                String.format("应该创建%d个SKU（%d内存×%d存储×%d处理器）",
                        expectedSkuCount, memoryValues.size(), storageValues.size(), processorValues.size()));

        log.info("========== 多SKU笔记本电脑商品新增测试通过，共创建{}个SKU ==========", savedSkus.size());
    }

    /**
     * 构建单SKU商品BO
     */
    private ProductSpuBO buildSingleSkuSpuBO(Long categoryId, Long brandId,
                                             String name, String keyword,
                                             String introduction, String img,
                                             List<String> sliderImgs) {
        ProductSpuBO spuBO = new ProductSpuBO();
        spuBO.setName(name);
        spuBO.setKeyword(keyword);
        spuBO.setSpuType(SpuType.PHYSICAL);
        spuBO.setCategoryId(categoryId);
        spuBO.setBrandId(brandId);
        spuBO.setImg(img);
        spuBO.setSliderImgList(sliderImgs);
        spuBO.setIntroduction(introduction);
        spuBO.setDescription(introduction + " - 详细描述信息");
        spuBO.setSpecType(Boolean.FALSE); // 单规格
        spuBO.setDeliveryTemplateId(1L); // 默认包邮
        return spuBO;
    }

    /**
     * 构建多SKU商品BO
     */
    private ProductSpuBO buildMultiSkuSpuBO(Long categoryId, Long brandId,
                                            String name, String keyword,
                                            String introduction, String img,
                                            List<String> sliderImgs) {
        ProductSpuBO spuBO = new ProductSpuBO();
        spuBO.setName(name);
        spuBO.setKeyword(keyword);
        spuBO.setSpuType(SpuType.PHYSICAL);
        spuBO.setCategoryId(categoryId);
        spuBO.setBrandId(brandId);
        spuBO.setImg(img);
        spuBO.setSliderImgList(sliderImgs);
        spuBO.setIntroduction(introduction);
        spuBO.setDescription(introduction + " - 详细描述信息");
        spuBO.setSpecType(Boolean.TRUE); // 多规格
        spuBO.setDeliveryTemplateId(1L); // 默认包邮
        return spuBO;
    }

    /**
     * 打印商品SPU详情信息（控制台直观展示）
     * @param spuDetail 商品SPU详情对象
     */
    public void printSpuDetail(AppProductSpuDetailVO spuDetail) {
        if (spuDetail == null) {
            System.out.println("❌ 商品SPU详情为空");
            return;
        }

        String split = "================================================================================";
        System.out.println(split);
        System.out.println("🛍️  商品SPU详情信息");
        System.out.println(split);

        // ========== 基本信息 =========
        System.out.println("📋 【基本信息】");
        System.out.printf("  商品编号: %d\n", spuDetail.getId());
        System.out.printf("  商品名称: %s\n", spuDetail.getName());
        System.out.printf("  商品类型: %s\n", spuDetail.getSpuType());
        System.out.printf("  商品简介: %s\n", spuDetail.getIntroduction());
        System.out.printf("  商品分类: %d\n", spuDetail.getCategoryId());
        System.out.printf("  商品品牌: %d\n", spuDetail.getBrandId());
        System.out.printf("  封面图: %s\n", spuDetail.getImg());

        // 轮播图
        if (spuDetail.getSliderImgList() != null && !spuDetail.getSliderImgList().isEmpty()) {
            System.out.println("  轮播图:");
            for (int i = 0; i < spuDetail.getSliderImgList().size(); i++) {
                System.out.printf("    %d. %s\n", i + 1, spuDetail.getSliderImgList().get(i));
            }
        } else {
            System.out.println("  轮播图: 无");
        }

        // ========== 统计信息 =========
        System.out.println("\n📊 【统计信息】");
        System.out.printf("  商品销量: %,d 件\n", spuDetail.getSalesCount());

        // ========== SKU信息 =========
        System.out.println("\n📦 【SKU信息】");
        if (spuDetail.getSkus() != null && !spuDetail.getSkus().isEmpty()) {
            for (int i = 0; i < spuDetail.getSkus().size(); i++) {
                AppProductSpuDetailVO.Sku sku = spuDetail.getSkus().get(i);
                System.out.printf("  SKU #%d:\n", i + 1);
                System.out.printf("    SKU编号: %d\n", sku.getId());
                System.out.printf("    销售价: ¥%.2f\n", sku.getPrice() / 100.0);
                System.out.printf("    市场价: ¥%.2f\n", sku.getMarketPrice() / 100.0);
                System.out.printf("    库存: %d 件\n", sku.getStock());
                System.out.printf("    图片: %s\n", sku.getImg());

                // 商品属性
                if (sku.getProperties() != null && !sku.getProperties().isEmpty()) {
                    System.out.println("    商品属性:");
                    for (ProductSku.Property property : sku.getProperties()) {
                        System.out.printf("      - %s: %s\n",
                                property.getPropertyName() != null ? property.getPropertyName() : "未知属性",
                                property.getValueName() != null ? property.getValueName() : "未知值");
                    }
                } else {
                    System.out.println("    商品属性: 无");
                }
                System.out.println();
            }
        } else {
            System.out.println("  暂无SKU信息");
        }

        System.out.println(split);
        System.out.println("✅ 商品信息展示完毕");
        System.out.println(split);
    }

}

