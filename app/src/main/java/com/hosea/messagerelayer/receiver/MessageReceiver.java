package com.hosea.messagerelayer.receiver;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.telephony.SmsMessage;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.widget.Toast;

import com.blankj.utilcode.util.LogUtils;
import com.hosea.messagerelayer.bean.SmsBean;
import com.hosea.messagerelayer.confing.Constant;
import com.hosea.messagerelayer.service.SmsService;
import com.hosea.messagerelayer.utils.FormatMobile;
import com.hosea.messagerelayer.utils.NativeDataManager;
import com.hosea.messagerelayer.utils.db.DataBaseManager;

import androidx.core.app.ActivityCompat;

import java.util.ArrayList;

public class MessageReceiver extends BroadcastReceiver {
    public static final String TAG = "MessageReceiver";
    private NativeDataManager mNativeDataManager;

    /**
     * 静态 WakeLock，保证从收到广播到 Service 启动期间 CPU 不休眠。
     * 由 Service 在处理完成后释放。
     */
    private static PowerManager.WakeLock sWakeLock;

    public static synchronized void acquireWakeLock(Context context) {
        if (sWakeLock == null) {
            PowerManager pm = (PowerManager) context.getApplicationContext()
                    .getSystemService(Context.POWER_SERVICE);
            sWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                    "MessageRelayer:MessageReceiver");
            sWakeLock.setReferenceCounted(false);
        }
        // 超时 30 秒自动释放，避免泄漏
        sWakeLock.acquire(30 * 1000L);
    }

    public static synchronized void releaseWakeLock() {
        if (sWakeLock != null && sWakeLock.isHeld()) {
            sWakeLock.release();
        }
    }

    public MessageReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        // 立刻获取 WakeLock，防止 CPU 休眠
        acquireWakeLock(context);

        Toast.makeText(context, "收到消息", Toast.LENGTH_SHORT).show();
        LogUtils.i("MessageReceiver", "收到消息: ");

        this.mNativeDataManager = new NativeDataManager(context);
        DataBaseManager dataBaseManager = new DataBaseManager(context);
        ArrayList<SmsBean> smsIntercept = dataBaseManager.getSmsIntercept();

        if (mNativeDataManager.getReceiver()) {
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                Object[] pdus = (Object[]) bundle.get("pdus");
                int subscriptionId = bundle.getInt("subscription", -1);
                LogUtils.i(TAG, "当前卡的id: subscriptionId " + subscriptionId);
                SubscriptionManager subscriptionManager = SubscriptionManager.from(context);
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                    releaseWakeLock();
                    return;
                }
                SubscriptionInfo subscriptionInfo = subscriptionManager.getActiveSubscriptionInfo(subscriptionId);
                if (subscriptionInfo != null) {
                    CharSequence carrierName = subscriptionInfo.getCarrierName();
                    String number = subscriptionInfo.getNumber();
                    LogUtils.i(TAG, "卡的信息: carrierName " + carrierName + " number " + number);
                }

                String content = "";
                SmsMessage sms = null;
                String mobile = "";
                for (int i = 0; i < pdus.length; i++) {
                    sms = SmsMessage.createFromPdu((byte[]) pdus[i]);
                    content += sms.getMessageBody();
                    mobile = sms.getOriginatingAddress();
                }
                LogUtils.i("发送人的手机号", "mobile: " + mobile);
                for (int i = 0; i < smsIntercept.size(); i++) {
                    LogUtils.i("MessageReceiver", "smsIntercept.get(" + i + ").getPhone()" + smsIntercept.get(i).getPhone());
                    if (smsIntercept.get(i).getPhone().equals(mobile)) {
                        LogUtils.i("MessageReceiver", "intercept---->" + mobile);
                        releaseWakeLock();
                        return;
                    }
                }
                if (FormatMobile.hasPrefix(mobile)) {
                    mobile = FormatMobile.formatMobile(mobile);
                }
                LogUtils.i(TAG, "sendSms: " + mobile + " -> content " + content + " -> 卡:" + subscriptionId);
                startSmsService(context, mobile, content, subscriptionId);
            }
        } else {
            releaseWakeLock();
        }
    }

    public static void startSmsService(Context context, String mobile, String content, int subscriptionId) {
        if (FormatMobile.hasPrefix(mobile)) {
            mobile = FormatMobile.formatMobile(mobile);
        }

        Intent serviceIntent = new Intent(context, SmsService.class);
        serviceIntent.putExtra(Constant.EXTRA_MESSAGE_CONTENT, content);
        serviceIntent.putExtra(Constant.EXTRA_MESSAGE_MOBILE, mobile);
        serviceIntent.putExtra(Constant.EXTRA_MESSAGE_RECEIVED_MOBILE_SUBID, subscriptionId);

        // Android 8.0+ 必须使用 startForegroundService
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }
    }
}
