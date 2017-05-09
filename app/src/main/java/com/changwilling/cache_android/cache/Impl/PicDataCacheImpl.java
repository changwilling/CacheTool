package com.changwilling.cache_android.cache.Impl;

import android.content.Context;
import android.graphics.Bitmap;
import android.text.TextUtils;
import android.util.LruCache;
import android.widget.ImageView;

import com.changwilling.cache_android.cache.IPicDataCache;
import com.changwilling.cache_android.service.IOkhttpService;
import com.changwilling.cache_android.service.Impl.OkhttpServiceImpl;
import com.changwilling.cache_android.util.DiskLruCache;
import com.changwilling.cache_android.util.DiskLruCacheUtil;
import com.changwilling.cache_android.util.ImageUtil;
import com.changwilling.cache_android.util.ThreadUtils;

import java.io.File;
import java.io.InputStream;
import java.util.HashMap;

import rx.Observable;
import rx.Subscriber;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * Created by changwilling on 17/2/16.
 * 图片缓存方案实现类，实现方法为3级缓存
 * 首先判断本地有没有该图片文件，如果没有，返回null，让用户去申请
 * 如果有，则获取
 * 因此提供的方法需要包括：1，缓存路径下是否存在文件的方法 ； 2.从本地或者网络获取数据的方法
 */

public class PicDataCacheImpl implements IPicDataCache {
    private final String TAG=PicDataCacheImpl.class.getSimpleName();
    private static volatile PicDataCacheImpl instance;
    /**
     * 硬盘缓存核心管理类
     */
    private DiskLruCache mDiskLruCache;
    /**
     * 硬盘缓存管理工具
     */
    private DiskLruCacheUtil mDiskLruCacheUtil;
    /**内存缓存管理工具,第一个参数对应url，第二个参数对应jsonStr*/
    private LruCache<String, Bitmap> mLruCache;
    /**
     * 缓存该文件类型的文件名
     */
    private final String fileName="PicFile";

    private final static HashMap<String,ImageView> img_map=new HashMap<>();

