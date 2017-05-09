package com.changwilling.cache_android.util;

import android.os.Handler;
import android.os.Looper;

public class ThreadUtils {
    /**
     * 子线程执行task
     */
    public static void runInThread(Runnable task) {
        new Thread(task).start();
    }

    /**
     * 创建一个主线程中handler
     */
    public static Handler mHandler = new Handler(Looper.getMainLooper());

    /**
     * UI线程执行task
     */
    public static void runInUIThread(Runnable task) {
        mHandler.post(task);
    }
}
