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
import com.ww.mall.mvc.dao.MqMsgLogDao;
import com.ww.mall.mvc.entity.MqMsgLogEntity;
import com.ww.mall.mvc.service.MqMsgLogService;
import com.ww.mall.mvc.view.form.admin.MqMsgLogForm;
import com.ww.mall.mvc.view.vo.admin.MqMsgLogVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * MQ消费日志
 *
 * @author ww
 * @date 2021-06-30 09:30:37
 */
@Service("mqMsgLogService")
public class MqMsgLogServiceImpl extends ServiceImpl<MqMsgLogDao, MqMsgLogEntity> implements MqMsgLogService {

    /**
     * 分页
     *
     * @param pagination 分页条件
     * @param query      查询条件
     * @return PageInfo
     */
    @Override
    public PageInfo<MqMsgLogVO> page(Pagination pagination, QueryWrapper<MqMsgLogEntity> query) {
        MyPageHelper.startPage(pagination, MqMsgLogVO.class);
        List<MqMsgLogEntity> list = list(query);
        return new MyPageInfo<>(list).convert(entity -> {
            MqMsgLogVO vo = new MqMsgLogVO();
            BeanCopierUtils.copyProperties(entity, vo);
            return vo;
        });
    }

    /**
     * 详情
     *
     * @param id 主键ID
     * @return MqMsgLogVO
     */
    @Override
    public MqMsgLogVO info(Long id) {
        MqMsgLogEntity entity = super.getById(id);
        MqMsgLogVO vo = new MqMsgLogVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }

    /**
     * 新增
     *
     * @param form 表单信息
     * @return MqMsgLogVO
     */
    @Override
    public MqMsgLogVO save(MqMsgLogForm form) {
        MqMsgLogEntity entity = new MqMsgLogEntity();
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
    public MqMsgLogVO update(MqMsgLogForm form) {
        MqMsgLogEntity entity = super.getById(form.getId());
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
        MqMsgLogEntity entity = super.getById(id);
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
