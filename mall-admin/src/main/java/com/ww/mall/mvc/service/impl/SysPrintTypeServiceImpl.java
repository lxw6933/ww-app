package com.ww.mall.mvc.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageInfo;
import com.ww.mall.common.common.BatchProcessingResult;
import com.ww.mall.common.exception.ValidatorException;
import com.ww.mall.common.utils.BeanCopierUtils;
import com.ww.mall.config.mybatisplus.page.MyPageHelper;
import com.ww.mall.config.mybatisplus.page.MyPageInfo;
import com.ww.mall.config.mybatisplus.page.Pagination;
import com.ww.mall.mvc.dao.SysPrintTypeDao;
import com.ww.mall.mvc.entity.SysPrintTypeEntity;
import com.ww.mall.mvc.service.SysPrintTypeService;
import com.ww.mall.mvc.view.form.admin.SysPrintTypeForm;
import com.ww.mall.mvc.view.vo.admin.SysPrintTypeVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @description:
 * @author: ww
 * @create: 2021-05-17 13:47
 */
@Service
public class SysPrintTypeServiceImpl extends ServiceImpl<SysPrintTypeDao, SysPrintTypeEntity> implements SysPrintTypeService {

    /**
     * 分页
     *
     * @param pagination 分页条件
     * @param query      查询条件
     * @return PageInfo
     */
    @Override
    public PageInfo<SysPrintTypeVO> page(Pagination pagination, QueryWrapper<SysPrintTypeEntity> query) {
        MyPageHelper.startPage(pagination, SysPrintTypeVO.class);
        List<SysPrintTypeEntity> list = list(query);
        return new MyPageInfo<>(list).convert(entity -> {
            SysPrintTypeVO vo = new SysPrintTypeVO();
            BeanCopierUtils.copyProperties(entity, vo);
            return vo;
        });
    }

    @Override
    public SysPrintTypeVO info(Long id) {
        return null;
    }

    /**
     * 详情
     *
     * @param id 主键ID
     * @return SysPrintTypeVO
     */
    public SysPrintTypeVO info(String id) {
        SysPrintTypeEntity entity = super.getById(id);
        if (entity == null) {
            throw new ValidatorException("信息不存在");
        }
        SysPrintTypeVO vo = new SysPrintTypeVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }

    /**
     * 新增
     *
     * @param form 表单信息
     * @return SysPrintTypeVO
     */
    @Override
    public SysPrintTypeVO save(SysPrintTypeForm form) {
        SysPrintTypeEntity entity = new SysPrintTypeEntity();
        BeanUtils.copyProperties(form, entity);
        super.save(entity);
        return info(entity.getId());
    }

    /**
     * 编辑
     *
     * @param form 表单信息
     */
    @Override
    public SysPrintTypeVO update(SysPrintTypeForm form) {
        SysPrintTypeEntity entity = super.getById(form.getId());
        if (entity == null) {
            throw new ValidatorException("信息不存在");
        }
        BeanUtils.copyProperties(form, entity);
        super.updateById(entity);
        return info(entity.getId());
    }

    /**
     * 删除
     *
     * @param id 主键ID
     */
    @Override
    public void deleteById(Long id) {
        SysPrintTypeEntity entity = super.getById(id);
        if (entity == null) {
            throw new ValidatorException("信息不存在");
        }
        super.removeById(id);
    }

    /**
     * 批量删除
     *
     * @param ids 主键ID
     */
    @Override
    public BatchProcessingResult batchDelete(List<Long> ids) {
        BatchProcessingResult result = new BatchProcessingResult();
        for (Long id : ids) {
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

