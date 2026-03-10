package com.hosea.messagerelayer.activity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
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
import com.hosea.messagerelayer.utils.ConfigExportImportManager;
import com.hosea.messagerelayer.utils.NativeDataManager;

import com.yanzhenjie.permission.Permission;

import org.json.JSONObject;

public class MainActivity extends BaseActivity implements View.OnClickListener {

    private static final int REQUEST_IMPORT_CONFIG = 1001;

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

        // 转发日志
        menu.add("转发日志").setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                startActivity(new Intent(MainActivity.this, ForwardingLogActivity.class));
                return true;
            }
        }).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

        // 导出配置
        menu.add("导出配置").setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                doExportConfig();
                return true;
            }
        }).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

        // 导入配置
        menu.add("导入配置").setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                doImportConfig();
                return true;
            }
        }).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

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

    private void doExportConfig() {
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... voids) {
                try {
                    return ConfigExportImportManager.exportToFile(MainActivity.this);
                } catch (Exception e) {
                    return null;
                }
            }

            @Override
            protected void onPostExecute(String fileName) {
                if (fileName != null) {
                    Toast.makeText(MainActivity.this,
                            "配置已导出到 Downloads/" + fileName + "\n注意：文件包含敏感信息，请妥善保管",
                            Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(MainActivity.this, "导出失败", Toast.LENGTH_SHORT).show();
                }
            }
        }.execute();
    }

    private void doImportConfig() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        // 兼容部分文件管理器不识别 json 类型
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"application/json", "text/plain", "*/*"});
        startActivityForResult(intent, REQUEST_IMPORT_CONFIG);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMPORT_CONFIG && resultCode == RESULT_OK && data != null) {
            final Uri uri = data.getData();
            if (uri == null) return;

            new AlertDialog.Builder(this)
                    .setTitle("确认导入")
                    .setMessage("确定要导入配置吗？当前所有配置将被覆盖。")
                    .setPositiveButton("导入", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            performImport(uri);
                        }
                    })
                    .setNegativeButton("取消", null)
                    .show();
        }
    }

    private void performImport(final Uri uri) {
        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... voids) {
                try {
                    JSONObject json = ConfigExportImportManager.readFromUri(MainActivity.this, uri);
                    ConfigExportImportManager.importConfig(MainActivity.this, json);
                    return true;
                } catch (Exception e) {
                    return false;
                }
            }

            @Override
            protected void onPostExecute(Boolean success) {
                if (success) {
                    Toast.makeText(MainActivity.this, "配置导入成功，部分设置可能需要重启应用生效", Toast.LENGTH_LONG).show();
                    // 刷新菜单状态
                    invalidateOptionsMenu();
                } else {
                    Toast.makeText(MainActivity.this, "导入失败，请检查文件格式", Toast.LENGTH_SHORT).show();
                }
            }
        }.execute();
    }

}