    private Context context;
    private PicDataCacheImpl(Context context){
        //获得内存的最大可用值
        int maxMemory=(int) Runtime.getRuntime().maxMemory();
        //设置图片内存缓存数据的最大为最大值的1/8
        int cacheSize = maxMemory / 8;
        mLruCache=new LruCache<String, Bitmap>(cacheSize){
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                return bitmap.getByteCount();//与cacheSize的大小一致
            }
        };
        mDiskLruCacheUtil = new DiskLruCacheUtil(context);
        mDiskLruCache = mDiskLruCacheUtil.doOpen(fileName);//Video相关数据缓存放到jsonStr目录下
        this.context=context;
    }
    public static PicDataCacheImpl getInstance(Context context){
        if (instance==null){
            synchronized (PicDataCacheImpl.class){
                if (instance==null){
                    instance=new PicDataCacheImpl(context);
                }
            }
        }
        return instance;
    }

    /**
     * 根据图片数据的id获取图片数据的方法
     * 这个方法也要在工作线程中执行
     * @return 返回null表示不存在该文件
     */
    public String getFilePathByUrl(String fileUrl){
        //这里其实就是做了一个加密处理url获取文件名的过程，然后查找看是否存在文件
        String fileName = mDiskLruCacheUtil.doGetPicFileName(fileUrl, mDiskLruCache);
        if (!TextUtils.isEmpty(fileName)){//文件名不为空，说明，存在该文件
            File diskCacheDir = mDiskLruCacheUtil.getDiskCacheDir(context, this.fileName);
            File fileCacheDir=new File(diskCacheDir,fileName);//获得文件路径
            File[] files = diskCacheDir.listFiles();
            for (File f:files) {
                String absolutePath = f.getAbsolutePath();
                if (absolutePath.contains(fileCacheDir.getAbsolutePath())){
                    return fileCacheDir.getAbsolutePath();
                }
            }
        }
        return null;
    }

    /**
     *  当然应该区别显示两种方法（一种加载全图，一种加载缩略图）
     *  @param picUrl 获取图片的url，后缀加上了文件的id，具有唯一性
     *  @return
     */
    public Observable<Bitmap> loadBitmap(String picUrl, ImageView imageView, boolean isThumbnail){
        synchronized (img_map){
            if (imageView==null){
                return null;
            }
            img_map.put(picUrl,imageView);//放进去
        }
        //1.从内存缓存中获取
        Bitmap bitmap = mLruCache.get(picUrl);
        if (bitmap!=null){
            return Observable.create(new Observable.OnSubscribe<Bitmap>() {
                @Override
                public void call(Subscriber<? super Bitmap> subscriber) {
                    setBitmap(bitmap,picUrl,isThumbnail);
                    subscriber.onNext(bitmap);
                    subscriber.onCompleted();
                }
            });
        }

        //2.从磁盘缓存中取
        Bitmap bitmapDisk = mDiskLruCacheUtil.doGetPic(picUrl, mDiskLruCache);
        if (bitmapDisk!=null){//有数据
            //放到内存缓存中
            mLruCache.put(picUrl,bitmapDisk);
            //返回
            return Observable.create(new Observable.OnSubscribe<Bitmap>() {
                @Override
                public void call(Subscriber<? super Bitmap> subscriber) {
                    //做内存缓存
                    mLruCache.put(picUrl,bitmapDisk);
                    setBitmap(bitmapDisk,picUrl,isThumbnail);
                    subscriber.onNext(bitmapDisk);
                    subscriber.onCompleted();
                }
            });
        }

        //3.从网络中取
        IOkhttpService okhttpService= OkhttpServiceImpl.getInstance();
        Observable<InputStream> observable = okhttpService.downloadFileByPost(picUrl).subscribeOn(Schedulers.io());
        return observable.observeOn(Schedulers.io()).map(new Func1<InputStream, Bitmap>() {
            @Override
            public Bitmap call(InputStream inputStream) {
                if (inputStream!=null){
                    //没有该路径的话，就需要重新创建一个
                    File cachePath = mDiskLruCacheUtil.getDiskCacheDir(context, fileName);
                    if (!cachePath.exists()) {
                        cachePath.mkdirs();
                    }
                    //这里从网络获取后，需要做硬盘缓存和内存缓存，然后再读取
                    mDiskLruCacheUtil.doEditFile(inputStream,picUrl,mDiskLruCache);
                    //还得重新磁盘缓存中找
                    Bitmap bitmapNew = mDiskLruCacheUtil.doGetPic(picUrl, mDiskLruCache);
                    //做内存缓存
                    mLruCache.put(picUrl,bitmapNew);
                    //给对应的imageView设置图片
                    setBitmap(bitmapNew, picUrl,isThumbnail);
                    return bitmapNew;
                }
                return null;
            }
        });

    }

    /**
     * 设置bitmap图片数据的方法
     * @param bitmapNew 重新获取的bitmap数据
     * @param picUrl bitmap对应的url
     */
    private void setBitmap(Bitmap bitmapNew, String picUrl, boolean isThumbnail) {
        //判断是否是缩略图
        if (isThumbnail){//将bitmapNew进行压缩处理，获得占用内从更小的图片
            ImageUtil imageUtil=new ImageUtil();
            //with 和height 都定义一个较小的值
            Bitmap bitmapThumbnail = imageUtil.ratio(bitmapNew, 120, 120);
            setBitmapOnUIThread(bitmapThumbnail,picUrl);
            return;
        }
        setBitmapOnUIThread(bitmapNew, picUrl);
    }

    private void setBitmapOnUIThread(final Bitmap bitmapNew, final String picUrl) {
        ThreadUtils.runInUIThread(new Runnable() {
            @Override
            public void run() {
                ImageView ivNow=img_map.get(picUrl);
                ivNow.setImageBitmap(bitmapNew);
                //完成后要将url从map中取出
                img_map.remove(picUrl);
            }
        });
    }

}
