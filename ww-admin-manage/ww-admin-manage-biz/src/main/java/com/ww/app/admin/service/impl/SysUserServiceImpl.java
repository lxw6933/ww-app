package com.ww.app.admin.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.lang.tree.Tree;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mzt.logapi.context.LogRecordContext;
import com.mzt.logapi.service.impl.DiffParseFunction;
import com.mzt.logapi.starter.annotation.LogRecord;
import com.ww.app.admin.dao.SysUserMapper;
import com.ww.app.admin.entity.SysMenu;
import com.ww.app.admin.entity.SysRole;
import com.ww.app.admin.entity.SysUser;
import com.ww.app.admin.service.BaseService;
import com.ww.app.admin.service.SysUserService;
import com.ww.app.admin.user.bo.SysUserLoginBO;
import com.ww.app.admin.user.dto.SysUserDTO;
import com.ww.app.admin.utils.PasswordUtil;
import com.ww.app.admin.view.form.ModifyPasswordForm;
import com.ww.app.admin.view.form.SysUserForm;
import com.ww.app.admin.view.form.UserAndRoleForm;
import com.ww.app.admin.view.query.SysUserPageQuery;
import com.ww.app.admin.view.vo.CurrentSysUserInfoVO;
import com.ww.app.admin.view.vo.SysMenuTreeNodeVO;
import com.ww.app.admin.view.vo.SysRoleSelectVO;
import com.ww.app.admin.view.vo.SysUserVO;
import com.ww.app.common.common.IdForm;
import com.ww.app.common.common.AdminUser;
import com.ww.app.common.common.AppPageResult;
import com.ww.app.common.constant.Constant;
import com.ww.app.common.exception.ApiException;
import com.ww.app.common.utils.AuthorizationContext;
import com.ww.app.mybatis.common.AppPlusPageResult;
import com.ww.app.redis.annotation.Resubmission;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static com.ww.app.admin.constant.LogRecordConstants.*;
import static com.ww.app.admin.utils.PasswordUtil.DEFAULT_PASSWORD;
import static com.ww.app.common.utils.CollectionUtils.convertList;

/**
 * @author ww
 * @create 2024-05-20 14:02:20
 * @description:
 */
@Slf4j
@Service
public class SysUserServiceImpl extends BaseService<SysUserMapper, SysUser> implements SysUserService {

    @Override
    public AppPageResult<SysUserVO> page(SysUserPageQuery query) {
        IPage<SysUser> page = new Page<>(query.getPageNum(), query.getPageSize());
        this.page(page, query.getQueryWrapper());
        return new AppPlusPageResult<>(page, sysUser -> {
            SysUserVO vo = new SysUserVO();
            BeanUtils.copyProperties(sysUser, vo);
            return vo;
        });
    }

