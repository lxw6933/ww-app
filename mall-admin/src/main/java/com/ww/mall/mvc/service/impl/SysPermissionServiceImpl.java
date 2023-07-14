package com.ww.mall.mvc.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageInfo;
import com.ww.mall.common.common.BatchProcessingResult;
import com.ww.mall.common.exception.ValidatorException;
import com.ww.mall.config.mybatisplus.page.MyPageHelper;
import com.ww.mall.config.mybatisplus.page.MyPageInfo;
import com.ww.mall.config.mybatisplus.page.Pagination;
import com.ww.mall.mvc.dao.SysPermissionDao;
import com.ww.mall.mvc.entity.SysPermissionEntity;
import com.ww.mall.mvc.service.SysPermissionService;
import com.ww.mall.mvc.view.form.admin.SysPermissionForm;
import com.ww.mall.mvc.view.vo.admin.SysPermissionVO;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 后台权限表
 *
 * @author ww
 * @date 2021-06-26 11:36:58
 */
@Service("sysPermissionService")
public class SysPermissionServiceImpl extends ServiceImpl<SysPermissionDao, SysPermissionEntity> implements SysPermissionService {

    /**
     * 分页
     * @param pagination 分页条件
     * @param query      查询条件
     * @return PageInfo
     */
    @Override
    public PageInfo<SysPermissionVO> page(Pagination pagination, QueryWrapper<SysPermissionEntity> query) {
        MyPageHelper.startPage(pagination, SysPermissionVO.class);
        List<SysPermissionEntity> list = list(query);
        return new MyPageInfo<>(list).convert(entity -> {
            SysPermissionVO vo = new SysPermissionVO();
            BeanCopierUtils.copyProperties(entity, vo);
            return vo;
        });
    }

    /**
     * 详情
     * @param id 主键ID
     * @return SysPermissionVO
     */
    @Override
    public SysPermissionVO info(Long id) {
        SysPermissionEntity entity = super.getById(id);
        if (entity == null) {
            throw new ValidatorException("信息不存在");
        }
        SysPermissionVO vo = new SysPermissionVO();
        BeanCopierUtils.copyProperties(entity, vo);
        return vo;
    }

    /**
     * 新增
     * @param form 表单信息
     * @return SysPermissionVO
     */
    @Override
    public SysPermissionVO save(SysPermissionForm form) {
        SysPermissionEntity entity = new SysPermissionEntity();
        BeanCopierUtils.copyProperties(form, entity);
        super.save(entity);
        return info(entity.getId());
    }

    /**
     * 编辑
     * @param form 表单信息
     */
    @Override
    public SysPermissionVO update(SysPermissionForm form) {
        SysPermissionEntity entity = super.getById(form.getId());
        if (entity == null) {
            throw new ValidatorException("信息不存在");
        }
        BeanCopierUtils.copyProperties(form, entity);
        super.updateById(entity);
        return info(entity.getId());
    }

    /**
     * 删除
     * @param id 主键ID
     */
    @Override
    public void deleteById(Long id) {
        SysPermissionEntity entity = super.getById(id);
        if (entity == null) {
            throw new ValidatorException("信息不存在");
        }
        super.removeById(id);
    }

    /**
     * 批量删除
     * @param ids 主键ID
     */
    @Override
    public BatchProcessingResult batchDelete(List<Long> ids) {
        BatchProcessingResult result = new BatchProcessingResult();
        for (Long id :ids){
            try {
                deleteById(id);
                result.addSuccess(1);
            } catch (Exception ex) {
                result.addError(1).addErrorMsg(ex.getMessage());
            }
        }
        return result;
    }
}
