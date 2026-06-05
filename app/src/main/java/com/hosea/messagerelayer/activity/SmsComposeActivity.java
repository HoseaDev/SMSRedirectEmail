package com.hosea.messagerelayer.activity;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.ContactsContract;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.blankj.utilcode.util.LogUtils;
import com.hosea.messagerelayer.R;
import com.hosea.messagerelayer.adapter.SmsBubbleAdapter;
import com.hosea.messagerelayer.utils.SmsRelayerManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 会话详情 + 发送短信，支持双卡
 */
public class SmsComposeActivity extends BaseActivity {

    private static final String TAG = "SmsComposeActivity";

    private RecyclerView mRecyclerView;
    private EditText mEtMessage;
    private EditText mEtRecipient;
    private LinearLayout mLayoutRecipient;
    private View mDividerRecipient;
    private ImageButton mBtnSend;
    /** 双卡时显示的 SIM 选择按钮，单卡时隐藏 */
    private TextView mBtnSimSelector;
    /** 从联系人选择收件人的按钮 */
    private ImageButton mBtnPickContact;
    /** 自绘 Toolbar 控件 */
    private ImageButton mBtnBack;
    private TextView mTvToolbarAvatar;
    private TextView mTvToolbarName;
    /** 从联系人选择 Activity 请求码 */
    private static final int REQUEST_PICK_CONTACT = 2001;
    private SmsBubbleAdapter mAdapter;

    private String mAddress;
    private long mThreadId = -1;
    private boolean mIsNewMessage;

