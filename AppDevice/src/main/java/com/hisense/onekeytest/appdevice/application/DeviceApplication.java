package com.hisense.onekeytest.appdevice.application;

import android.app.Application;

/**
 * Created by Administrator on 2017/3/3.
 */

public class DeviceApplication extends Application {

    private static DeviceApplication INSTANCE = null;

    public static DeviceApplication getIntence(){
        return INSTANCE;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        INSTANCE = this;
    }
}
