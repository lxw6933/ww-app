package com.ww.mall.product.controller.admin.property;

import com.ww.mall.product.service.property.ProductPropertyValueService;
import com.ww.mall.product.controller.admin.property.req.ProductPropertyValueBO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * @author ww
 * @create 2023-07-29- 11:21
 * @description:
 */
@RestController
@Tag(name = "管理后台 - 商品属性值")
@RequestMapping("/product/propertyValue")
public class ProductPropertyValueController {

    @Resource
    private ProductPropertyValueService productPropertyValueService;

    @PostMapping
    @Operation(summary = "新增商品属性值")
    public boolean add(@RequestBody @Validated ProductPropertyValueBO productPropertyValueBO) {
        return productPropertyValueService.add(productPropertyValueBO);
    }

    @PutMapping
    @Operation(summary = "编辑商品属性值")
    public boolean update(@RequestBody @Validated ProductPropertyValueBO productPropertyValueBO) {
        return productPropertyValueService.update(productPropertyValueBO);
    }

}
