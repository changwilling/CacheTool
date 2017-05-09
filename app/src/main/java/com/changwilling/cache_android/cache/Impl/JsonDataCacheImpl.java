package com.changwilling.cache_android.cache.Impl;

import android.content.Context;
import android.text.TextUtils;
import android.util.LruCache;

import com.changwilling.cache_android.cache.IExpireCacheManager;
import com.changwilling.cache_android.cache.IJsonDataCache;
import com.changwilling.cache_android.service.IOkhttpService;
import com.changwilling.cache_android.service.Impl.OkhttpServiceImpl;
import com.changwilling.cache_android.util.DiskLruCache;
import com.changwilling.cache_android.util.DiskLruCacheUtil;
import com.changwilling.cache_android.util.LogUtil;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * json数据的保存及缓存引擎，这个类在全局初始化一次就可以了
 * 如果以后需要加入缓存其他类型的数据：如String、文件等，其实性质一样，可以写一个base类，然后其他进行扩展
 * 当然，打开一个LruCache和DiscLrucache对象就可以了，同时任务
 * @author weilin
 */
public class JsonDataCacheImpl implements IJsonDataCache {
	private final String TAG=JsonDataCacheImpl.class.getSimpleName();
	/**单例化管理*/
	private static volatile JsonDataCacheImpl instance;
	private static Context context;
	public static JsonDataCacheImpl getInstance(Context context){
		if(instance==null){
			synchronized (JsonDataCacheImpl.class) {
				if(instance==null){
					JsonDataCacheImpl.context =context;
					instance=new JsonDataCacheImpl(context);
				}
			}
		}
		return instance;
	}
	/**硬盘缓存核心管理类*/
	private DiskLruCache mDiskLruCache;
	/**硬盘缓存管理工具*/
	private DiskLruCacheUtil mDiskLruCacheUtil;
	/**内存缓存管理工具,第一个参数对应url，第二个参数对应jsonStr*/
	private LruCache<String, String> mLruCache;
	/**json网络请求管理任务集合*/
	private Set<Observable<String>> taskCollection;
	private final String fileName="jsonStr";
	
	private JsonDataCacheImpl(Context context){//创建的时候，全局执行一次
		taskCollection = new HashSet<>();
		//获得内存的最大可用值
		int maxMemory=(int) Runtime.getRuntime().maxMemory();
		//设置Json缓存数据的最大为最大值的1/8
        int cacheSize = maxMemory / 8;
        mLruCache=new LruCache<String, String>(cacheSize){
        	@Override
        	protected int sizeOf(String key, String value) {
        		return value.getBytes().length;//与cacheSize的大小一致
        	}
        };
        mDiskLruCacheUtil=new DiskLruCacheUtil(context);
        mDiskLruCache=mDiskLruCacheUtil.doOpen(fileName);//json相关数据缓存放到jsonStr目录下
	}
	
	/**
	 * 加载jsonStr的方法，当然是post方法
	 * 加入了缓存有效期的判定方法
	 * 有一定的问题，因为url可能不具备唯一性，因此保存缓存的key需要加上id参数?
	 * expireKey:有效期判定的key
	 */
	public Observable<String> loadJsonStr(String jsonUrl, Map<String,String> params, String expireKey){
		String jsonStr=null;
		IExpireCacheManager expireManager = ExpireCacheManagerImpl.getInstance();
		//必须获得json的树状结构的数据，否则，无法适配
		boolean isExpire = expireManager.isExpire(expireKey);
		if (!isExpire){//没有过期，才会走前两步
			//1.如果内存缓存中有数据，则从内存中取
			jsonStr = mLruCache.get(jsonUrl);
			if (jsonStr!=null){
				String finalJsonStr = jsonStr;
				return Observable.create(new Observable.OnSubscribe<String>() {
					@Override
					public void call(Subscriber<? super String> subscriber) {
						subscriber.onNext(finalJsonStr);
						subscriber.onCompleted();
					}
				});
			}

			//从文件缓存中取数据
			jsonStr= mDiskLruCacheUtil.doGetJsonStr(jsonUrl, mDiskLruCache);
			if(!TextUtils.isEmpty(jsonStr)){
				if(mLruCache.get(jsonUrl)==null){//将数据放到内存缓存中
					mLruCache.put(jsonUrl, jsonStr);
				}
				String finalJsonStr1 = jsonStr;
				return Observable.create(new Observable.OnSubscribe<String>() {
					@Override
					public void call(Subscriber<? super String> subscriber) {
						subscriber.onNext(finalJsonStr1);
						subscriber.onCompleted();
					}
				});
			}
		}




		//post方法从网络获取json数据
		IOkhttpService service= OkhttpServiceImpl.getInstance();
		//启动任务，需要将任务添加到队列中
		Observable<String> observable = service.asynPostByURLAndMap(jsonUrl, params).subscribeOn(Schedulers.io());
		taskCollection.add(observable);

		return observable.observeOn(Schedulers.io()).map(new Func1<String, String>() {
			@Override
			public String call(String s) {
				File cachePath = mDiskLruCacheUtil.getDiskCacheDir(context, fileName);
				if (!cachePath.exists()) {
					cachePath.mkdirs();
				}
				//这里从网络获取后，需要做硬盘缓存和内存缓存，然后再读取
				mDiskLruCacheUtil.doEditJsonStr(jsonUrl,s, mDiskLruCache);
				//doGet不就是从硬盘缓存中取么？
				String jsonStr1 = mDiskLruCacheUtil.doGetJsonStr(jsonUrl, mDiskLruCache);
				LogUtil.i(TAG,"jsonStr1"+jsonStr1);
				if(jsonStr1!=null&&jsonUrl!=null&&mLruCache.get(jsonStr1)==null){//内存缓存中没有了，则放到内存缓存中
					mLruCache.put(jsonUrl, jsonStr1);
				}
				taskCollection.remove(observable);//从任务集合中移除这个任务
				return jsonStr1;
			}
		});


	}

	/** 将缓存记录同步到journal文件中
	 *  这句调用非常重要，否则缓存到磁盘中的文件，在journal日志文件中都显示dirty，后面没有跟随相应的clean
	 *  可以在activty的destroy方法中调用，因为频繁的调用也并不十分的好（写一次就需要修改journal文件）
	 *  */
	public void fluchCache() {
		if (mDiskLruCache != null) {
			try {
				mDiskLruCache.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	/** 获取缓存大小 */
	public long getCacheSize() {
		if (mDiskLruCache != null) {
			long size = mDiskLruCache.size();
			return size;
		}
		return 0;
	}


	/** 关闭 */
	public void stopCache() {
		if (mDiskLruCache != null) {
			try {
				mDiskLruCache.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**根据key删除内存缓存和磁盘缓存*/
	public void deleteCacheByKey(String key){
		try {
			if(mDiskLruCache!=null){
				mDiskLruCache.remove(key);
			}
			if(mLruCache!=null){
				mDiskLruCache.remove(key);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}
