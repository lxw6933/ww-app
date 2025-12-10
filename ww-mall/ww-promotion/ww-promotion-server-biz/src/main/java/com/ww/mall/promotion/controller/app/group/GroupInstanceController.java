package com.ww.mall.promotion.controller.app.group;

import com.ww.mall.promotion.controller.app.group.req.CreateGroupRequest;
import com.ww.mall.promotion.controller.app.group.req.JoinGroupRequest;
import com.ww.mall.promotion.service.group.GroupInstanceService;
import com.ww.mall.promotion.controller.app.group.res.GroupInstanceVO;
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
 * @description: 拼团实例控制器
 */
@Slf4j
@RestController
@RequestMapping("/promotion/group/instance/app")
@Tag(name = "拼团实例管理", description = "拼团实例相关接口，包括创建拼团、加入拼团、查询拼团等")
public class GroupInstanceController {

    @Resource
    private GroupInstanceService instanceService;

    @PostMapping("/create")
    @Operation(summary = "创建拼团", description = "用户发起拼团，创建新的拼团实例。需要先创建订单，然后调用此接口创建拼团")
    public GroupInstanceVO createGroup(@RequestBody @Validated CreateGroupRequest request) {
        return instanceService.createGroup(request);
    }

    @PostMapping("/join")
    @Operation(summary = "加入拼团", description = "用户加入已存在的拼团。需要先创建订单，然后调用此接口加入拼团")
    public GroupInstanceVO joinGroup(@RequestBody @Validated JoinGroupRequest request) {
        return instanceService.joinGroup(request);
    }

    @GetMapping("/detail/{groupId}")
    @Operation(summary = "查询拼团详情", description = "根据拼团ID查询拼团的详细信息，包括成员列表、状态、剩余名额等")
    public GroupInstanceVO getGroupDetail(
            @Parameter(description = "拼团ID", required = true) @PathVariable String groupId) {
        return instanceService.getGroupDetail(groupId);
    }

    @GetMapping("/user")
    @Operation(summary = "查询用户参与的拼团列表", description = "查询当前登录用户参与的所有拼团列表")
    public List<GroupInstanceVO> getUserGroups() {
        return instanceService.getUserGroups();
    }

    @GetMapping("/activity/{activityId}")
    @Operation(summary = "查询活动下的拼团列表", description = "查询指定活动下的所有拼团列表，可按状态筛选")
    public List<GroupInstanceVO> getActivityGroups(
            @Parameter(description = "活动ID", required = true) @PathVariable String activityId,
            @Parameter(description = "拼团状态：OPEN-进行中，SUCCESS-成功，FAILED-失败，不传则查询所有") 
            @RequestParam(required = false) String status) {
        return instanceService.getActivityGroups(activityId, status);
    }

}
