package com.hosea.messagerelayer.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;

/**
 * RESPOND_VIA_MESSAGE 空壳服务。
 * Android 要求默认短信应用必须声明此组件，但本应用不支持快速回复功能。
 */
public class RespondViaMessageService extends Service {

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        stopSelf();
        return START_NOT_STICKY;
    }
}
