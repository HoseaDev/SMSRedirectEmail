package com.hl.messagerelayer.service;

import android.app.IntentService;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

import com.hl.messagerelayer.activity.ContactListActivity;
import com.hl.messagerelayer.adapter.ContactListAdapter;
import com.hl.messagerelayer.bean.Contact;
import com.hl.messagerelayer.confing.Constant;
import com.hl.messagerelayer.utils.ContactManager;
import com.hl.messagerelayer.utils.EmailRelayerManager;
import com.hl.messagerelayer.utils.LogUtil;
import com.hl.messagerelayer.utils.NativeDataManager;
import com.hl.messagerelayer.utils.SmsRelayerManager;
import com.hl.messagerelayer.utils.WeChatRelayerManager;
import com.hl.messagerelayer.utils.db.DataBaseManager;

import java.util.ArrayList;
import java.util.Set;

public class SmsService extends IntentService {

    private NativeDataManager mNativeDataManager;
    private DataBaseManager mDataBaseManager;

    public SmsService() {
        super("SmsService");
    }

    public SmsService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        mNativeDataManager = new NativeDataManager(this);
        mDataBaseManager = new DataBaseManager(this);

        String mobile = intent.getStringExtra(Constant.EXTRA_MESSAGE_MOBILE);
        String content = intent.getStringExtra(Constant.EXTRA_MESSAGE_CONTENT);
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
                        LogUtil.e("找到了备注:" + mContactList.get(i).getContactNum());
                        LogUtil.e("找到了备注:" + mContactList.get(i).getContactName());
                        dContent = "联系人: " + mContactList.get(i).getContactName() + "\n" +
                                "发送号码: " + mContactList.get(i).getContactNum() + "\n" + dContent;
                    }
                }
                if (!dContent.contains("联系人:")) {
                    dContent = "发送号码: " + mobile + "\n" + dContent;

                }
                LogUtil.e("dContent:" + dContent);
                if (mNativeDataManager.getSmsRelay()) {
                    SmsRelayerManager.relaySms(mNativeDataManager, dContent);
                }
                if (mNativeDataManager.getEmailRelay()) {
                    dContent = dContent.replace("\n", "<br>");
                    LogUtil.e("email=>" + dContent);
                    EmailRelayerManager.relayEmail(mNativeDataManager, dContent);
                }
                if (mNativeDataManager.getWeChatRelay()) {
                    //这里如果要做好需要自己做一些判断..比如当前显示的是哪个界面.
                    //微信就不用管.其他界面就给提示.不能处理或者手动跳转处理.
                    //我这边用的测试机一直在聊天界面这里就先不处理这个东西了.
                    LogUtil.e("relayMessage: " + dContent);
                    WeChatRelayerManager.jumpWeChat(getBaseContext(), dContent);
                }
            }
        }).start();


    }

    @Override
    public void onDestroy() {
        mDataBaseManager.closeHelper();
        super.onDestroy();
    }
}
