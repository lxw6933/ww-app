package com.ww.mall.promotion.controller.admin.group.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import static com.ww.mall.promotion.constants.GroupValidationMessageConstants.ACTIVITY_NAME_REQUIRED;
import static com.ww.mall.promotion.constants.GroupValidationMessageConstants.END_TIME_REQUIRED;
import static com.ww.mall.promotion.constants.GroupValidationMessageConstants.EXPIRE_HOURS_POSITIVE;
import static com.ww.mall.promotion.constants.GroupValidationMessageConstants.EXPIRE_HOURS_REQUIRED;
import static com.ww.mall.promotion.constants.GroupValidationMessageConstants.GROUP_PRICE_POSITIVE;
import static com.ww.mall.promotion.constants.GroupValidationMessageConstants.GROUP_PRICE_REQUIRED;
import static com.ww.mall.promotion.constants.GroupValidationMessageConstants.REQUIRED_SIZE_POSITIVE;
import static com.ww.mall.promotion.constants.GroupValidationMessageConstants.REQUIRED_SIZE_REQUIRED;
import static com.ww.mall.promotion.constants.GroupValidationMessageConstants.SPU_ID_REQUIRED;
import static com.ww.mall.promotion.constants.GroupValidationMessageConstants.START_TIME_REQUIRED;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

/**
 * @author ww
 * @create 2025-12-08 17:10
 * @description: 拼团活动DTO
 */
@Data
@Schema(description = "拼团活动DTO")
public class GroupActivityBO {

    @Schema(description = "活动ID", example = "ACT123456")
    private String id;

    @Schema(description = "活动名称", requiredMode = REQUIRED, example = "双十一拼团活动")
    @NotNull(message = ACTIVITY_NAME_REQUIRED)
    private String name;

    @Schema(description = "活动描述", example = "双十一特惠拼团，2人成团")
    private String description;

    @Schema(description = "商品SPU ID", requiredMode = REQUIRED, example = "1001")
    @NotNull(message = SPU_ID_REQUIRED)
    private Long spuId;

    @Schema(description = "兼容字段：默认SKU ID，新设计以 skuRules 为准", example = "2001")
    private Long skuId;

    @Schema(description = "兼容字段：默认拼团价，新设计以 skuRules 为准", example = "99.00")
    private BigDecimal groupPrice;

    @Schema(description = "兼容字段：默认原价，新设计以 skuRules 为准", example = "199.00")
    private BigDecimal originalPrice;

    @Schema(description = "SKU规则列表，同一团允许购买不同SKU，按 SPU 维度共享拼团", requiredMode = REQUIRED)
    @Valid
    private List<GroupSkuRuleBO> skuRules;

    @Schema(description = "拼团人数要求", requiredMode = REQUIRED, example = "2")
    @NotNull(message = REQUIRED_SIZE_REQUIRED)
    @Positive(message = REQUIRED_SIZE_POSITIVE)
    private Integer requiredSize;

    @Schema(description = "拼团有效期（小时）", requiredMode = REQUIRED, example = "24")
    @NotNull(message = EXPIRE_HOURS_REQUIRED)
    @Positive(message = EXPIRE_HOURS_POSITIVE)
    private Integer expireHours;

    @Schema(description = "活动开始时间", requiredMode = REQUIRED, example = "2025-12-01 00:00:00")
    @NotNull(message = START_TIME_REQUIRED)
    private Date startTime;

    @Schema(description = "活动结束时间", requiredMode = REQUIRED, example = "2025-12-31 23:59:59")
    @NotNull(message = END_TIME_REQUIRED)
    private Date endTime;

    @Schema(description = "每人限购数量", example = "1")
    private Integer limitPerUser;

    @Schema(description = "活动图片URL", example = "https://example.com/image.jpg")
    private String imageUrl;

    @Schema(description = "排序权重", example = "100")
    private Integer sortWeight;

    /**
     * SKU 维度规则。
     */
    @Data
    @Schema(description = "拼团活动SKU规则")
    public static class GroupSkuRuleBO {

        @Schema(description = "SKU ID", requiredMode = REQUIRED, example = "2001")
        @NotNull(message = "SKU ID不能为空")
        private Long skuId;

        @Schema(description = "SKU拼团价", requiredMode = REQUIRED, example = "99.00")
        @NotNull(message = GROUP_PRICE_REQUIRED)
        @Positive(message = GROUP_PRICE_POSITIVE)
        private BigDecimal groupPrice;

        @Schema(description = "SKU原价", example = "199.00")
        private BigDecimal originalPrice;

        @Schema(description = "是否启用，1-启用，0-禁用", example = "1")
        private Integer enabled;
    }

}