    private int mSelectedSubId = -1;
    private List<SubscriptionInfo> mSimList = new ArrayList<>();
    private Map<Integer, Integer> mSubIdToSlot = new HashMap<>();

    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    private ContentObserver mSmsObserver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sms_compose);

        mRecyclerView      = findViewById(R.id.recycler_messages);
        mEtMessage         = findViewById(R.id.et_message);
        mEtRecipient       = findViewById(R.id.et_recipient);
        mLayoutRecipient   = findViewById(R.id.layout_recipient);
        mDividerRecipient  = findViewById(R.id.divider_recipient);
        mBtnSend           = findViewById(R.id.btn_send);
        mBtnSimSelector    = findViewById(R.id.btn_sim_selector);
        mBtnPickContact    = findViewById(R.id.btn_pick_contact);

        // 绑定自绘 Toolbar
        mBtnBack        = findViewById(R.id.btn_back);
        mTvToolbarAvatar = findViewById(R.id.tv_toolbar_avatar);
        mTvToolbarName   = findViewById(R.id.tv_toolbar_name);
        mBtnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // 点击联系人按钮，打开系统联系人选择界面
        mBtnPickContact.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent pick = new Intent(Intent.ACTION_PICK,
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
                startActivityForResult(pick, REQUEST_PICK_CONTACT);
            }
        });

        mAdapter = new SmsBubbleAdapter(this);
        LinearLayoutManager lm = new LinearLayoutManager(this);
        lm.setStackFromEnd(true);
        mRecyclerView.setLayoutManager(lm);
        mRecyclerView.setAdapter(mAdapter);

        loadSimList();
        parseIntent();
        updateSimSelector();

        // 气泡长按菜单
        mAdapter.setOnItemLongClickListener(new SmsBubbleAdapter.OnItemLongClickListener() {
            @Override
            public void onItemLongClick(final String body, final int type, final int position) {
                // 根据消息类型动态生成菜单项
                final java.util.List<String> items = new java.util.ArrayList<>();
                items.add("复制");
                if (type == 1) {
                    // 收到的消息：额外显示"转发"
                    items.add("转发");
                } else {
                    // 发出的消息：额外显示"重新发送"
                    items.add("重新发送");
                }
                new AlertDialog.Builder(SmsComposeActivity.this)
                        .setItems(items.toArray(new String[0]), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                String action = items.get(which);
                                if ("复制".equals(action)) {
                                    ClipboardManager cm = (ClipboardManager)
                                            getSystemService(Context.CLIPBOARD_SERVICE);
                                    if (cm != null) {
                                        cm.setPrimaryClip(ClipData.newPlainText("短信内容", body));
                                    }
                                    Toast.makeText(SmsComposeActivity.this, "已复制", Toast.LENGTH_SHORT).show();
                                } else if ("转发".equals(action)) {
                                    Intent fwd = new Intent(SmsComposeActivity.this, SmsComposeActivity.class);
                                    fwd.putExtra("new_message", true);
                                    fwd.putExtra("prefill_content", body);
                                    startActivity(fwd);
                                } else if ("重新发送".equals(action)) {
                                    mEtMessage.setText(body);
                                    sendMessage(mSelectedSubId);
                                }
                            }
                        })
                        .show();
            }
        });

        // 点击 SIM 选择按钮弹出选卡对话框
        mBtnSimSelector.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSimPicker(false);
            }
        });

        mBtnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage(mSelectedSubId);
            }
        });

        mSmsObserver = new ContentObserver(mHandler) {
            @Override
            public void onChange(boolean selfChange) {
                loadMessages();
            }
        };
        getContentResolver().registerContentObserver(
                Uri.parse("content://sms/"), true, mSmsObserver);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!mIsNewMessage && mAddress != null) {
            loadMessages();
            markAsReadAsync();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mSmsObserver != null) {
            getContentResolver().unregisterContentObserver(mSmsObserver);
        }
        mAdapter.closeCursor();
        mExecutor.shutdown();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PICK_CONTACT && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri == null) return;
            Cursor c = getContentResolver().query(uri,
                    new String[]{
                            ContactsContract.CommonDataKinds.Phone.NUMBER,
                            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
                    }, null, null, null);
            if (c != null && c.moveToFirst()) {
                String phone = c.getString(0);
                String name  = c.getString(1);
                c.close();
                // 清理号码中的空格和连字符
                if (phone != null) phone = phone.replaceAll("[\\s\\-]", "");
                mEtRecipient.setText(phone);
                // 设置头像首字符
                String initial = (name != null && !name.isEmpty())
                        ? name.substring(0, 1).toUpperCase(Locale.getDefault()) : "#";
                updateToolbar(name != null ? name : phone, initial);
            }
            if (c != null && !c.isClosed()) c.close();
        }
    }

    // -------------------------------------------------------------------------
    // 初始化
    // -------------------------------------------------------------------------

    private void loadSimList() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED) return;

        SubscriptionManager sm = SubscriptionManager.from(this);
        List<SubscriptionInfo> list = sm.getActiveSubscriptionInfoList();
        if (list != null) {
            mSimList.addAll(list);
            for (SubscriptionInfo info : list) {
                mSubIdToSlot.put(info.getSubscriptionId(), info.getSimSlotIndex());
            }
        }
        if (!mSimList.isEmpty()) {
            mSelectedSubId = mSimList.get(0).getSubscriptionId();
        }
    }

    private void parseIntent() {
        Intent intent = getIntent();
        mIsNewMessage = intent.getBooleanExtra("new_message", false);

        if (mIsNewMessage) {
            mLayoutRecipient.setVisibility(View.VISIBLE);
            mDividerRecipient.setVisibility(View.VISIBLE);
            updateToolbar("新建短信", "+");
            // 处理转发预填内容
            String prefill = intent.getStringExtra("prefill_content");
            if (!TextUtils.isEmpty(prefill)) {
                mEtMessage.setText(prefill);
                mEtMessage.setSelection(prefill.length());
            }
            return;
        }

        mAddress  = intent.getStringExtra("address");
        mThreadId = intent.getLongExtra("thread_id", -1);

        // 支持外部 sms:// smsto:// scheme
        if (mAddress == null && intent.getData() != null) {
            Uri data = intent.getData();
            String scheme = data.getScheme();
            if ("sms".equals(scheme) || "smsto".equals(scheme)
                    || "mms".equals(scheme) || "mmsto".equals(scheme)) {
                mAddress = data.getSchemeSpecificPart();
                if (mAddress != null) {
                    mAddress = mAddress.replaceAll("[^0-9+]", "");
                }
            }
        }

        if (mAddress == null) {
            mLayoutRecipient.setVisibility(View.VISIBLE);
            mDividerRecipient.setVisibility(View.VISIBLE);
            updateToolbar("新建短信", "+");
            mIsNewMessage = true;
            return;
        }

        // 后台查联系人名
        final String addr = mAddress;
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                final String name = lookupContactName(addr);
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (name != null && !name.isEmpty()) {
                            String initial = name.substring(0, 1).toUpperCase(java.util.Locale.getDefault());
                            updateToolbar(name, initial);
                        } else {
                            // 没有联系人名时，用号码最后一个字符作为头像
                            String initial = addr.isEmpty() ? "#" : addr.substring(addr.length() - 1);
                            updateToolbar(addr, initial);
                        }
                    }
                });
            }
        });

        loadMessages();
    }

    // -------------------------------------------------------------------------
    // SIM 卡选择按钮
    // 双卡时：左侧显示绿色"卡1"/"卡2"圆角方块，点击弹出选卡对话框
    // 单卡时：隐藏，用户无感知
    // -------------------------------------------------------------------------

    private void updateSimSelector() {
        if (mSimList.size() <= 1) {
            mBtnSimSelector.setVisibility(View.GONE);
            return;
        }
        mBtnSimSelector.setVisibility(View.VISIBLE);
        for (SubscriptionInfo info : mSimList) {
            if (info.getSubscriptionId() == mSelectedSubId) {
                mBtnSimSelector.setText("卡" + (info.getSimSlotIndex() + 1));
                return;
            }
        }
    }

    /** sendAfterPick=true 表示选完卡后立即发送，false 表示只切卡不发 */
    private void showSimPicker(final boolean sendAfterPick) {
        if (mSimList.size() <= 1) {
            if (sendAfterPick) sendMessage(mSelectedSubId);
            return;
        }
        String[] items = new String[mSimList.size()];
        for (int i = 0; i < mSimList.size(); i++) {
            SubscriptionInfo info = mSimList.get(i);
            items[i] = "卡" + (info.getSimSlotIndex() + 1) + "    " + info.getCarrierName();
        }
        new AlertDialog.Builder(this)
                .setTitle("选择 SIM 卡")
                .setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mSelectedSubId = mSimList.get(which).getSubscriptionId();
                        updateSimSelector();
                        if (sendAfterPick) sendMessage(mSelectedSubId);
                    }
                })
                .show();
    }

    // -------------------------------------------------------------------------
    // 消息列表
    // -------------------------------------------------------------------------

    private void loadMessages() {
        if (mAddress == null) return;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)
                != PackageManager.PERMISSION_GRANTED) return;

        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                String selection;
                String[] selectionArgs;
                if (mThreadId > 0) {
                    selection     = "thread_id = ?";
                    selectionArgs = new String[]{String.valueOf(mThreadId)};
                } else {
                    selection     = "address = ?";
                    selectionArgs = new String[]{mAddress};
                }
                final Cursor cursor = getContentResolver().query(
                        Uri.parse("content://sms/"),
                        new String[]{"_id", "body", "date", "type", "address", "sub_id"},
                        selection, selectionArgs, "date ASC");

                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mAdapter.setCursor(cursor, mSubIdToSlot);
                        if (cursor != null && cursor.getCount() > 0) {
                            mRecyclerView.scrollToPosition(cursor.getCount() - 1);
                        }
                    }
                });
            }
        });
    }

    // -------------------------------------------------------------------------
    // 发送
    // -------------------------------------------------------------------------

    private void sendMessage(final int subId) {
        String address = mAddress;

        if (mIsNewMessage || TextUtils.isEmpty(address)) {
            address = mEtRecipient.getText().toString().trim();
            if (TextUtils.isEmpty(address)) {
                Toast.makeText(this, "请输入收件人号码", Toast.LENGTH_SHORT).show();
                return;
            }
            mAddress = address;
            mIsNewMessage = false;
            mLayoutRecipient.setVisibility(View.GONE);
            mDividerRecipient.setVisibility(View.GONE);
            final String finalAddr = mAddress;
            mExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    final String name = lookupContactName(finalAddr);
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            String displayName = name != null ? name : finalAddr;
                            String initial = (name != null && !name.isEmpty())
                                    ? name.substring(0, 1).toUpperCase(java.util.Locale.getDefault())
                                    : (finalAddr.isEmpty() ? "#" : finalAddr.substring(finalAddr.length() - 1));
                            updateToolbar(displayName, initial);
                        }
                    });
                }
            });
        }

        final String content = mEtMessage.getText().toString().trim();
        if (TextUtils.isEmpty(content)) {
            Toast.makeText(this, "请输入短信内容", Toast.LENGTH_SHORT).show();
            return;
        }

        mEtMessage.setText("");

        final String finalAddress = address;
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    SmsRelayerManager.sendSmsInternal(
                            SmsComposeActivity.this, subId, finalAddress, content, "compose");
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(SmsComposeActivity.this,
                                    "短信已发送", Toast.LENGTH_SHORT).show();
                            loadMessages();
                        }
                    });
                } catch (final Exception e) {
                    LogUtils.e(TAG, "发送异常: " + e.getMessage());
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(SmsComposeActivity.this,
                                    "发送失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });
    }

    // -------------------------------------------------------------------------
    // 标记已读
    // -------------------------------------------------------------------------

    private void markAsReadAsync() {
        if (mAddress == null) return;
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    ContentValues values = new ContentValues();
                    values.put("read", 1);
                    values.put("seen", 1);
                    String where;
                    String[] whereArgs;
                    if (mThreadId > 0) {
                        where     = "thread_id = ? AND read = 0";
                        whereArgs = new String[]{String.valueOf(mThreadId)};
                    } else {
                        where     = "address = ? AND read = 0";
                        whereArgs = new String[]{mAddress};
                    }
                    getContentResolver().update(
                            Uri.parse("content://sms/"), values, where, whereArgs);
                } catch (Exception e) {
                    LogUtils.e(TAG, "标记已读失败: " + e.getMessage());
                }
            }
        });
    }

    // -------------------------------------------------------------------------
    // 工具
    // -------------------------------------------------------------------------

    private String lookupContactName(String phone) {
        if (phone == null || phone.isEmpty()) return null;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) return null;
        try {
            Uri uri = Uri.withAppendedPath(
                    ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phone));
            Cursor c = getContentResolver().query(uri,
                    new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME}, null, null, null);
            if (c != null) {
                try {
                    if (c.moveToFirst()) return c.getString(0);
                } finally {
                    c.close();
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    /**
     * 更新自绘 Toolbar：标题文字 + 头像首字符，同步更新气泡头像
     *
     * @param name    显示名称（联系人名或号码）
     * @param initial 头像首字符
     */
    private void updateToolbar(String name, String initial) {
        if (mTvToolbarName != null) mTvToolbarName.setText(name);
        if (mTvToolbarAvatar != null) mTvToolbarAvatar.setText(initial);
        if (mAdapter != null) mAdapter.setContactInitial(initial);
    }
}
