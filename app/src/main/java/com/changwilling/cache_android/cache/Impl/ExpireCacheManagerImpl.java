package com.changwilling.cache_android.cache.Impl;


import com.changwilling.cache_android.cache.IExpireCacheManager;
import com.changwilling.cache_android.dao.ExpireCacheSqlManager;
import com.changwilling.cache_android.entity.ExpireCacheEntity;
import com.changwilling.cache_android.util.LogUtil;

/**
 * Created by changwilling on 17/1/9.
 */

public class ExpireCacheManagerImpl implements IExpireCacheManager {
    private static final String TAG=ExpireCacheManagerImpl.class.getSimpleName();
    private volatile static ExpireCacheManagerImpl instance;
    private ExpireCacheManagerImpl(){
    }
    public static ExpireCacheManagerImpl getInstance(){
        if (instance==null){
            synchronized (ExpireCacheManagerImpl.class){
                if (instance==null){
                    instance=new ExpireCacheManagerImpl();
                }
            }
        }
        return instance;
    }

    @Override
    public boolean isExpire(String key) {
        //缓存有效期的策略是将有效期模块对应的字段和有效期数值保存到数据表中，因此先从数据表中读取判断是否过期
        ExpireCacheEntity entity= ExpireCacheSqlManager.getExpireCacheByCacheKey(key);
        if(entity!=null){
            long expireEndTime=entity.getExpireEndTime();
            long chaTime=expireEndTime- System.currentTimeMillis();
            LogUtil.w(TAG,"isExpire验证 chaTime"+chaTime);
            if(expireEndTime> System.currentTimeMillis()){//没过期
                LogUtil.w(TAG,"isExpire验证 返回boolean :"+false);
                return false;
            }
        }
        LogUtil.w(TAG,"isExpire验证 返回boolean :"+true);
        return true;
    }

    @Override
    public void setExpire(String key, long expireDuringTime) {
        ExpireCacheEntity entity=new ExpireCacheEntity();
        entity.setExpireCacheKeyId(key);
        long expireEndTime=expireDuringTime+ System.currentTimeMillis();
        entity.setExpireEndTime(expireEndTime);
        ExpireCacheSqlManager.insertExpireCache(entity);//插入新的有效期或者更新旧的有效期
    }
}
