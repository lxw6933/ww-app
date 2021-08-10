package com.ww.mall.mvc.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ww.mall.mvc.dao.SysJobDetailLogDao;
import com.ww.mall.mvc.entity.SysJobDetailLogEntity;
import com.ww.mall.mvc.service.SysJobDetailLogService;
import org.springframework.stereotype.Service;

/**
 * 任务记录日志
 *
 * @author ww
 * @date 2021-05-14 09:30:37
 */
@Service("sysJobDetailLogService")
public class SysJobDetailLogServiceImpl extends ServiceImpl<SysJobDetailLogDao, SysJobDetailLogEntity> implements SysJobDetailLogService {

}
