package com.hosea.messagerelayer;

import android.app.Application;

import com.blankj.utilcode.util.CrashUtils;
import com.blankj.utilcode.util.LogUtils;

/**
 * Created by heliu on 2018/7/23.
 */

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        //需要在 Application 的 onCreate() 中调用一次 DaemonEnv.initialize()
        CrashUtils.init(new CrashUtils.OnCrashListener() {
            @Override
            public void onCrash(CrashUtils.CrashInfo crashInfo) {
                LogUtils.e("----->onCrash: " + crashInfo);
            }
        });


        final LogUtils.Config config = LogUtils.getConfig()
                .setLogSwitch(true)// 设置 log 总开关，包括输出到控制台和文件，默认开
                .setConsoleSwitch(true)// 设置是否输出到控制台开关，默认开
                .setGlobalTag("")// 设置 log 全局标签，默认为空
                .setSaveDays(20)
                // 当全局标签不为空时，我们输出的 log 全部为该 tag，
                // 为空时，如果传入的 tag 为空那就显示类名，否则显示 tag
                .setLogHeadSwitch(true)// 设置 log 头信息开关，默认为开
//        testVersion   0000-0>BuildConfig.DEBUG
                .setLog2FileSwitch(true)// 打印 log 时是否存到文件的开关，默认关
                .setDir("")// 当自定义路径为空时，写入应用的/cache/log/目录中
                .setFilePrefix("sre")// 当文件前缀为空时，默认为"util"，即写入文件为"util-MM-dd.txt"
                .setBorderSwitch(false)// 输出日志是否带边框开关，默认开
                .setConsoleFilter(LogUtils.V)// log 的控制台过滤器，和 logcat 过滤器同理，默认 Verbose
                .setFileFilter(LogUtils.V)// log 文件过滤器，和 logcat 过滤器同理，默认 Verbose
                .setStackDeep(1);// log 栈深度，默认为 1
        new Thread(new Runnable() {
            @Override
            public void run() {
                LogUtils.d(config.toString());
            }
        }).start();

    }
}
