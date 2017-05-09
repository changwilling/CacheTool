package com.changwilling.cache_android.util;
/**
 * create date：2016/10/31 on ${Time}
 * description：
 * author:chang weilin
 */

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.text.TextUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * create by weilin at 2016/10/31
 * 硬盘缓存的操作主要包括写入、访问、删除
 */
public class DiskLruCacheUtil {
    private final String TAG=DiskLruCacheUtil.class.getSimpleName();
    private Context context;

    public DiskLruCacheUtil(Context context) {
        this.context = context;
    }

    /**
     * 打开DiskLruCache的方法
     */
    public DiskLruCache doOpen(String filePath) {//表示保存到cache目录下的filePath文件目录下
        DiskLruCache mDiskLruCache = null;
        File cachePath = getDiskCacheDir(context, filePath);//uniqueName传值为jsonStr，表示jsonStr都存到这里，
        // 比如网易新闻缓存目录下可以看到bitmap,Object等文件
        if (!cachePath.exists()) {
            cachePath.mkdirs();
        }
        try {
            //创建一个DiskLruCache的实例
            mDiskLruCache = DiskLruCache.open(cachePath, getAppVersion(), 1, 10 * 1024 * 1024);
            //通常最大为10兆就可以了
        } catch (IOException e) {
            e.printStackTrace();
        }
        return mDiskLruCache;
    }

