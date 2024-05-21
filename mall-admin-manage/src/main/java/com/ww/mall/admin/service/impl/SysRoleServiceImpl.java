package com.ww.mall.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ww.mall.admin.dao.SysRoleMapper;
import com.ww.mall.admin.entity.SysRole;
import com.ww.mall.admin.service.BaseService;
import com.ww.mall.admin.service.SysRoleService;
import com.ww.mall.admin.view.form.SysRoleForm;
import com.ww.mall.admin.view.query.SysRolePageQuery;
import com.ww.mall.admin.view.vo.SysRoleVO;
import com.ww.mall.common.common.MallAdminUser;
import com.ww.mall.common.enums.SysPlatformType;
import com.ww.mall.common.exception.ApiException;
import com.ww.mall.redis.annotation.MallResubmission;
import com.ww.mall.web.cmmon.MallPageResult;
import com.ww.mall.web.utils.AuthorizationContext;
import com.ww.mall.web.view.form.IdForm;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author ww
 * @create 2024-05-20 14:02:20
 * @description:
 */
@Service("sysRoleService")
public class SysRoleServiceImpl extends BaseService<SysRoleMapper, SysRole> implements SysRoleService {

    @Override
    public MallPageResult<SysRoleVO> page(SysRolePageQuery query) {
        return null;
    }

    @Override
    public SysRoleVO info(Long id) {
        MallAdminUser adminUser = AuthorizationContext.getAdminUser();
        SysRole sysRole;
        if (adminUser.getPlatform() == SysPlatformType.BOSS) {
            // BOSS账号可以查看所有角色信息
            sysRole = this.getById(id);
        } else {
            // 非BOSS账号，只能查看当前平台的角色
            sysRole = this.getOne(new QueryWrapper<SysRole>()
                    .eq("id", id)
                    .eq("platform_id", adminUser.getPlatformId())
                    .eq("platform", adminUser.getPlatform())
            );
        }
        if (sysRole == null) {
            throw new ApiException("角色信息不存在");
        }
        SysRoleVO vo = new SysRoleVO();
        BeanUtils.copyProperties(sysRole, vo);
        return vo;
    }

    @Override
    @Transactional
    @MallResubmission
    public boolean save(SysRoleForm form) {
        MallAdminUser adminUser = AuthorizationContext.getAdminUser();

        if (adminUser.getPlatform() == SysPlatformType.BOSS) {
            // 校验角色编号在同一平台下唯一
            SysRole exist = sf.getSysRoleService().getOne(new QueryWrapper<SysRole>()
                    .eq("role_no", form.getRoleNo())
                    .eq("platform", adminUser.getPlatform())
            );
            // BOSS账号可以添加所有平台的角色
            SysRole sysRole = new SysRole();
            BeanUtils.copyProperties(form, sysRole);
        } else {
            // 校验角色编号在同一平台下唯一
            SysRole exist = sf.getSysRoleService().getOne(new QueryWrapper<SysRole>()
                    .eq("role_no", form.getRoleNo())
                    .eq("platform_id", adminUser.getPlatformId())
                    .eq("platform", adminUser.getPlatform())
            );
            // 非BOSS账号，只能添加当前平台的角色
        }
        return true;
    }

    @Override
    @Transactional
    @MallResubmission
    public boolean update(SysRoleForm form) {
        return false;
    }

    @Override
    @Transactional
    @MallResubmission
    public boolean delete(IdForm form) {
        MallAdminUser adminUser = AuthorizationContext.getAdminUser();
        boolean success;
        if (adminUser.getPlatform() == SysPlatformType.BOSS) {
            // BOSS账号可以删除任意平台角色
            success = this.removeById(form.getId());
        } else {
            // 非BOSS账号，只能删除当前平台的角色
            success = this.remove(new QueryWrapper<SysRole>()
                    .eq("id", form.getId())
                    .eq("platform_id", adminUser.getPlatformId())
                    .eq("platform", adminUser.getPlatform())
            );
        }
        if (success) {
            // 删除sys_user_role相关记录
            df.getSysRoleMapper().deleteRoleOfUser(form.getId());
            // 删除sys_role_menu相关记录
            df.getSysRoleMapper().deleteRoleOfMenu(form.getId());
        }
        return success;
    }

}

