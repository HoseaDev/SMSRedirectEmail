package com.hosea.messagerelayer.activity;

import android.content.DialogInterface;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;

import com.hosea.messagerelayer.R;
import com.hosea.messagerelayer.listener.ICustomCompletedListener;
import com.hosea.messagerelayer.utils.NativeDataManager;

import com.yanzhenjie.permission.Permission;


public class SmsRelayerActivity extends BaseActivity
        implements CompoundButton.OnCheckedChangeListener, View.OnClickListener {

    private Switch mSmsSwitchSelf;
    private RelativeLayout mMobileRelativeSelf, mMobileRelativeRule;
    private TextView mMobileTextSelf, mMobileTextRule;

    private NativeDataManager mNativeDataManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sms_relayer);
        initActionbar();

        init();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    private void initActionbar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
    }

    private void init() {
        mNativeDataManager = new NativeDataManager(this);

        initView();
        initData();
        initListener();

    }

    private void initView() {
//        mSmsSwitch = (Switch) findViewById(R.id.switch_sms);
//        mMobileRelative = (RelativeLayout) findViewById(R.id.layout_mobile);
//        mMobileText = (TextView) findViewById(R.id.text_mobile);


        mSmsSwitchSelf = (Switch) findViewById(R.id.switch_self);
        mMobileRelativeSelf = (RelativeLayout) findViewById(R.id.layout_self);
        mMobileTextSelf = (TextView) findViewById(R.id.text_self);

        mMobileRelativeRule = (RelativeLayout) findViewById(R.id.layout_rule);
        mMobileTextRule = (TextView) findViewById(R.id.text_rule);
    }

    private void initData() {
//        if (mNativeDataManager.getSmsRelay()) {
//            mSmsSwitch.setChecked(true);
//        } else {
//            mSmsSwitch.setChecked(false);
//        }

        if (mNativeDataManager.getInnerRelay()) {
            mSmsSwitchSelf.setChecked(true);
        } else {
            mSmsSwitchSelf.setChecked(false);
        }
//        mMobileText.setText(mNativeDataManager.getObjectMobile());
        mMobileTextSelf.setText(mNativeDataManager.getInnerMobile());
        mMobileTextRule.setText(mNativeDataManager.getInnerRule());
    }

    private void initListener() {
//        mSmsSwitch.setOnCheckedChangeListener(this);

//        mMobileRelative.setOnClickListener(this);
        mSmsSwitchSelf.setOnCheckedChangeListener(this);

        mMobileRelativeSelf.setOnClickListener(this);
        mMobileRelativeRule.setOnClickListener(this);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, final boolean isChecked) {
        switch (buttonView.getId()) {
            case R.id.switch_self:
                requestPermission(new ICustomCompletedListener() {
                    @Override
                    public void success() {
                        if (mNativeDataManager.getSendSMSHint()) {
                            Toast.makeText(SmsRelayerActivity.this, "发送短信权限比较特殊.部分手机可能会再下一次开启软件的时候再次请求.如遇这种情况可以在应用程序中手动给允许权限!", Toast.LENGTH_LONG).show();
                            mNativeDataManager.setSendSMSHint(false);
                        }
                        if (buttonView.getId() == R.id.switch_self) {
                            InnerSmsChecked(isChecked);
                        } else {
                            smsChecked(isChecked);
                        }
                    }

                    @Override
                    public void failed(String msg) {

                    }
                }, Permission.SEND_SMS);
                break;
        }
    }

    /**
     * 使用短信转发至指定手机号的Switch的事件方法
     *
     * @param isChecked
     */
    private void smsChecked(boolean isChecked) {
        if (isChecked) {
            mNativeDataManager.setSmsRelay(true);
        } else {
            mNativeDataManager.setSmsRelay(false);
        }
    }

    /**
     * 使用短信转发至指定手机号的Switch的事件方法
     *
     * @param isChecked
     */
    private void InnerSmsChecked(boolean isChecked) {
        if (isChecked) {
            mNativeDataManager.setInnerRelay(true);
        } else {
            mNativeDataManager.setInnerRelay(false);
        }
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
//            case R.id.layout_mobile:
//                showEditDialog(v);
//                break;
            case R.id.layout_self:
                showEditDialog(v);
                break;
            case R.id.layout_rule:
                showEditDialog(v);
                break;
        }
    }

    private void showEditDialog(View v) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_edit, null, false);
        final EditText mobileEdit = (EditText) view.findViewById(R.id.dialog_edit);
//        String mobileText = mMobileText.getText().toString();
//        if (!mobileText.equals("点击设置")) {
//            mobileEdit.setText(mobileText);
//        }

        builder.setView(view);
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (v.getId() == R.id.layout_self) {
                    mNativeDataManager.setInnerMobile(mobileEdit.getText().toString());
                    mMobileTextSelf.setText(mobileEdit.getText());
                }else {
                    mNativeDataManager.setInnerRule(mobileEdit.getText().toString());
                    mMobileTextRule.setText(mobileEdit.getText());
                }

            }
        });
        builder.show();
    }
}
