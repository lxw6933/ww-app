package com.ww.mall.admin.service.impl;

import cn.hutool.core.lang.Assert;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ww.mall.admin.dao.SysUserMapper;
import com.ww.mall.admin.entity.SysMenu;
import com.ww.mall.admin.entity.SysRole;
import com.ww.mall.admin.entity.SysUser;
import com.ww.mall.admin.service.BaseService;
import com.ww.mall.admin.service.SysUserService;
import com.ww.mall.admin.utils.PasswordUtil;
import com.ww.mall.admin.view.form.ModifyPasswordForm;
import com.ww.mall.admin.view.form.SysUserForm;
import com.ww.mall.admin.view.form.UserAndRoleForm;
import com.ww.mall.admin.view.query.SysUserPageQuery;
import com.ww.mall.admin.view.vo.CurrentSysUserInfoVO;
import com.ww.mall.admin.view.vo.SysMenuVO;
import com.ww.mall.admin.view.vo.SysRoleVO;
import com.ww.mall.admin.view.vo.SysUserVO;
import com.ww.mall.annotation.plugs.redis.MallResubmission;
import com.ww.mall.common.common.MallAdminUser;
import com.ww.mall.common.constant.Constant;
import com.ww.mall.common.exception.ApiException;
import com.ww.mall.common.common.MallPageResult;
import com.ww.mall.mybatisplus.MallPlusPageResult;
import com.ww.mall.web.utils.AuthorizationContext;
import com.ww.mall.web.view.bo.SysUserLoginBO;
import com.ww.mall.web.view.dto.SysUserDTO;
import com.ww.mall.web.view.form.IdForm;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * @author ww
 * @create 2024-05-20 14:02:20
 * @description:
 */
@Slf4j
@Service
public class SysUserServiceImpl extends BaseService<SysUserMapper, SysUser> implements SysUserService {

    @Override
    public MallPageResult<SysUserVO> page(SysUserPageQuery query) {
        IPage<SysUser> page = new Page<>(query.getPageNum(), query.getPageSize());
        this.page(page, query.getQueryWrapper());
        return new MallPlusPageResult<>(page, sysUser -> {
            SysUserVO vo = new SysUserVO();
            BeanUtils.copyProperties(sysUser, vo);
            return vo;
        });
    }

    @Override
    @Transactional
    @MallResubmission
    public boolean save(SysUserForm form) {
        SysUser sysUser = this.getOne(new QueryWrapper<SysUser>().eq("username", form.getUsername()));
        Assert.isNull(sysUser, () -> new ApiException("账号已存在"));
        SysUser newSysUser = new SysUser();
        BeanUtils.copyProperties(form, newSysUser);
        String salt = PasswordUtil.generateSalt();
        newSysUser.setPassword(PasswordUtil.resetPassword(salt));
        newSysUser.setSalt(salt);
        newSysUser.setValid(true);
        newSysUser.setStatus(true);
        this.save(newSysUser);
        saveUserRoles(newSysUser.getId(), form.getRoleIds());
        return true;
    }

    @Override
    @Transactional
    @MallResubmission
    public boolean update(SysUserForm form) {
        SysUser sysUser = this.getById(form.getId());
        Assert.notNull(sysUser, () -> new ApiException("信息不存在"));

        if (Objects.equals(Constant.SUPER_ADMIN_MANAGER_ID, form.getId())) {
            throw new ApiException("禁止修改超管账号的信息");
        }
        // 账号不能更新
        form.setUsername(sysUser.getUsername());
        BeanUtils.copyProperties(form, sysUser);
        this.updateById(sysUser);
        // 判断角色是否变化
        List<Long> userRoleIds = df.getSysUserMapper().findRoleIdsByUserId(sysUser.getId());
        if (CollectionUtils.isEmpty(userRoleIds)) {
            saveUserRoles(sysUser.getId(), form.getRoleIds());
        } else {
            if (!CollectionUtils.isEqualCollection(form.getRoleIds(), userRoleIds)) {
                // 删除之前所有的关联信息，新增目前的关联信息
                df.getSysUserMapper().deleteUserOfRole(sysUser.getId());
                saveUserRoles(sysUser.getId(), form.getRoleIds());
            }
        }
        return true;
    }

    /**
     * 维护用户角色关联信息
     *
     * @param sysUserId 用户id
     * @param roleIds 角色id集合
     */
    private void saveUserRoles(Long sysUserId, List<Long> roleIds) {
        if (CollectionUtils.isNotEmpty(roleIds)) {
            UserAndRoleForm data = new UserAndRoleForm();
            data.setUserId(sysUserId);
            data.setRoleIds(roleIds);
            df.getSysUserMapper().addUserOfRoleInfo(data);
        }
    }

    @Override
    public SysUserVO info(Long userId) {
        SysUserVO sysUserVO = new SysUserVO();
        SysUser sysUser = this.getById(userId);
        Assert.notNull(sysUser, () -> new ApiException("信息不存在"));
        BeanUtils.copyProperties(sysUser, sysUserVO);
        return sysUserVO;
    }

    @Override
    @MallResubmission
    public boolean modifySysUserStatus(Long userId, boolean status) {
        MallAdminUser adminUser = AuthorizationContext.getAdminUser();
        if (Objects.equals(adminUser.getUserId(), userId)) {
            throw new ApiException("禁止修改自己账号的状态");
        }
        if (Objects.equals(Constant.SUPER_ADMIN_MANAGER_ID, userId)) {
            throw new ApiException("禁止修改超管账号的状态");
        }
        return this.update(new UpdateWrapper<SysUser>()
                .set("status", status)
                .eq("id", userId)
        );
    }

