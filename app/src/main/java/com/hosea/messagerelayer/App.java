package com.hosea.messagerelayer;

import android.app.Application;

/**
 * Created by heliu on 2018/7/23.
 */

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        //需要在 Application 的 onCreate() 中调用一次 DaemonEnv.initialize()
    }
}