    /**
     * 将JsonString写入硬盘缓存的方法,写入操作借助DiscLruCache.Editor这个类来完成
     * 这里修改为直接将json保存到磁盘
     * 如果需要扩展，需要把jsonStr转为对应的流对象
     */
    public void doEditJsonStr(String jsonUrl, String jsonStr, DiskLruCache mDiskLruCache) {
        try {
            String key = hashkeyForDisk(jsonUrl);//将url加密处理，作为key
            DiskLruCache.Editor editor = mDiskLruCache.edit(key);
            if (editor!=null){
                OutputStream outputStream = editor.newOutputStream(0);//输出流，制定了文件输出的位置
                if (jsonStr!=null&&!TextUtils.isEmpty(jsonStr)){
                    ByteArrayInputStream inputStream=new ByteArrayInputStream(jsonStr.getBytes());
                    //根据输入流和输出流将jsonStr保存到磁盘中
                    boolean isSuccess = setDatasToDisk(outputStream, inputStream);
                    if (isSuccess){
                        editor.commit();
                    }else {
                        editor.abort();
                    }
                }
            }
            mDiskLruCache.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 编辑输入流加入到DisLruCache
     */
    public void doEditFile(InputStream inputStream, String bitmapUrl, DiskLruCache mDiskLruCache){
        try {
            String key = hashkeyForDisk(bitmapUrl);//将url加密处理，作为key
            DiskLruCache.Editor editor = mDiskLruCache.edit(key);
            if (editor!=null){
                OutputStream outputStream = editor.newOutputStream(0);//输出流，制定了文件输出的位置
                if (inputStream!=null){
                    boolean isSuccess=setDatasToDisk(outputStream,inputStream);
                    if (isSuccess){
                        editor.commit();
                    }else {
                        editor.abort();
                    }
                }
            }
            mDiskLruCache.flush();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * 获取jsonStr的方法
     * 从磁盘缓存中获取
     * @param mDiskLruCache
     * @return
     */
    public String doGetJsonStr(String jsonUrl, DiskLruCache mDiskLruCache) {
        try {
            String key = hashkeyForDisk(jsonUrl);//同样算法，可以找到同样的key
            DiskLruCache.Snapshot snapshot = mDiskLruCache.get(key);
            if (snapshot != null) {
                InputStream in = snapshot.getInputStream(0);
                String jsonStr=readStr(in);
                return jsonStr;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }



    public Bitmap doGetPic(String picUrl, DiskLruCache mDiskLruCache){
        try {
            String key=hashkeyForDisk(picUrl);
            DiskLruCache.Snapshot snapshot = mDiskLruCache.get(key);
            if (snapshot!=null){
                InputStream in = snapshot.getInputStream(0);
                Bitmap bitmap = BitmapFactory.decodeStream(in);
                return bitmap;
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }



    /**
     * 获得图片文件，返回文件名（一般是key）
     * 区别在于不需要获取snapshot的过程，否则带回获取的时候，还要执行一次，Lru算法重复
     * @return 文件名
     */
    public String doGetPicFileName(String picId, DiskLruCache mDiskLruCache){
        try {
            String key=hashkeyForDisk(picId);
            return key;
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 获得文件的url的过程
     * @param fileUrl
     * @param mDiskLruCache
     * @return
     */
    public String doGetFileName(String fileUrl, DiskLruCache mDiskLruCache){
        try {
            String key=hashkeyForDisk(fileUrl);
            DiskLruCache.Snapshot snapshot = mDiskLruCache.get(key);
            if (snapshot != null) {
                //说明有该文件，那么下来就可以读取了
                //返回文件路径
                return key;
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 从缓存中移除图片的方法
     */
    public void doRemove(String url, DiskLruCache mDiskLruCache) {
        try {
            String key = hashkeyForDisk(url);
            mDiskLruCache.remove(key);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 将数据存入磁盘
     ** @param outputStream
     * @return
     */
    private boolean setDatasToDisk(OutputStream outputStream, InputStream inputStream) {
        HttpURLConnection urlConnection = null;
        BufferedOutputStream out = null;
        BufferedInputStream in = null;
        try {
            //中间隔断，从源头控制OOM的出现
            in=new BufferedInputStream(inputStream,8*1024);
            out=new BufferedOutputStream(outputStream,8*1024);
            int b;
            while((b=in.read())!=-1){//还能读取下一个缓冲-->8k读取
            	out.write(b);//写入
            }
            out.flush();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            try {
                if (out != null) {
                    out.close();
                }
                if (in != null) {
                    in.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }


    /**
     * 使用md5算法对key（imageurl）加密并返回-->安全原因不能直接使用imageUrl
     *
     * @param
     * @return
     */
    private String hashkeyForDisk(String key) {
        String cacheKey;
        try {
            MessageDigest mDigest = MessageDigest.getInstance("MD5");
            mDigest.update(key.getBytes());
            cacheKey = bytesToHexString(mDigest.digest());
        } catch (NoSuchAlgorithmException e) {
            cacheKey = String.valueOf(key.hashCode());
            e.printStackTrace();
        }
        return cacheKey;
    }

    private String bytesToHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            String hex = Integer.toHexString(0xFF & bytes[i]);
            if (hex.length() == 1) {
                sb.append('0');
            }
            sb.append(hex);
        }
        return sb.toString();
    }


    /**
     * 获得App的版本号
     *
     * @return
     */
    private int getAppVersion() {
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context
                    .getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return 1;
    }

    /**
     * 获得硬盘缓存地址的方法
     * @return
     */
    public File getDiskCacheDir(Context context, String uniqueName) {
        String cachePath = null;
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()) ||
                !Environment.isExternalStorageRemovable()) {//有外存sdcard，并且状态可用没有被拔掉
            cachePath = context.getExternalCacheDir().getPath();
        } else {//外置sdcard不可用
            cachePath = context.getCacheDir().getPath();
        }
        return new File(cachePath + File.separator + uniqueName);
    }
    
    public static String readStr(InputStream in) throws IOException {
        return readStr(in, "UTF-8");
    }

    public static String readStr(InputStream in, String charset) throws IOException {
        if (TextUtils.isEmpty(charset)) charset = "UTF-8";

        if (!(in instanceof BufferedInputStream)) {
            in = new BufferedInputStream(in);
        }
        Reader reader = new InputStreamReader(in, charset);
        StringBuilder sb = new StringBuilder();
        char[] buf = new char[1024];
        int len;
        while ((len = reader.read(buf)) >= 0) {
            sb.append(buf, 0, len);
        }
        return sb.toString();
    }
}
