package com.ww.mall.promotion.controller.app.group;

import com.ww.app.common.common.AppPage;
import com.ww.app.common.common.AppPageResult;
import com.ww.mall.promotion.controller.app.group.res.GroupInstanceVO;
import com.ww.mall.promotion.service.group.GroupInstanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;

/**
 * 拼团实例查询控制器。
 * <p>
 * 该控制器仅对外提供拼团详情与列表查询能力，不再暴露直接开团、参团或支付回调入口。
 *
 * @author ww
 * @create 2025-12-08 18:00
 * @description: 拼团实例查询接口
 */
@Slf4j
@RestController
@RequestMapping("/promotion/group/instance/app")
@Tag(name = "拼团实例查询", description = "拼团实例查询接口")
public class GroupInstanceController {

    @Resource
    private GroupInstanceService instanceService;

    /**
     * 查询拼团详情。
     *
     * @param groupId 拼团ID
     * @return 拼团详情
     */
    @GetMapping("/detail/{groupId}")
    @Operation(summary = "查询拼团详情", description = "根据拼团ID查询拼团的详细信息，包括成员列表、状态、剩余名额等")
    public GroupInstanceVO getGroupDetail(
            @Parameter(description = "拼团ID", required = true) @PathVariable String groupId) {
        return instanceService.getGroupDetail(groupId);
    }

    /**
     * 分页查询当前登录用户参与的拼团列表。
     *
     * @param page 分页参数
     * @return 分页结果
     */
    @GetMapping("/user/page")
    @Operation(summary = "分页查询用户参与的拼团列表", description = "按页查询当前登录用户参与过的拼团列表，默认按最近参与时间倒序返回")
    public AppPageResult<GroupInstanceVO> getUserGroups(@Valid AppPage page) {
        return instanceService.getUserGroups(page);
    }

    /**
     * 分页查询活动下的拼团列表。
     *
     * @param activityId 活动ID
     * @param status 拼团状态
     * @param page 分页参数
     * @return 分页结果
     */
    @GetMapping("/activity/{activityId}/page")
    @Operation(summary = "分页查询活动下的拼团列表", description = "按页查询指定活动下的拼团列表，可按状态筛选，默认按创建时间倒序返回")
    public AppPageResult<GroupInstanceVO> getActivityGroups(
            @Parameter(description = "活动ID", required = true) @PathVariable String activityId,
            @Parameter(description = "拼团状态：OPEN-进行中，SUCCESS-成功，FAILED-失败，不传则查询所有")
            @RequestParam(required = false) String status,
            @Valid AppPage page) {
        return instanceService.getActivityGroups(activityId, status, page);
    }
}