    @Override
    @MallResubmission
    public boolean delete(IdForm form) {
        MallAdminUser adminUser = AuthorizationContext.getAdminUser();
        if (Objects.equals(adminUser.getUserId(), form.getId())) {
            throw new ApiException("禁止删除自己账号");
        }
        if (Objects.equals(Constant.SUPER_ADMIN_MANAGER_ID, form.getId())) {
            throw new ApiException("禁止删除超管账号");
        }
        return this.update(new UpdateWrapper<SysUser>()
                .set("valid", false)
                .eq("id", form.getId())
        );
    }

    @Override
    @Transactional
    @MallResubmission
    public boolean modifyPassword(ModifyPasswordForm modifyPasswordForm) {
        MallAdminUser adminUser = AuthorizationContext.getAdminUser();
        Assert.isTrue(modifyPasswordForm.getNewPassword().equals(modifyPasswordForm.getConfirmNewPassword()), () -> new ApiException("新密码前后两次不一致"));
        // 校验旧密码是否一致
        SysUser sysUser = this.getById(adminUser.getUserId());
        boolean checkPassword = PasswordUtil.checkPassword(modifyPasswordForm.getOldPassword(), sysUser.getSalt() + sysUser.getPassword());
        Assert.isTrue(checkPassword, () -> new ApiException("旧密码错误"));
        // 更新密码
        String salt = PasswordUtil.generateSalt();
        String newPassword = PasswordUtil.generatePassword(modifyPasswordForm.getNewPassword(), salt);
        return this.update(new UpdateWrapper<SysUser>()
                .set("salt", salt)
                .set("password", newPassword)
                .eq("id", adminUser.getUserId())
        );
    }

    @Override
    @MallResubmission
    public boolean resetPassword(Long userId) {
        String salt = PasswordUtil.generateSalt();
        String password = PasswordUtil.resetPassword(salt);
        return this.update(new UpdateWrapper<SysUser>()
                .set("salt", salt)
                .set("password", password)
                .eq("id", userId)
        );
    }

    @Override
    public SysUserVO info(String username, String password) {
        SysUser sysUser = this.getOne(new QueryWrapper<SysUser>().eq("username", username));
        Assert.notNull(sysUser, () -> new ApiException("用户账号不存在"));
        Assert.isTrue(sysUser.getStatus(), () -> new ApiException("账号状态异常"));
        Assert.isTrue(sysUser.getValid(), () -> new ApiException("账号无效"));
        boolean checkPassword = PasswordUtil.checkPassword(password, sysUser.getSalt() + sysUser.getPassword());
        Assert.isTrue(checkPassword, () -> new ApiException("密码错误"));
        SysUserVO sysUserVO = new SysUserVO();
        BeanUtils.copyProperties(sysUser, sysUserVO);
        return sysUserVO;
    }

    @Override
    public List<SysRoleVO> queryUserOfRole(Long userId) {
        List<Long> userRoleIdList = df.getSysUserMapper().findRoleIdsByUserId(userId);
        if (CollectionUtils.isEmpty(userRoleIdList)) {
            return Collections.emptyList();
        }
        List<SysRole> userRoleList = sf.getSysRoleService().listByIds(userRoleIdList);
        List<SysRoleVO> userRoleVOList = new ArrayList<>();
        userRoleList.forEach(target -> {
            SysRoleVO vo = new SysRoleVO();
            BeanUtils.copyProperties(target, vo);
            userRoleVOList.add(vo);
        });
        return userRoleVOList;
    }

    @Override
    public List<SysMenuVO> queryUserOfMenu(Long userId) {
        List<Long> userRoleIdList = df.getSysUserMapper().findRoleIdsByUserId(userId);
        if (CollectionUtils.isEmpty(userRoleIdList)) {
            return Collections.emptyList();
        }
        List<Long> userMenuIdList = df.getSysUserMapper().findMenuIdsByRoleIds(userRoleIdList);
        if (CollectionUtils.isEmpty(userMenuIdList)) {
            return Collections.emptyList();
        }
        List<SysMenu> userMenuList = sf.getSysMenuService().listByIds(userMenuIdList);
        List<SysMenuVO> userMenuVOList = new ArrayList<>();
        userMenuList.forEach(target -> {
            SysMenuVO vo = new SysMenuVO();
            BeanUtils.copyProperties(target, vo);
            userMenuVOList.add(vo);
        });
        return userMenuVOList;
    }

    @Override
    public SysUserDTO login(SysUserLoginBO form) {
        SysUserVO sysUserVO = this.info(form.getUsername(), form.getPassword());
        SysUserDTO sysUserDTO = new SysUserDTO();
        sysUserDTO.setId(sysUserVO.getId());
        sysUserDTO.setMobile(sysUserVO.getPhone());
        sysUserDTO.setUsername(sysUserVO.getUsername());
        sysUserDTO.setNickname(sysUserVO.getNickname());
        sysUserDTO.setHeadPicture(sysUserVO.getHeadPicture());
        return sysUserDTO;
    }

    @Override
    public CurrentSysUserInfoVO selfInfo() {
        MallAdminUser adminUser = AuthorizationContext.getAdminUser();
        SysUserVO sysUserVO = this.info(adminUser.getUserId());
        CurrentSysUserInfoVO currentSysUserInfoVO = new CurrentSysUserInfoVO();
        BeanUtils.copyProperties(sysUserVO, currentSysUserInfoVO);
        currentSysUserInfoVO.setUserId(adminUser.getUserId());
        return currentSysUserInfoVO;
    }

}

