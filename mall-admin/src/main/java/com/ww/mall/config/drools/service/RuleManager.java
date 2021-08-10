package com.ww.mall.config.drools.service;

import com.ww.mall.config.drools.DroolsRuleUtils;
import com.ww.mall.config.drools.entity.Calculation;
import com.ww.mall.mvc.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.kie.api.KieBase;
import org.kie.api.runtime.KieSession;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @description:
 * @author: ww
 * @create: 2021/5/22 下午4:29
 **/
@Slf4j
@Component
public class RuleManager {

    @Resource
    private KieBase kieBase;

    /**
     * hello world drools
     */
    public void rule() {
        KieSession kieSession = kieBase.newKieSession();
        kieSession.fireAllRules();
        kieSession.dispose();
    }

    public Calculation calculation(Calculation calculation) {
        KieSession kieSession = kieBase.newKieSession();
        kieSession.insert(calculation);
        kieSession.fireAllRules();
        kieSession.dispose();
        return calculation;
    }

    public List<String> userByDrlString(User user, String drl) throws Exception {
        List<String> list = new ArrayList<>();
        KieSession kieSession = DroolsRuleUtils.getKieSessionByDrlString(drl);
        kieSession.setGlobal("listRules",list);
        kieSession.insert(user);
        kieSession.getAgenda().getAgendaGroup("sign").setFocus();
        kieSession.fireAllRules();
        for (String s : list) {
            System.out.println(s);
        }
        kieSession.dispose();
        return list;
    }

}
