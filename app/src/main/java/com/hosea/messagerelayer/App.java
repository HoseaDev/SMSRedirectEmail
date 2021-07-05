package com.hosea.messagerelayer;

import android.app.Application;

import com.hosea.messagerelayer.service.TraceServiceImpl;
import com.xdandroid.hellodaemon.DaemonEnv;

/**
 * Created by heliu on 2018/7/23.
 */

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        //需要在 Application 的 onCreate() 中调用一次 DaemonEnv.initialize()
        DaemonEnv.initialize(this, TraceServiceImpl.class, DaemonEnv.DEFAULT_WAKE_UP_INTERVAL);
        TraceServiceImpl.sShouldStopService = false;
        DaemonEnv.startServiceMayBind(TraceServiceImpl.class);
    }
}
