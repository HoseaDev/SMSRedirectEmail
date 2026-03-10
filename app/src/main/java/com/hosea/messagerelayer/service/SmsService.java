package com.hosea.messagerelayer.service;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.PowerManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.blankj.utilcode.util.LogUtils;
import com.hosea.messagerelayer.R;
import com.hosea.messagerelayer.bean.Contact;
import com.hosea.messagerelayer.confing.Constant;
import com.hosea.messagerelayer.utils.ContactManager;
import com.hosea.messagerelayer.utils.EmailRelayerManager;
import com.hosea.messagerelayer.utils.NativeDataManager;
import com.hosea.messagerelayer.utils.SmsRelayerManager;
import com.hosea.messagerelayer.utils.db.DataBaseManager;

import java.util.ArrayList;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 短信转发服务。
 * 从 IntentService 改为普通 Service + HandlerThread，
 * 以兼容 Android 8.0+ 后台限制，并使用 WakeLock 防止 CPU 休眠导致转发中断。
 */
public class SmsService extends Service {

    private static final String TAG = "SmsService";
    private static final String CHANNEL_ID = "SmsServiceChannel";

    private HandlerThread mHandlerThread;
    private Handler mWorkHandler;
    private PowerManager.WakeLock mWakeLock;

    @Override
    public void onCreate() {
        super.onCreate();
        mHandlerThread = new HandlerThread("SmsServiceThread");
        mHandlerThread.start();
        mWorkHandler = new Handler(mHandlerThread.getLooper());

        // 获取 WakeLock，防止处理短信期间 CPU 休眠
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MessageRelayer:SmsService");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // 先变成前台服务，避免被系统秒杀
        createNotificationChannel();
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("短信转发中")
                .setContentText("正在处理短信转发...")
                .setSmallIcon(R.mipmap.icon)
                .build();

        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(2, notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        } else {
            startForeground(2, notification);
        }

        if (intent == null) {
            stopSelfSafe(startId);
            return START_NOT_STICKY;
        }

        final String mobile = intent.getStringExtra(Constant.EXTRA_MESSAGE_MOBILE);
        final String content = intent.getStringExtra(Constant.EXTRA_MESSAGE_CONTENT);
        final int subId = intent.getIntExtra(Constant.EXTRA_MESSAGE_RECEIVED_MOBILE_SUBID, -1);
        final int currentStartId = startId;

        // 获取 WakeLock，超时 60 秒自动释放（防止泄漏）
        mWakeLock.acquire(60 * 1000L);

        mWorkHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    handleSms(mobile, content, subId);
                } catch (Exception e) {
                    LogUtils.e(TAG, "处理短信转发异常: " + e.getMessage());
                } finally {
                    if (mWakeLock.isHeld()) {
                        mWakeLock.release();
                    }
                    stopSelfSafe(currentStartId);
                }
            }
        });

        return START_NOT_STICKY;
    }

    private void handleSms(String mobile, String content, int subId) {
        NativeDataManager nativeDataManager = new NativeDataManager(this);
        DataBaseManager dataBaseManager = new DataBaseManager(this);

        Set<String> keySet = nativeDataManager.getKeywordSet();
        ArrayList<Contact> contactList = dataBaseManager.getAllContact();

        // 无转发规则
        if (keySet.size() == 0 && contactList.size() == 0) {
            relayMessage(nativeDataManager, content, mobile, subId);
        } else if (keySet.size() != 0 && contactList.size() == 0) {
            // 仅有关键字规则
            for (String key : keySet) {
                if (content.contains(key)) {
                    relayMessage(nativeDataManager, content, mobile, subId);
                    break;
                }
            }
        } else if (keySet.size() == 0 && contactList.size() != 0) {
            // 仅有手机号规则
            for (Contact contact : contactList) {
                if (contact.getContactNum().equals(mobile)) {
                    relayMessage(nativeDataManager, content, mobile, subId);
                    break;
                }
            }
        } else {
            // 两种规则共存
            out:
            for (Contact contact : contactList) {
                if (contact.getContactNum().equals(mobile)) {
                    for (String key : keySet) {
                        if (content.contains(key)) {
                            relayMessage(nativeDataManager, content, mobile, subId);
                            break out;
                        }
                    }
                }
            }
        }

        dataBaseManager.closeHelper();
    }

    private void relayMessage(NativeDataManager mNativeDataManager, String content, String mobile, int subId) {
        SubscriptionManager subscriptionManager = SubscriptionManager.from(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        SubscriptionInfo subscriptionInfo = subscriptionManager.getActiveSubscriptionInfo(subId);
        String receivedMobile = "";
        if (subscriptionInfo != null) {
            CharSequence carrierName = subscriptionInfo.getCarrierName();
            receivedMobile = subscriptionInfo.getNumber();
            LogUtils.i(" carrierName " + carrierName + " number " + receivedMobile);
            if (receivedMobile == null || receivedMobile.isEmpty()) {
                LogUtils.w(TAG, "无法获取到手机号");
            }
        }

        String dContent = content;
        String suffix = mNativeDataManager.getContentSuffix();
        String prefix = mNativeDataManager.getContentPrefix();
        if (suffix != null) {
            dContent = dContent + suffix;
        }
        if (prefix != null) {
            dContent = prefix + dContent;
        }
        ArrayList<Contact> mContactList = ContactManager.getContactList(this);
        for (int i = 0; i < mContactList.size(); i++) {
            if (mContactList.get(i).getContactNum().equals(mobile)) {
                LogUtils.i("找到了备注:" + mContactList.get(i).getContactNum());
                LogUtils.i("找到了备注:" + mContactList.get(i).getContactName());
                dContent = "联系人: " + mContactList.get(i).getContactName() + "\n" +
                        "发送号码: " + mContactList.get(i).getContactNum() + "\n" + dContent;
            }
        }
        String extractCode = extractCode(dContent);
        if (!dContent.contains("联系人:")) {
            dContent = "发送号码: " + mobile + "\n" + dContent;
        }
        LogUtils.i("最终转发出的内容:" + dContent);

        if (mNativeDataManager.getEmailRelay()) {
            dContent = dContent.replace("\n", "<br>");
            LogUtils.i("\n换成br =>" + dContent);

            String tail = getSimTail(receivedMobile, subId);
            String title = (extractCode == null)
                    ? "尾号:" + tail
                    : "尾号:" + tail + "->验证码:" + extractCode;

            LogUtils.i("准备发送邮件:", title, dContent);
            EmailRelayerManager.relayEmail(mNativeDataManager, title, dContent);
        }
        LogUtils.i("mobile=>" + mobile);
        if (mNativeDataManager.getInnerRelay() && mobile.equals(mNativeDataManager.getInnerMobile())) {
            int sIndex = content.indexOf(mNativeDataManager.getInnerRule());
            if (sIndex != -1) {
                LogUtils.i("转发内部短信");
                String transferPhone = content.substring(0, sIndex);
                String transferContent = content.substring(sIndex + 1);
                SmsRelayerManager.relaySms(this, transferPhone, transferContent, subId);
            }
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        if (mHandlerThread != null) {
            mHandlerThread.quitSafely();
        }
        super.onDestroy();
    }

    private void stopSelfSafe(int startId) {
        stopSelf(startId);
    }

    public static String extractCode(String content) {
        if (content.contains("码") || content.contains("code")) {
            String regex = "(\\d{4,6})";
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(content);
            if (matcher.find()) {
                return matcher.group();
            }
            return null;
        } else {
            return null;
        }
    }

    private String getSimTail(String number, int subId) {
        if (number == null) {
            return fallbackSimTag(subId);
        }
        String digits = number.replaceAll("\\D+", "");
        if (digits.isEmpty()) {
            return fallbackSimTag(subId);
        }
        int len = digits.length();
        return (len <= 4) ? digits : digits.substring(len - 4);
    }

    private String fallbackSimTag(int subId) {
        if (subId >= 0) {
            return "卡" + (subId + 1);
        }
        return "未知";
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "短信转发服务",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }
}
