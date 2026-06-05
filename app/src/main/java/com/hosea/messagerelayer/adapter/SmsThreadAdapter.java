package com.hosea.messagerelayer.adapter;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.hosea.messagerelayer.R;
import com.hosea.messagerelayer.bean.SmsThread;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * 短信会话列表 Adapter
 * 每行显示：圆形头像（取首字符）、联系人名/号码、最后一条短信摘要、时间、未读角标、双卡标记
 */
public class SmsThreadAdapter extends RecyclerView.Adapter<SmsThreadAdapter.ViewHolder> {

    private final Context mContext;
    /** 完整原始数据，用于搜索/筛选时重置 */
    private List<SmsThread> mAllData = new ArrayList<>();
    /** 当前展示的数据（过滤后） */
    private List<SmsThread> mData = new ArrayList<>();
    private OnItemClickListener mListener;
    private OnItemLongClickListener mLongClickListener;

    /** 时间格式：今天显示 HH:mm */
    private final SimpleDateFormat mFmtTime = new SimpleDateFormat("HH:mm", Locale.getDefault());
    /** 时间格式：其他日期显示 MM/dd */
    private final SimpleDateFormat mFmtDate = new SimpleDateFormat("MM/dd", Locale.getDefault());

    public SmsThreadAdapter(Context context) {
        this.mContext = context;
    }

    // ----------------------------- 数据接口 ------------------------------------

    /**
     * 更新列表数据并刷新，同时保存完整数据用于过滤
     */
    public void setData(List<SmsThread> data) {
        mAllData.clear();
        if (data != null) mAllData.addAll(data);
        mData.clear();
        mData.addAll(mAllData);
        notifyDataSetChanged();
    }

    /**
     * 按关键词和 SIM 卡槽过滤列表
     *
     * @param keyword  搜索关键词，匹配联系人名或短信内容；空字符串表示不限
     * @param simSlot  SIM 卡槽索引（0=卡1, 1=卡2）；-1 表示全部
     */
    public void filter(String keyword, int simSlot) {
        mData.clear();
        for (SmsThread t : mAllData) {
            boolean matchSim = (simSlot < 0) || (t.getSimSlot() == simSlot);
            boolean matchKey = TextUtils.isEmpty(keyword)
                    || t.getDisplayName().contains(keyword)
                    || (t.getLastBody() != null && t.getLastBody().contains(keyword));
            if (matchSim && matchKey) mData.add(t);
        }
        notifyDataSetChanged();
    }

    /** 条目点击监听接口 */
    public interface OnItemClickListener {
        void onItemClick(SmsThread thread);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.mListener = listener;
    }

    /** 条目长按监听接口 */
    public interface OnItemLongClickListener {
        void onItemLongClick(SmsThread thread, int position);
    }

    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        this.mLongClickListener = listener;
    }

    // ----------------------------- RecyclerView 实现 --------------------------

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.item_sms_thread, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SmsThread thread = mData.get(position);

        // --- 头像：联系人名取首字，纯数字号码取后两位 ---
        String displayName = thread.getDisplayName();
        String avatarChar;
        if (displayName.isEmpty()) {
            avatarChar = "#";
        } else if (displayName.matches("[0-9+\\-\\s]+")) {
            // 纯数字号码：取末尾两位，更有区分度
            String digits = displayName.replaceAll("[^0-9]", "");
            avatarChar = digits.length() >= 2
                    ? digits.substring(digits.length() - 2)
                    : digits;
        } else {
            avatarChar = displayName.substring(0, 1).toUpperCase(Locale.getDefault());
        }
        holder.tvAvatar.setText(avatarChar);

        // --- 联系人名/号码 ---
        holder.tvName.setText(displayName);

        // --- 最后一条短信摘要 ---
        holder.tvPreview.setText(thread.getLastBody() != null ? thread.getLastBody() : "");

        // --- 时间格式化 ---
        holder.tvTime.setText(formatTime(thread.getLastDate()));

        // --- 未读数角标 ---
        int unread = thread.getUnreadCount();
        if (unread > 0) {
            holder.tvUnreadCount.setVisibility(View.VISIBLE);
            holder.tvUnreadCount.setText(unread > 99 ? "99+" : String.valueOf(unread));
        } else {
            holder.tvUnreadCount.setVisibility(View.GONE);
        }

        // --- 双卡标记：simSlot >= 0 时显示"卡1"或"卡2" ---
        int simSlot = thread.getSimSlot();
        if (simSlot >= 0) {
            holder.tvSimSlot.setVisibility(View.VISIBLE);
            // simSlot 0 = 卡1，simSlot 1 = 卡2
            holder.tvSimSlot.setText("卡" + (simSlot + 1));
        } else {
            holder.tvSimSlot.setVisibility(View.GONE);
        }

        // --- 点击事件 ---
        holder.itemView.setOnClickListener(v -> {
            if (mListener != null) {
                mListener.onItemClick(thread);
            }
        });

        // --- 长按事件 ---
        final int pos = position;
        holder.itemView.setOnLongClickListener(v -> {
            if (mLongClickListener != null) {
                mLongClickListener.onItemLongClick(thread, pos);
            }
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    // ----------------------------- 时间格式化 ---------------------------------

    /**
     * 格式化时间戳：
     * - 今天：HH:mm
     * - 昨天：昨天
     * - 其他：MM/dd
     */
    private String formatTime(long timestamp) {
        if (timestamp <= 0) return "";

        Calendar msgCal = Calendar.getInstance();
        msgCal.setTimeInMillis(timestamp);

        Calendar todayCal = Calendar.getInstance();

        // 判断是否是今天
        boolean isToday = msgCal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR)
                && msgCal.get(Calendar.DAY_OF_YEAR) == todayCal.get(Calendar.DAY_OF_YEAR);
        if (isToday) {
            return mFmtTime.format(msgCal.getTime());
        }

        // 判断是否是昨天
        todayCal.add(Calendar.DAY_OF_YEAR, -1);
        boolean isYesterday = msgCal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR)
                && msgCal.get(Calendar.DAY_OF_YEAR) == todayCal.get(Calendar.DAY_OF_YEAR);
        if (isYesterday) {
            return "昨天";
        }

        return mFmtDate.format(msgCal.getTime());
    }

    // ----------------------------- ViewHolder --------------------------------

    static class ViewHolder extends RecyclerView.ViewHolder {
        /** 圆形头像（首字符） */
        TextView tvAvatar;
        /** 联系人名或号码 */
        TextView tvName;
        /** 最后一条短信摘要 */
        TextView tvPreview;
        /** 时间 */
        TextView tvTime;
        /** 未读数角标 */
        TextView tvUnreadCount;
        /** 双卡标记（卡1/卡2） */
        TextView tvSimSlot;

        ViewHolder(View itemView) {
            super(itemView);
            tvAvatar      = itemView.findViewById(R.id.tv_avatar);
            tvName        = itemView.findViewById(R.id.tv_name);
            tvPreview     = itemView.findViewById(R.id.tv_preview);
            tvTime        = itemView.findViewById(R.id.tv_time);
            tvUnreadCount = itemView.findViewById(R.id.tv_unread_count);
            tvSimSlot     = itemView.findViewById(R.id.tv_sim_slot);
        }
    }
}
