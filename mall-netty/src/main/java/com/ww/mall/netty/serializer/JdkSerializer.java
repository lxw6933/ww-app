package com.ww.mall.netty.serializer;

import com.ww.mall.common.enums.GlobalResCodeConstants;
import com.ww.mall.common.exception.ApiException;
import com.ww.mall.netty.enums.SerializerTypeEnum;
import org.springframework.stereotype.Component;

import java.io.*;

/**
 * @author ww
 * @create 2024-05-06 22:34
 * @description:
 */
@Component
public class JdkSerializer implements MallSerializer {

    @Override
    public SerializerTypeEnum type() {
        return SerializerTypeEnum.JAVA;
    }

    @Override
    public <T> byte[] serialize(T object) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(object);
            return bos.toByteArray();
        } catch (Exception e) {
            throw new ApiException(GlobalResCodeConstants.SYSTEM_ERROR);
        }
    }

    @Override
    public <T> T deserialize(Class<T> clazz, byte[] bytes) {
        try {
            ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes));
            return (T) ois.readObject();
        } catch (Exception e) {
            throw new ApiException(GlobalResCodeConstants.SYSTEM_ERROR);
        }
    }
}
