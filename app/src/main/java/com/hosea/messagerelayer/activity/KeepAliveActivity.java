package com.hosea.messagerelayer.activity;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import com.hosea.messagerelayer.R;
import com.hosea.messagerelayer.utils.KeepAliveScheduler;
import com.hosea.messagerelayer.utils.NativeDataManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 短信定时保号配置页面。
 * 根据当前插入的 SIM 卡数量，动态显示每张卡的独立配置区块。
 */
public class KeepAliveActivity extends BaseActivity {

    private static final String[] INTERVAL_OPTIONS = {"1天", "3天", "7天", "15天", "30天", "自定义"};
    private static final int[] INTERVAL_DAYS = {1, 3, 7, 15, 30, -1};

    private LinearLayout mContainer;
    private NativeDataManager mDataManager;
    private List<SubscriptionInfo> mSimList = new ArrayList<>();
    private List<SimCardViewHolder> mViewHolders = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_keep_alive);

        mDataManager = new NativeDataManager(this);
        mContainer = findViewById(R.id.container_sim_cards);

        loadSimCards();
    }

    @Override
    protected void onResume() {
        super.onResume();
        for (SimCardViewHolder holder : mViewHolders) {
            refreshTimeDisplay(holder);
        }
    }

    private void loadSimCards() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "需要电话权限才能获取 SIM 卡信息", Toast.LENGTH_SHORT).show();
            return;
        }

        SubscriptionManager subscriptionManager = SubscriptionManager.from(this);
        List<SubscriptionInfo> list = subscriptionManager.getActiveSubscriptionInfoList();

        mSimList.clear();
        mViewHolders.clear();
        mContainer.removeAllViews();

        if (list == null || list.isEmpty()) {
            TextView emptyView = new TextView(this);
            emptyView.setText("未检测到 SIM 卡");
            emptyView.setTextSize(16);
            emptyView.setPadding(0, 32, 0, 32);
            mContainer.addView(emptyView);
            return;
        }

        mSimList.addAll(list);

        for (int i = 0; i < mSimList.size(); i++) {
            SubscriptionInfo info = mSimList.get(i);
            int subId = info.getSubscriptionId();
            String title = "卡" + (i + 1) + " (" + info.getCarrierName() + ")";

            View itemView = LayoutInflater.from(this).inflate(R.layout.item_keep_alive_sim, mContainer, false);
            SimCardViewHolder holder = new SimCardViewHolder(itemView, subId);
            holder.tvSimTitle.setText(title);

            setupHolder(holder);
            restoreConfig(holder);

            mContainer.addView(itemView);
            mViewHolders.add(holder);
        }
    }

    private void setupHolder(final SimCardViewHolder holder) {
        final int subId = holder.subId;

        // 间隔 Spinner
        ArrayAdapter<String> intervalAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, INTERVAL_OPTIONS);
        intervalAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        holder.spinnerInterval.setAdapter(intervalAdapter);

        // 开关
        holder.switchKeepAlive.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (holder.isUpdatingUI) return;

                if (isChecked) {
                    if (!validateConfig(holder)) {
                        holder.isUpdatingUI = true;
                        holder.switchKeepAlive.setChecked(false);
                        holder.isUpdatingUI = false;
                        return;
                    }
                    saveConfig(holder);
                    mDataManager.setKeepAliveEnabled(subId, true);
                    KeepAliveScheduler.scheduleNext(KeepAliveActivity.this, subId);
                    Toast.makeText(KeepAliveActivity.this, holder.tvSimTitle.getText() + " 短信保号已开启", Toast.LENGTH_SHORT).show();
                } else {
                    mDataManager.setKeepAliveEnabled(subId, false);
                    KeepAliveScheduler.cancel(KeepAliveActivity.this, subId);
                    Toast.makeText(KeepAliveActivity.this, holder.tvSimTitle.getText() + " 短信保号已关闭", Toast.LENGTH_SHORT).show();
                }
                refreshTimeDisplay(holder);
            }
        });

        // 间隔选择
        holder.spinnerInterval.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (holder.isUpdatingUI) return;
                if (position < INTERVAL_DAYS.length && INTERVAL_DAYS[position] == -1) {
                    holder.layoutCustomInterval.setVisibility(View.VISIBLE);
                } else {
                    holder.layoutCustomInterval.setVisibility(View.GONE);
                    if (position < INTERVAL_DAYS.length) {
                        mDataManager.setKeepAliveIntervalDays(subId, INTERVAL_DAYS[position]);
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // 目标号码
        holder.layoutTargetMobile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showInputDialog("设置目标号码", mDataManager.getKeepAliveTargetMobile(subId),
                        new InputCallback() {
                            @Override
                            public void onInput(String value) {
                                mDataManager.setKeepAliveTargetMobile(subId, value);
                                holder.tvTargetMobile.setText(value);
                            }
                        });
            }
        });

        // 短信内容
        holder.layoutSmsContent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showInputDialog("设置短信内容", mDataManager.getKeepAliveSmsContent(subId),
                        new InputCallback() {
                            @Override
                            public void onInput(String value) {
                                mDataManager.setKeepAliveSmsContent(subId, value);
                                holder.tvSmsContent.setText(value);
                            }
                        });
            }
        });

        // 测试按钮
        holder.btnTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!validateConfig(holder)) {
                    return;
                }
                saveConfig(holder);
                KeepAliveScheduler.scheduleTest(KeepAliveActivity.this, subId);
                refreshTimeDisplay(holder);
                Toast.makeText(KeepAliveActivity.this,
                        holder.tvSimTitle.getText() + " 将在10秒后发送测试短信",
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void restoreConfig(SimCardViewHolder holder) {
        holder.isUpdatingUI = true;
        int subId = holder.subId;

        holder.switchKeepAlive.setChecked(mDataManager.getKeepAliveEnabled(subId));
        holder.tvTargetMobile.setText(mDataManager.getKeepAliveTargetMobile(subId));
        holder.tvSmsContent.setText(mDataManager.getKeepAliveSmsContent(subId));

        int days = mDataManager.getKeepAliveIntervalDays(subId);
        boolean found = false;
        for (int i = 0; i < INTERVAL_DAYS.length; i++) {
            if (INTERVAL_DAYS[i] == days) {
                holder.spinnerInterval.setSelection(i);
                found = true;
                break;
            }
        }
        if (!found && days > 0) {
            holder.spinnerInterval.setSelection(INTERVAL_DAYS.length - 1);
            holder.etCustomDays.setText(String.valueOf(days));
            holder.layoutCustomInterval.setVisibility(View.VISIBLE);
        }

        refreshTimeDisplay(holder);
        holder.isUpdatingUI = false;
    }

    private void refreshTimeDisplay(SimCardViewHolder holder) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        int subId = holder.subId;

        long lastTime = mDataManager.getKeepAliveLastSendTime(subId);
        holder.tvLastSendTime.setText(lastTime > 0 ? sdf.format(new Date(lastTime)) : "暂无记录");

        long nextTime = mDataManager.getKeepAliveNextSendTime(subId);
        holder.tvNextSendTime.setText(
                nextTime > 0 ? sdf.format(new Date(nextTime)) : "未设置");
    }

    private boolean validateConfig(SimCardViewHolder holder) {
        int subId = holder.subId;

        String mobile = mDataManager.getKeepAliveTargetMobile(subId);
        if (mobile == null || mobile.isEmpty()) {
            Toast.makeText(this, "请先设置目标号码", Toast.LENGTH_SHORT).show();
            return false;
        }

        String content = mDataManager.getKeepAliveSmsContent(subId);
        if (content == null || content.isEmpty()) {
            Toast.makeText(this, "请先设置短信内容", Toast.LENGTH_SHORT).show();
            return false;
        }

        int intervalPos = holder.spinnerInterval.getSelectedItemPosition();
        if (intervalPos < INTERVAL_DAYS.length && INTERVAL_DAYS[intervalPos] == -1) {
            String customDaysStr = holder.etCustomDays.getText().toString().trim();
            if (customDaysStr.isEmpty()) {
                Toast.makeText(this, "请输入自定义天数", Toast.LENGTH_SHORT).show();
                return false;
            }
            int customDays = Integer.parseInt(customDaysStr);
            if (customDays <= 0) {
                Toast.makeText(this, "天数必须大于 0", Toast.LENGTH_SHORT).show();
                return false;
            }
            mDataManager.setKeepAliveIntervalDays(subId, customDays);
        }

        return true;
    }

    private void saveConfig(SimCardViewHolder holder) {
        int subId = holder.subId;
        int intervalPos = holder.spinnerInterval.getSelectedItemPosition();
        if (intervalPos < INTERVAL_DAYS.length && INTERVAL_DAYS[intervalPos] != -1) {
            mDataManager.setKeepAliveIntervalDays(subId, INTERVAL_DAYS[intervalPos]);
        }
    }

    private void showInputDialog(String title, String currentValue, final InputCallback callback) {
        final EditText editText = new EditText(this);
        if (currentValue != null) {
            editText.setText(currentValue);
        }
        editText.setPadding(50, 30, 50, 30);

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(editText)
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String value = editText.getText().toString().trim();
                        if (!value.isEmpty()) {
                            callback.onInput(value);
                        }
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private interface InputCallback {
        void onInput(String value);
    }

    /**
     * 每张 SIM 卡配置项的 ViewHolder
     */
    private static class SimCardViewHolder {
        final int subId;
        final TextView tvSimTitle;
        final Switch switchKeepAlive;
        final LinearLayout layoutTargetMobile;
        final TextView tvTargetMobile;
        final Spinner spinnerInterval;
        final LinearLayout layoutCustomInterval;
        final EditText etCustomDays;
        final LinearLayout layoutSmsContent;
        final TextView tvSmsContent;
        final TextView tvLastSendTime;
        final TextView tvNextSendTime;
        final Button btnTest;
        boolean isUpdatingUI = false;

        SimCardViewHolder(View itemView, int subId) {
            this.subId = subId;
            tvSimTitle = itemView.findViewById(R.id.tv_sim_title);
            switchKeepAlive = itemView.findViewById(R.id.switch_keep_alive);
            layoutTargetMobile = itemView.findViewById(R.id.layout_target_mobile);
            tvTargetMobile = itemView.findViewById(R.id.tv_target_mobile);
            spinnerInterval = itemView.findViewById(R.id.spinner_interval);
            layoutCustomInterval = itemView.findViewById(R.id.layout_custom_interval);
            etCustomDays = itemView.findViewById(R.id.et_custom_days);
            layoutSmsContent = itemView.findViewById(R.id.layout_sms_content);
            tvSmsContent = itemView.findViewById(R.id.tv_sms_content);
            tvLastSendTime = itemView.findViewById(R.id.tv_last_send_time);
            tvNextSendTime = itemView.findViewById(R.id.tv_next_send_time);
            btnTest = itemView.findViewById(R.id.btn_test);
        }
    }
}
