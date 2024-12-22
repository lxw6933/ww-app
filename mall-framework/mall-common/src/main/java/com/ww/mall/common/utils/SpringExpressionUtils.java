package com.ww.mall.common.utils;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import org.apache.commons.lang3.function.TriConsumer;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author ww
 * @create 2024-11-11- 13:59
 * @description:
 */
public class SpringExpressionUtils {

    /**
     * el表达式解析器
     */
    private static final SpelExpressionParser parser = new SpelExpressionParser();

    /**
     * 方法参数名发现器
     */
    private static final ParameterNameDiscoverer paramsNameDiscoverer = new DefaultParameterNameDiscoverer();

    private SpringExpressionUtils() {
    }

    /**
     * 从切面中，单个解析 el 表达式的结果
     *
     * @param joinPoint 切面点
     * @param el        el表达式
     * @return el表达式解析后的值
     */
    public static Object parseExpression(JoinPoint joinPoint, String el) {
        Map<String, Object> result = parseExpressions(joinPoint, Collections.singletonList(el), null);
        return result.get(el);
    }

    public static Object parseExpression(JoinPoint joinPoint, String el, TriConsumer<Expression, EvaluationContext, String> triConsumer) {
        Map<String, Object> result = parseExpressions(joinPoint, Collections.singletonList(el), triConsumer);
        return result.get(el);
    }

    /**
     * 批量解析EL表达式的结果
     *
     * @param joinPoint   切面点
     * @param els         el表达式数组
     * @param triConsumer 处理el变量
     * @return key: 表达式 value: 对应值
     */
    public static Map<String, Object> parseExpressions(JoinPoint joinPoint, List<String> els, TriConsumer<Expression, EvaluationContext, String> triConsumer) {
        if (CollUtil.isEmpty(els)) {
            return MapUtil.newHashMap();
        }

        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        Method method = methodSignature.getMethod();
        // 使用spring的ParameterNameDiscoverer获取方法形参名数组
        String[] paramNames = paramsNameDiscoverer.getParameterNames(method);
        // Spring 的表达式上下文对象
        EvaluationContext context = new StandardEvaluationContext();
        // 给上下文赋值
        if (ArrayUtil.isNotEmpty(paramNames)) {
            Object[] args = joinPoint.getArgs();
            for (int i = 0; i < paramNames.length; i++) {
                context.setVariable(paramNames[i], args[i]);
            }
        }
        // 逐个参数解析
        Map<String, Object> result = MapUtil.newHashMap(els.size(), true);
        els.forEach(key -> {
            Expression expression = parser.parseExpression(key);
            Object value = expression.getValue(context);
            result.put(key, value);
            if (triConsumer != null) {
                triConsumer.accept(expression, context, Optional.ofNullable(value).orElse("").toString());
            }
        });
        return result;
    }

    /**
     * 解析el表达式的结果 [from beanFactory]
     *
     * @param el el 表达式
     * @return el表达式解析后的值
     */
    public static Object parseExpression(String el) {
        if (StrUtil.isBlank(el)) {
            return null;
        }
        Expression expression = parser.parseExpression(el);
        StandardEvaluationContext context = new StandardEvaluationContext();
        context.setBeanResolver(new BeanFactoryResolver(SpringUtil.getApplicationContext()));
        return expression.getValue(context);
    }

}
