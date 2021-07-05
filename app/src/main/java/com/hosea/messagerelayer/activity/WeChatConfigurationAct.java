//package com.hl.messagerelayer.activity;
//
//import android.content.Intent;
//import android.content.pm.PackageManager;
//import android.os.Bundle;
//
//import android.view.View;
//import android.widget.CompoundButton;
//import android.widget.Switch;
//
//import androidx.appcompat.app.AppCompatActivity;
//
//import com.hl.messagerelayer.R;
//import com.hl.messagerelayer.utils.NativeDataManager;
//import com.hl.messagerelayer.utils.OpenAccessibilitySettingHelper;
//
///**
// * Created by heliu on 2018/6/29.
// */
//
//public class WeChatConfigurationAct extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener {
//    //    EditText edit;
//    Switch mWeChatSwith;
//    NativeDataManager mNativeDataManager;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activitiy_wechat);
//        mNativeDataManager = new NativeDataManager(this);
//        initView();
//
//    }
//
//    private void initView() {
////        edit = (EditText) findViewById(R.id.edit);
//        mWeChatSwith = (Switch) findViewById(R.id.switch_wechat);
//        findViewById(R.id.open_accessibility_setting).setOnClickListener(clickListener);
//        findViewById(R.id.btn_save).setOnClickListener(clickListener);
//
//        if (mNativeDataManager.getWeChatRelay()) {
//            mWeChatSwith.setChecked(true);
//        } else {
//            mWeChatSwith.setChecked(false);
//        }
//        mWeChatSwith.setOnCheckedChangeListener(this);
//    }
//
//    @Override
//    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//        switch (buttonView.getId()) {
//            case R.id.switch_wechat:
//                weChatCheck(isChecked);
//                break;
//        }
//    }
//
//
//    /**
//     * 使用短信转发至指定手机号的Switch的事件方法
//     *
//     * @param isChecked
//     */
//    private void weChatCheck(boolean isChecked) {
//        if (isChecked) {
//            mNativeDataManager.setWeChatRelay(true);
//        } else {
//            mNativeDataManager.setWeChatRelay(false);
//        }
//    }
//
//    View.OnClickListener clickListener = new View.OnClickListener() {
//        @Override
//        public void onClick(View v) {
//            switch (v.getId()) {
//                case R.id.open_accessibility_setting:
//                    OpenAccessibilitySettingHelper.jumpToSettingPage(getBaseContext());
//                    break;
//                case R.id.btn_save:
//                    openWeChatApplication();
//                    break;
//            }
//        }
//    };
//
////    public boolean checkParams() {
////        if (TextUtils.isEmpty(editIndex.getText().toString())) {
////            Toast.makeText(getBaseContext(), "起始下标不能为空", Toast.LENGTH_SHORT).show();
////            return false;
////        }
////
////        if (TextUtils.isEmpty(editCount.getText().toString())) {
////            Toast.makeText(getBaseContext(), "图片总数不能为空", Toast.LENGTH_SHORT).show();
////            return false;
////        }
////
////        if (Integer.valueOf(editCount.getText().toString()) > 9) {
////            Toast.makeText(getBaseContext(), "图片总数不能超过9张", Toast.LENGTH_SHORT).show();
////            return false;
////        }
////
////        return true;
////    }
//
//    private void saveData() {
//
////        if (!checkParams()) {
////            return;
////        }
//
////        int index = Integer.valueOf(editIndex.getText().toString());
////        int count = Integer.valueOf(editCount.getText().toString());
//
////        SharedPreferences sharedPreferences = getSharedPreferences(Constant.WECHAT_STORAGE, Activity.MODE_MULTI_PROCESS);
////        editor.putString(Constant.WECHAT_NAME, edit.getText().toString());
////        editor.putInt(Constant.INDEX, index);
////        editor.putInt(Constant.COUNT, count);
////        if (editor.commit()) {
////            Toast.makeText(getBaseContext(), "保存成功", Toast.LENGTH_LONG).show();
////            openWeChatApplication();//打开微信应用
////        } else {
////            Toast.makeText(getBaseContext(), "保存失败", Toast.LENGTH_LONG).show();
////        }
//    }
//
//    private void openWeChatApplication() {
//        PackageManager packageManager = getBaseContext().getPackageManager();
//        Intent it = packageManager.getLaunchIntentForPackage("com.tencent.mm");
//        startActivity(it);
//    }
//}
