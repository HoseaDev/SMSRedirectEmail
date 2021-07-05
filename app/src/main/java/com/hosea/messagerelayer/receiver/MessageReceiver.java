package com.hosea.messagerelayer.receiver;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.Toast;

import com.hosea.messagerelayer.bean.SmsBean;
import com.hosea.messagerelayer.confing.Constant;
import com.hosea.messagerelayer.service.SmsService;
import com.hosea.messagerelayer.utils.FormatMobile;
import com.hosea.messagerelayer.utils.NativeDataManager;
import com.hosea.messagerelayer.utils.db.DataBaseManager;

import java.util.ArrayList;

public class MessageReceiver extends BroadcastReceiver {

    private NativeDataManager mNativeDataManager;

    public MessageReceiver() {

    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Toast.makeText(context, "收到消息", Toast.LENGTH_SHORT).show();
        Log.i("MessageReceiver", "收到消息: ");
        this.mNativeDataManager = new NativeDataManager(context);
        DataBaseManager dataBaseManager = new DataBaseManager(context);
        ArrayList<SmsBean> smsIntercept = dataBaseManager.getSmsIntercept();
        if (mNativeDataManager.getReceiver()) {
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                Object[] pdus = (Object[]) bundle.get("pdus");
                String content = "";
                SmsMessage sms = null;
                String mobile = "";
                //这里把他原来的拼上了.应该不会有其他问题..
                for (int i = 0; i < pdus.length; i++) {
                    sms = SmsMessage.createFromPdu((byte[]) pdus[i]);
                    content += sms.getMessageBody();
                    mobile = sms.getOriginatingAddress();
                }
                Log.i("MessageReceiver", "mobile: " + mobile);
                for (int i = 0; i < smsIntercept.size(); i++) {
                    Log.i("MessageReceiver", "smsIntercept.get(" + i + ").getPhone()" + smsIntercept.get(i).getPhone());
                    if (smsIntercept.get(i).getPhone().equals(mobile)) {
                        //黑名单短信
                        Log.i("MessageReceiver", "intercept---->" + mobile);
                        return;
                    }
                }
                if (FormatMobile.hasPrefix(mobile)) {
                    mobile = FormatMobile.formatMobile(mobile);
                }
                startSmsService(context, mobile, content);
            }
        }
    }

    private ComponentName startSmsService(final Context context, String mobile, String content) {


//        String mobile = sms.getOriginatingAddress();//发送短信的手机号码

        if (FormatMobile.hasPrefix(mobile)) {
            mobile = FormatMobile.formatMobile(mobile);
        }
//        String content = sms.getMessageBody();//短信内容

        Intent serviceIntent = new Intent(context, SmsService.class);
        serviceIntent.putExtra(Constant.EXTRA_MESSAGE_CONTENT, content);
        serviceIntent.putExtra(Constant.EXTRA_MESSAGE_MOBILE, mobile);

        return context.startService(serviceIntent);
    }


}
