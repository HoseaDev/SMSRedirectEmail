package com.hosea.messagerelayer.receiver;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.util.Log;
import android.widget.Toast;

import com.blankj.utilcode.util.LogUtils;
import com.hosea.messagerelayer.bean.SmsBean;
import com.hosea.messagerelayer.confing.Constant;
import com.hosea.messagerelayer.service.SmsService;
import com.hosea.messagerelayer.utils.FormatMobile;
import com.hosea.messagerelayer.utils.NativeDataManager;
import com.hosea.messagerelayer.utils.db.DataBaseManager;

import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.SmsManager;

import androidx.core.app.ActivityCompat;

import java.util.List;

import java.util.ArrayList;
import java.util.List;

public class MessageReceiver extends BroadcastReceiver {
    public static final String TAG = "MessageReceiver";
    private NativeDataManager mNativeDataManager;

    public MessageReceiver() {

    }

    @Override
    public void onReceive(Context context, Intent intent) {
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
                LogUtils.i(TAG, "当前卡的id: subscriptionId " + subscriptionId); //1是卡1，2是卡2
                SubscriptionManager subscriptionManager = SubscriptionManager.from(context);
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                SubscriptionInfo subscriptionInfo = subscriptionManager.getActiveSubscriptionInfo(subscriptionId);
                if (subscriptionInfo != null) {
                    // 获取SIM卡的详细信息
                    CharSequence carrierName = subscriptionInfo.getCarrierName();
                    String number = subscriptionInfo.getNumber();
                    LogUtils.i(TAG, "卡的信息: carrierName " + carrierName + " number " + number);
                    // 使用这些信息来判断是哪个SIM卡
                }

                String content = "";
                SmsMessage sms = null;
                String mobile = "";
                //这里把他原来的拼上了.应该不会有其他问题..
                for (int i = 0; i < pdus.length; i++) {
                    sms = SmsMessage.createFromPdu((byte[]) pdus[i]);
                    content += sms.getMessageBody();
                    mobile = sms.getOriginatingAddress();


                }
                LogUtils.i("发送人的手机号", "mobile: " + mobile);
                for (int i = 0; i < smsIntercept.size(); i++) {
                    LogUtils.i("MessageReceiver", "smsIntercept.get(" + i + ").getPhone()" + smsIntercept.get(i).getPhone());
                    if (smsIntercept.get(i).getPhone().equals(mobile)) {
                        //黑名单短信
                        LogUtils.i("MessageReceiver", "intercept---->" + mobile);
                        return;
                    }
                }
                if (FormatMobile.hasPrefix(mobile)) {
                    mobile = FormatMobile.formatMobile(mobile);
                }
                LogUtils.i(TAG, "sendSms: " + mobile + " -> content " + content + " -> 卡:" + subscriptionId);
                //判断是否选择卡1还是卡2发送，
//                sendSms(context, mobile, content, mNativeDataManager.getSimIndex());
                startSmsService(context, mobile, content, subscriptionId);
            }
        }
    }

    private ComponentName startSmsService(final Context context, String mobile, String content, int subscriptionId) {


//        String mobile = sms.getOriginatingAddress();//发送短信的手机号码

        if (FormatMobile.hasPrefix(mobile)) {
            mobile = FormatMobile.formatMobile(mobile);
        }
//        String content = sms.getMessageBody();//短信内容

        Intent serviceIntent = new Intent(context, SmsService.class);
        serviceIntent.putExtra(Constant.EXTRA_MESSAGE_CONTENT, content);
        serviceIntent.putExtra(Constant.EXTRA_MESSAGE_MOBILE, mobile);
        serviceIntent.putExtra(Constant.EXTRA_MESSAGE_RECEIVED_MOBILE_SUBID, subscriptionId);

        return context.startService(serviceIntent);
    }


    // 发送短信的通用方法

}
