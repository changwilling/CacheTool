package com.changwilling.cache_android.util;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by changwilling on 17/1/10.
 */

public class LogUtil {
    private static boolean isPrint=true;
    private static boolean isDebug=true;

    public static final String TAG="HYLManager";
    public static final String MSG="log msg is null";
    public static List<String> loglist;

    //这里的v代表verbose啰嗦的意思
    public static void v(String tag, String msg){
        print(Log.VERBOSE,tag,msg);
    }
    //仅输出debug调试
    public static void v(String msg){
        v(TAG,msg);
    }
    public static void d(String tag, String msg){
        print(Log.DEBUG,tag,msg);
    }
    public static void d(String msg){

    }
    //一般提示性的消息information，它不会输出Log.v和Log.d的信息，但会显示i、w和e的信息
    public static void i(String tag, String msg){
        print(Log.INFO,tag,msg);
    }
    public static void i(String msg){
        i(TAG,msg);
    }
    //可以看作为warning警告，一般需要我们注意优化Android代码，同时选择它后还会输出Log.e的信息
    public static void w(String tag, String msg){
        print(Log.WARN,tag,msg);
    }
    public static void w(String msg){
        w(TAG,msg);
    }
    //可以想到error错误，这里仅显示红色的错误信息，这些错误就需要我们认真的分析，查看栈的信息了
    public static void e(String tag, String msg){
        print(Log.ERROR,tag,msg);
    }
    public static void e(String msg){
        e(TAG,msg);
    }

    private static void print(int mode, final String tag, String msg){
        if(!isPrint){
            return;
        }
        if(msg==null){
            Log.e(tag,MSG);
            return;
        }
        switch (mode){
            case Log.VERBOSE:
                Log.v(tag,msg);
                break;
            case Log.DEBUG:
                Log.d(tag,msg);
                break;
            case Log.INFO:
                Log.i(tag,msg);
                break;
            case Log.WARN:
                Log.w(tag,msg);
                break;
            case Log.ERROR:
                Log.e(tag,msg);
                break;
            default:
                Log.d(tag,msg);
                break;
        }
    }
    private static void print(boolean flag,String msg){
        if(flag&&loglist!=null){
            loglist.add(msg);
        }
    }

    public static void setState(boolean flag){
        if(flag){
            if (loglist==null){
                loglist=new ArrayList<>();
            }else {
                loglist.clear();
            }
        }else {
            if (loglist!=null){
                loglist.clear();
                loglist=null;
            }
        }
        isDebug=flag;
    }
}
