package com.hl.messagerelayer.activity;

import android.app.ListActivity;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.hl.messagerelayer.R;
import com.hl.messagerelayer.adapter.SmsAdapter;
import com.hl.messagerelayer.bean.SmsBean;
import com.hl.messagerelayer.utils.db.DataBaseManager;

import java.util.ArrayList;


/**
 * Created by heliu on 2018/7/17.
 */

public class SmsBlackActivity extends ListActivity {

    private ArrayList<SmsBean> mData = new ArrayList<>();
    private ListView lv;
    private SmsAdapter adapter;
    private DataBaseManager dataBaseManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initAdapter();

    }


    private void initAdapter() {
        dataBaseManager = new DataBaseManager(this);
        ArrayList<SmsBean> smsIntercept = dataBaseManager.getSmsIntercept();
        mData.addAll(smsIntercept);
        adapter = new SmsAdapter(mData, this);
        setListAdapter(adapter);


    }

    @Override
    protected void onListItemClick(ListView l, View v, final int position, long id) {
        new AlertDialog.Builder(SmsBlackActivity.this).setTitle("是否取消拉黑").setMessage("重新收到对方短信").setPositiveButton("取消拉黑", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                SmsBean smsBean = mData.get(position);
                dataBaseManager.deleteSmsFromMobile(smsBean.getPhone());
                Toast.makeText(SmsBlackActivity.this, "移除成功", Toast.LENGTH_SHORT).show();
                mData.remove(position);
                adapter.notifyDataSetChanged();
            }
        }).setNegativeButton("否", null).show();

    }

    private void initView() {

        lv = (ListView) findViewById(R.id.lv);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {

            }
        });
    }

    private Uri SMS_INBOX = Uri.parse("content://sms/");

    public void getSmsFromPhone() {
        ContentResolver cr = getContentResolver();
        String[] projection = new String[]{"_id", "address", "person", "body", "date", "type"};
        Cursor cur = cr.query(SMS_INBOX, projection, null, null, "date desc");
        if (null == cur) {
            Log.i("ooc", "************cur == null");
            return;
        }
        while (cur.moveToNext()) {
            String number = cur.getString(cur.getColumnIndex("address"));//手机号
            String name = cur.getString(cur.getColumnIndex("person"));//联系人姓名列表
            String body = cur.getString(cur.getColumnIndex("body"));//短信内容
            //至此就获得了短信的相关的内容, 以下是把短信加入map中，构建listview,非必要。
            SmsBean smsBean = new SmsBean();
            smsBean.setContent(body);
            smsBean.setName(name);
            smsBean.setPhone(number);
            mData.add(smsBean);

        }
    }
}
