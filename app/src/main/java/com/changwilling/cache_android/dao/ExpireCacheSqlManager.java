package com.changwilling.cache_android.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.text.TextUtils;

import com.changwilling.cache_android.entity.ExpireCacheEntity;
import com.changwilling.cache_android.util.LogUtil;

/**
 * Created by changwilling on 17/1/9.
 */

public class ExpireCacheSqlManager extends AbsSqlManager {
    private static final String TAG=ExpireCacheSqlManager.class.getSimpleName();
    private static ExpireCacheSqlManager instance;

    private static ExpireCacheSqlManager getInstance() {
        if (instance == null) {
            synchronized (ExpireCacheSqlManager.class) {
                if (instance == null) {
                    instance = new ExpireCacheSqlManager();
                }
            }
        }
        return instance;
    }

    /**
     * 插入或者更新一条缓存数据的方法
     *
     * @return
     */
    public synchronized static long insertExpireCache(ExpireCacheEntity entity) {
        if (entity == null || TextUtils.isEmpty(entity.getExpireCacheKeyId())) {
            return -1;
        }
        try {
            ContentValues values = new ContentValues();
            values.put(ExpireCacheColum.EXPIRECACHEKEY_ID, entity.getExpireCacheKeyId());
            values.put(ExpireCacheColum.EXPIRE_ENDTIME, entity.getExpireEndTime());
            if (!isHasExpireCache(entity.getExpireCacheKeyId())) {//没有该项数据，插入操作
                long insert = getInstance().sqliteDB().insert(DatabaseHelper.TABLES_NAME_EXPIRECACHE, null, values);
                LogUtil.i(TAG,"insert:"+insert);
                return insert;
            } else {//更新操纵
                int update = getInstance().sqliteDB().update(DatabaseHelper.TABLES_NAME_EXPIRECACHE, values,
                        ExpireCacheColum.EXPIRECACHEKEY_ID + " = '" + entity.getExpireCacheKeyId() + "'", null);
                LogUtil.i(TAG,"update:"+update);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    /**
     * 根据id，判断是否已经有了该项数据
     *
     * @param expireCacheKeyId
     * @return
     */
    private static boolean isHasExpireCache(String expireCacheKeyId) {
        String sql = "select " + ExpireCacheColum.EXPIRECACHEKEY_ID + " from " + DatabaseHelper.TABLES_NAME_EXPIRECACHE
                + " where " + ExpireCacheColum.EXPIRECACHEKEY_ID + " = '" + expireCacheKeyId + "'";
        Cursor cursor = getInstance().sqliteDB().rawQuery(sql, null);
        LogUtil.i(TAG,"isHasExpireCache  cursor:"+cursor.getCount());
        if (cursor != null && cursor.getCount() > 0) {
            cursor.close();
            return true;
        }
        return false;
    }

    /**
     * 根据cacheKeyId获取缓存实体类数据的方法，cacheKeyId是唯一的
     * @param expireCacheKeyId
     * @return
     */
    public static ExpireCacheEntity getExpireCacheByCacheKey(String expireCacheKeyId){
        Cursor cursor = getInstance().sqliteDB().query(DatabaseHelper.TABLES_NAME_EXPIRECACHE,
                new String[]{
                        ExpireCacheColum.EXPIRECACHEKEY_ID,
                        ExpireCacheColum.EXPIRE_ENDTIME
                }, ExpireCacheColum.EXPIRECACHEKEY_ID + " = ?",
                new String[]{expireCacheKeyId}, null, null, null);
        if(cursor!=null&&cursor.getCount()>0){
            ExpireCacheEntity entity=new ExpireCacheEntity();
            boolean hasNext=cursor.moveToNext();
            if(hasNext){
                String expireCacheId=cursor.getString(cursor.getColumnIndex(ExpireCacheColum.EXPIRECACHEKEY_ID));
                long expireEndTime = cursor.getLong(cursor.getColumnIndex(ExpireCacheColum.EXPIRE_ENDTIME));
                entity.setExpireCacheKeyId(expireCacheKeyId);
                entity.setExpireEndTime(expireEndTime);
                cursor.close();
                return entity;
            }
        }
        return null;
    }

    /**
     * 重置
     */
    public static void reset(){
        getInstance().release();
    }

    @Override
    protected void release() {
        super.release();
        instance=null;
    }
}