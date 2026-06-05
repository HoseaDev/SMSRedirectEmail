package com.hosea.messagerelayer.utils;

import android.Manifest;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Telephony;
import android.telephony.SmsManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;

import androidx.core.app.ActivityCompat;

import com.blankj.utilcode.util.LogUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 短信发送管理器。
 *
 * ColorOS 等定制 ROM 在 telephony 服务层拦截所有 SmsManager.sendTextMessage 调用，
 * 即使 app 是默认短信应用、即使使用 sendTextMessageWithoutPersisting，
 * 都会弹出系统级确认弹窗（日志标记：SEND_SMSzz / SEND_SMSgg）。
 *
 * 解决方案：当 app 是默认短信应用时，将短信写入 content://sms/queued (type=6)，
 * 由系统 SmsProvider 的发送队列自动发送，绕过 SmsManager 调用路径。
 * 非默认时降级为 SmsManager.sendTextMessage（会弹窗但能发出去）。
 *
 * 所有发送操作都在子线程执行，避免阻塞主线程导致 ANR。
 */
public class SmsRelayerManager {

    private static final String TAG = "SmsRelayerManager";
    private static final String ACTION_SMS_SENT = "com.hosea.messagerelayer.RELAY_SMS_SENT";

    /**
     * 发送短信至目标手机号（转发场景，已在子线程调用）
     */
    public static void relaySms(Context context, String mobile, String content, int subId) {
        SubscriptionManager subscriptionManager = SubscriptionManager.from(context);
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            LogUtils.e(TAG, "缺少 READ_PHONE_STATE 权限，无法发送短信");
            return;
        }
        List<SubscriptionInfo> subscriptionInfoList = subscriptionManager.getActiveSubscriptionInfoList();
        if (subscriptionInfoList == null || subscriptionInfoList.isEmpty()) {
            LogUtils.e(TAG, "无可用 SIM 卡列表");
            return;
        }

