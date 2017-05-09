package com.changwilling.cache_android.cache;

/**
 * Created by changwilling on 17/1/9.
 * 数据有效期管理接口
 */

public interface IExpireCacheManager {

    long TIME_FOR_EXPIRE_TEST=30*1000;//有效期是30秒
    String KEY_FOR_EXPIRE_TEST="KEY_FOR_EXPIRE_TEST";

    /**
     * 根据key判定是否过期的方法
     * @param key
     * @return
     */
    boolean isExpire(String key);

    /**
     * 设置缓存，一般在网络获取数据成功之后调用
     * @param key
     * @param expireDuringTime 缓存有效期，如5分钟，单位为miliseconds
     */
    void setExpire(String key, long expireDuringTime);
}
