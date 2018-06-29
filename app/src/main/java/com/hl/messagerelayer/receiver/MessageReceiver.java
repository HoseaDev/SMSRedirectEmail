package com.hl.messagerelayer.receiver;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.Toast;

import com.hl.messagerelayer.activity.ContactListActivity;
import com.hl.messagerelayer.adapter.ContactListAdapter;
import com.hl.messagerelayer.bean.Contact;
import com.hl.messagerelayer.confing.Constant;
import com.hl.messagerelayer.service.SmsService;
import com.hl.messagerelayer.utils.ContactManager;
import com.hl.messagerelayer.utils.FormatMobile;
import com.hl.messagerelayer.utils.LogUtil;
import com.hl.messagerelayer.utils.NativeDataManager;

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
