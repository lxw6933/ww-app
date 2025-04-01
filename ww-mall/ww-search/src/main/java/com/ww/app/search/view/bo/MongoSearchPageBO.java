package com.ww.app.search.view.bo;

import com.ww.app.common.exception.ApiException;
import com.ww.app.common.utils.SpecialCharacterUtil;
import com.ww.app.mongodb.common.AbstractMongoPage;
import com.ww.app.search.entity.mongo.ProductDoc;
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
public class MongoSearchPageBO extends AbstractMongoPage<ProductDoc> {

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
    private SearchScopeBO searchScopeBO;

    /**
     * 排序BO
     */
    private SearchSortBO searchSortBO;

    /**
     * 区间BO
     */
    private SearchRangeBO searchRangeBO;

    /**
     * 是否积分渠道
     */
    private boolean integralChannel;

    @Override
    public Criteria buildQuery() {
        Criteria criteria = new Criteria();
        criteria.and("channelId").is(this.channelId)
                .and("skuStatus").is(1)
                .and("upStatus").is(1)
                .and("openSearch").is(1)
                .and("brandAuthExpire").is(0);
        if (StringUtils.isNotEmpty(this.keyword)) {
            String escapedKeyword = SpecialCharacterUtil.escapeSpecialCharacters(this.keyword);
            String pattern = ".*" + escapedKeyword + ".*";
            criteria.orOperator(
                    Criteria.where("spuTitle").regex(pattern, "i"),
                    Criteria.where("spuSubTitle").regex(pattern, "i")
            );
        }
        if (this.merchantId != null) {
            criteria.and("merchantId").is(this.merchantId);
        }
        if (this.searchScopeBO != null && this.searchScopeBO.support()) {
            switch (this.searchScopeBO.getRangeType()) {
                case SMS:
                    if (this.searchScopeBO.isContain()) {
                        criteria.and("smsId").in(this.searchScopeBO.getIdList());
                    } else {
                        criteria.and("smsId").nin(this.searchScopeBO.getIdList());
                    }
                    break;
                case SPU:
                    if (this.searchScopeBO.isContain()) {
                        criteria.and("spuId").in(this.searchScopeBO.getIdList());
                    } else {
                        criteria.and("spuId").nin(this.searchScopeBO.getIdList());
                    }
                    break;
                case BRAND:
                    if (this.searchScopeBO.isContain()) {
                        criteria.and("brandId").in(this.searchScopeBO.getIdList());
                    } else {
                        criteria.and("brandId").nin(this.searchScopeBO.getIdList());
                    }
                    break;
                case CATEGORY:
                    if (this.searchScopeBO.isContain()) {
                        criteria.and("categoryId").in(this.searchScopeBO.getIdList());
                    } else {
                        criteria.and("categoryId").nin(this.searchScopeBO.getIdList());
                    }
                    break;
                default:
            }
        }
        if (this.searchRangeBO != null && this.searchRangeBO.support()) {
            switch (this.searchRangeBO.getRangeType()) {
                case INTEGRAL:
                    criteria.andOperator(
                            Criteria.where("integral").gte(this.searchRangeBO.getMin()),
                            Criteria.where("integral").lte(this.searchRangeBO.getMax())
                    );
                    break;
                case PRICE:
                    criteria.andOperator(
                            Criteria.where("salePrice").gte(this.searchRangeBO.getMin()),
                            Criteria.where("salePrice").lte(this.searchRangeBO.getMax())
                    );
                    break;
                default:
            }
        }
        return criteria;
    }

    @Override
    public Sort buildSort() {
        List<Sort.Order> sortFieldList = new ArrayList<>();
        if (this.searchSortBO != null && this.searchSortBO.support()) {
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
                    if (this.integralChannel) {
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
            if (this.integralChannel) {
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
