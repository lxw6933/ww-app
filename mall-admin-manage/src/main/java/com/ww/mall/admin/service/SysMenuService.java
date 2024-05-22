package com.ww.mall.admin.service;

import cn.hutool.core.lang.tree.Tree;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ww.mall.admin.entity.SysMenu;
import com.ww.mall.admin.view.form.SysMenuForm;
import com.ww.mall.admin.view.vo.SysMenuVO;
import com.ww.mall.common.enums.SysPlatformType;
import com.ww.mall.web.view.form.IdForm;

import java.util.List;

/**
 * @author ww
 * @create 2024-05-20 14:02:20
 * @description:
 */
public interface SysMenuService extends IService<SysMenu> {

    /**
     * menu tree data
     *
     * @return tree
     */
    List<Tree<Long>> tree(SysPlatformType platform);

    /**
     * 详情
     * @param id 主键ID
     * @return SysPermissionVO
     */
    SysMenuVO info(Long id);

    /**
     * 新增
     * @param form 表单信息
     * @return SysPermissionVO
     */
    boolean save(SysMenuForm form);

    /**
     * 编辑
     * @param form 表单信息
     */
    boolean update(SysMenuForm form);

    /**
     * 删除
     * @param idForm idForm
     */
    boolean delete(IdForm idForm);

}

