package com.ww.mall.web.template;

/**
 * @author ww
 * @create 2024-04-11- 14:00
 * @description: T:数据类型 R:查询数据参数类型
 */
public abstract class AbstractMallCacheTemplate<T, R> {

    private static final Object lockObj = new Object();

    public final R getData(T param) {
        R data = secondCache(param);
        if (data == null) {
            data = firstCache(param);
            if (data == null) {
                synchronized (lockObj) {
                    data = this.getData(param);
                    if (data == null) {
                        data = databaseData(param);
                        if (data != null) {
                            setCache(data);
                        }
                    }
                }
            }
        }
        return data;
    }

    protected R secondCache(T param) {
        return null;
    }

    abstract protected R firstCache(T param);

    abstract protected R databaseData(T param);

    abstract protected void setCache(R data);

}
