package com.changwilling.cache_android.util;

import android.content.Context;
import android.content.SharedPreferences;

public class SpTools {
	private Context context;
	private String cacheFileName;
	/**
	 * 需要传入缓存的文件名-->与你缓存的内容具有相关性
	 * 因为不同的用户，使用的偏好设置的不同，因此必须考虑sp文件的对用户的私有化
	 * 对于需要私有化的，在自己设置文件名的时候设置，因为有的需要一样的文件名（比如不同用户使用同一usercache）
	 * @param cacheFileName
     */
	public SpTools(Context context, String cacheFileName){
		this.context=context;
		this.cacheFileName=cacheFileName;
	}
	public  void putString(String key, String value){
		SharedPreferences sp = context.getSharedPreferences(cacheFileName, Context.MODE_PRIVATE);
		sp.edit().putString(key, value).commit();
	}
	
	public String getString(String key, String defValue){
		SharedPreferences sp = context.getSharedPreferences(cacheFileName, Context.MODE_PRIVATE);
		return sp.getString(key, defValue);
	}

	public  void putInt(String key, int value){
		SharedPreferences sp = context.getSharedPreferences(cacheFileName, Context.MODE_PRIVATE);
		sp.edit().putInt(key, value).commit();
	}

	public  int getInt(String key, int defValue){
		SharedPreferences sp = context.getSharedPreferences(cacheFileName, Context.MODE_PRIVATE);
		return sp.getInt(key, defValue);
	}
	
	public  void putBoolean(String key, Boolean value){
		SharedPreferences sp = context.getSharedPreferences(cacheFileName, Context.MODE_PRIVATE);
		sp.edit().putBoolean(key, value).commit();
	}
	
	public  boolean getBoolean(String key, boolean defValue){
		SharedPreferences sp = context.getSharedPreferences(cacheFileName, Context.MODE_PRIVATE);
		return sp.getBoolean(key, defValue);
	}
	
	public void putLong(String key, Long value){
		SharedPreferences sp = context.getSharedPreferences(cacheFileName, Context.MODE_PRIVATE);
		sp.edit().putLong(key, value).commit();
	}
	
	public long getLong(String key, Long defValue ){
		SharedPreferences sp=context.getSharedPreferences(cacheFileName, Context.MODE_PRIVATE);
		return sp.getLong(key, defValue);
	}
	
}
