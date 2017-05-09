package com.changwilling.cache_android.service;

import java.io.File;
import java.io.InputStream;
import java.util.Map;

import rx.Observable;

/**
 * Created by changwilling on 17/2/8.
 * 均为异步请求方式
 */

public interface IOkhttpService {
    //get
    Observable<String> asynGetByURL(String url);//通过url的Get请求方式，返回json 的 String
    //post
    Observable<String> asynPostByURLAndMap(String url, Map<String, String> map);//post请求，html表单提交
    //post 下载
    Observable<InputStream> downloadFileByPost(String fileUrl);
    //将文件下载到指定目录，并且对文件名进行加密处理再保存
    Observable<Boolean> downLoadFileToDestFile(String fileUrl, String destFileDir);
    //将文件下载到指定目录，文件名不加密处理
    Observable<Boolean> downloadFileToDestiFileWithoutNameEncrypt(String fileUrl, File destFile);
}
