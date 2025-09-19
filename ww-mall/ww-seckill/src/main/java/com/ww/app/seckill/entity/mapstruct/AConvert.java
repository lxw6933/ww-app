package com.ww.app.seckill.entity.mapstruct;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * @author ww
 * @create 2025-09-19 13:35
 * @description:
 */
@Mapper
public interface AConvert {

    AConvert INSTANCE = Mappers.getMapper(AConvert.class);

    B toB(A a);

    static void main(String[] args) {
        A a = new A("jack", 23, false);
        B b = AConvert.INSTANCE.toB(a);
        System.out.println(b);
    }

}
