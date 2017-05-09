package com.changwilling.cache_android.cache;

import java.util.Map;

import rx.Observable;

/**
 * Created by changwilling on 17/2/10.
 */

public interface IJsonDataCache {
    Observable<String> loadJsonStr(String jsonUrl, Map<String, String> params, String expireKey);
    void fluchCache();
    long getCacheSize();
    void stopCache();
    void deleteCacheByKey(String key);
}
