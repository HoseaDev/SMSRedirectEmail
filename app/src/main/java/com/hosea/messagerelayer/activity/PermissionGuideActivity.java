package com.hosea.messagerelayer.activity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.hosea.messagerelayer.R;
import com.hosea.messagerelayer.listener.ICustomCompletedListener;
import com.hosea.messagerelayer.utils.BackgroundSettingsHelper;
import com.yanzhenjie.permission.Permission;

public class PermissionGuideActivity extends BaseActivity {

    private LinearLayout mItemSms, mItemPhone, mItemContacts, mItemSendSms;
    private LinearLayout mItemBattery, mItemAutostart, mItemNotification;

    private ImageView mIconSms, mIconPhone, mIconContacts, mIconSendSms;
    private ImageView mIconBattery, mIconAutostart, mIconNotification;

    private TextView mStatusSms, mStatusPhone, mStatusContacts, mStatusSendSms;
    private TextView mStatusBattery, mStatusAutostart, mStatusNotification;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permission_guide);

        initViews();
        initClickListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshAllStatus();
    }

    private void initViews() {
        mItemSms = findViewById(R.id.item_sms_permission);
        mItemPhone = findViewById(R.id.item_phone_permission);
        mItemContacts = findViewById(R.id.item_contacts_permission);
        mItemSendSms = findViewById(R.id.item_send_sms_permission);
        mItemBattery = findViewById(R.id.item_battery_optimization);
        mItemAutostart = findViewById(R.id.item_autostart);
        mItemNotification = findViewById(R.id.item_notification);

        mIconSms = findViewById(R.id.icon_sms_permission);
        mIconPhone = findViewById(R.id.icon_phone_permission);
        mIconContacts = findViewById(R.id.icon_contacts_permission);
        mIconSendSms = findViewById(R.id.icon_send_sms_permission);
        mIconBattery = findViewById(R.id.icon_battery_optimization);
        mIconAutostart = findViewById(R.id.icon_autostart);
        mIconNotification = findViewById(R.id.icon_notification);

        mStatusSms = findViewById(R.id.status_sms_permission);
        mStatusPhone = findViewById(R.id.status_phone_permission);
        mStatusContacts = findViewById(R.id.status_contacts_permission);
        mStatusSendSms = findViewById(R.id.status_send_sms_permission);
        mStatusBattery = findViewById(R.id.status_battery_optimization);
        mStatusAutostart = findViewById(R.id.status_autostart);
        mStatusNotification = findViewById(R.id.status_notification);
    }

    private void initClickListeners() {
        mItemSms.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestPermission(new PermissionRefreshListener(),
                        Permission.RECEIVE_SMS, Permission.READ_SMS);
            }
        });

        mItemPhone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestPermission(new PermissionRefreshListener(),
                        Permission.READ_PHONE_STATE);
            }
        });

        mItemContacts.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestPermission(new PermissionRefreshListener(),
                        Permission.READ_CONTACTS);
            }
        });

        mItemSendSms.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestPermission(new PermissionRefreshListener(),
                        Permission.SEND_SMS);
            }
        });

        mItemBattery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestIgnoreBatteryOptimization();
            }
        });

        mItemAutostart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BackgroundSettingsHelper.openAutoStartSettings(PermissionGuideActivity.this);
            }
        });

        mItemNotification.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    requestPermission(new PermissionRefreshListener(),
                            Manifest.permission.POST_NOTIFICATIONS);
                } else {
                    // Android 13 以下跳转到通知设置页
                    openNotificationSettings();
                }
            }
        });
    }

    private void refreshAllStatus() {
        // 短信权限（RECEIVE_SMS + READ_SMS）
        boolean smsGranted = checkPermission(Manifest.permission.RECEIVE_SMS)
                && checkPermission(Manifest.permission.READ_SMS);
        updateItemStatus(mIconSms, mStatusSms, smsGranted);

        // 电话状态权限
        boolean phoneGranted = checkPermission(Manifest.permission.READ_PHONE_STATE);
        updateItemStatus(mIconPhone, mStatusPhone, phoneGranted);

        // 联系人权限
        boolean contactsGranted = checkPermission(Manifest.permission.READ_CONTACTS);
        updateItemStatus(mIconContacts, mStatusContacts, contactsGranted);

        // 发送短信权限
        boolean sendSmsGranted = checkPermission(Manifest.permission.SEND_SMS);
        updateItemStatus(mIconSendSms, mStatusSendSms, sendSmsGranted);

        // 电池优化白名单
        boolean batteryOptimized = isIgnoringBatteryOptimizations();
        updateItemStatus(mIconBattery, mStatusBattery, batteryOptimized);

        // 厂商自启动（无法自动检测）
        mIconAutostart.setImageResource(R.drawable.ic_status_unknown);
        mStatusAutostart.setText(R.string.perm_status_unknown);

        // 通知权限
        boolean notificationEnabled = NotificationManagerCompat.from(this).areNotificationsEnabled();
        updateItemStatus(mIconNotification, mStatusNotification, notificationEnabled);
    }

    private boolean checkPermission(String permission) {
        return ContextCompat.checkSelfPermission(this, permission)
                == android.content.pm.PackageManager.PERMISSION_GRANTED;
    }

    private void updateItemStatus(ImageView icon, TextView status, boolean granted) {
        if (granted) {
            icon.setImageResource(R.drawable.ic_status_granted);
            status.setText(R.string.perm_status_granted);
        } else {
            icon.setImageResource(R.drawable.ic_status_denied);
            status.setText(R.string.perm_status_denied);
        }
    }

    private boolean isIgnoringBatteryOptimizations() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            return pm.isIgnoringBatteryOptimizations(getPackageName());
        }
        return true;
    }

    private void requestIgnoreBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        }
    }

    private void openNotificationSettings() {
        Intent intent = new Intent();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            intent.setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
            intent.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
        } else {
            intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + getPackageName()));
        }
        startActivity(intent);
    }

    private class PermissionRefreshListener implements ICustomCompletedListener {
        @Override
        public void success() {
            refreshAllStatus();
        }

        @Override
        public void failed(String msg) {
            refreshAllStatus();
        }
    }
}
