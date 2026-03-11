package com.hosea.messagerelayer.utils;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;

import androidx.core.app.ActivityCompat;

import com.blankj.utilcode.util.LogUtils;
import com.hosea.messagerelayer.confing.Constant;
import com.hosea.messagerelayer.receiver.KeepAliveSmsReceiver;

import java.util.List;

/**
 * 定时保号闹钟调度器。
 * 封装 AlarmManager 操作，支持按 subId 为每张 SIM 卡独立设置闹钟。
 */
public class KeepAliveScheduler {

    private static final int REQUEST_CODE_BASE = 9001;

    /**
     * 为指定 SIM 卡设置下次保号闹钟
     */
    public static void scheduleNext(Context context, int subId) {
        NativeDataManager mgr = new NativeDataManager(context);
        if (!mgr.getKeepAliveEnabled(subId)) {
            return;
        }

        int intervalDays = mgr.getKeepAliveIntervalDays(subId);
        if (intervalDays <= 0) {
            return;
        }

        long intervalMillis = (long) intervalDays * 24 * 60 * 60 * 1000;
        long now = System.currentTimeMillis();
        long nextTime = now + intervalMillis;

        mgr.setKeepAliveNextSendTime(subId, nextTime);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pendingIntent = createPendingIntent(context, subId);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextTime, pendingIntent);
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, nextTime, pendingIntent);
        }

        LogUtils.i("KeepAliveScheduler", "已设置 subId=" + subId + " 的下次保号闹钟: " + nextTime + ", 间隔: " + intervalDays + "天");
    }

    /**
     * 测试用：2分钟后触发一次保号短信发送
     */
    /**
     * 测试用：10秒后触发一次保号短信发送，带测试标记绕过开关检查
     */
    public static void scheduleTest(Context context, int subId) {
        long testTime = System.currentTimeMillis() + 10 * 1000;

        NativeDataManager mgr = new NativeDataManager(context);
        mgr.setKeepAliveNextSendTime(subId, testTime);

        Intent intent = new Intent(context, KeepAliveSmsReceiver.class);
        intent.putExtra(Constant.EXTRA_KEEP_ALIVE_SUB_ID, subId);
        intent.putExtra(Constant.EXTRA_KEEP_ALIVE_IS_TEST, true);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        // 使用不同 requestCode 避免覆盖正常闹钟
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, REQUEST_CODE_BASE + subId + 50000, intent, flags);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, testTime, pendingIntent);
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, testTime, pendingIntent);
        }

        LogUtils.i("KeepAliveScheduler", "已设置 subId=" + subId + " 的测试闹钟，10秒后触发");
    }

    /**
     * 取消指定 SIM 卡的保号闹钟
     */
    public static void cancel(Context context, int subId) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pendingIntent = createPendingIntent(context, subId);
        alarmManager.cancel(pendingIntent);
        pendingIntent.cancel();

        NativeDataManager mgr = new NativeDataManager(context);
        mgr.setKeepAliveNextSendTime(subId, 0);

        LogUtils.i("KeepAliveScheduler", "已取消 subId=" + subId + " 的保号闹钟");
    }

    /**
     * 遍历所有活跃 SIM 卡，对已开启保号的卡重新设置闹钟（开机恢复用）
     */
    public static void scheduleAllEnabled(Context context) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        SubscriptionManager subscriptionManager = SubscriptionManager.from(context);
        List<SubscriptionInfo> list = subscriptionManager.getActiveSubscriptionInfoList();
        if (list == null || list.isEmpty()) {
            return;
        }

        NativeDataManager mgr = new NativeDataManager(context);
        for (SubscriptionInfo info : list) {
            int subId = info.getSubscriptionId();
            if (mgr.getKeepAliveEnabled(subId)) {
                LogUtils.i("KeepAliveScheduler", "恢复 subId=" + subId + " 的保号闹钟");
                scheduleNext(context, subId);
            }
        }
    }

    private static PendingIntent createPendingIntent(Context context, int subId) {
        Intent intent = new Intent(context, KeepAliveSmsReceiver.class);
        intent.putExtra(Constant.EXTRA_KEEP_ALIVE_SUB_ID, subId);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        return PendingIntent.getBroadcast(context, REQUEST_CODE_BASE + subId, intent, flags);
    }
}
