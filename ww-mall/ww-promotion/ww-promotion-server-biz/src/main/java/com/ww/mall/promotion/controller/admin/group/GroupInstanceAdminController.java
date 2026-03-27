package com.ww.mall.promotion.controller.admin.group;

import com.ww.mall.promotion.engine.GroupCommandService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 拼团实例管理控制器。
 * <p>
 * 该控制器聚焦拼团实例运行态的管理操作，
 * 当前提供“待退款成员检查”和“退款补偿重发”两个排障入口。
 *
 * @author ww
 * @create 2026-03-26
 * @description: 拼团实例管理接口
 */
@Slf4j
@RestController
@RequestMapping("/promotion/group/instance/admin")
@Tag(name = "拼团实例管理", description = "拼团实例运行态管理接口")
public class GroupInstanceAdminController {

    @Resource
    private GroupCommandService groupCommandService;

    /**
     * 判断当前拼团是否仍存在待退款成员。
     *
     * @param groupId 团ID
     * @return true-存在待退款成员
     */
    @GetMapping("/refund/pending/{groupId}")
    @Operation(summary = "检查拼团待退款状态", description = "检查指定拼团是否仍存在 FAILED_REFUND_PENDING 成员")
    public Boolean hasPendingRefund(
            @Parameter(description = "拼团ID", required = true) @PathVariable String groupId) {
        return groupCommandService.hasPendingRefund(groupId);
    }

    /**
     * 触发指定拼团的待退款成员补偿重发。
     *
     * @param groupId 团ID
     * @param reason 触发原因
     * @return 本次成功投递的退款补偿消息数
     */
    @PostMapping("/refund/compensate/{groupId}")
    @Operation(summary = "重发拼团退款补偿", description = "对指定 FAILED 拼团中仍处于待退款状态的成员重新投递退款补偿消息")
    public Integer triggerPendingRefundCompensation(
            @Parameter(description = "拼团ID", required = true) @PathVariable String groupId,
            @Parameter(description = "补偿原因，不传则回退为拼团失败原因或默认文案")
            @RequestParam(required = false) String reason) {
        return groupCommandService.triggerPendingRefundCompensation(groupId, reason);
    }
}
