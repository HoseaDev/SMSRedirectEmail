package com.hosea.messagerelayer.activity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.hosea.messagerelayer.R;
import com.hosea.messagerelayer.listener.ICustomCompletedListener;
import com.hosea.messagerelayer.service.ForegroundService;
import com.hosea.messagerelayer.utils.BackgroundSettingsHelper;
import com.hosea.messagerelayer.utils.NativeDataManager;

import com.yanzhenjie.permission.Permission;

public class MainActivity extends BaseActivity implements View.OnClickListener {

    private RelativeLayout mSmsLayout, mEmailLayout, mRuleLayout, mPermissionLayout;
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
                Intent serviceIntent = new Intent(MainActivity.this, ForegroundService.class);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent);
                } else {
                    startService(serviceIntent);
                }
                // 引导用户关闭电池优化
                requestIgnoreBatteryOptimization();
                // 引导用户开启厂商自启动/后台运行设置
                showBackgroundGuideIfNeeded();
            }

            @Override
            public void failed(String msg) {
                Toast.makeText(MainActivity.this, "不给权限没法完...再见!", Toast.LENGTH_LONG).show();
                finish();
            }
        }, Permission.READ_SMS, Permission.RECEIVE_SMS, Permission.READ_CONTACTS, Permission.READ_PHONE_STATE, Permission.SEND_SMS);


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
        mPermissionLayout = (RelativeLayout) findViewById(R.id.permission_guide_layout);

        mSmsLayout.setOnClickListener(this);
        mEmailLayout.setOnClickListener(this);
        mRuleLayout.setOnClickListener(this);
        mPermissionLayout.setOnClickListener(this);
    }

    /**
     * 主动请求忽略电池优化，弹出系统对话框让用户一键允许
     */
    private void requestIgnoreBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (!pm.isIgnoringBatteryOptimizations(getPackageName())) {
                Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            }
        }
    }

    /**
     * 首次启动时弹窗引导用户设置厂商自启动/后台运行权限
     */
    private void showBackgroundGuideIfNeeded() {
        if (BackgroundSettingsHelper.hasGuided(this)) {
            return;
        }

        String message = BackgroundSettingsHelper.getGuideMessage();
        new AlertDialog.Builder(this)
                .setTitle("重要：后台保活设置")
                .setMessage(message)
                .setPositiveButton("去设置", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        BackgroundSettingsHelper.markGuided(MainActivity.this);
                        BackgroundSettingsHelper.openAutoStartSettings(MainActivity.this);
                    }
                })
                .setNegativeButton("已设置，不再提示", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        BackgroundSettingsHelper.markGuided(MainActivity.this);
                    }
                })
                .setCancelable(false)
                .show();
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
            case R.id.permission_guide_layout:
                startActivity(new Intent(this, PermissionGuideActivity.class));
                break;
        }
    }


}
