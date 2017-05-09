package com.changwilling.cache_android.service.Impl;

import com.changwilling.cache_android.service.IOkhttpService;
import com.changwilling.cache_android.util.FileUtil;
import com.changwilling.cache_android.util.LogUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import rx.Observable;
import rx.Subscriber;
import rx.schedulers.Schedulers;

/**
 * Created by changwilling on 17/2/7.
 * 网络请求实现类
 */

public class OkhttpServiceImpl implements IOkhttpService {
    private final String TAG=OkhttpServiceImpl.class.getSimpleName();
    private OkHttpClient client;
    private volatile static OkhttpServiceImpl manager;

    private OkhttpServiceImpl(){
        client=new OkHttpClient();
    }

    /**
     * 单例模式获取对象
     *
     * */
    public static OkhttpServiceImpl getInstance(){
        if(manager==null){
            synchronized (OkhttpServiceImpl.class){
                if (manager==null){
                    manager=new OkhttpServiceImpl();
                }
            }
        }
        return manager;
    }

    /**
     * 异步的get请求，通过RxJava编程的方式, 返回结果为Json
     * @param url
     */
    public Observable<String> asynGetByURL(String url){
        return Observable.create(new Observable.OnSubscribe<String>() {
            @Override
            public void call(Subscriber<? super String> subscriber) {//IO线程，请求url，使用okhttp
                if (subscriber.isUnsubscribed()){//被取消订阅关系
                    return;
                }
                Request request=new Request.Builder().url(url).build();
                client.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        subscriber.onError(e);
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {//成功，返回的时jsonString
                        if (response.isSuccessful()){
                            String jsonStr = response.body().string();
                            subscriber.onNext(jsonStr);
                        }
                        //无论返回的响应是否成功，都需要执行onComplete
                        subscriber.onCompleted();//调用了，才会调用
                    }
                });

            }
        }).subscribeOn(Schedulers.io());//表示订阅在io线程
    }

    /**
     * 通过post方法请求，返回json string类型数据
     * @param url 请求服务器的url
     * @param params map形式的参数
     * @return
     */
    @Override
    public Observable<String> asynPostByURLAndMap(String url, Map<String, String> params) {
        return Observable.create(new Observable.OnSubscribe<String>() {

            @Override
            public void call(Subscriber<? super String> subscriber) {
                if (subscriber.isUnsubscribed()){
                    return;
                }
                FormBody.Builder form_builder=new FormBody.Builder();//表单对象，包含以input开始的对象，以html表单为主
                if (params!=null&&!params.isEmpty()){
                    for (Map.Entry<String,String> entry:params.entrySet()){
                        form_builder.add(entry.getKey(),entry.getValue());
                    }
                }
                RequestBody request_body=form_builder.build();
                Request request=new Request.Builder().url(url).post(request_body).build();//采用post方法
                client.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        subscriber.onError(e);
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        if (response!=null&&response.isSuccessful()){
                            String jsonStr = response.body().string();
                            subscriber.onNext(jsonStr);
                        }
                        subscriber.onCompleted();
                    }
                });
            }
        });
    }

    /**
     * 下载文件到本地磁盘的方法,根据url使用get方法
     * @param fileUrl 文件的url
     * @return 返回imputStream输入流
     */
    public Observable<InputStream> downloadFileByPost(String fileUrl){
        return Observable.create(new Observable.OnSubscribe<InputStream>() {
            @Override
            public void call(Subscriber<? super InputStream> subscriber) {
                if (subscriber.isUnsubscribed()){
                    return;
                }
                Request request=new Request.Builder().url(fileUrl).build();
                client.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        subscriber.onError(e);
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        //返回成功
                        try {
                            if (response!=null){
                                InputStream inputStream = response.body().byteStream();
                                if (inputStream!=null){
                                    subscriber.onNext(inputStream);
                                }
                            }
                            //没有运行时错误，则也要onComplete,但是不一定执行onNext,因此关键业务在onNext中执行
                            subscriber.onCompleted();
                        }catch (Exception e){
                            subscriber.onError(e);
                            e.printStackTrace();
                        }
                    }
                });
            }
        });
    }

    /**
     * 下载文件到本地指定路径
     * @return 下载成功与否
     */
    public Observable<Boolean> downLoadFileToDestFile(String fileUrl, String destFileDir){
        return Observable.create(new Observable.OnSubscribe<Boolean>() {
            @Override
            public void call(Subscriber<? super Boolean> subscriber) {
                if (subscriber.isUnsubscribed()){
                    return;
                }
                //根据url生成加密文件
                String filename= FileUtil.getFileNameByUrlWithEncode(fileUrl);
                File file=new File(destFileDir,filename);
                if (file.exists()){
                    file.delete();//之前有就删除
                }

                Request request=new Request.Builder().url(fileUrl).build();
                client.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        subscriber.onError(e);
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        //返回成功
                        try {
                            InputStream is = null;
                            byte[] buf = new byte[2048];
                            int len = 0;
                            FileOutputStream fos = null;
                            try {
                                long total = response.body().contentLength();
                                LogUtil.e(TAG, "total------>" + total);
                                long current = 0;
                                is = response.body().byteStream();
                                fos = new FileOutputStream(file);
                                while ((len = is.read(buf)) != -1) {
                                    current += len;
                                    fos.write(buf, 0, len);
                                    LogUtil.e(TAG, "current------>" + current);
                                }
                                LogUtil.e(TAG, "current------>" + current);
                                fos.flush();
                                subscriber.onNext(true);
                                subscriber.onCompleted();
                            } catch (IOException e) {//no such file or directory exception
                                LogUtil.e(TAG, e.toString());
                                subscriber.onError(e);
                            } finally {
                                try {
                                    if (is != null) {
                                        is.close();
                                    }
                                    if (fos != null) {
                                        fos.close();
                                    }
                                } catch (IOException e) {
                                    LogUtil.e(TAG, e.toString());
                                }
                            }
                        }catch (Exception e){
                            subscriber.onError(e);
                            e.printStackTrace();
                        }
                    }
                });
            }
        });
    }

    @Override
    public Observable<Boolean> downloadFileToDestiFileWithoutNameEncrypt(String fileUrl, File destFile) {
        return Observable.create(new Observable.OnSubscribe<Boolean>() {
            @Override
            public void call(Subscriber<? super Boolean> subscriber) {
                if (subscriber.isUnsubscribed()){
                    return;
                }
                //根据url生成加密文件
                if (destFile.exists()){
                    destFile.delete();//之前有就删除
                }

                Request request=new Request.Builder().url(fileUrl).build();
                client.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        subscriber.onError(e);
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        //返回成功
                        try {
                            InputStream is = null;
                            byte[] buf = new byte[2048];
                            int len = 0;
                            FileOutputStream fos = null;
                            try {
                                long total = response.body().contentLength();
                                LogUtil.e(TAG, "total------>" + total);
                                long current = 0;
                                is = response.body().byteStream();
                                fos = new FileOutputStream(destFile);
                                while ((len = is.read(buf)) != -1) {
                                    current += len;
                                    fos.write(buf, 0, len);
                                    LogUtil.e(TAG, "current------>" + current);
                                }
                                LogUtil.e(TAG, "current------>" + current);
                                fos.flush();
                                subscriber.onNext(true);
                                subscriber.onCompleted();
                            } catch (IOException e) {//no such file or directory exception
                                LogUtil.e(TAG, e.toString());
                                subscriber.onError(e);
                            } finally {
                                try {
                                    if (is != null) {
                                        is.close();
                                    }
                                    if (fos != null) {
                                        fos.close();
                                    }
                                } catch (IOException e) {
                                    LogUtil.e(TAG, e.toString());
                                }
                            }
                        }catch (Exception e){
                            subscriber.onError(e);
                            e.printStackTrace();
                        }
                    }
                });
            }
        });
    }


    private String getNameFromUrl(String url) {
        return url.substring(url.lastIndexOf("/") + 1);
    }
}
