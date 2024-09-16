package com.ww.mall.admin.service.impl;

import cn.hutool.core.lang.Assert;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ww.mall.admin.dao.SysRoleMapper;
import com.ww.mall.admin.entity.SysRole;
import com.ww.mall.admin.service.BaseService;
import com.ww.mall.admin.service.SysRoleService;
import com.ww.mall.admin.view.form.RoleAndMenuForm;
import com.ww.mall.admin.view.form.SysRoleForm;
import com.ww.mall.admin.view.query.SysRolePageQuery;
import com.ww.mall.admin.view.vo.SysRoleSelectVO;
import com.ww.mall.admin.view.vo.SysRoleVO;
import com.ww.mall.annotation.plugs.redis.MallResubmission;
import com.ww.mall.common.exception.ApiException;
import com.ww.mall.common.common.MallPageResult;
import com.ww.mall.mybatisplus.MallPlusPageResult;
import com.ww.mall.web.view.form.IdForm;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * @author ww
 * @create 2024-05-20 14:02:20
 * @description:
 */
@Service
public class SysRoleServiceImpl extends BaseService<SysRoleMapper, SysRole> implements SysRoleService {

    @Override
    public MallPageResult<SysRoleVO> page(SysRolePageQuery query) {
        IPage<SysRole> page = new Page<>(query.getPageNum(), query.getPageSize());
        this.page(page, query.getQueryWrapper());
        return new MallPlusPageResult<>(page, sysRole -> {
            SysRoleVO vo = new SysRoleVO();
            BeanUtils.copyProperties(sysRole, vo);
            return vo;
        });
    }

    @Override
    public SysRoleVO info(Long id) {
        SysRole sysRole = this.getById(id);
        Assert.notNull(sysRole, () -> new ApiException("信息不存在"));
        SysRoleVO vo = new SysRoleVO();
        BeanUtils.copyProperties(sysRole, vo);
        List<Long> roleMenuIds = df.getSysRoleMapper().findMenuIdsByRoleId(id);
        vo.setPermissionIds(roleMenuIds);
        return vo;
    }

    @Override
    @Transactional
    @MallResubmission
    public boolean save(SysRoleForm form) {
        SysRole sysRole = new SysRole();
        BeanUtils.copyProperties(form, sysRole);
        sysRole.setStatus(true);
        this.save(sysRole);
        saveRolePermissions(sysRole.getId(), form.getPermissionIds());
        return true;
    }

    @Override
    @Transactional
    @MallResubmission
    public boolean update(SysRoleForm form) {
        SysRole sysRole = this.getById(form.getId());
        Assert.notNull(sysRole, () -> new ApiException("信息不存在"));

        BeanUtils.copyProperties(form, sysRole);
        this.updateById(sysRole);
        // 判断权限是否变化
        List<Long> rolePermissionIds = df.getSysRoleMapper().findMenuIdsByRoleId(sysRole.getId());
        if (CollectionUtils.isEmpty(rolePermissionIds)) {
            saveRolePermissions(sysRole.getId(), form.getPermissionIds());
        } else {
            if (!CollectionUtils.isEqualCollection(form.getPermissionIds(), rolePermissionIds)) {
                // 删除之前所有的关联信息，新增目前的关联信息
                df.getSysRoleMapper().removeRoleAndPermission(sysRole.getId());
                saveRolePermissions(sysRole.getId(), form.getPermissionIds());
            }
        }
        return true;
    }

    @Override
    @MallResubmission
    public boolean delete(IdForm idForm) {
        return this.removeById(idForm.getId());
    }

    @Override
    @MallResubmission(expire = 1)
    public boolean modifyStatus(Long roleId) {
        SysRole sysRole = this.getById(roleId);
        Assert.notNull(sysRole, () -> new ApiException("角色信息异常"));
        sysRole.setStatus(!sysRole.getStatus());
        return this.updateById(sysRole);
    }

    @Override
    public List<SysRoleSelectVO> getAllRole() {
        List<SysRole> allRoles = this.list();
        List<SysRoleSelectVO> roleSelectList = new ArrayList<>();
        allRoles.forEach(role -> {
            SysRoleSelectVO vo = new SysRoleSelectVO();
            vo.setId(role.getId());
            vo.setName(role.getName());
            roleSelectList.add(vo);
        });
        return roleSelectList;
    }

    /**
     * 维护角色权限关联信息
     *
     * @param sysRoleId 角色id
     * @param permissionIds 权限id集合
     */
    private void saveRolePermissions(Long sysRoleId, List<Long> permissionIds) {
        if (CollectionUtils.isNotEmpty(permissionIds)) {
            // 保存角色权限关联信息
            RoleAndMenuForm data = new RoleAndMenuForm();
            data.setRoleId(sysRoleId);
            data.setPermissionIds(permissionIds);
            df.getSysRoleMapper().insertRoleAndPermission(data);
        }
    }

}

