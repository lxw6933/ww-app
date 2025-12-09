package com.ww.mall.promotion.controller.admin.group.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.Date;

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
    @NotNull(message = "活动名称不能为空")
    private String name;

    @Schema(description = "活动描述", example = "双十一特惠拼团，2人成团")
    private String description;

    @Schema(description = "商品SPU ID", requiredMode = REQUIRED, example = "1001")
    @NotNull(message = "商品SPU ID不能为空")
    private Long spuId;

    @Schema(description = "商品SKU ID", requiredMode = REQUIRED, example = "2001")
    @NotNull(message = "商品SKU ID不能为空")
    private Long skuId;

    @Schema(description = "拼团价格", requiredMode = REQUIRED, example = "99.00")
    @NotNull(message = "拼团价格不能为空")
    @Positive(message = "拼团价格必须大于0")
    private BigDecimal groupPrice;

    @Schema(description = "原价", example = "199.00")
    private BigDecimal originalPrice;

    @Schema(description = "拼团人数要求", requiredMode = REQUIRED, example = "2")
    @NotNull(message = "拼团人数不能为空")
    @Positive(message = "拼团人数必须大于0")
    private Integer requiredSize;

    @Schema(description = "拼团有效期（小时）", requiredMode = REQUIRED, example = "24")
    @NotNull(message = "拼团有效期不能为空")
    @Positive(message = "拼团有效期必须大于0")
    private Integer expireHours;

    @Schema(description = "活动开始时间", requiredMode = REQUIRED, example = "2025-12-01 00:00:00")
    @NotNull(message = "活动开始时间不能为空")
    private Date startTime;

    @Schema(description = "活动结束时间", requiredMode = REQUIRED, example = "2025-12-31 23:59:59")
    @NotNull(message = "活动结束时间不能为空")
    private Date endTime;

    @Schema(description = "库存总数", requiredMode = REQUIRED, example = "1000")
    @NotNull(message = "库存总数不能为空")
    @Positive(message = "库存总数必须大于0")
    private Integer totalStock;

    @Schema(description = "每人限购数量", example = "1")
    private Integer limitPerUser;

    @Schema(description = "活动图片URL", example = "https://example.com/image.jpg")
    private String imageUrl;

    @Schema(description = "排序权重", example = "100")
    private Integer sortWeight;

}
