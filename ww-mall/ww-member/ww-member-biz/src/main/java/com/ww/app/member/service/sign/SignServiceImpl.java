package com.ww.app.member.service.sign;

import com.ww.app.common.common.ClientUser;
import com.ww.app.common.exception.ApiException;
import com.ww.app.member.component.SignComponent;
import com.ww.app.member.strategy.sign.SignStrategy;
import com.ww.app.member.util.SignDateValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static com.ww.app.member.member.enums.ErrorCodeConstants.NOT_RESIGN_FUTURE_DATE;
import static com.ww.app.member.member.enums.ErrorCodeConstants.RESIGN_DATE_NOT_PERIOD;

/**
 * @author ww
 * @create 2023-07-21- 09:17
 * @description: 签到业务实现类
 */
@Slf4j
@Service
public class SignServiceImpl implements SignService {

    @Resource
    private SignComponent signComponent;

    private SignStrategy defaultStrategy;

    @PostConstruct
    public void init() {
        defaultStrategy = signComponent.getDefaultStrategy();
    }

    @Override
    public int doSign(LocalDate date, ClientUser clientUser) {
        // 获取当天日期字符串
        LocalDate today = LocalDate.now();
        // 判断是签到还是补签
        if (date == null || date.isEqual(today)) {
            // 使用策略模式进行签到
            return defaultStrategy.doSign(today, clientUser);
        } else {
            // 如果是未来日期，抛出异常
            if (date.isAfter(today)) {
                throw new ApiException(NOT_RESIGN_FUTURE_DATE);
            }
            // 获取签到策略并基于周期进行补签校验（仅允许当前周期内的过去日期）
            if (!SignDateValidator.isValidResignDate(date, defaultStrategy.getType())) {
                throw new ApiException(RESIGN_DATE_NOT_PERIOD);
            }
            // 返回连续签到天数
            return defaultStrategy.doSign(date, clientUser);
        }
    }

    @Override
    public int getContinuousSignCount(LocalDate date, ClientUser clientUser) {
        return defaultStrategy.getContinuousSignCount(date, clientUser);
    }

    @Override
    public int getSignCount(LocalDate date, ClientUser clientUser) {
        return defaultStrategy.getSignCount(date, clientUser);
    }

    @Override
    @Deprecated
    public Map<LocalDate, Boolean> getSignInfo(LocalDate date, ClientUser clientUser) {
        return defaultStrategy.getSignInfo(date, clientUser);
    }

    @Override
    public List<Boolean> getSignDetailInfo(ClientUser clientUser) {
        return defaultStrategy.getSignDetailInfo(clientUser);
    }

}
