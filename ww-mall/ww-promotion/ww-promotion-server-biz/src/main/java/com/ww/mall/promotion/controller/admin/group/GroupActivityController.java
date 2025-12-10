package com.ww.mall.promotion.controller.admin.group;

import com.ww.mall.promotion.controller.admin.group.req.GroupActivityBO;
import com.ww.mall.promotion.entity.group.GroupActivity;
import com.ww.mall.promotion.service.group.GroupActivityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author ww
 * @create 2025-12-08 18:00
 * @description: 拼团活动控制器
 */
@Slf4j
@RestController
@RequestMapping("/promotion/group/activity/admin")
@Tag(name = "拼团活动管理", description = "拼团活动相关接口，包括活动的创建、更新、查询等")
public class GroupActivityController {

    @Resource
    private GroupActivityService activityService;

    @PostMapping("/create")
    @Operation(summary = "创建拼团活动", description = "创建新的拼团活动，包括活动信息、商品信息、价格、人数要求等")
    public GroupActivity createActivity(@RequestBody @Validated GroupActivityBO bo) {
        return activityService.createActivity(bo);
    }

    @PutMapping("/update")
    @Operation(summary = "更新拼团活动", description = "更新已存在的拼团活动信息，活动进行中时不允许修改关键信息")
    public GroupActivity updateActivity(@RequestBody @Validated GroupActivityBO bo) {
        return activityService.updateActivity(bo);
    }

    @GetMapping("/detail/{activityId}")
    @Operation(summary = "查询活动详情", description = "根据活动ID查询拼团活动的详细信息")
    public GroupActivity getActivityDetail(@Parameter(description = "活动ID", required = true) @PathVariable String activityId) {
        return activityService.getActivityById(activityId);
    }

    @GetMapping("/list/active")
    @Operation(summary = "查询进行中的活动列表", description = "查询当前时间处于活动期间内的所有拼团活动")
    public List<GroupActivity> listActiveActivities() {
        return activityService.listActiveActivities();
    }

    @GetMapping("/list/spu/{spuId}")
    @Operation(summary = "根据SPU ID查询活动", description = "根据商品SPU ID查询相关的拼团活动列表")
    public List<GroupActivity> getActivitiesBySpuId(
            @Parameter(description = "商品SPU ID", required = true) @PathVariable Long spuId) {
        return activityService.getActivitiesBySpuId(spuId);
    }

    @PutMapping("/enable/{activityId}")
    @Operation(summary = "启用/禁用活动", description = "启用或禁用拼团活动，禁用后活动将不可见")
    public Void enableActivity(
            @Parameter(description = "活动ID", required = true) @PathVariable String activityId,
            @Parameter(description = "启用状态：1-启用，0-禁用", required = true) @RequestParam Integer enabled) {
        activityService.enableActivity(activityId, enabled);
        return null;
    }

}
