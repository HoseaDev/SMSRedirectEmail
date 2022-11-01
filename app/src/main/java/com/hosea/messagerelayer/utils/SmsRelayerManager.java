package com.hosea.messagerelayer.utils;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsManager;

import com.hosea.messagerelayer.activity.MainActivity;

import java.util.ArrayList;

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
     * @param dataManager
     * @param content      短信内容
     */
    public static void relaySms(Context context, String mobile, String content) {

        android.telephony.SmsManager smsManager = android.telephony.SmsManager.getDefault();
        if (content.length()>70) {//短信内容大于70字数
            ArrayList<String> divideContents =smsManager.divideMessage(content);//将短信切分成集合
            smsManager.sendMultipartTextMessage(mobile, null, divideContents, null, null);
        }else{
            smsManager.sendTextMessage(mobile, null, content, null, null);
        }
    }


}
