package com.ww.mall.search.view.bo;

import cn.hutool.core.collection.CollUtil;
import com.ww.mall.web.cmmon.MallPage;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
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
     * 搜索指定spu范围【活动商品、优惠券适用商品等】
     */
    private List<Long> specifySpuIdList;

    /**
     * 搜索指定sms范围【活动商品、优惠券适用商品等】
     */
    private List<Long> specifySmsIdList;

    /**
     * 搜索指定category范围【活动商品、优惠券适用商品等】
     */
    private List<Long> categoryIdList;

    /**
     * 是否积分排序【true：顺序  false：倒叙】
     */
    private Boolean integralSort;

    /**
     * 是否销量排序【true：顺序  false：倒叙】
     */
    private Boolean saleNumberSort;

    /**
     * 是否按照时间排序【true：顺序  false：倒叙】
     */
    private Boolean timeSort;

    /**
     * 是否按照销售价排序【true：顺序  false：倒叙】
     */
    private Boolean priceSort;

    public AggregationOperation buildQueryCriteriaAggregation() {
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
        if (CollUtil.isNotEmpty(this.specifySpuIdList)) {
            criteria.and("spuId").in(this.specifySpuIdList);
        }
        if (CollUtil.isNotEmpty(this.specifySmsIdList)) {
            criteria.and("smsId").in(this.specifySmsIdList);
        }
        if (CollUtil.isNotEmpty(this.categoryIdList)) {
            if (this.categoryIdList.size() > 1) {
                criteria.and("categoryId").in(this.categoryIdList);
            } else {
                criteria.and("categoryId").is(this.categoryIdList.get(0));
            }
        }
        return Aggregation.match(criteria);
    }

    public AggregationOperation buildSortAggregation(boolean integralChannel) {
        List<Sort.Order> sortFieldList = new ArrayList<>();
        if (this.integralSort != null && integralChannel) {
            sortFieldList.add(this.integralSort ? Sort.Order.asc("minFixIntegral") : Sort.Order.desc("minFixIntegral"));
        }
        if (this.saleNumberSort != null) {
            sortFieldList.add(this.saleNumberSort ? Sort.Order.asc("spuSaleNumber") : Sort.Order.desc("spuSaleNumber"));
        }
        if (this.timeSort != null) {
            sortFieldList.add(this.timeSort ? Sort.Order.asc("upTime") : Sort.Order.desc("upTime"));
        }
        if (this.priceSort != null) {
            sortFieldList.add(this.priceSort ? Sort.Order.asc("minFixPrice") : Sort.Order.desc("minFixPrice"));
        }
        if (sortFieldList.isEmpty()) {
            if (integralChannel) {
                sortFieldList.add(Sort.Order.asc("minFixIntegral"));
                sortFieldList.add(Sort.Order.asc("minFixPrice"));
            }
            sortFieldList.add(Sort.Order.desc("spuSaleNumber"));
            sortFieldList.add(Sort.Order.desc("upTime"));
        }
        return Aggregation.sort(Sort.by(sortFieldList));
    }

    public AggregationOperation buildGroup(boolean integralChannel) {
        GroupOperation groupOperation = Aggregation.group("channelId", "spuId")
                .first("spuId").as("spuId")
                .first("skuId").as("skuId")
                .first("smsId").as("smsId")
                .first("brandId").as("brandId")
                .first("spuTitle").as("spuTitle")
                .first("spuSubTitle").as("spuSubTitle")
                .first("salePrice").as("salePrice")
                .first("suggestSalesPrice").as("suggestSalesPrice");
        if (integralChannel) {
            groupOperation.first("minFixPrice").as("minFixPrice")
                    .first("minFixIntegral").as("minFixIntegral");
        }
        groupOperation.count().as("count");
        return groupOperation;
    }

}
