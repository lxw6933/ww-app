package com.ww.mall.redis.aspect;

import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

public abstract class MallAbstractAspect {

    protected final SpelExpressionParser parser = new SpelExpressionParser();

    public static class MyStandardEvaluationContext extends StandardEvaluationContext {
        public MyStandardEvaluationContext(String[] parameterNames, Object[] parameterValues) {
            if (parameterNames == null) {
                return;
            }
            for (int i = 0; i < parameterNames.length; i++) {
                String paramName = parameterNames[i];
                Object paramValue = parameterValues[i];
                setVariable(paramName, paramValue);
            }
        }
    }

}
