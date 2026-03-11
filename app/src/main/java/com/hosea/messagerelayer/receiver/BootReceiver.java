package com.hosea.messagerelayer.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.blankj.utilcode.util.LogUtils;
import com.hosea.messagerelayer.service.ForegroundService;
import com.hosea.messagerelayer.utils.KeepAliveScheduler;

/**
 * 开机自启动广播接收器。
 * 手机重启后自动拉起前台服务，确保短信转发不中断。
 */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)
                || Intent.ACTION_LOCKED_BOOT_COMPLETED.equals(action)) {
            LogUtils.i("BootReceiver", "开机自启动，拉起前台服务");
            Intent serviceIntent = new Intent(context, ForegroundService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }

            // 恢复所有已开启的定时保号闹钟
            KeepAliveScheduler.scheduleAllEnabled(context);
        }
    }
}
