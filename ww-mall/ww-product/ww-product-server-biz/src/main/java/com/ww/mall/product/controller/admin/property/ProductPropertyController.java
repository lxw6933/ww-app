package com.ww.mall.product.controller.admin.property;

import com.ww.mall.product.service.property.ProductPropertyService;
import com.ww.mall.product.controller.admin.property.req.ProductPropertyBO;
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
@Tag(name = "管理后台 - 商品属性名")
@RequestMapping("/product/property")
public class ProductPropertyController {

    @Resource
    private ProductPropertyService productPropertyService;

    @PostMapping
    @Operation(summary = "新增商品属性名")
    public boolean add(@RequestBody @Validated ProductPropertyBO productPropertyBO) {
        return productPropertyService.add(productPropertyBO);
    }

    @PutMapping
    @Operation(summary = "编辑商品属性名")
    public boolean update(@RequestBody @Validated ProductPropertyBO productPropertyBO) {
        return productPropertyService.update(productPropertyBO);
    }

}
