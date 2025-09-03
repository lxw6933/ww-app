package com.ww.app.admin.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.Assert;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mzt.logapi.context.LogRecordContext;
import com.mzt.logapi.service.impl.DiffParseFunction;
import com.mzt.logapi.starter.annotation.LogRecord;
import com.ww.app.admin.dao.SysRoleMapper;
import com.ww.app.admin.entity.SysRole;
import com.ww.app.admin.service.BaseService;
import com.ww.app.admin.service.SysRoleService;
import com.ww.app.admin.view.form.RoleAndMenuForm;
import com.ww.app.admin.view.form.SysRoleForm;
import com.ww.app.admin.view.query.SysRolePageQuery;
import com.ww.app.admin.view.vo.SysRoleSelectVO;
import com.ww.app.admin.view.vo.SysRoleVO;
import com.ww.app.common.common.AppPageResult;
import com.ww.app.common.common.IdForm;
import com.ww.app.common.exception.ApiException;
import com.ww.app.mybatis.common.AppPlusPageResult;
import com.ww.app.redis.annotation.Resubmission;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static com.ww.app.admin.constant.LogRecordConstants.SYSTEM_ROLE_CREATE_SUB_TYPE;
import static com.ww.app.admin.constant.LogRecordConstants.SYSTEM_ROLE_CREATE_SUCCESS;
import static com.ww.app.admin.constant.LogRecordConstants.SYSTEM_ROLE_DELETE_SUB_TYPE;
import static com.ww.app.admin.constant.LogRecordConstants.SYSTEM_ROLE_DELETE_SUCCESS;
import static com.ww.app.admin.constant.LogRecordConstants.SYSTEM_ROLE_TYPE;
import static com.ww.app.admin.constant.LogRecordConstants.SYSTEM_ROLE_UPDATE_SUB_TYPE;
import static com.ww.app.admin.constant.LogRecordConstants.SYSTEM_ROLE_UPDATE_SUCCESS;

/**
 * @author ww
 * @create 2024-05-20 14:02:20
 * @description:
 */
@Slf4j
@Service
public class SysRoleServiceImpl extends BaseService<SysRoleMapper, SysRole> implements SysRoleService {

    @Override
    public AppPageResult<SysRoleVO> page(SysRolePageQuery query) {
        IPage<SysRole> page = new Page<>(query.getPageNum(), query.getPageSize());
        this.page(page, query.getQueryWrapper());
        return new AppPlusPageResult<>(page, sysRole -> {
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
    @Resubmission
    @LogRecord(type = SYSTEM_ROLE_TYPE, subType = SYSTEM_ROLE_CREATE_SUB_TYPE, bizNo = "{{#form.id}}", success = SYSTEM_ROLE_CREATE_SUCCESS)
    public boolean save(SysRoleForm form) {
        log.info("保存角色");
        SysRole sysRole = new SysRole();
        BeanUtils.copyProperties(form, sysRole);
        sysRole.setRoleNo(sysRole.getName());
        sysRole.setStatus(true);
        this.save(sysRole);
        saveRolePermissions(sysRole.getId(), form.getPermissionIds());
        // 记录操作日志上下文
        LogRecordContext.putVariable("role", sysRole);
        return true;
    }

    @Override
    @Transactional
    @Resubmission
    @LogRecord(type = SYSTEM_ROLE_TYPE, subType = SYSTEM_ROLE_UPDATE_SUB_TYPE, bizNo = "{{#form.id}}", success = SYSTEM_ROLE_UPDATE_SUCCESS)
    public boolean update(SysRoleForm form) {
        SysRole oldSysRole = this.getById(form.getId());
        Assert.notNull(oldSysRole, () -> new ApiException("信息不存在"));

        SysRole updateSysRole = BeanUtil.toBean(form, SysRole.class);
        this.updateById(updateSysRole);
        // 判断权限是否变化
        List<Long> rolePermissionIds = df.getSysRoleMapper().findMenuIdsByRoleId(updateSysRole.getId());
        if (CollectionUtils.isEmpty(rolePermissionIds)) {
            saveRolePermissions(updateSysRole.getId(), form.getPermissionIds());
        } else {
            if (CollectionUtils.isEmpty(form.getPermissionIds()) || !CollectionUtils.isEqualCollection(form.getPermissionIds(), rolePermissionIds)) {
                // 删除之前所有的关联信息，新增目前的关联信息
                df.getSysRoleMapper().removeRoleAndPermission(updateSysRole.getId());
                saveRolePermissions(updateSysRole.getId(), form.getPermissionIds());
            }
        }
        // 记录操作日志上下文
        LogRecordContext.putVariable("role", oldSysRole);
        LogRecordContext.putVariable(DiffParseFunction.OLD_OBJECT, BeanUtil.toBean(oldSysRole, SysRoleForm.class));
        return true;
    }

    @Override
    @Resubmission
    @LogRecord(type = SYSTEM_ROLE_TYPE, subType = SYSTEM_ROLE_DELETE_SUB_TYPE, bizNo = "{{#idForm.id}}", success = SYSTEM_ROLE_DELETE_SUCCESS)
    public boolean delete(IdForm idForm) {
        SysRole sysRole = this.getById(idForm.getId());
        Assert.notNull(sysRole, () -> new ApiException("信息不存在"));
        this.removeById(idForm.getId());
        // 记录操作日志上下文
        LogRecordContext.putVariable("role", sysRole);
        return true;
    }

    @Override
    @Resubmission(expire = 1)
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
     * @param sysRoleId     角色id
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

