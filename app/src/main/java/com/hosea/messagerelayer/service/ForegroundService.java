package com.hosea.messagerelayer.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.hosea.messagerelayer.R;

public class ForegroundService extends Service {

    private static final String CHANNEL_ID = "ForegroundServiceChannel";

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotificationChannel();
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("前台服务通知")
                .setContentText("这是一个保持应用活跃的前台服务，我活着才有希望。")
                .setSmallIcon(R.mipmap.icon)
                // 如果有，可以设置点击通知后的动作
                //.setContentIntent(pendingIntent)
                .build();


        if (Build.VERSION.SDK_INT >= 29) {
            // 选择与你的业务相符的类型，且必须包含在 Manifest 的 android:foregroundServiceType 中
            startForeground(
                    1,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                    // 或者：ServiceInfo.FOREGROUND_SERVICE_TYPE_REMOTE_MESSAGING
                    // 如果你两者都需要，可根据具体任务在不同分支传其一；
                    // Android 13+ 也支持按位或传多类型，但要与 Manifest 对齐。
            );
        } else {
            // 老系统仍然用两参版本
            startForeground(1, notification);
        }

//        startForeground(1, notification);

        // 如果你希望服务在被杀死后有尝试重新启动的行为，可以返回 START_STICKY
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "前台服务通道",
                    NotificationManager.IMPORTANCE_DEFAULT
            );

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }
}
