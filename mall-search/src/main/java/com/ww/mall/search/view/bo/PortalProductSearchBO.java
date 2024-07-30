package com.ww.mall.search.view.bo;

import cn.hutool.core.collection.CollectionUtil;
import com.ww.mall.common.exception.ApiException;
import com.ww.mall.web.cmmon.MallPage;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.query.Criteria;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

/**
 * @author ww
 * @create 2024-07-23- 18:53
 * @description: 门户搜索BO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PortalProductSearchBO extends MallPage {

    /**
     * 搜索关键词
     */
    private String keyword;

    /**
     * 店铺id
     */
    private Long merchantId;

    /**
     * 渠道id
     */
    @NotNull(message = "渠道不能为空")
    private Long channelId;

    /**
     * 适用范围BO
     */
    private SearchRangeBO searchRangeBO;

    /**
     * 排序BO
     */
    private SearchSortBO searchSortBO;

    public Criteria buildQueryCriteria() {
        Criteria criteria = new Criteria();
        criteria.and("channelId").is(this.channelId)
                .and("skuStatus").is(1)
                .and("upStatus").is(1)
                .and("openSearch").is(1)
                .and("brandAuthExpire").is(0);
        if (StringUtils.isNotEmpty(this.keyword)) {
            String pattern = ".*" + this.keyword + ".*";
            criteria.orOperator(
                    Criteria.where("spuTitle").regex(pattern, "i"),
                    Criteria.where("spuSubTitle").regex(pattern, "i")
            );
        }
        if (this.merchantId != null) {
            criteria.and("merchantId").is(this.merchantId);
        }
        if (this.searchRangeBO != null && CollectionUtil.isNotEmpty(this.searchRangeBO.getIdList()) && this.searchRangeBO.getRangeType() != null) {
            switch (this.searchRangeBO.getRangeType()) {
                case SMS:
                    criteria.and("smsId").in(this.searchRangeBO.getIdList());
                    break;
                case SPU:
                    criteria.and("spuId").in(this.searchRangeBO.getIdList());
                    break;
                case BRAND:
                    criteria.and("brandId").in(this.searchRangeBO.getIdList());
                    break;
                case CATEGORY:
                    criteria.and("categoryId").in(this.searchRangeBO.getIdList());
                    break;
                default:
            }
        }
        return criteria;
    }

    public Sort buildSort(boolean integralChannel) {
        List<Sort.Order> sortFieldList = new ArrayList<>();
        if (this.searchSortBO != null) {
            boolean asc = Boolean.TRUE.equals(this.searchSortBO.getSort());
            switch (this.searchSortBO.getSortType()) {
                case INTEGRAL:
                    sortFieldList.add(asc ? Sort.Order.asc("minFixIntegral") : Sort.Order.desc("minFixIntegral"));
                    break;
                case SALE_NUMBER:
                    sortFieldList.add(asc ? Sort.Order.asc("spuSaleNumber") : Sort.Order.desc("spuSaleNumber"));
                    break;
                case TIME:
                    sortFieldList.add(asc ? Sort.Order.asc("upTime") : Sort.Order.desc("upTime"));
                    break;
                case PRICE:
                    if (integralChannel) {
                        sortFieldList.add(asc ? Sort.Order.asc("minFixPrice") : Sort.Order.desc("minFixPrice"));
                    } else {
                        sortFieldList.add(asc ? Sort.Order.asc("salePrice") : Sort.Order.desc("salePrice"));
                    }
                    break;
                default:
                    throw new ApiException("数据异常");
            }
        }
        if (sortFieldList.isEmpty()) {
            if (integralChannel) {
                sortFieldList.add(Sort.Order.asc("minFixIntegral"));
                sortFieldList.add(Sort.Order.asc("minFixPrice"));
            }
            sortFieldList.add(Sort.Order.desc("spuSaleNumber"));
            sortFieldList.add(Sort.Order.desc("upTime"));
        }
        return Sort.by(sortFieldList);
    }

    public AggregationOperation buildGroup() {
        return Aggregation.group("channelId", "spuId")
                .first("spuId").as("spuId")
                .first("skuId").as("skuId")
                .first("smsId").as("smsId")
                .first("brandId").as("brandId")
                .first("spuTitle").as("spuTitle")
                .first("spuSubTitle").as("spuSubTitle")
                .first("salePrice").as("salePrice")
                .first("suggestSalesPrice").as("suggestSalesPrice")
                .first("minFixPrice").as("minFixPrice")
                .first("minFixIntegral").as("minFixIntegral")
                .count().as("count");
    }

}
