package com.hosea.messagerelayer.activity;

import android.Manifest;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.ContactsContract;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.hosea.messagerelayer.bean.SmsBean;
import com.hosea.messagerelayer.utils.db.DataBaseManager;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.blankj.utilcode.util.LogUtils;
import com.hosea.messagerelayer.R;
import com.hosea.messagerelayer.adapter.SmsThreadAdapter;
import com.hosea.messagerelayer.bean.SmsThread;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 短信收件箱——按会话分组显示，支持双卡标记
 */
public class SmsInboxActivity extends BaseActivity {

    private static final String TAG = "SmsInboxActivity";
    private static final String NOTIF_CHANNEL_ID = "sms_inbox";
    private static final int NOTIF_ID_BASE = 5000;

    private RecyclerView mRecyclerView;
    private TextView mTvEmpty;
    private SmsThreadAdapter mAdapter;

    /** 搜索框 */
    private EditText mEtSearch;
    /** SIM 卡筛选按钮 */
    private TextView mBtnFilterSim;
    /** 当前搜索关键词，空字符串表示不限 */
    private String mSearchKeyword = "";
    /** 当前筛选的 SIM 卡槽，-1 表示全部 */
    private int mFilterSimSlot = -1;

    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    private final Handler mHandler = new Handler(Looper.getMainLooper());

