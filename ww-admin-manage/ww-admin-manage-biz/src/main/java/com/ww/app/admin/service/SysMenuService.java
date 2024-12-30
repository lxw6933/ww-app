package com.ww.app.admin.service;

import cn.hutool.core.lang.tree.Tree;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ww.app.admin.entity.SysMenu;
import com.ww.app.admin.enums.SysMenuType;
import com.ww.app.admin.view.form.SysMenuForm;
import com.ww.app.admin.view.vo.SysMenuParentVO;
import com.ww.app.admin.view.vo.SysMenuVO;
import com.ww.app.common.common.IdForm;

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
    List<Tree<Long>> tree();

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

    /**
     * 获取菜单类型下的上一级父级菜单
     *
     * @param menuType 菜单类型
     * @return SysMenuParentVO
     */
    List<SysMenuParentVO> allParent(SysMenuType menuType);
}

