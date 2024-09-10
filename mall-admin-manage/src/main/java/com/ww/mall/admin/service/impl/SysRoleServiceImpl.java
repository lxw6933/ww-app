package com.ww.mall.admin.service.impl;

import com.ww.mall.admin.dao.SysRoleMapper;
import com.ww.mall.admin.entity.SysRole;
import com.ww.mall.admin.service.BaseService;
import com.ww.mall.admin.service.SysRoleService;
import com.ww.mall.admin.view.form.SysRoleForm;
import com.ww.mall.admin.view.query.SysRolePageQuery;
import com.ww.mall.admin.view.vo.SysRoleVO;
import com.ww.mall.annotation.plugs.redis.MallResubmission;
import com.ww.mall.common.exception.ApiException;
import com.ww.mall.web.cmmon.MallPageResult;
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
        SysRole sysRole = this.getById(id);
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
        SysRole sysRole = new SysRole();
        BeanUtils.copyProperties(form, sysRole);
        this.save(sysRole);
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
        boolean success = this.removeById(form.getId());
        if (success) {
            // 删除sys_user_role相关记录
            df.getSysRoleMapper().deleteRoleOfUser(form.getId());
            // 删除sys_role_menu相关记录
            df.getSysRoleMapper().deleteRoleOfMenu(form.getId());
        }
        return success;
    }

}

