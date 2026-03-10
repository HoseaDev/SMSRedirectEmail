package com.hosea.messagerelayer.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.hosea.messagerelayer.R;
import com.hosea.messagerelayer.bean.ForwardingLog;
import com.hosea.messagerelayer.utils.db.ForwardingLogManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

/**
 * 转发日志页面
 */
public class ForwardingLogActivity extends AppCompatActivity {

    private ListView mListView;
    private TextView mEmptyText;
    private ForwardingLogManager mLogManager;
    private ArrayList<ForwardingLog> mLogs = new ArrayList<>();
    private LogAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forwarding_log);

        mListView = findViewById(R.id.list_forwarding_log);
        mEmptyText = findViewById(R.id.text_empty);
        mLogManager = new ForwardingLogManager(this);
        mAdapter = new LogAdapter();
        mListView.setAdapter(mAdapter);

        loadLogs();
    }

    @Override
    protected void onDestroy() {
        mLogManager.closeHelper();
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add("清空日志").setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                showClearConfirmDialog();
                return true;
            }
        }).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        return super.onCreateOptionsMenu(menu);
    }

    private void showClearConfirmDialog() {
        new AlertDialog.Builder(this)
                .setTitle("确认清空")
                .setMessage("确定要清空所有转发日志吗？")
                .setPositiveButton("清空", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        new AsyncTask<Void, Void, Void>() {
                            @Override
                            protected Void doInBackground(Void... voids) {
                                mLogManager.clearAllLogs();
                                return null;
                            }

                            @Override
                            protected void onPostExecute(Void v) {
                                loadLogs();
                            }
                        }.execute();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void loadLogs() {
        new AsyncTask<Void, Void, ArrayList<ForwardingLog>>() {
            @Override
            protected ArrayList<ForwardingLog> doInBackground(Void... voids) {
                return mLogManager.queryLogs(200, 0);
            }

            @Override
            protected void onPostExecute(ArrayList<ForwardingLog> result) {
                mLogs.clear();
                mLogs.addAll(result);
                mAdapter.notifyDataSetChanged();

                if (mLogs.isEmpty()) {
                    mEmptyText.setVisibility(View.VISIBLE);
                    mListView.setVisibility(View.GONE);
                } else {
                    mEmptyText.setVisibility(View.GONE);
                    mListView.setVisibility(View.VISIBLE);
                }
            }
        }.execute();
    }

    private class LogAdapter extends BaseAdapter {

        private SimpleDateFormat sdf = new SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault());

        @Override
        public int getCount() {
            return mLogs.size();
        }

        @Override
        public ForwardingLog getItem(int position) {
            return mLogs.get(position);
        }

        @Override
        public long getItemId(int position) {
            return mLogs.get(position).getId();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.item_forwarding_log, parent, false);
            }

            ForwardingLog log = getItem(position);

            TextView typeText = convertView.findViewById(R.id.text_relay_type);
            TextView statusText = convertView.findViewById(R.id.text_status);
            TextView timestampText = convertView.findViewById(R.id.text_timestamp);
            TextView senderText = convertView.findViewById(R.id.text_sender);
            TextView contentText = convertView.findViewById(R.id.text_content_preview);
            TextView errorText = convertView.findViewById(R.id.text_error_msg);

            // 转发类型
            if ("email".equals(log.getRelayType())) {
                typeText.setText("邮件");
                typeText.setBackgroundColor(0xFF2980b9);
            } else {
                typeText.setText("短信");
                typeText.setBackgroundColor(0xFF27ae60);
            }

            // 状态
            if (log.getStatus() == 1) {
                statusText.setText("成功");
                statusText.setTextColor(0xFF27ae60);
            } else {
                statusText.setText("失败");
                statusText.setTextColor(0xFFe74c3c);
            }

            // 时间
            timestampText.setText(sdf.format(new Date(log.getTimestamp())));

            // 发送号码
            senderText.setText("来自: " + (log.getSenderMobile() != null ? log.getSenderMobile() : "未知"));

            // 内容预览
            contentText.setText(log.getContentPreview());

            // 错误信息
            if (log.getStatus() == 0 && log.getErrorMsg() != null && !log.getErrorMsg().isEmpty()) {
                errorText.setVisibility(View.VISIBLE);
                errorText.setText("错误: " + log.getErrorMsg());
            } else {
                errorText.setVisibility(View.GONE);
            }

            return convertView;
        }
    }
}
