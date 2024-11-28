package com.hosea.messagerelayer.receiver;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import com.blankj.utilcode.util.LogUtils;
import com.hosea.messagerelayer.bean.SmsBean;
import com.hosea.messagerelayer.confing.Constant;
import com.hosea.messagerelayer.service.SmsService;
import com.hosea.messagerelayer.utils.FormatMobile;
import com.hosea.messagerelayer.utils.NativeDataManager;
import com.hosea.messagerelayer.utils.db.DataBaseManager;

import java.util.ArrayList;

public class BatterReceiver extends BroadcastReceiver {
    private NativeDataManager mNativeDataManager;

    public BatterReceiver() {

    }

    @Override
    public void onReceive(Context context, Intent intent) {

//        LogUtils.i("MessageReceiver", "收到消息: ");
//        String action = intent.getAction();
//        this.mNativeDataManager = new NativeDataManager(context);
//        if (Intent.ACTION_BATTERY_LOW.equals(action)) {
//            // 处理电池电量低的情况
//            LogUtils.d("BatteryStateReceiver", "Battery is low. Time to save energy.");
//            Toast.makeText(context, "低电量模式", Toast.LENGTH_SHORT).show();
//
//            if (mNativeDataManager.getReceiver()) {
//                MessageReceiver.startSmsService(context, "00000000000", "手机电池电量过低，请及时充电.", 0);
//            }
//
//        } else if (Intent.ACTION_BATTERY_OKAY.equals(action)) {
//            Toast.makeText(context, "电池电量正常", Toast.LENGTH_SHORT).show();
//            // 处理电池电量恢复的情况
//            LogUtils.d("BatteryStateReceiver", "Battery is okay now. Back to normal operations.");
//            if (mNativeDataManager.getReceiver()) {
//                MessageReceiver.startSmsService(context, "00000000000", "手机电量正常", 0);
//            }
//
//        }


    }


}