    /** 联系人名缓存，避免重复查询 */
    private static final Map<String, String> sContactCache = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sms_inbox);

        mRecyclerView = findViewById(R.id.recycler_threads);
        mTvEmpty = findViewById(R.id.tv_empty);

        mAdapter = new SmsThreadAdapter(this);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setAdapter(mAdapter);

        // 绑定搜索框与 SIM 筛选按钮
        mEtSearch = findViewById(R.id.et_search);
        mBtnFilterSim = findViewById(R.id.btn_filter_sim);

        // 搜索框文本变化时实时过滤
        mEtSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mSearchKeyword = s != null ? s.toString().trim() : "";
                applyFilter();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // SIM 筛选按钮点击，弹出选择对话框
        mBtnFilterSim.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSimFilterDialog();
            }
        });

        mAdapter.setOnItemClickListener(new SmsThreadAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(SmsThread thread) {
                Intent intent = new Intent(SmsInboxActivity.this, SmsComposeActivity.class);
                intent.putExtra("address", thread.getAddress());
                intent.putExtra("thread_id", thread.getThreadId());
                startActivity(intent);
            }
        });

        mAdapter.setOnItemLongClickListener(new SmsThreadAdapter.OnItemLongClickListener() {
            @Override
            public void onItemLongClick(final SmsThread thread, final int position) {
                final String address = thread.getAddress();
                final String displayName = thread.getDisplayName();
                String[] options = {"拉黑此号码", "删除此会话", "发送短信"};
                new AlertDialog.Builder(SmsInboxActivity.this)
                        .setItems(options, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (which == 0) {
                                    // 拉黑此号码
                                    SmsBean bean = new SmsBean();
                                    bean.setPhone(address);
                                    bean.setName(displayName);
                                    new DataBaseManager(SmsInboxActivity.this).addSMSIntercept(bean);
                                    Toast.makeText(SmsInboxActivity.this,
                                            "已拉黑 " + address, Toast.LENGTH_SHORT).show();
                                    loadThreads();
                                } else if (which == 1) {
                                    // 删除此会话（二次确认）
                                    new AlertDialog.Builder(SmsInboxActivity.this)
                                            .setMessage("确定删除与 " + displayName + " 的全部短信？")
                                            .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface d, int w) {
                                                    mExecutor.execute(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            getContentResolver().delete(
                                                                    Uri.parse("content://sms/"),
                                                                    "thread_id = ?",
                                                                    new String[]{String.valueOf(thread.getThreadId())});
                                                            mHandler.post(new Runnable() {
                                                                @Override
                                                                public void run() {
                                                                    loadThreads();
                                                                }
                                                            });
                                                        }
                                                    });
                                                }
                                            })
                                            .setNegativeButton("取消", null)
                                            .show();
                                } else if (which == 2) {
                                    // 发送短信
                                    Intent intent = new Intent(SmsInboxActivity.this, SmsComposeActivity.class);
                                    intent.putExtra("address", address);
                                    startActivity(intent);
                                }
                            }
                        })
                        .show();
            }
        });

        findViewById(R.id.fab_new_sms).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SmsInboxActivity.this, SmsComposeActivity.class);
                intent.putExtra("new_message", true);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadThreads();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mExecutor.shutdown();
    }

    /**
     * 根据当前关键词和 SIM 卡槽对列表进行过滤，并同步空状态视图
     */
    private void applyFilter() {
        mAdapter.filter(mSearchKeyword, mFilterSimSlot);
        if (mAdapter.getItemCount() == 0) {
            mTvEmpty.setVisibility(View.VISIBLE);
            mRecyclerView.setVisibility(View.GONE);
        } else {
            mTvEmpty.setVisibility(View.GONE);
            mRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    /**
     * 弹出 SIM 卡筛选对话框，选项根据已激活 SIM 卡动态生成
     */
    private void showSimFilterDialog() {
        // 获取当前激活的 SIM 卡列表
        List<SubscriptionInfo> sims = new ArrayList<>();
        try {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
                    == PackageManager.PERMISSION_GRANTED) {
                SubscriptionManager sm = SubscriptionManager.from(this);
                List<SubscriptionInfo> active = sm.getActiveSubscriptionInfoList();
                if (active != null) sims.addAll(active);
            }
        } catch (Exception e) {
            LogUtils.w(TAG, "获取 SIM 卡列表失败: " + e.getMessage());
        }

        // 动态构建选项：第一项"全部"，后续每张卡一项
        final int[] slotValues = new int[1 + sims.size()];
        String[] labels = new String[1 + sims.size()];
        slotValues[0] = -1;
        labels[0] = "全部";
        for (int i = 0; i < sims.size(); i++) {
            SubscriptionInfo info = sims.get(i);
            slotValues[i + 1] = info.getSimSlotIndex();
            labels[i + 1] = "卡" + (info.getSimSlotIndex() + 1)
                    + "（" + info.getCarrierName() + "）";
        }

        new AlertDialog.Builder(this)
                .setTitle("按 SIM 卡筛选")
                .setItems(labels, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mFilterSimSlot = slotValues[which];
                        mBtnFilterSim.setText(labels[which]);
                        applyFilter();
                    }
                })
                .show();
    }

    private void loadThreads() {
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                final List<SmsThread> threads = queryThreads();
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mAdapter.setData(threads);
                        // 数据加载完后，应用当前搜索/筛选条件
                        applyFilter();
                    }
                });
            }
        });
    }

    /**
     * 后台线程：查询所有会话，支持双卡 simSlot 标记
     */
    private List<SmsThread> queryThreads() {
        List<SmsThread> result = new ArrayList<>();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            return result;
        }

        // 构建 subId → slotIndex 映射
        Map<Integer, Integer> subIdToSlot = buildSubIdToSlotMap();

        ContentResolver resolver = getContentResolver();
        Cursor cursor = resolver.query(
                Uri.parse("content://sms/"),
                new String[]{"_id", "thread_id", "address", "body", "date", "read", "type", "sub_id"},
                null, null, "date DESC");

        if (cursor == null) return result;

        Map<Long, SmsThread> threadMap = new LinkedHashMap<>();

        try {
            int colThreadId = cursor.getColumnIndex("thread_id");
            int colAddress  = cursor.getColumnIndex("address");
            int colBody     = cursor.getColumnIndex("body");
            int colDate     = cursor.getColumnIndex("date");
            int colRead     = cursor.getColumnIndex("read");
            int colSubId    = cursor.getColumnIndex("sub_id");

            while (cursor.moveToNext()) {
                long threadId = cursor.getLong(colThreadId);
                int  read     = cursor.getInt(colRead);

                if (threadMap.containsKey(threadId)) {
                    // 累加未读
                    if (read == 0) {
                        SmsThread t = threadMap.get(threadId);
                        t.setUnreadCount(t.getUnreadCount() + 1);
                    }
                } else {
                    SmsThread t = new SmsThread();
                    t.setThreadId(threadId);
                    t.setAddress(cursor.getString(colAddress));
                    t.setLastBody(cursor.getString(colBody));
                    t.setLastDate(cursor.getLong(colDate));
                    t.setUnreadCount(read == 0 ? 1 : 0);

                    // 双卡：记录最新一条消息来自哪张卡
                    if (colSubId >= 0 && !cursor.isNull(colSubId)) {
                        int subId = cursor.getInt(colSubId);
                        Integer slot = subIdToSlot.get(subId);
                        t.setSimSlot(slot != null ? slot : -1);
                    } else {
                        t.setSimSlot(-1);
                    }

                    threadMap.put(threadId, t);
                }
            }
        } finally {
            cursor.close();
        }

        result.addAll(threadMap.values());

        // 批量解析联系人名
        boolean canReadContacts = ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED;

        if (canReadContacts) {
            for (SmsThread t : result) {
                String address = t.getAddress();
                if (address == null || address.isEmpty()) continue;

                if (sContactCache.containsKey(address)) {
                    String cached = sContactCache.get(address);
                    if (cached != null) t.setContactName(cached);
                    continue;
                }

                String name = lookupContactName(address);
                sContactCache.put(address, name);
                if (name != null) t.setContactName(name);
            }
        }

        return result;
    }

    private String lookupContactName(String phone) {
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
        } catch (Exception e) {
            LogUtils.w(TAG, "查询联系人异常: " + e.getMessage());
        }
        return null;
    }

    /** 构建 subId → slotIndex 的映射表 */
    private Map<Integer, Integer> buildSubIdToSlotMap() {
        Map<Integer, Integer> map = new HashMap<>();
        try {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
                    == PackageManager.PERMISSION_GRANTED) {
                android.telephony.SubscriptionManager sm =
                        android.telephony.SubscriptionManager.from(this);
                List<android.telephony.SubscriptionInfo> list = sm.getActiveSubscriptionInfoList();
                if (list != null) {
                    for (android.telephony.SubscriptionInfo info : list) {
                        map.put(info.getSubscriptionId(), info.getSimSlotIndex());
                    }
                }
            }
        } catch (Exception e) {
            LogUtils.w(TAG, "获取 SIM 卡列表异常: " + e.getMessage());
        }
        return map;
    }

    // -------------------------------------------------------------------------
    // 静态方法：供外部调用
    // -------------------------------------------------------------------------

    /**
     * 异步查询全局未读短信总数，供 MainActivity 等界面在角标/悬浮窗上使用。
     * 在后台线程执行查询，不阻塞主线程；结果通过回调在主线程返回。
     *
     * @param context  上下文（Application 级别也可）
     * @param callback 结果回调接口，在主线程执行
     */
    public static void getUnreadCount(Context context,
                                       final UnreadCountCallback callback) {
        final Handler mainHandler = new Handler(Looper.getMainLooper());
        new Thread(new Runnable() {
            @Override
            public void run() {
                int count = 0;
                Cursor cursor = null;
                try {
                    cursor = context.getContentResolver().query(
                            Uri.parse("content://sms/inbox"),
                            new String[]{"_id"},
                            "read=0",
                            null,
                            null);
                    if (cursor != null) {
                        count = cursor.getCount();
                    }
                } catch (Exception e) {
                    LogUtils.w(TAG, "getUnreadCount 查询失败: " + e.getMessage());
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }
                final int finalCount = count;
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (callback != null) {
                            callback.onResult(finalCount);
                        }
                    }
                });
            }
        }).start();
    }

    /**
     * getUnreadCount 的异步回调接口
     */
    public interface UnreadCountCallback {
        /**
         * @param unreadCount 当前未读短信总数，在主线程回调
         */
        void onResult(int unreadCount);
    }

    /**
     * 收到新短信时发系统通知，点击跳转对应会话
     */
    public static void showNewMessageNotification(Context context,
                                                   String address, String body, long threadId) {
        NotificationManager nm =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    NOTIF_CHANNEL_ID, "新短信", NotificationManager.IMPORTANCE_HIGH);
            nm.createNotificationChannel(channel);
        }

        Intent intent = new Intent(context, SmsComposeActivity.class);
        intent.putExtra("address", address);
        intent.putExtra("thread_id", threadId);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        int piFlags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            piFlags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pi = PendingIntent.getActivity(
                context, (int) (address.hashCode()), intent, piFlags);

        String displayName = address;
        // 尝试从缓存取联系人名
        if (sContactCache.containsKey(address) && sContactCache.get(address) != null) {
            displayName = sContactCache.get(address);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NOTIF_CHANNEL_ID)
                .setSmallIcon(R.mipmap.icon)
                .setContentTitle(displayName)
                .setContentText(body)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pi);

        nm.notify(NOTIF_ID_BASE + address.hashCode(), builder.build());
    }
}
