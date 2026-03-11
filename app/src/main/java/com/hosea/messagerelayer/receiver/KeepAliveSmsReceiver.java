package com.hosea.messagerelayer.receiver;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.Manifest;
import android.os.Build;
import android.telephony.SmsManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;

import androidx.core.app.ActivityCompat;

import com.blankj.utilcode.util.LogUtils;
import com.hosea.messagerelayer.confing.Constant;
import com.hosea.messagerelayer.service.ForegroundService;
import com.hosea.messagerelayer.utils.EmailRelayerManager;
import com.hosea.messagerelayer.utils.KeepAliveScheduler;
import com.hosea.messagerelayer.utils.NativeDataManager;
import com.hosea.messagerelayer.utils.db.ForwardingLogManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 保号短信定时发送接收器。
 * 由 AlarmManager 触发，从 Intent extra 获取 subId，发送对应 SIM 卡的保号短信。
 */
public class KeepAliveSmsReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        final int subId = intent.getIntExtra(Constant.EXTRA_KEEP_ALIVE_SUB_ID, -1);
        final boolean isTest = intent.getBooleanExtra(Constant.EXTRA_KEEP_ALIVE_IS_TEST, false);

        if (subId < 0) {
            LogUtils.w("KeepAliveSmsReceiver", "未获取到 subId，跳过");
            return;
        }

        LogUtils.i("KeepAliveSmsReceiver", "闹钟触发，subId=" + subId + ", 测试=" + isTest);

        final NativeDataManager mgr = new NativeDataManager(context);

        if (!isTest && !mgr.getKeepAliveEnabled(subId)) {
            LogUtils.i("KeepAliveSmsReceiver", "subId=" + subId + " 保号开关已关闭，跳过");
            return;
        }

        final String targetMobile = mgr.getKeepAliveTargetMobile(subId);
        final String smsContent = mgr.getKeepAliveSmsContent(subId);

        if (targetMobile == null || targetMobile.isEmpty()) {
            LogUtils.w("KeepAliveSmsReceiver", "subId=" + subId + " 目标号码未设置，跳过");
            return;
        }

        final PendingResult pendingResult = goAsync();
        final Context appContext = context.getApplicationContext();

        new Thread(new Runnable() {
            @Override
            public void run() {
                // 第一步：发送短信并获取结果
                String sendError = sendSms(appContext, targetMobile, smsContent, subId);
                boolean smsSent = (sendError == null);

                LogUtils.i("KeepAliveSmsReceiver",
                        "发送结果: subId=" + subId + ", 目标=" + targetMobile +
                        ", 成功=" + smsSent + (sendError != null ? ", 错误=" + sendError : ""));

                // 记录转发日志
                String relayType = isTest ? "KEEP_ALIVE_TEST" : "KEEP_ALIVE";
                ForwardingLogManager.logRelay(appContext, targetMobile, relayType,
                        smsContent, smsSent ? 1 : 0, sendError);

                // 更新上次发送时间
                long now = System.currentTimeMillis();
                mgr.setKeepAliveLastSendTime(subId, now);

                // 发送邮件通知结果
                sendEmailNotification(appContext, mgr, targetMobile, subId, smsSent, sendError, now, isTest);

                if (!isTest) {
                    KeepAliveScheduler.scheduleNext(appContext, subId);
                } else {
                    mgr.setKeepAliveNextSendTime(subId, 0);
                }

                ForegroundService.updateNotification(appContext);
                pendingResult.finish();
            }
        }).start();
    }

    /**
     * 发送短信，做好前置检查。
     * 返回 null 表示已提交发送（无异常），返回错误信息表示发送前就失败了。
     */
    private String sendSms(Context context, String mobile, String content, int subId) {
        // 检查权限
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED) {
            return "缺少 READ_PHONE_STATE 权限";
        }
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            return "缺少 SEND_SMS 权限";
        }

        // 获取 SIM 卡列表
        SubscriptionManager subscriptionManager = SubscriptionManager.from(context);
        List<SubscriptionInfo> list = subscriptionManager.getActiveSubscriptionInfoList();
        if (list == null || list.isEmpty()) {
            return "无可用 SIM 卡列表";
        }

        // 查找目标 SIM 卡
        SmsManager smsManager = null;
        String simInfo = "";
        for (SubscriptionInfo info : list) {
            if (info.getSubscriptionId() == subId) {
                smsManager = SmsManager.getSmsManagerForSubscriptionId(subId);
                simInfo = "卡: " + info.getCarrierName() + " (subId=" + subId + ")";
                break;
            }
        }

        if (smsManager == null) {
            // 列出所有可用 SIM 卡帮助排查
            StringBuilder sb = new StringBuilder("未找到 subId=").append(subId).append(" 的 SIM 卡。可用卡: ");
            for (SubscriptionInfo info : list) {
                sb.append("[").append(info.getCarrierName())
                  .append(" subId=").append(info.getSubscriptionId()).append("] ");
            }
            return sb.toString();
        }

        LogUtils.i("KeepAliveSmsReceiver",
                "准备发送: " + simInfo + " → " + mobile + " 内容: " + content);

        try {
            if (content.length() > 70) {
                ArrayList<String> parts = smsManager.divideMessage(content);
                smsManager.sendMultipartTextMessage(mobile, null, parts, null, null);
                LogUtils.i("KeepAliveSmsReceiver", "长短信已提交发送，共 " + parts.size() + " 段");
            } else {
                smsManager.sendTextMessage(mobile, null, content, null, null);
                LogUtils.i("KeepAliveSmsReceiver", "短信已提交发送");
            }
            return null; // 提交成功
        } catch (Exception e) {
            LogUtils.e("KeepAliveSmsReceiver", "sendTextMessage 异常: " + e.getMessage());
            return "发送异常: " + e.getClass().getSimpleName() + " - " + e.getMessage();
        }
    }

    private void sendEmailNotification(Context context, NativeDataManager mgr,
                                       String targetMobile, int subId,
                                       boolean success, String errorMsg, long sendTime,
                                       boolean isTest) {
        if (!mgr.getEmailRelay()) {
            return;
        }

        String emailPassword = mgr.getEmailPassword();
        if (emailPassword == null || emailPassword.isEmpty()) {
            return;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        String timeStr = sdf.format(new Date(sendTime));

        String testLabel = isTest ? "【测试】" : "";
        String title;
        StringBuilder content = new StringBuilder();

        if (success) {
            title = testLabel + "保号短信已提交发送（SIM " + subId + "）";
            content.append(testLabel).append("保号短信已提交运营商发送<br><br>");
        } else {
            title = testLabel + "保号短信发送失败（SIM " + subId + "）";
            content.append(testLabel).append("保号短信发送失败<br><br>");
        }

        content.append("目标号码: ").append(targetMobile).append("<br>");
        content.append("SIM卡 ID: ").append(subId).append("<br>");
        content.append("短信内容: ").append(mgr.getKeepAliveSmsContent(subId)).append("<br>");
        content.append("发送时间: ").append(timeStr).append("<br>");

        if (!success && errorMsg != null) {
            content.append("错误信息: ").append(errorMsg).append("<br>");
        }

        EmailRelayerManager.relayEmail(mgr, title, content.toString());
    }
}
