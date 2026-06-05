package com.hosea.messagerelayer.adapter;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.hosea.messagerelayer.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

/**
 * 短信气泡列表 Adapter
 * 直接持有 Cursor，支持两种 ViewType：
 *   TYPE_RECEIVED = 收到的消息（左对齐气泡）
 *   TYPE_SENT     = 发出的消息（右对齐气泡）
 *
 * 双卡标记：对于收到的消息，读取 sub_id 字段，
 * 通过传入的 subIdToSlot 映射换算出卡槽，并在气泡下方显示"卡1"/"卡2"
 */
public class SmsBubbleAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    /** 收到的消息（type=1） */
    private static final int TYPE_RECEIVED = 1;
    /** 发出的消息（type=2） */
    private static final int TYPE_SENT = 2;

    private final Context mContext;

    /** 联系人首字符，用于收到消息的头像显示 */
    private String mContactInitial = "?";

    /** 长按监听接口 */
    public interface OnItemLongClickListener {
        void onItemLongClick(String body, int type, int position);
    }

    private OnItemLongClickListener mLongClickListener;

    public void setContactInitial(String initial) {
        mContactInitial = (initial != null && !initial.isEmpty()) ? initial : "?";
    }

    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        mLongClickListener = listener;
    }

    /** 当前绑定的短信 Cursor（SMS Telephony 数据库查询结果） */
    private Cursor mCursor;

    /**
     * subId → slotIndex 映射，用于将系统 sub_id 转换为"卡1/卡2"显示
     * key: subscription_id，value: 卡槽索引（0=卡1，1=卡2）
     */
    private Map<Integer, Integer> mSubIdToSlot;

    /** 时间格式：MM/dd HH:mm */
    private final SimpleDateFormat mFmtDateTime =
            new SimpleDateFormat("MM/dd HH:mm", Locale.getDefault());

    // ----------------------------- Cursor 列索引缓存 --------------------------

    /** Cursor 中 body 列的索引，-1 表示未初始化 */
    private int mColBody    = -1;
    /** Cursor 中 date 列的索引 */
    private int mColDate    = -1;
    /** Cursor 中 type 列的索引（1=收，2=发） */
    private int mColType    = -1;
    /** Cursor 中 sub_id 列的索引（可能不存在，为 -1） */
    private int mColSubId   = -1;

    public SmsBubbleAdapter(Context context) {
        this.mContext = context;
    }

    // ----------------------------- 数据接口 ------------------------------------

    /**
     * 更新 Cursor 数据源与 subId→slotIndex 映射，并刷新列表
     *
     * @param cursor      SMS Telephony 数据库查询 Cursor
     * @param subIdToSlot subscription_id 到 卡槽索引 的映射（可为 null）
     */
    public void setCursor(Cursor cursor, Map<Integer, Integer> subIdToSlot) {
        // 关闭旧 Cursor
        if (mCursor != null && !mCursor.isClosed()) {
            mCursor.close();
        }
        mCursor = cursor;
        mSubIdToSlot = subIdToSlot;

        // 缓存列索引，避免每次 onBindViewHolder 重复查找
        if (mCursor != null) {
            mColBody  = safeGetColumnIndex(mCursor, "body");
            mColDate  = safeGetColumnIndex(mCursor, "date");
            mColType  = safeGetColumnIndex(mCursor, "type");
            mColSubId = safeGetColumnIndex(mCursor, "sub_id");
        } else {
            mColBody = mColDate = mColType = mColSubId = -1;
        }

        notifyDataSetChanged();
    }

    /**
     * 关闭并释放当前持有的 Cursor，在 Activity/Fragment 销毁时调用
     */
    public void closeCursor() {
        if (mCursor != null && !mCursor.isClosed()) {
            mCursor.close();
        }
        mCursor = null;
    }

    // ----------------------------- RecyclerView 实现 --------------------------

    @Override
    public int getItemCount() {
        return (mCursor != null) ? mCursor.getCount() : 0;
    }

    @Override
    public int getItemViewType(int position) {
        if (mCursor == null || !mCursor.moveToPosition(position)) {
            return TYPE_SENT;
        }
        if (mColType < 0) return TYPE_SENT;
        int type = mCursor.getInt(mColType);
        // type=1 是收到，type=2 是发出；其余按发出处理
        return (type == TYPE_RECEIVED) ? TYPE_RECEIVED : TYPE_SENT;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        if (viewType == TYPE_RECEIVED) {
            View view = inflater.inflate(R.layout.item_sms_bubble_received, parent, false);
            return new ReceivedViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_sms_bubble_sent, parent, false);
            return new SentViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (mCursor == null || !mCursor.moveToPosition(position)) return;

        // 读取公共字段
        String body = (mColBody >= 0) ? mCursor.getString(mColBody) : "";
        long date   = (mColDate >= 0) ? mCursor.getLong(mColDate)   : 0L;
        String timeStr = (date > 0) ? mFmtDateTime.format(new Date(date)) : "";

        // 读取 type 字段，用于长按回调
        int type = (mColType >= 0) ? mCursor.getInt(mColType) : TYPE_SENT;
        final String finalBody = body;
        final int finalType = type;
        final int finalPosition = position;

        if (holder instanceof ReceivedViewHolder) {
            // --- 收到的消息 ---
            ReceivedViewHolder vh = (ReceivedViewHolder) holder;
            vh.tvAvatar.setText(mContactInitial);
            vh.tvMessage.setText(body != null ? body : "");
            vh.tvTime.setText(timeStr);

            // 双卡标记：读取 sub_id，通过映射换算卡槽
            if (mColSubId >= 0 && mSubIdToSlot != null) {
                int subId = mCursor.getInt(mColSubId);
                Integer slotIndex = mSubIdToSlot.get(subId);
                if (slotIndex != null && slotIndex >= 0) {
                    vh.tvSimSlot.setVisibility(View.VISIBLE);
                    vh.tvSimSlot.setText("卡" + (slotIndex + 1));
                } else {
                    vh.tvSimSlot.setVisibility(View.GONE);
                }
            } else {
                vh.tvSimSlot.setVisibility(View.GONE);
            }

            // 长按监听
            vh.tvMessage.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if (mLongClickListener != null) {
                        mLongClickListener.onItemLongClick(finalBody, finalType, finalPosition);
                    }
                    return true;
                }
            });

        } else if (holder instanceof SentViewHolder) {
            // --- 发出的消息 ---
            SentViewHolder vh = (SentViewHolder) holder;
            vh.tvMessage.setText(body != null ? body : "");
            vh.tvTime.setText(timeStr);

            // 长按监听
            vh.tvMessage.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if (mLongClickListener != null) {
                        mLongClickListener.onItemLongClick(finalBody, finalType, finalPosition);
                    }
                    return true;
                }
            });
        }
    }

    // ----------------------------- 工具方法 -----------------------------------

    /**
     * 安全获取列索引，列不存在时返回 -1 而不抛异常
     */
    private int safeGetColumnIndex(Cursor cursor, String columnName) {
        try {
            return cursor.getColumnIndex(columnName);
        } catch (Exception e) {
            return -1;
        }
    }

    // ----------------------------- ViewHolder --------------------------------

    /** 收到消息的 ViewHolder（左对齐气泡） */
    static class ReceivedViewHolder extends RecyclerView.ViewHolder {
        /** 头像（联系人首字符） */
        TextView tvAvatar;
        /** 消息内容 */
        TextView tvMessage;
        /** 时间 */
        TextView tvTime;
        /** 双卡标记（卡1/卡2），可 GONE */
        TextView tvSimSlot;

        ReceivedViewHolder(View itemView) {
            super(itemView);
            tvAvatar  = itemView.findViewById(R.id.tv_avatar);
            tvMessage = itemView.findViewById(R.id.tv_message);
            tvTime    = itemView.findViewById(R.id.tv_time);
            tvSimSlot = itemView.findViewById(R.id.tv_sim_slot);
        }
    }

    /** 发出消息的 ViewHolder（右对齐气泡） */
    static class SentViewHolder extends RecyclerView.ViewHolder {
        /** 消息内容 */
        TextView tvMessage;
        /** 时间 */
        TextView tvTime;

        SentViewHolder(View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tv_message);
            tvTime    = itemView.findViewById(R.id.tv_time);
        }
    }
}
