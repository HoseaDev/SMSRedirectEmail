package com.hosea.messagerelayer.activity;

import android.Manifest;
import android.app.role.RoleManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.provider.Telephony;
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

    private static final int REQUEST_DEFAULT_SMS = 2001;

    private LinearLayout mItemDefaultSms;
    private ImageView mIconDefaultSms;
    private TextView mStatusDefaultSms;
    private TextView mActionDefaultSms;

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
        mItemDefaultSms = findViewById(R.id.item_default_sms);
        mIconDefaultSms = findViewById(R.id.icon_default_sms);
        mStatusDefaultSms = findViewById(R.id.status_default_sms);
        mActionDefaultSms = findViewById(R.id.action_default_sms);

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
        mItemDefaultSms.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleDefaultSmsApp();
            }
        });

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
                    openNotificationSettings();
                }
            }
        });
    }

    private void refreshAllStatus() {
        // 默认短信应用
        boolean isDefault = isDefaultSmsApp();
        mIconDefaultSms.setImageResource(isDefault
                ? R.drawable.ic_status_granted : R.drawable.ic_status_denied);
        mStatusDefaultSms.setText(isDefault
                ? R.string.perm_default_sms_granted : R.string.perm_default_sms_denied);
        mActionDefaultSms.setText(isDefault
                ? R.string.perm_default_sms_restore : R.string.perm_default_sms_set);

        // 短信权限
        boolean smsGranted = checkPermission(Manifest.permission.RECEIVE_SMS)
                && checkPermission(Manifest.permission.READ_SMS);
        updateItemStatus(mIconSms, mStatusSms, smsGranted);

        // 电话状态权限
        updateItemStatus(mIconPhone, mStatusPhone,
                checkPermission(Manifest.permission.READ_PHONE_STATE));

        // 联系人权限
        updateItemStatus(mIconContacts, mStatusContacts,
                checkPermission(Manifest.permission.READ_CONTACTS));

        // 发送短信权限
        updateItemStatus(mIconSendSms, mStatusSendSms,
                checkPermission(Manifest.permission.SEND_SMS));

        // 电池优化白名单
        updateItemStatus(mIconBattery, mStatusBattery, isIgnoringBatteryOptimizations());

        // 厂商自启动（无法自动检测）
        mIconAutostart.setImageResource(R.drawable.ic_status_unknown);
        mStatusAutostart.setText(R.string.perm_status_unknown);

        // 通知权限
        updateItemStatus(mIconNotification, mStatusNotification,
                NotificationManagerCompat.from(this).areNotificationsEnabled());
    }

    private boolean isDefaultSmsApp() {
        String defaultPkg = Telephony.Sms.getDefaultSmsPackage(this);
        return getPackageName().equals(defaultPkg);
    }

    private void toggleDefaultSmsApp() {
        if (isDefaultSmsApp()) {
            // 已是默认，引导恢复系统短信
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startActivity(new Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS));
            } else {
                Intent intent = new Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);
                intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME,
                        Telephony.Sms.getDefaultSmsPackage(this));
                startActivity(intent);
            }
        } else {
            // 请求设为默认
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                RoleManager roleManager = (RoleManager) getSystemService(Context.ROLE_SERVICE);
                if (roleManager.isRoleAvailable(RoleManager.ROLE_SMS)) {
                    startActivityForResult(
                            roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS),
                            REQUEST_DEFAULT_SMS);
                }
            } else {
                Intent intent = new Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);
                intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, getPackageName());
                startActivity(intent);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_DEFAULT_SMS) {
            refreshAllStatus();
        }
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
