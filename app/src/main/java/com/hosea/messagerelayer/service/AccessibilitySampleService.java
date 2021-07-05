package com.hosea.messagerelayer.service;

import android.accessibilityservice.AccessibilityService;
import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.hosea.messagerelayer.confing.Constant;
import com.hosea.messagerelayer.utils.LogUtil;
import com.hosea.messagerelayer.utils.NativeDataManager;

import java.util.List;


/**
 * Created by Edward on 2018-01-30.
 */
@TargetApi(18)
public class AccessibilitySampleService extends AccessibilityService {
    private final int TEMP = 2000;
    public static final String WECHAT_ACTION = "wechat:com.hl.messagerelayer.service.AccessibilitySampleService";
    private MyBroadcastReceiver myReceiver;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        flag = false;
        LogUtil.e("成功连上");


        IntentFilter intentFilter = new IntentFilter();

        intentFilter.addAction(WECHAT_ACTION);
        if (dataManager == null) {
            dataManager = new NativeDataManager(this);
        }
        myReceiver = new MyBroadcastReceiver();
        registerReceiver(myReceiver, intentFilter);

    }

    private class MyBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            LogUtil.e("接收到:" + intent.getStringExtra(Constant.WECHAT_CONTENT));
            if (accessibilityNodeInfo == null) {
                return;
            }
            List<AccessibilityNodeInfo> edit = accessibilityNodeInfo.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/ac8");
            if (edit.size() == 0) {
                return;
            }
            pasteContent(edit.get(0), intent.getStringExtra(Constant.WECHAT_CONTENT));
            sendChatMsg();
        }
    }

    private AccessibilityNodeInfo accessibilityNodeInfo;

    /**
     * 是否已经发送过朋友圈，true已经发送，false还未发送
     */
    public static boolean flag = false;
    public static boolean isMsg = true;
    NativeDataManager dataManager;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        int eventType = event.getEventType();
        LogUtil.e(eventType + "             " + Integer.toHexString(eventType) + "         " + event.getClassName());
        accessibilityNodeInfo = getRootInActiveWindow();
        switch (eventType) {
            case AccessibilityEvent.TYPE_VIEW_SCROLLED:
//                if (!flag && event.getClassName().equals("android.widget.ListView")) {
//                    clickCircleOfFriendsBtn();//点击发送朋友圈按钮
//                }

//                break;
            case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
//                if (isMsg)
//                    clickListMyRecivedName();
//                if (event.getClassName().equals("com.tencent.mm.ui.LauncherUI")) {//第一次启动app
//                    flag = false;
//                    jumpToCircleOfFriends();//进入朋友圈页面
//                }
//
//
//                clickToCamera();
//
//
//                if (!flag && event.getClassName().equals("com.tencent.mm.plugin.sns.ui.SnsUploadUI")) {
//                    String content = sharedPreferences.getString(Constant.CONTENT, "");
//                    inputContentFinish(content);//写入要发送的朋友圈内容
//                }
//
//                if (!flag && event.getClassName().equals("com.tencent.mm.plugin.gallery.ui.AlbumPreviewUI")) {
//                    new Handler().postDelayed(new Runnable() {
//                        @Override
//                        public void run() {
//                            if (sharedPreferences != null) {
//                                int index = sharedPreferences.getInt(Constant.INDEX, 0);
//                                int count = sharedPreferences.getInt(Constant.COUNT, 0);
//                                choosePicture(index, count);
//                            }
//                        }
//                    }, TEMP);
//                }

                break;
        }
    }

    /**
     * 跳进朋友圈
     */
    private void jumpToCircleOfFriends() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                List<AccessibilityNodeInfo> list = accessibilityNodeInfo.findAccessibilityNodeInfosByText("朋友圈");
                if (list != null && list.size() != 0) {
                    AccessibilityNodeInfo tempInfo = list.get(0);
                    if (tempInfo != null && tempInfo.getParent() != null) {
                        tempInfo.getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    }
                }
            }
        }, TEMP);
    }

    /**
     * 点击右上角的拍照
     */
    private void clickToCamera() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                List<AccessibilityNodeInfo> list = accessibilityNodeInfo.findAccessibilityNodeInfosByText("拍照分享");
                if (list != null && list.size() != 0) {
                    AccessibilityNodeInfo tempInfo = list.get(0);
                    if (tempInfo != null && tempInfo.getParent() != null) {
                        tempInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    }
                }
            }
        }, TEMP);
    }


    /**
     * 点击列表中自己的名字
     */
    private void clickListMyRecivedName() {
//        new Handler().postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                List<AccessibilityNodeInfo> listivew = accessibilityNodeInfo.findAccessibilityNodeInfosByText("鸡翅膀");
//
//                if (listivew.size() > 0) {
//                    List<AccessibilityNodeInfo> child = listivew.get(0).findAccessibilityNodeInfosByText("鸡翅膀");
//                    if (child.size()>0){
//                        LogUtil.e("child:"+child.get(0));
//                        LogUtil.e("child:"+child.get(0).getParent());
//                        LogUtil.e("child:"+child.get(0).getViewIdResourceName());
//                        LogUtil.e("child:"+child.get(0).getChildCount());
//                        LogUtil.e("child:"+child.get(0).getClassName());
//                        LogUtil.e("child:"+child.get(0).getContentDescription());
//                        LogUtil.e("child:"+child.get(0).getLabeledBy());
//                        LogUtil.e("child:"+child.get(0).getLabelFor());
//                        LogUtil.e("child:"+child.get(0).getWindowId());
//                        child.get(0).performAction(AccessibilityNodeInfo.ACTION_CLICK);
//                    }
//                }
//            }
//        }, TEMP);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                List<AccessibilityNodeInfo> list = accessibilityNodeInfo.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/as6");

                for (int i = 0; i < list.size(); i++) {
                    LogUtil.e("--->" + list.get(i).getText() + "");
                    if (list.get(i).getText().toString().equals("逝去")) {
                        LogUtil.e("--->click");
                        list.get(i).getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        try {
                            Thread.sleep(1000);
                            List<AccessibilityNodeInfo> edit = accessibilityNodeInfo.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/ac8");
                            pasteContent(edit.get(0), "hahaha");
                            sendChatMsg();
                            //这里先一条一条的测试.
                            isMsg = false;
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
//                if (list.size() > 0) {
//                    LogUtil.e(list.size() + "");
//                    for (int i = 0; i < list.get(0).getChildCount(); i++) {
//                        LogUtil.e(list.get(0).getChild(i).getContentDescription() + "");
//                    }
//                    LogUtil.e(list.get(0).getChild(0).findAccessibilityNodeInfosByText("鸡翅膀").get(0).getContentDescription() + "");
//                    LogUtil.e("--->: " + list.get(0).getChild(0));
//                    LogUtil.e("--->: " + list.get(0).getChild(0).getChildCount());
//                } else {
//                    LogUtil.e("节点小于0");
//                }

//                for (int i = 0; i < accessibilityNodeInfo.getChild(0).getChildCount(); i++) {
//                    LogUtil.e("child object--->: " + accessibilityNodeInfo.getChild(0).getChild(i));
//                    LogUtil.e("child--->: " + accessibilityNodeInfo.getChild(0).getChild(i).getContentDescription());
//                }
//                if (list.size() >= 2) {
//                    AccessibilityNodeInfo tempInfo = list.get(1);
//                    if (tempInfo != null) {
//                        tempInfo.getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK);
//                    }
//                }
            }
        }, TEMP);
    }


    /**
     * 粘贴文本
     *
     * @param tempInfo
     * @param contentStr
     * @return true 粘贴成功，false 失败
     */
    private boolean pasteContent(AccessibilityNodeInfo tempInfo, String contentStr) {
        if (tempInfo == null) {
            return false;
        }
        if (tempInfo.isEnabled() && tempInfo.isClickable() && tempInfo.isFocusable()) {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("text", contentStr);
            if (clipboard == null) {
                return false;
            }
            clipboard.setPrimaryClip(clip);
            tempInfo.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
            tempInfo.performAction(AccessibilityNodeInfo.ACTION_PASTE);
            return true;
        }
        return false;
    }

    private boolean sendMsg() {
        List<AccessibilityNodeInfo> list = accessibilityNodeInfo.findAccessibilityNodeInfosByText("发表");//微信6.6.6版本修改为发表
        if (performClickBtn(list)) {
            flag = true;//标记为已发送
            return true;
        }
        return false;
    }

    /**
     * 写入朋友圈内容
     *
     * @param contentStr
     */
    private void inputContentFinish(final String contentStr) {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (accessibilityNodeInfo == null) {
                    return;
                }
                List<AccessibilityNodeInfo> nodeInfoList = accessibilityNodeInfo.findAccessibilityNodeInfosByText("添加照片按钮");
                if (nodeInfoList == null ||
                        nodeInfoList.size() == 0 ||
                        nodeInfoList.get(0) == null ||
                        nodeInfoList.get(0).getParent() == null ||
                        nodeInfoList.get(0).getParent().getParent() == null ||
                        nodeInfoList.get(0).getParent().getParent().getParent() == null ||
                        nodeInfoList.get(0).getParent().getParent().getParent().getChildCount() == 0) {
                    return;
                }
                AccessibilityNodeInfo tempInfo = nodeInfoList.get(0).getParent().getParent().getParent().getChild(1);//微信6.6.6
                if (pasteContent(tempInfo, contentStr)) {
                    sendMsg();
                }
            }
        }, TEMP);
    }


    private void sendChatMsg() {
        List<AccessibilityNodeInfo> list = accessibilityNodeInfo.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/acd");//微信6.6.6版本修改为发表
        if (performClickBtn(list)) {
            LogUtil.e("成功");
        } else {
            LogUtil.e("失败");
        }

    }

    /**
     * @param accessibilityNodeInfoList
     * @return
     */
    private boolean performClickBtn(List<AccessibilityNodeInfo> accessibilityNodeInfoList) {
        if (accessibilityNodeInfoList != null && accessibilityNodeInfoList.size() != 0) {
            for (int i = 0; i < accessibilityNodeInfoList.size(); i++) {
                AccessibilityNodeInfo accessibilityNodeInfo = accessibilityNodeInfoList.get(i);
                if (accessibilityNodeInfo != null) {
                    if (accessibilityNodeInfo.isClickable() && accessibilityNodeInfo.isEnabled()) {
                        accessibilityNodeInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * 选择图片
     *
     * @param startPicIndex 从第startPicIndex张开始选
     * @param picCount      总共选picCount张
     */
    private void choosePicture(final int startPicIndex, final int picCount) {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (accessibilityNodeInfo == null) {
                    return;
                }
                List<AccessibilityNodeInfo> accessibilityNodeInfoList = accessibilityNodeInfo.findAccessibilityNodeInfosByText("预览");
                if (accessibilityNodeInfoList == null ||
                        accessibilityNodeInfoList.size() == 0 ||
                        accessibilityNodeInfoList.get(0).getParent() == null ||
                        accessibilityNodeInfoList.get(0).getParent().getChildCount() == 0) {
                    return;
                }
                AccessibilityNodeInfo tempInfo = accessibilityNodeInfoList.get(0).getParent().getChild(3);

                for (int j = startPicIndex; j < startPicIndex + picCount; j++) {
                    AccessibilityNodeInfo childNodeInfo = tempInfo.getChild(j);
                    if (childNodeInfo != null) {
                        for (int k = 0; k < childNodeInfo.getChildCount(); k++) {
                            if (childNodeInfo.getChild(k).isEnabled() && childNodeInfo.getChild(k).isClickable()) {
                                childNodeInfo.getChild(k).performAction(AccessibilityNodeInfo.ACTION_CLICK);//选中图片
                            }
                        }
                    }
                }

                List<AccessibilityNodeInfo> finishList = accessibilityNodeInfo.findAccessibilityNodeInfosByText("完成(" + picCount + "/9)");//点击确定
                performClickBtn(finishList);
            }
        }, TEMP);
    }


    /**
     * 点击发送朋友圈按钮
     */
    private void clickCircleOfFriendsBtn() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (accessibilityNodeInfo == null) {
                    return;
                }

                List<AccessibilityNodeInfo> accessibilityNodeInfoList = accessibilityNodeInfo.findAccessibilityNodeInfosByText("更多功能按钮");
                performClickBtn(accessibilityNodeInfoList);
                openAlbum();
            }
        }, TEMP);
    }


    /**
     * 打开相册
     */
    private void openAlbum() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (accessibilityNodeInfo == null) {
                    return;
                }

                List<AccessibilityNodeInfo> accessibilityNodeInfoList = accessibilityNodeInfo.findAccessibilityNodeInfosByText("从相册选择");
                traverseNode(accessibilityNodeInfoList);
            }
        }, TEMP);
    }

    private boolean traverseNode(List<AccessibilityNodeInfo> accessibilityNodeInfoList) {
        if (accessibilityNodeInfoList != null && accessibilityNodeInfoList.size() != 0) {
            AccessibilityNodeInfo accessibilityNodeInfo = accessibilityNodeInfoList.get(0).getParent();
            if (accessibilityNodeInfo != null && accessibilityNodeInfo.getChildCount() != 0) {
                accessibilityNodeInfo = accessibilityNodeInfo.getChild(0);
                if (accessibilityNodeInfo != null) {
                    accessibilityNodeInfo = accessibilityNodeInfo.getParent();
                    if (accessibilityNodeInfo != null) {
                        accessibilityNodeInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK);//点击从相册中选择
                        return true;
                    }
                }
            }
        }
        return false;
    }


    @Override
    public void onInterrupt() {

    }


    /**
     * Called by the system to notify a Service that it is no longer used and is being removed.  The
     * service should clean up any resources it holds (threads, registered
     * receivers, etc) at this point.  Upon return, there will be no more calls
     * in to this Service object and it is effectively dead.  Do not call this method directly.
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        LogUtil.e("服务被杀死!");
        unregisterReceiver(myReceiver);
    }
}
