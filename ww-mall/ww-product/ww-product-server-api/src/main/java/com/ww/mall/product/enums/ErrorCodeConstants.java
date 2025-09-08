package com.ww.mall.product.enums;

import com.ww.app.common.common.ResCode;

/**
 * @author ww
 * @create 2025-09-05 23:05
 * @description:
 */
public interface ErrorCodeConstants {

    // ========== 商品分类相关 1-008-001-000 ============
    ResCode CATEGORY_NOT_EXISTS = new ResCode(1_008_001_000, "商品分类不存在");
    ResCode CATEGORY_PARENT_NOT_EXISTS = new ResCode(1_008_001_001, "父分类不存在");
    ResCode CATEGORY_PARENT_NOT_FIRST_LEVEL = new ResCode(1_008_001_002, "父分类不能是二级分类");
    ResCode CATEGORY_EXISTS_CHILDREN = new ResCode(1_008_001_003, "存在子分类，无法删除");
    ResCode CATEGORY_DISABLED = new ResCode(1_008_001_004, "商品分类({})已禁用，无法使用");
    ResCode CATEGORY_HAVE_BIND_SPU = new ResCode(1_008_001_005, "类别下存在商品，无法删除");

    // ========== 商品品牌相关编号 1-008-002-000 ==========
    ResCode BRAND_NOT_EXISTS = new ResCode(1_008_002_000, "品牌不存在");
    ResCode BRAND_DISABLED = new ResCode(1_008_002_001, "品牌已禁用");
    ResCode BRAND_NAME_EXISTS = new ResCode(1_008_002_002, "品牌名称已存在");

    // ========== 商品属性项 1-008-003-000 ==========
    ResCode PROPERTY_NOT_EXISTS = new ResCode(1_008_003_000, "属性项不存在");
    ResCode PROPERTY_EXISTS = new ResCode(1_008_003_001, "属性项的名称已存在");
    ResCode PROPERTY_DELETE_FAIL_VALUE_EXISTS = new ResCode(1_008_003_002, "属性项下存在属性值，无法删除");

    // ========== 商品属性值 1-008-004-000 ==========
    ResCode PROPERTY_VALUE_NOT_EXISTS = new ResCode(1_008_004_000, "属性值不存在");
    ResCode PROPERTY_VALUE_EXISTS = new ResCode(1_008_004_001, "属性值的名称已存在");


}
