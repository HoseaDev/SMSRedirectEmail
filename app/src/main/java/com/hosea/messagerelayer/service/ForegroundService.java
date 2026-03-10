package com.hosea.messagerelayer.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.hosea.messagerelayer.R;
import com.hosea.messagerelayer.utils.NativeDataManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ForegroundService extends Service {

    private static final String CHANNEL_ID = "ForegroundServiceChannel";
    private static final int NOTIFICATION_ID = 1;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotificationChannel();
        Notification notification = buildNotification(this);

        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            );
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }

        // 被系统杀死后自动重启，确保前台服务常驻
        return START_STICKY;
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

    /**
     * 构建动态通知，显示总闸状态和最后转发时间
     */
    private static Notification buildNotification(Context context) {
        NativeDataManager mgr = new NativeDataManager(context);
        boolean receiverOn = mgr.getReceiver();
        long lastTime = mgr.getLastRelayTime();

        String switchStatus = receiverOn ? "已开启" : "已关闭";
        String timeInfo;
        if (lastTime > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("MM-dd HH:mm", Locale.getDefault());
            timeInfo = "最后转发: " + sdf.format(new Date(lastTime));
        } else {
            timeInfo = "暂无转发记录";
        }

        String contentText = "总闸: " + switchStatus + " | " + timeInfo;

        return new NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle("短信转发助手")
                .setContentText(contentText)
                .setSmallIcon(R.mipmap.icon)
                .setOngoing(true)
                .build();
    }

    /**
     * 外部调用：更新前台服务通知内容（转发成功后调用）
     */
    public static void updateNotification(Context context) {
        try {
            // 确保通知渠道已创建
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel(
                        CHANNEL_ID,
                        "前台服务通道",
                        NotificationManager.IMPORTANCE_DEFAULT
                );
                NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                nm.createNotificationChannel(channel);
            }
            Notification notification = buildNotification(context);
            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            nm.notify(NOTIFICATION_ID, notification);
        } catch (Exception e) {
            // 通知更新失败不影响主流程
        }
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
