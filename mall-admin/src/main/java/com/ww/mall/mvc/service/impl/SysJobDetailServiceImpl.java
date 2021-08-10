package com.ww.mall.mvc.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ww.mall.mvc.dao.SysJobDetailDao;
import com.ww.mall.mvc.entity.SysJobDetailEntity;
import com.ww.mall.mvc.entity.dto.SysJobDetailDTO;
import com.ww.mall.mvc.service.SysJobDetailService;
import com.ww.mall.mvc.view.query.admin.SysJobDetailQuery;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 任务详情
 *
 * @author ww
 * @date 2021-05-14 09:30:37
 */
@Service("sysJobDetailService")
public class SysJobDetailServiceImpl extends ServiceImpl<SysJobDetailDao, SysJobDetailEntity> implements SysJobDetailService {


    @Override
    public SysJobDetailEntity getByMd5(String md5) {
        return getOne(new QueryWrapper<SysJobDetailEntity>()
                .eq("`md5`", md5));
    }

    @Override
    public List<SysJobDetailDTO> listExecutionJob() {
        SysJobDetailQuery query = new SysJobDetailQuery();
        query.setEnabled(true);
        return getBaseMapper().selectJobDetailDTO(query);
    }

    @Override
    public List<SysJobDetailDTO> listStopJob() {
        SysJobDetailQuery query = new SysJobDetailQuery();
        query.setEnabled(false);
        return getBaseMapper().selectJobDetailDTO(query);
    }
}
