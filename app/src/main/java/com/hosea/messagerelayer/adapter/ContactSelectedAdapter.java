package com.hosea.messagerelayer.adapter;

import android.content.Context;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.RecyclerView;

import com.hosea.messagerelayer.R;
import com.hosea.messagerelayer.adapter.holder.ContactHolder;
import com.hosea.messagerelayer.bean.Contact;
import com.hosea.messagerelayer.utils.db.DataBaseManager;

import java.util.List;

/**
 * Created by WHF on 2017/3/28.
 */

public class ContactSelectedAdapter extends RecyclerView.Adapter<ContactHolder> {

    private LayoutInflater mInflater;
    private List<Contact> mSelectedList;
    private Context mContext;

    public ContactSelectedAdapter(Context context, List<Contact> seletedList) {
        this.mContext = context;
        this.mInflater = LayoutInflater.from(context);
        this.mSelectedList = seletedList;
    }


    private View getItemView(LayoutInflater inflater, ViewGroup parent) {
        return inflater.inflate(R.layout.item_selected_contact,parent,false);
    }

    @Override
    public ContactHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = getItemView(mInflater,parent);
        ContactHolder holder = new ContactHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(ContactHolder holder, int position) {
        final Contact contact = mSelectedList.get(position);
        holder.mContactName.setText(contact.getContactName());
        holder.mContactNum.setText(contact.getContactNum());

        holder.mRightIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DataBaseManager dataBaseManager = new DataBaseManager(mContext);
                dataBaseManager.deleteContactFromMobile(contact.getContactNum());
                mSelectedList.remove(contact);
                dataBaseManager.closeHelper();
                notifyDataSetChanged();
            }
        });
    }

    @Override
    public int getItemCount() {
        return mSelectedList.size();
    }

    /**
     * 外部调用的更新数据
     * @param contactList
     */
    public void notifyUpdata(List<Contact> contactList){
        mSelectedList = contactList;
        notifyDataSetChanged();
    }
}
