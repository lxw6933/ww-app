package com.ww.mall.promotion.service.group.convert;

import com.ww.mall.promotion.controller.admin.group.req.GroupActivityBO;
import com.ww.mall.promotion.controller.app.group.res.GroupInstanceVO;
import com.ww.mall.promotion.entity.group.GroupActivity;
import com.ww.mall.promotion.entity.group.GroupInstance;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

/**
 * @author ww
 * @create 2025-12-09 15:17
 * @description:
 */
@Mapper
public interface GroupConvert {

    GroupConvert INSTANCE = Mappers.getMapper(GroupConvert.class);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "soldCount", ignore = true)
    @Mapping(target = "enabled", ignore = true)
    GroupActivity groupActivityBOToActivity(GroupActivityBO bo);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "soldCount", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "enabled", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    void updateGroupActivity(GroupActivityBO bo, @MappingTarget GroupActivity entity);

    GroupInstanceVO groupInstanceToVO(GroupInstance instance);
}
