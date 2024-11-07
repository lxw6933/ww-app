package com.ww.mall.admin.component;

import cn.hutool.extra.servlet.ServletUtil;
import cn.hutool.http.Header;
import com.mzt.logapi.beans.LogRecord;
import com.mzt.logapi.service.ILogRecordService;
import com.ww.mall.admin.service.OperateLogService;
import com.ww.mall.admin.view.dto.OperateLogDTO;
import com.ww.mall.common.enums.UserType;
import com.ww.mall.common.thread.ThreadMdcUtil;
import com.ww.mall.utils.AuthorizationContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author ww
 * @create 2024-09-19- 11:30
 * @description:
 */
@Slf4j
public class MongoOperateLogComponent implements ILogRecordService {

    @Resource
    private OperateLogService operateLogService;

    @Override
    public void record(LogRecord logRecord) {
        OperateLogDTO reqDTO = new OperateLogDTO();
        try {
            // traceId
            reqDTO.setTraceId(ThreadMdcUtil.getTraceId());
            // 用户信息
            reqDTO.setUserId(AuthorizationContext.getAdminUser().getId());
            reqDTO.setUserType(UserType.ADMIN);
            // 模块信息
            reqDTO.setType(logRecord.getType());
            reqDTO.setSubType(logRecord.getSubType());
            reqDTO.setBizId(logRecord.getBizNo());
            reqDTO.setAction(logRecord.getAction());
            reqDTO.setExtra(logRecord.getExtra());
            reqDTO.setCreateTime(logRecord.getCreateTime());
            // 请求信息
            RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
            if ((requestAttributes instanceof ServletRequestAttributes)) {
                HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
                reqDTO.setRequestMethod(request.getMethod());
                reqDTO.setRequestUrl(request.getRequestURI());
                reqDTO.setUserIp(ServletUtil.getClientIP(request));
                reqDTO.setUserAgent(request.getHeader(Header.USER_AGENT.toString()));
            }
            operateLogService.save(reqDTO);
        } catch (Exception e) {
            log.error("logRecord:[{}]出现异常", logRecord, e);
        }
    }

    @Override
    public List<LogRecord> queryLog(String bizNo, String type) {
        throw new UnsupportedOperationException("使用 OperateLogService 进行操作日志的查询");
    }

    @Override
    public List<LogRecord> queryLogByBizNo(String bizNo, String type, String subType) {
        throw new UnsupportedOperationException("使用 OperateLogService 进行操作日志的查询");
    }

}
