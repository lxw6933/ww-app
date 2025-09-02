package com.ww.app.admin.service.impl;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.lang.tree.Tree;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ww.app.admin.dao.SysMenuMapper;
import com.ww.app.admin.entity.SysMenu;
import com.ww.app.admin.enums.SysMenuType;
import com.ww.app.admin.service.BaseService;
import com.ww.app.admin.service.SysMenuService;
import com.ww.app.admin.view.form.SysMenuForm;
import com.ww.app.admin.view.vo.SysMenuParentVO;
import com.ww.app.admin.view.vo.SysMenuTreeNodeVO;
import com.ww.app.admin.view.vo.SysMenuVO;
import com.ww.app.common.common.IdForm;
import com.ww.app.common.exception.ApiException;
import com.ww.app.redis.annotation.Resubmission;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author ww
 * @create 2024-05-20 14:02:20
 * @description:
 */
@Service("sysMenuService")
public class SysMenuServiceImpl extends BaseService<SysMenuMapper, SysMenu> implements SysMenuService {

    @Override
    public List<Tree<Long>> tree() {
        List<SysMenu> menuList = this.list(new QueryWrapper<SysMenu>()
                // .eq("valid", true)
        );
        return SysMenuTreeNodeVO.menuTree(menuList);
    }

    @Override
    public SysMenuVO info(Long id) {
        SysMenu sysMenu = this.getById(id);
        Assert.notNull(sysMenu, () -> new ApiException("信息不存在"));
        SysMenuVO vo = new SysMenuVO();
        BeanUtils.copyProperties(sysMenu, vo);
        return vo;
    }

    @Override
    @Resubmission
    public boolean save(SysMenuForm form) {
        SysMenu sysMenu = new SysMenu();
        BeanUtils.copyProperties(form, sysMenu);
        return this.save(sysMenu);
    }

    @Override
    @Resubmission
    public boolean update(SysMenuForm form) {
        SysMenu sysMenu = this.getById(form.getId());
        Assert.notNull(sysMenu, () -> new ApiException("信息不存在"));
        BeanUtils.copyProperties(form, sysMenu);
        return this.updateById(sysMenu);
    }

    @Override
    @Resubmission
    public boolean delete(IdForm idForm) {
        return this.removeById(idForm.getId());
    }

    // @Override
    // public List<SysMenuParentVO> allParent(SysMenuType menuType) {
    //     List<SysMenu> allParentMenu = new ArrayList<>();
    //     switch (menuType) {
    //         case LEVEL_1_MENU:
    //             break;
    //         case LEVEL_2_MENU:
    //             allParentMenu = sf.getSysMenuService().list(new QueryWrapper<SysMenu>().eq("type", SysMenuType.LEVEL_1_MENU));
    //             break;
    //         case BUTTON:
    //         case ROUTE_PAGE:
    //         case ROUTE_BUTTON:
    //         default:
    //             allParentMenu = sf.getSysMenuService().list(new QueryWrapper<SysMenu>().eq("type", SysMenuType.LEVEL_2_MENU));
    //     }
    //     List<SysMenuParentVO> result = new ArrayList<>();
    //     allParentMenu.forEach(res -> {
    //         SysMenuParentVO vo = new SysMenuParentVO();
    //         vo.setId(res.getId());
    //         vo.setType(res.getType());
    //         vo.setName(res.getName());
    //         result.add(vo);
    //     });
    //     return result;
    // }

    @Override
    public boolean pathExists(Long id, String path) {
        if (StringUtils.isEmpty(path)) {
            return false;
        }
        return sf.getSysMenuService()
                .lambdaQuery()
                .eq(SysMenu::getUrl, path)
                .ne(Objects.nonNull(id), SysMenu::getId, id)
                .exists();
    }

    @Override
    public boolean nameExists(Long id, String name) {
        if (StringUtils.isEmpty(name)) {
            return false;
        }
        return sf.getSysMenuService()
                .lambdaQuery()
                .eq(SysMenu::getName, name)
                .ne(Objects.nonNull(id), SysMenu::getId, id)
                .exists();
    }
}

