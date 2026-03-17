package com.ww.mall.promotion.controller.admin.group;

import com.ww.mall.promotion.controller.admin.group.res.GroupAdminDetailVO;
import com.ww.mall.promotion.service.group.GroupAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 拼团后台聚合查询控制器。
 *
 * @author ww
 * @create 2026-03-17
 * @description: 提供客服后台查看拼团详情和成员轨迹的统一入口
 */
@RestController
@RequestMapping("/promotion/group/admin")
@Tag(name = "拼团后台聚合查询", description = "客服后台查看拼团详情、成员状态和轨迹")
public class GroupAdminController {

    @Resource
    private GroupAdminService groupAdminService;

    @GetMapping("/detail/{groupId}")
    @Operation(summary = "查询拼团后台详情", description = "返回团摘要以及每个成员的拼团轨迹，便于客服排障和回访")
    public GroupAdminDetailVO detail(
            @Parameter(description = "拼团ID", required = true) @PathVariable String groupId) {
        return groupAdminService.getDetail(groupId);
    }
}