    @Override
    @Transactional
    @Resubmission
    @LogRecord(type = SYSTEM_USER_TYPE, subType = SYSTEM_USER_CREATE_SUB_TYPE, bizNo = "{{#user.id}}", success = SYSTEM_USER_CREATE_SUCCESS)
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
        // 记录操作日志上下文
        LogRecordContext.putVariable("user", newSysUser);
        return true;
    }

    @Override
    @Transactional
    @Resubmission
    @LogRecord(type = SYSTEM_USER_TYPE, subType = SYSTEM_USER_UPDATE_SUB_TYPE, bizNo = "{{#form.id}}", success = SYSTEM_USER_UPDATE_SUCCESS)
    public boolean update(SysUserForm form) {
        SysUser oldSysUser = this.getById(form.getId());
        Assert.notNull(oldSysUser, () -> new ApiException("信息不存在"));

        if (Objects.equals(Constant.SUPER_ADMIN_MANAGER_ID, form.getId())) {
            throw new ApiException("禁止修改超管账号的信息");
        }
        // 账号不能更新
        SysUser updateSysUser = BeanUtil.toBean(form, SysUser.class);
        this.updateById(updateSysUser);
        // 判断角色是否变化
        List<Long> userRoleIds = df.getSysUserMapper().findRoleIdsByUserId(updateSysUser.getId());
        if (CollectionUtils.isEmpty(userRoleIds)) {
            saveUserRoles(updateSysUser.getId(), form.getRoleIds());
        } else {
            if (!CollectionUtils.isEqualCollection(form.getRoleIds(), userRoleIds)) {
                // 删除之前所有的关联信息，新增目前的关联信息
                df.getSysUserMapper().deleteUserOfRole(updateSysUser.getId());
                saveUserRoles(updateSysUser.getId(), form.getRoleIds());
            }
        }
        // 记录操作日志上下文
        LogRecordContext.putVariable("user", oldSysUser);
        LogRecordContext.putVariable(DiffParseFunction.OLD_OBJECT, BeanUtil.toBean(oldSysUser, SysUserForm.class));
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
        // 查询用户角色信息
        List<Long> userRoleIds = df.getSysUserMapper().findRoleIdsByUserId(userId);
        sysUserVO.setRoleIds(userRoleIds);
        return sysUserVO;
    }

    @Override
    @Resubmission(expire = 1)
    public boolean modifySysUserStatus(Long userId, boolean status) {
        AdminUser adminUser = AuthorizationContext.getAdminUser();
        if (Objects.equals(adminUser.getId(), userId)) {
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
    @Resubmission
    @LogRecord(type = SYSTEM_USER_TYPE, subType = SYSTEM_USER_DELETE_SUB_TYPE, bizNo = "{{#form.id}}", success = SYSTEM_USER_DELETE_SUCCESS)
    public boolean delete(IdForm form) {
        AdminUser adminUser = AuthorizationContext.getAdminUser();
        if (Objects.equals(adminUser.getId(), form.getId())) {
            throw new ApiException("禁止删除自己账号");
        }
        if (Objects.equals(Constant.SUPER_ADMIN_MANAGER_ID, form.getId())) {
            throw new ApiException("禁止删除超管账号");
        }
        SysUser sysUser = this.getById(form.getId());
        Assert.notNull(sysUser, () -> new ApiException("信息不存在"));
        this.update(new UpdateWrapper<SysUser>()
                .set("valid", false)
                .eq("id", form.getId())
        );
        // 记录操作日志上下文
        LogRecordContext.putVariable("user", sysUser);
        return true;
    }

    @Override
    @Transactional
    @Resubmission
    public boolean modifyPassword(ModifyPasswordForm modifyPasswordForm) {
        AdminUser adminUser = AuthorizationContext.getAdminUser();
        Assert.isTrue(modifyPasswordForm.getNewPassword().equals(modifyPasswordForm.getConfirmNewPassword()), () -> new ApiException("新密码前后两次不一致"));
        // 校验旧密码是否一致
        SysUser sysUser = this.getById(adminUser.getId());
        boolean checkPassword = PasswordUtil.checkPassword(modifyPasswordForm.getOldPassword(), sysUser.getSalt() + sysUser.getPassword());
        Assert.isTrue(checkPassword, () -> new ApiException("旧密码错误"));
        // 更新密码
        String salt = PasswordUtil.generateSalt();
        String newPassword = PasswordUtil.generatePassword(modifyPasswordForm.getNewPassword(), salt);
        return this.update(new UpdateWrapper<SysUser>()
                .set("salt", salt)
                .set("password", newPassword)
                .eq("id", adminUser.getId())
        );
    }

    @Override
    @Resubmission(expire = 1)
    @LogRecord(type = SYSTEM_USER_TYPE, subType = SYSTEM_USER_UPDATE_PASSWORD_SUB_TYPE, bizNo = "{{#userId}}", success = SYSTEM_USER_UPDATE_PASSWORD_SUCCESS)
    public boolean resetPassword(Long userId) {
        SysUser sysUser = this.getById(userId);
        Assert.notNull(sysUser, () -> new ApiException("信息不存在"));
        String salt = PasswordUtil.generateSalt();
        String password = PasswordUtil.resetPassword(salt);
        this.update(new UpdateWrapper<SysUser>()
                .set("salt", salt)
                .set("password", password)
                .eq("id", userId)
        );
        // 记录操作日志上下文
        LogRecordContext.putVariable("user", sysUser);
        LogRecordContext.putVariable("newPassword", DEFAULT_PASSWORD);
        return true;
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
    public List<SysRoleSelectVO> queryUserOfRole(Long userId) {
        List<Long> userRoleIdList = df.getSysUserMapper().findRoleIdsByUserId(userId);
        if (CollectionUtils.isEmpty(userRoleIdList)) {
            return Collections.emptyList();
        }
        List<SysRole> userRoleList = sf.getSysRoleService().listByIds(userRoleIdList);
        List<SysRoleSelectVO> userRoleVOList = new ArrayList<>();
        userRoleList.forEach(role -> {
            SysRoleSelectVO vo = new SysRoleSelectVO();
            vo.setId(role.getId());
            vo.setName(role.getName());
            userRoleVOList.add(vo);
        });
        return userRoleVOList;
    }

    private List<SysMenu> queryUserOfMenu(Long userId) {
        List<SysMenu> menuList;
        if (Objects.equals(Constant.SUPER_ADMIN_MANAGER_ID, userId)) {
            menuList = sf.getSysMenuService().list(new QueryWrapper<SysMenu>()
                    .eq("valid", true)
            );
        } else {
            List<Long> userRoleIdList = df.getSysUserMapper().findRoleIdsByUserId(userId);
            if (CollectionUtils.isEmpty(userRoleIdList)) {
                return Collections.emptyList();
            }
            List<Long> userMenuIdList = df.getSysUserMapper().findMenuIdsByRoleIds(userRoleIdList);
            if (CollectionUtils.isEmpty(userMenuIdList)) {
                return Collections.emptyList();
            }
            menuList = sf.getSysMenuService().list(new QueryWrapper<SysMenu>()
                    .eq("valid", true)
                    .in("id", userMenuIdList)
            );
        }
        return menuList;
    }

    @Override
    @Resubmission
    public SysUserDTO login(SysUserLoginBO form) {
        SysUserVO sysUserVO = this.info(form.getUsername(), form.getPassword());
        SysUserDTO sysUserDTO = new SysUserDTO();
        sysUserDTO.setId(sysUserVO.getId());
        sysUserDTO.setMobile(sysUserVO.getPhone());
        sysUserDTO.setUsername(sysUserVO.getUsername());
        sysUserDTO.setRealName(sysUserVO.getRealName());
        sysUserDTO.setAvatar(sysUserVO.getAvatar());
        // 加载用户权限到redis
        List<SysMenu> userMenuList = this.queryUserOfMenu(sysUserVO.getId());
        List<String> userAuthorities = convertList(userMenuList, SysMenu::getPermission);
        redisTemplate.opsForValue().set(sf.getAuthorityRedisKeyBuilder().buildUserAuthoritiesKey(sysUserVO.getId()), userAuthorities);
        sysUserDTO.setAuthorities(userAuthorities);
        return sysUserDTO;
    }

    @Override
    public CurrentSysUserInfoVO selfInfo() {
        AdminUser adminUser = AuthorizationContext.getAdminUser();
        SysUserVO sysUserVO = this.info(adminUser.getId());
        CurrentSysUserInfoVO currentSysUserInfoVO = new CurrentSysUserInfoVO();
        BeanUtils.copyProperties(sysUserVO, currentSysUserInfoVO);
        currentSysUserInfoVO.setUserId(adminUser.getId());
        // 用户权限信息
        List<SysMenu> sysMenuList = this.queryUserOfMenu(adminUser.getId());
        List<Tree<Long>> userMenuTreeList = SysMenuTreeNodeVO.menuTree(sysMenuList);
        currentSysUserInfoVO.setMenuTreeList(userMenuTreeList);
        currentSysUserInfoVO.setPermissions(convertList(sysMenuList, SysMenu::getPermission));
        return currentSysUserInfoVO;
    }

    @Override
    @Resubmission
    public boolean modifyStatus(Long userId) {
        if (Objects.equals(Constant.SUPER_ADMIN_MANAGER_ID, userId)) {
            throw new ApiException("禁止修改超管账号的信息");
        }
        SysUser sysUser = this.getById(userId);
        Assert.notNull(sysUser, () -> new ApiException("用户信息异常"));
        sysUser.setStatus(!sysUser.getStatus());
        return this.updateById(sysUser);
    }

    @Override
    public SysUserDTO loadUserDetails(String username) {
        SysUser sysUser = this.getOne(new QueryWrapper<SysUser>().eq("username", username).eq("valid", true));
        // 加载用户权限到redis
        List<SysMenu> userMenuList = this.queryUserOfMenu(sysUser.getId());
        List<String> userAuthorities = convertList(userMenuList, SysMenu::getPermission);
        redisTemplate.opsForValue().set(sf.getAuthorityRedisKeyBuilder().buildUserAuthoritiesKey(sysUser.getId()), JSON.toJSONString(userAuthorities));
        SysUserDTO sysUserDTO = BeanUtil.toBean(sysUser, SysUserDTO.class);
        sysUserDTO.setAuthorities(userAuthorities);
        return sysUserDTO;
    }

}

