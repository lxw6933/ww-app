package com.ww.app.admin.service;

import cn.hutool.core.lang.tree.Tree;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ww.app.admin.entity.SysMenu;
import com.ww.app.admin.view.form.SysMenuForm;
import com.ww.app.admin.view.vo.SysMenuVO;
import com.ww.app.common.common.IdForm;

import java.util.List;

/**
 * @author ww
 * @create 2024-05-20 14:02:20
 */
public interface SysMenuService extends IService<SysMenu> {

    /**
     * menu tree data
     *
     * @return tree
     */
    List<Tree<Long>> tree();

    /**
     * 详情
     *
     * @param id 主键ID
     * @return SysPermissionVO
     */
    SysMenuVO info(Long id);

    /**
     * 新增
     *
     * @param form 表单信息
     * @return SysPermissionVO
     */
    boolean save(SysMenuForm form);

    /**
     * 编辑
     *
     * @param form 表单信息
     */
    boolean update(SysMenuForm form);

    /**
     * 删除
     *
     * @param idForm idForm
     */
    boolean delete(IdForm idForm);

    /**
     * 校验菜单路由地址是否存在
     *
     * @param id   主键，编辑时存在
     * @param path 路由地址，允许为空，为空时不进行校验，返回false
     * @return 当路由地址不为空时，如果存在返回true，否则返回false
     */
    boolean pathExists(Long id, String path);

    /**
     * 校验菜单名称是否存在
     *
     * @param id   主键，编辑时存在
     * @param path 菜单名称，允许为空，为空时不进行校验，返回false
     * @return 当菜单名称不为空时，如果存在返回true，否则返回false
     */
    boolean nameExists(Long id, String name);
}

