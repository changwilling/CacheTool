package com.changwilling.cache_android.app;

import android.app.Application;

/**
 * Created by changwilling on 2017/5/9.
 *
 */

public class TApplication extends Application {

    private static TApplication instance;
    public static TApplication getInstance(){
        return instance;
    }

    @Override
    public void onCreate() {
        instance=this;
        super.onCreate();
    }
}
