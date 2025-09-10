package com.ww.mall.product.view.query;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.ww.app.common.common.AppPage;
import com.ww.app.mybatis.core.LambdaQueryWrapperX;
import com.ww.mall.product.entity.spu.ProductSpu;
import com.ww.mall.product.enums.SpuStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * @author ww
 * @create 2025-09-10 10:25
 * @description:
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ProductSpuPageQuery extends AppPage {

    /**
     * 出售中商品
     */
    public static final Integer FOR_SALE = 0;

    /**
     * 已下架商品
     */
    public static final Integer DOWN = 1;

    /**
     * 已售空商品
     */
    public static final Integer SOLD_OUT = 2;

    /**
     * 已冻结商品
     */
    public static final Integer FREEZE = 3;

    @Schema(description = "商品名称")
    private String name;

    @Schema(description = "商品编码")
    private String spuCode;

    @Schema(description = "商品分类Id")
    private Long categoryId;

    @Schema(description = "商品品牌Id")
    private Long brandId;

    @Schema(description = "开始时间")
    private Date startTime;

    @Schema(description = "结束时间")
    private Date endTime;

    @Schema(description = "前端请求的tab类型")
    private Integer tabType = 0;

    public Wrapper<ProductSpu> buildQuery() {
        LambdaQueryWrapperX<ProductSpu> queryWrapperX = new LambdaQueryWrapperX<ProductSpu>()
                .likeIfPresent(ProductSpu::getName, this.name)
                .eqIfPresent(ProductSpu::getSpuCode, this.spuCode)
                .eqIfPresent(ProductSpu::getBrandId, this.brandId)
                .eqIfPresent(ProductSpu::getCategoryId, this.categoryId)
                .betweenIfPresent(ProductSpu::getCreateTime, this.startTime, this.endTime);
        appendTabQuery(tabType, queryWrapperX);
        return queryWrapperX;
    }

    private void appendTabQuery(Integer tabType, LambdaQueryWrapperX<ProductSpu> queryWrapperX) {
        // 出售中商品
        if (ObjectUtil.equals(FOR_SALE, tabType)) {
            queryWrapperX.eqIfPresent(ProductSpu::getStatus, SpuStatus.UP);
        }
        // 已下架商品
        if (ObjectUtil.equals(DOWN, tabType)) {
            queryWrapperX.eqIfPresent(ProductSpu::getStatus, SpuStatus.DOWN);
        }
        // 已冻结商品
        if (ObjectUtil.equals(FREEZE, tabType)) {
            queryWrapperX.eqIfPresent(ProductSpu::getStatus, SpuStatus.FREEZE);
        }
        // 已售空商品
//        if (ObjectUtil.equals(SOLD_OUT, tabType)) {
//            queryWrapperX.eqIfPresent(ProductSpu::getStock, 0);
//        }
    }

}
