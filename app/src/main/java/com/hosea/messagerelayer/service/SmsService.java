package com.hosea.messagerelayer.service;

import android.Manifest;
import android.app.IntentService;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import com.blankj.utilcode.util.LogUtils;
import com.hosea.messagerelayer.bean.Contact;
import com.hosea.messagerelayer.confing.Constant;
import com.hosea.messagerelayer.utils.ContactManager;
import com.hosea.messagerelayer.utils.EmailRelayerManager;
import com.hosea.messagerelayer.utils.NativeDataManager;
import com.hosea.messagerelayer.utils.SmsRelayerManager;
import com.hosea.messagerelayer.utils.db.DataBaseManager;

import java.util.ArrayList;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SmsService extends IntentService {

    private NativeDataManager mNativeDataManager;
    private DataBaseManager mDataBaseManager;

    public SmsService() {
        super("SmsService");
    }

    public SmsService(String name) {
        super(name);
    }

    private int subId = 0;

    @Override
    protected void onHandleIntent(Intent intent) {
        mNativeDataManager = new NativeDataManager(this);
        mDataBaseManager = new DataBaseManager(this);

        String mobile = intent.getStringExtra(Constant.EXTRA_MESSAGE_MOBILE);
        String content = intent.getStringExtra(Constant.EXTRA_MESSAGE_CONTENT);
        subId = intent.getIntExtra(Constant.EXTRA_MESSAGE_RECEIVED_MOBILE_SUBID, -1);
        Set<String> keySet = mNativeDataManager.getKeywordSet();
        ArrayList<Contact> contactList = mDataBaseManager.getAllContact();
        //无转发规则
        if (keySet.size() == 0 && contactList.size() == 0) {
            relayMessage(content, mobile);
        } else if (keySet.size() != 0 && contactList.size() == 0) {//仅有关键字规则
            for (String key : keySet) {
                if (content.contains(key)) {
                    relayMessage(content, mobile);
                    break;
                }
            }
        } else if (keySet.size() == 0 && contactList.size() != 0) {//仅有手机号规则
            for (Contact contact : contactList) {
                if (contact.getContactNum().equals(mobile)) {
                    relayMessage(content, mobile);
                    break;
                }
            }
        } else {//两种规则共存
            out:
            for (Contact contact : contactList) {
                if (contact.getContactNum().equals(mobile)) {
                    for (String key : keySet) {
                        if (content.contains(key)) {
                            relayMessage(content, mobile);
                            break out;
                        }
                    }
                }
            }
        }
    }

    private void relayMessage(final String content, final String mobile) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                SubscriptionManager subscriptionManager = SubscriptionManager.from(SmsService.this);
                if (ActivityCompat.checkSelfPermission(SmsService.this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                SubscriptionInfo subscriptionInfo = subscriptionManager.getActiveSubscriptionInfo(subId);
                String receivedMobile = "";
                if (subscriptionInfo != null) {
                    // 获取SIM卡的详细信息
                    CharSequence carrierName = subscriptionInfo.getCarrierName();
                    receivedMobile = subscriptionInfo.getNumber();
                    LogUtils.i(" carrierName " + carrierName + " number " + receivedMobile);
                    // 使用这些信息来判断是哪个SIM卡
                    if (receivedMobile.isEmpty()) {
                        Toast.makeText(SmsService.this, "无法获取到手机号，请确实手机信息权限", Toast.LENGTH_LONG).show();
                    }
                }


                String dContent = content;
                String suffix = mNativeDataManager.getContentSuffix();
                String prefix = mNativeDataManager.getContentPrefix();
                if (suffix != null) {
                    dContent = dContent + suffix;
                }
                if (prefix != null) {
                    dContent = prefix + dContent;
                }
                ArrayList<Contact> mContactList = ContactManager.getContactList(SmsService.this);
                for (int i = 0; i < mContactList.size(); i++) {
                    if (mContactList.get(i).getContactNum().equals(mobile)) {
                        LogUtils.i("找到了备注:" + mContactList.get(i).getContactNum());
                        LogUtils.i("找到了备注:" + mContactList.get(i).getContactName());
                        dContent = "联系人: " + mContactList.get(i).getContactName() + "\n" +
                                "发送号码: " + mContactList.get(i).getContactNum() + "\n" + dContent;
                    }
                }
                String extractCode = extractCode(dContent);
                if (!dContent.contains("联系人:")) {
                    dContent = "发送号码: " + mobile + "\n" + dContent;
                    //+86XXXXXXXXXXX
//                    dContent = receivedMobile.substring(10) + "->" + mobile + "\n" + dContent;

                }
                LogUtils.i("最终转发出的内容:" + dContent);
//                if (mNativeDataManager.getSmsRelay()) {
//                    SmsRelayerManager.relaySms(SmsService.this, mNativeDataManager.getObjectMobile(), dContent, mNativeDataManager.getSimIndex());
//                }
                if (mNativeDataManager.getEmailRelay()) {
                    dContent = dContent.replace("\n", "<br>");
                    LogUtils.i("\n换成br =>" + dContent);

                    String tail = getSimTail(receivedMobile);
                    String title = (extractCode == null)
                            ? "尾号:" + tail
                            : "尾号:" + tail + "->验证码:" + extractCode;

                    LogUtils.i("准备发送邮件:", title, dContent);
                    EmailRelayerManager.relayEmail(mNativeDataManager, title, dContent);
                }
                LogUtils.i("mobile=>" + mobile);
                if (mNativeDataManager.getInnerRelay() && mobile.equals(mNativeDataManager.getInnerMobile())) {

                    int sIndex = content.indexOf(mNativeDataManager.getInnerRule());
                    if (sIndex != -1) {
                        LogUtils.i("转发内部短信");
                        String transferPhone = content.substring(0, sIndex);
                        String transferContent = content.substring(sIndex + 1);
                        //这里simIndex卡1是0卡2是1
                        SmsRelayerManager.relaySms(SmsService.this, transferPhone, transferContent, subId);
                    }

                }

//                if (mNativeDataManager.getWeChatRelay()) {
//                    //这里如果要做好需要自己做一些判断..比如当前显示的是哪个界面.
//                    //微信就不用管.其他界面就给提示.不能处理或者手动跳转处理.
//                    //我这边用的测试机一直在聊天界面这里就先不处理这个东西了.
//                     LogUtils.i("relayMessage: " + dContent);
//                    WeChatRelayerManager.jumpWeChat(getBaseContext(), dContent);
//                }
            }
        }).start();


    }

    @Override
    public void onDestroy() {
        mDataBaseManager.closeHelper();
        super.onDestroy();
    }

    public static String extractCode(String content) {
        // 正则表达式匹配4位数字验证码
        if (content.contains("码") || content.contains("code")) {
            String regex = "(\\d{4,6})";
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(content);

            if (matcher.find()) {
                // 返回第一个匹配的验证码
                return matcher.group();
            }
            // 如果没有找到匹配的，返回null
            return null;
        } else {
            return null;
        }
    }

    /**
     * 取 SIM 号码尾号（默认后4位）；当无法获取到号码时返回一个可读占位，避免崩溃
     */
    private String getSimTail(String number) {
        if (number == null) {
            return fallbackSimTag();
        }
        // 仅保留数字（去掉+86、空格、短横等）
        String digits = number.replaceAll("\\D+", "");
        if (digits.isEmpty()) {
            return fallbackSimTag();
        }
        // 后4位；长度不足4则全量返回
        int len = digits.length();
        return (len <= 4) ? digits : digits.substring(len - 4);
    }

    private String fallbackSimTag() {
        // subId 为 subscriptionId；给出“卡1/卡2”之类的可读占位，避免 "尾号:" 空串
        if (subId >= 0) {
            return "卡" + (subId + 1);
        }
        return "未知";
    }

}
