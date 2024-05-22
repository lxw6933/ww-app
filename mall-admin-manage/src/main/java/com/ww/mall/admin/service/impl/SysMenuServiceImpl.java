package com.ww.mall.admin.service.impl;

import cn.hutool.core.lang.tree.Tree;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ww.mall.admin.dao.SysMenuMapper;
import com.ww.mall.admin.entity.SysMenu;
import com.ww.mall.admin.service.BaseService;
import com.ww.mall.admin.service.SysMenuService;
import com.ww.mall.admin.view.form.SysMenuForm;
import com.ww.mall.admin.view.vo.SysMenuTreeNodeVO;
import com.ww.mall.admin.view.vo.SysMenuVO;
import com.ww.mall.common.enums.SysPlatformType;
import com.ww.mall.common.exception.ApiException;
import com.ww.mall.web.view.form.IdForm;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author ww
 * @create 2024-05-20 14:02:20
 * @description:
 */
@Service("sysMenuService")
public class SysMenuServiceImpl extends BaseService<SysMenuMapper, SysMenu> implements SysMenuService {

    @Override
    public List<Tree<Long>> tree(SysPlatformType platform) {
        List<SysMenu> menuList = this.list(new QueryWrapper<SysMenu>()
                .eq("valid", true)
                .eq("platform", platform)
        );
        return SysMenuTreeNodeVO.menuTree(menuList);
    }

    @Override
    public SysMenuVO info(Long id) {
        SysMenu sysMenu = this.getById(id);
        if (sysMenu == null) {
            throw new ApiException("信息不存在");
        }
        SysMenuVO vo = new SysMenuVO();
        BeanUtils.copyProperties(sysMenu, vo);
        return vo;
    }

    @Override
    public boolean save(SysMenuForm form) {
        SysMenu sysMenu = new SysMenu();
        BeanUtils.copyProperties(form, sysMenu);
        return this.save(sysMenu);
    }

    @Override
    public boolean update(SysMenuForm form) {
        SysMenu sysMenu = this.getById(form.getId());
        if (sysMenu == null) {
            throw new ApiException("信息不存在");
        }
        BeanUtils.copyProperties(form, sysMenu);
        return this.updateById(sysMenu);
    }

    @Override
    public boolean delete(IdForm idForm) {
        return this.removeById(idForm.getId());
    }
}

