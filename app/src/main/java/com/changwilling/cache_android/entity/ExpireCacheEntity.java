package com.changwilling.cache_android.entity;

import java.io.Serializable;

/**
 * Created by changwilling on 17/1/9.
 */

public class ExpireCacheEntity implements Serializable {
    private String expireCacheKeyId;//缓存类型对应的唯一id
    private long expireEndTime;//缓存有效期结束时间

    public String getExpireCacheKeyId() {
        return expireCacheKeyId;
    }

    public void setExpireCacheKeyId(String expireCacheKeyId) {
        this.expireCacheKeyId = expireCacheKeyId;
    }

    public long getExpireEndTime() {
        return expireEndTime;
    }

    public void setExpireEndTime(long expireEndTime) {
        this.expireEndTime = expireEndTime;
    }

    @Override
    public String toString() {
        return "ExpireCacheEntity{" +
                "expireCacheKeyId='" + expireCacheKeyId + '\'' +
                ", expireEndTime=" + expireEndTime +
                '}';
    }
}
