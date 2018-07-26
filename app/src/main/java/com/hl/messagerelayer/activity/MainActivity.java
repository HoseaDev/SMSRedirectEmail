package com.hl.messagerelayer.activity;

import android.Manifest;
import android.content.Intent;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.hl.messagerelayer.R;
import com.hl.messagerelayer.listener.ICustomCompletedListener;
import com.hl.messagerelayer.service.TraceServiceImpl;
import com.hl.messagerelayer.utils.NativeDataManager;
import com.xdandroid.hellodaemon.DaemonEnv;
import com.xdandroid.hellodaemon.IntentWrapper;
import com.yanzhenjie.permission.AndPermission;
import com.yanzhenjie.permission.Permission;
import com.yanzhenjie.permission.Rationale;

import java.util.List;

public class MainActivity extends BaseActivity implements View.OnClickListener {

    private RelativeLayout mSmsLayout, mEmailLayout, mRuleLayout, mWeChatLayout;
    private NativeDataManager mNativeDataManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        mNativeDataManager = new NativeDataManager(this);
        initView();
        requestPermission(new ICustomCompletedListener() {
            @Override
            public void success() {

                TraceServiceImpl.sShouldStopService = false;
                DaemonEnv.startServiceMayBind(TraceServiceImpl.class);
            }

            @Override
            public void failed(String msg) {
                Toast.makeText(MainActivity.this, "不给权限没法完...再见!", Toast.LENGTH_LONG).show();
                finish();
            }
        }, Permission.READ_SMS, Permission.RECEIVE_SMS, Permission.READ_CONTACTS);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Boolean isReceiver = mNativeDataManager.getReceiver();
        final MenuItem menuItem = menu.add("开关");
        if (isReceiver) {
            menuItem.setIcon(R.mipmap.ic_send_on);
        } else {
            menuItem.setIcon(R.mipmap.ic_send_off);
        }

        menuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Boolean receiver = mNativeDataManager.getReceiver();
                if (receiver) {
                    mNativeDataManager.setReceiver(false);
                    menuItem.setIcon(R.mipmap.ic_send_off);
                    Toast.makeText(MainActivity.this, "总闸已关闭", Toast.LENGTH_SHORT).show();
                } else {
                    mNativeDataManager.setReceiver(true);
                    menuItem.setIcon(R.mipmap.ic_send_on);
                    IntentWrapper.whiteListMatters(MainActivity.this, "轨迹跟踪服务的持续运行");
                    Toast.makeText(MainActivity.this, "总闸已开启", Toast.LENGTH_SHORT).show();
                }
                return true;
            }
        }).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

        menu.add("关于").setIcon(R.mipmap.ic_about)
                .setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        startActivity(new Intent(MainActivity.this, AboutActivity.class));
                        return false;
                    }
                }).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

        return super.onCreateOptionsMenu(menu);
    }

    private void initView() {
        mSmsLayout = (RelativeLayout) findViewById(R.id.sms_relay_layout);
        mEmailLayout = (RelativeLayout) findViewById(R.id.email_relay_layout);
        mRuleLayout = (RelativeLayout) findViewById(R.id.rule_layout);
        mWeChatLayout = (RelativeLayout) findViewById(R.id.wechat_relay_layout);

        mSmsLayout.setOnClickListener(this);
        mEmailLayout.setOnClickListener(this);
        mRuleLayout.setOnClickListener(this);
        mWeChatLayout.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.sms_relay_layout:
                startActivity(new Intent(this, SmsRelayerActivity.class));
                break;
            case R.id.email_relay_layout:
                startActivity(new Intent(this, EmailRelayerActivity.class));
                break;
            case R.id.rule_layout:
                startActivity(new Intent(this, RuleActivity.class));
                break;
            case R.id.wechat_relay_layout:
                startActivity(new Intent(this, WeChatConfigurationAct.class));
                break;
        }
    }


}