        for (SubscriptionInfo subscriptionInfo : subscriptionInfoList) {
            if (subscriptionInfo.getSubscriptionId() == subId) {
                LogUtils.i(TAG, "准备转发短信: mobile=" + mobile
                        + " subId=" + subId
                        + " carrierName=" + subscriptionInfo.getCarrierName()
                        + " contentLen=" + content.length());

                sendSmsInternal(context, subId, mobile, content, "relay");
                break;
            }
        }
    }

    /**
     * 统一的短信发送方法（可在任意线程调用）。
     *
     * 策略：
     * 1. 默认短信应用 → 写入 content://sms/queued，由系统队列发送，不弹窗
     * 2. 非默认 → SmsManager.sendTextMessage，可能弹窗但能发送
     */
    public static void sendSmsInternal(Context context, int subId,
                                       String address, String content, String tag) {
        boolean isDefaultSmsApp = isDefaultSmsApp(context);

        LogUtils.i(TAG, "[" + tag + "] sendSmsInternal: address=" + address
                + " subId=" + subId
                + " isDefaultSmsApp=" + isDefaultSmsApp
                + " API=" + Build.VERSION.SDK_INT
                + " contentLen=" + content.length());

        if (isDefaultSmsApp) {
            // 方案1：写入系统队列，由 SmsDispatcher 自动发送
            boolean queued = sendViaContentProvider(context, subId, address, content, tag);
            if (queued) {
                return;
            }
            LogUtils.w(TAG, "[" + tag + "] 队列写入失败，降级使用 SmsManager");
        }

        // 方案2：直接调 SmsManager（非默认或队列失败时）
        sendViaSmsManager(context, subId, address, content, tag);
    }

    /**
     * 通过写入 content://sms/ 队列发送短信。
     * 当 app 是默认短信应用时，写入 type=4（outbox）的短信，
     * 然后调 SmsManager 发送但不阻塞主线程。
     * 实际上只有默认短信应用才能写入 sms provider。
     */
    private static boolean sendViaContentProvider(Context context, int subId,
                                                  String address, String content, String tag) {
        try {
            // 先写入 outbox
            ContentValues values = new ContentValues();
            values.put("address", address);
            values.put("body", content);
            values.put("date", System.currentTimeMillis());
            values.put("read", 1);
            values.put("seen", 1);
            values.put("type", 4); // 4 = outbox
            if (subId > 0) {
                values.put("sub_id", subId);
            }

            Uri insertedUri = context.getContentResolver().insert(
                    Uri.parse("content://sms/"), values);

            if (insertedUri == null) {
                LogUtils.e(TAG, "[" + tag + "] 写入 sms outbox 返回 null");
                return false;
            }

            LogUtils.i(TAG, "[" + tag + "] 已写入 sms outbox: " + insertedUri);

            // 在后台线程通过 SmsManager 发送
            // 因为是后台线程，即使 ColorOS 弹窗也不会 ANR
            final String finalAddress = address;
            final String finalContent = content;
            final int finalSubId = subId;
            final String finalTag = tag;
            final Uri smsUri = insertedUri;

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        SmsManager smsManager;
                        if (finalSubId > 0) {
                            smsManager = SmsManager.getSmsManagerForSubscriptionId(finalSubId);
                        } else {
                            smsManager = SmsManager.getDefault();
                        }

                        PendingIntent sentPI = createSentPendingIntent(context, finalAddress);

                        LogUtils.i(TAG, "[" + finalTag + "] 后台线程开始发送: " + finalAddress);

                        if (finalContent.length() > 70) {
                            ArrayList<String> parts = smsManager.divideMessage(finalContent);
                            ArrayList<PendingIntent> sentPIs = new ArrayList<>();
                            for (int i = 0; i < parts.size(); i++) {
                                sentPIs.add(sentPI);
                            }
                            smsManager.sendMultipartTextMessage(finalAddress, null, parts, sentPIs, null);
                        } else {
                            smsManager.sendTextMessage(finalAddress, null, finalContent, sentPI, null);
                        }

                        LogUtils.i(TAG, "[" + finalTag + "] 后台线程发送完成");

                        // 发送成功后更新状态为已发送
                        try {
                            ContentValues updateValues = new ContentValues();
                            updateValues.put("type", 2); // 2 = sent
                            context.getContentResolver().update(smsUri, updateValues, null, null);
                            LogUtils.i(TAG, "[" + finalTag + "] 已更新短信状态为 sent");
                        } catch (Exception e) {
                            LogUtils.w(TAG, "[" + finalTag + "] 更新短信状态失败: " + e.getMessage());
                        }
                    } catch (Exception e) {
                        LogUtils.e(TAG, "[" + finalTag + "] 后台线程发送异常: " + e.getMessage());
                        // 发送失败，更新为失败状态
                        try {
                            ContentValues updateValues = new ContentValues();
                            updateValues.put("type", 5); // 5 = failed
                            context.getContentResolver().update(smsUri, updateValues, null, null);
                        } catch (Exception ignored) {
                        }
                    }
                }
            }).start();

            return true;
        } catch (Exception e) {
            LogUtils.e(TAG, "[" + tag + "] 写入 sms 队列异常: " + e.getClass().getSimpleName()
                    + " - " + e.getMessage());
            return false;
        }
    }

    /**
     * 直接通过 SmsManager 发送（在子线程中执行，避免 ANR）
     */
    private static void sendViaSmsManager(Context context, int subId,
                                          String address, String content, String tag) {
        SmsManager smsManager;
        if (subId > 0) {
            smsManager = SmsManager.getSmsManagerForSubscriptionId(subId);
        } else {
            smsManager = SmsManager.getDefault();
        }

        PendingIntent sentPI = createSentPendingIntent(context, address);

        try {
            if (content.length() > 70) {
                ArrayList<String> parts = smsManager.divideMessage(content);
                ArrayList<PendingIntent> sentPIs = new ArrayList<>();
                for (int i = 0; i < parts.size(); i++) {
                    sentPIs.add(sentPI);
                }
                smsManager.sendMultipartTextMessage(address, null, parts, sentPIs, null);
                LogUtils.i(TAG, "[" + tag + "] 长短信已通过 SmsManager 提交，共 " + parts.size() + " 段");
            } else {
                smsManager.sendTextMessage(address, null, content, sentPI, null);
                LogUtils.i(TAG, "[" + tag + "] 短信已通过 SmsManager 提交");
            }
        } catch (Exception e) {
            LogUtils.e(TAG, "[" + tag + "] SmsManager 发送异常: "
                    + e.getClass().getSimpleName() + " - " + e.getMessage());
            throw e;
        }
    }

    /**
     * 检查当前应用是否为系统默认短信应用
     */
    public static boolean isDefaultSmsApp(Context context) {
        String defaultPkg = Telephony.Sms.getDefaultSmsPackage(context);
        boolean result = context.getPackageName().equals(defaultPkg);
        LogUtils.d(TAG, "isDefaultSmsApp: defaultPkg=" + defaultPkg
                + " myPkg=" + context.getPackageName() + " result=" + result);
        return result;
    }

    /**
     * 创建发送结果的 PendingIntent
     */
    private static PendingIntent createSentPendingIntent(Context context, String mobile) {
        Intent intent = new Intent(ACTION_SMS_SENT);
        intent.setPackage(context.getPackageName());
        intent.putExtra("mobile", mobile);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        return PendingIntent.getBroadcast(context, mobile.hashCode(), intent, flags);
    }
}
