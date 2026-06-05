package com.hosea.messagerelayer.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * WAP_PUSH_DELIVER 空壳接收器。
 * Android 要求默认短信应用必须声明此组件，但本应用不处理彩信。
 */
public class MmsReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // 空实现，仅满足默认短信应用的系统要求
    }
}
