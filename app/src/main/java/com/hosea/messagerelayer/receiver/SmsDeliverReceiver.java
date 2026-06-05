package com.hosea.messagerelayer.receiver;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SmsMessage;

import com.blankj.utilcode.util.LogUtils;
import com.hosea.messagerelayer.bean.SmsBean;
import com.hosea.messagerelayer.utils.FormatMobile;
import com.hosea.messagerelayer.utils.NativeDataManager;
import com.hosea.messagerelayer.activity.SmsInboxActivity;
import com.hosea.messagerelayer.utils.db.DataBaseManager;

import java.util.ArrayList;

/**
 * 当本 app 是系统默认短信应用时，系统发送 SMS_DELIVER 广播。
 * 职责：
 * 1. 将短信写入系统短信数据库（默认短信 app 的义务）
 * 2. 委托 SmsService 执行转发逻辑
 */
public class SmsDeliverReceiver extends BroadcastReceiver {

    private static final String TAG = "SmsDeliverReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        MessageReceiver.acquireWakeLock(context);

        LogUtils.i(TAG, "收到 SMS_DELIVER 广播");

        Bundle bundle = intent.getExtras();
        if (bundle == null) {
            MessageReceiver.releaseWakeLock();
            return;
        }

        Object[] pdus = (Object[]) bundle.get("pdus");
        if (pdus == null || pdus.length == 0) {
            MessageReceiver.releaseWakeLock();
            return;
        }

        String format = bundle.getString("format");
        int subscriptionId = bundle.getInt("subscription", -1);

        StringBuilder contentBuilder = new StringBuilder();
        String mobile = "";
        long timestamp = System.currentTimeMillis();

        for (Object pdu : pdus) {
            SmsMessage sms;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                sms = SmsMessage.createFromPdu((byte[]) pdu, format);
            } else {
                sms = SmsMessage.createFromPdu((byte[]) pdu);
            }
            contentBuilder.append(sms.getMessageBody());
            mobile = sms.getOriginatingAddress();
            timestamp = sms.getTimestampMillis();
        }

        String content = contentBuilder.toString();

        // 写入系统短信数据库
        try {
            ContentValues values = new ContentValues();
            values.put("address", mobile);
            values.put("body", content);
            values.put("date", timestamp);
            values.put("read", 0);
            values.put("seen", 0);
            values.put("type", 1);
            if (subscriptionId >= 0) {
                values.put("sub_id", subscriptionId);
            }
            context.getContentResolver().insert(
                    Uri.parse("content://sms/inbox"), values);
            LogUtils.i(TAG, "短信已写入系统数据库");
            // 发送新短信通知
            SmsInboxActivity.showNewMessageNotification(context, mobile, content, 0);
        } catch (Exception e) {
            LogUtils.e(TAG, "写入系统短信数据库失败: " + e.getMessage());
        }

        NativeDataManager nativeDataManager = new NativeDataManager(context);
        if (!nativeDataManager.getReceiver()) {
            MessageReceiver.releaseWakeLock();
            return;
        }

        DataBaseManager dataBaseManager = new DataBaseManager(context);
        ArrayList<SmsBean> smsIntercept = dataBaseManager.getSmsIntercept();
        for (int i = 0; i < smsIntercept.size(); i++) {
            if (smsIntercept.get(i).getPhone().equals(mobile)) {
                LogUtils.i(TAG, "黑名单拦截: " + mobile);
                MessageReceiver.releaseWakeLock();
                return;
            }
        }

        if (FormatMobile.hasPrefix(mobile)) {
            mobile = FormatMobile.formatMobile(mobile);
        }

        LogUtils.i(TAG, "委托转发: " + mobile + " 卡:" + subscriptionId);
        MessageReceiver.startSmsService(context, mobile, content, subscriptionId);
    }
}
