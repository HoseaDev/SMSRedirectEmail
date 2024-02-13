package com.hosea.messagerelayer.utils;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;

import androidx.core.app.ActivityCompat;

import com.hosea.messagerelayer.activity.MainActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by WHF on 2017/3/26.
 */

public class SmsRelayerManager {
//    /**
//     * 发送短信至目标手机号
//     * @param dataManager
//     * @param content      短信内容
//     */
//    public static void relaySms(Context context, NativeDataManager dataManager, String content) {
//        String objectMobile = dataManager.getObjectMobile();
//        android.telephony.SmsManager smsManager = android.telephony.SmsManager.getDefault();
//        if (content.length()>70) {//短信内容大于70字数
//            ArrayList<String> divideContents =smsManager.divideMessage(content);//将短信切分成集合
//            smsManager.sendMultipartTextMessage(objectMobile, null, divideContents, null, null);
//        }else{
//            smsManager.sendTextMessage(objectMobile, null, content, null, null);
//        }
//    }

    /**
     * 发送短信至目标手机号
     *
     * @param dataManager
     * @param content     短信内容
     */
    public static void relaySms(Context context, String mobile, String content, int simSlotIndex) {

//        android.telephony.SmsManager smsManager = android.telephony.SmsManager.getDefault();
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
        List<SubscriptionInfo> subscriptionInfoList = subscriptionManager.getActiveSubscriptionInfoList();

        if (subscriptionInfoList != null && subscriptionInfoList.size() > simSlotIndex) {
            SubscriptionInfo subscriptionInfo = subscriptionInfoList.get(simSlotIndex);
            int subscriptionId = subscriptionInfo.getSubscriptionId();

            SmsManager smsManager = SmsManager.getSmsManagerForSubscriptionId(subscriptionId);
            if (content.length() > 70) {//短信内容大于70字数
                ArrayList<String> divideContents = smsManager.divideMessage(content);//将短信切分成集合
                smsManager.sendMultipartTextMessage(mobile, null, divideContents, null, null);
            } else {
                smsManager.sendTextMessage(mobile, null, content, null, null);
            }
        }


    }


}
