package com.hl.messagerelayer.utils;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import com.hl.messagerelayer.confing.Constant;
import com.hl.messagerelayer.service.AccessibilitySampleService;

/**
 * Created by heliu on 2018/6/29.
 */

public class WeChatRelayerManager {
    /**
     * 发信到微信.
     *
     * @param dataManager
     * @param content     短信内容
     */


    public static void jumpWeChat(Context context, String content) {
        PackageManager packageManager = context.getPackageManager();
        Intent it = packageManager.getLaunchIntentForPackage("com.tencent.mm");
        context.startActivity(it);
        Intent intent = new Intent();
        intent.setAction(AccessibilitySampleService.WECHAT_ACTION);
        intent.putExtra(Constant.WECHAT_CONTENT, content);
        context.sendBroadcast(intent);
    }
}
