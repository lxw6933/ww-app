package com.ww.mall.promotion.service.group;

import com.ww.mall.promotion.controller.admin.group.res.GroupAdminDetailVO;

/**
 * 拼团后台查询服务。
 *
 * @author ww
 * @create 2026-03-17
 * @description: 为客服后台提供团级和成员轨迹级聚合查询
 */
public interface GroupAdminService {

    /**
     * 查询拼团后台详情。
     *
     * @param groupId 拼团ID
     * @return 后台详情
     */
    GroupAdminDetailVO getDetail(String groupId);
}
