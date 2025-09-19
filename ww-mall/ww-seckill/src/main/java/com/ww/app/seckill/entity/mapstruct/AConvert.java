package com.ww.app.seckill.entity.mapstruct;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.Arrays;
import java.util.List;

/**
 * @author ww
 * @create 2025-09-19 13:35
 * @description:
 */
@Mapper
public interface AConvert {

    AConvert INSTANCE = Mappers.getMapper(AConvert.class);

    B toB(A a);
    List<B> toBList(List<A> a);

    static void main(String[] args) {
        A a1 = new A("jack1", 23, false);
        A a2 = new A("jack2", 24, true);
        A a3 = new A("jack3", 25, false);
        A a4 = new A("jack4", 26, true);
        List<A> aList = Arrays.asList(a1, a2, a3, a4);
//        B b = AConvert.INSTANCE.toB(a1);
//        System.out.println(b);
        AConvert.INSTANCE.toBList(aList).forEach(System.out::println);
        long s = System.currentTimeMillis();
        for (int i = 0; i < 10000000; i++) {
            AConvert.INSTANCE.toB(a1);   // cost: 6
//            BeanUtil.toBean(a1, B.class);  // cost: 44107
        }
        long e = System.currentTimeMillis();
        System.out.println(e - s);

    }

}
