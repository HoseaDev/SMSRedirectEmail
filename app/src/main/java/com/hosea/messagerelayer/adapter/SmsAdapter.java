package com.hosea.messagerelayer.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.hosea.messagerelayer.R;
import com.hosea.messagerelayer.bean.SmsBean;

import java.util.ArrayList;

/**
 * Created by heliu on 2018/7/17.
 */

public class SmsAdapter extends BaseAdapter {
    private ArrayList<SmsBean> mList;
    private Context mCtx;

    public SmsAdapter(ArrayList<SmsBean> mList, Context mCtx) {
        this.mList = mList;
        this.mCtx = mCtx;
    }

    @Override
    public int getCount() {
        return mList.size();
    }

    @Override
    public SmsBean getItem(int position) {
        return mList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        SmsViewHolder viewHolder = null;
        if (convertView == null) {
            convertView = LayoutInflater.from(mCtx).inflate(R.layout.item_sms, null);
            viewHolder = new SmsViewHolder();
            viewHolder.name = (TextView) convertView.findViewById(R.id.name);
            viewHolder.phone = (TextView) convertView.findViewById(R.id.phone);
            viewHolder.content = (TextView) convertView.findViewById(R.id.content);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (SmsViewHolder) convertView.getTag();
        }
        SmsBean item = getItem(position);
        viewHolder.phone.setText(item.getPhone());
        viewHolder.name.setText(item.getName());
        viewHolder.content.setText(item.getContent());
        return convertView;
    }

    private    class SmsViewHolder {
        TextView name;
        TextView content;
        TextView phone;
    }
}
