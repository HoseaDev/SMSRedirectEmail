package com.hosea.messagerelayer.activity;

import android.content.ContentResolver;
import android.content.DialogInterface;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;

import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.blankj.utilcode.util.LogUtils;
import com.hosea.messagerelayer.R;
import com.hosea.messagerelayer.adapter.SmsAdapter;
import com.hosea.messagerelayer.bean.SmsBean;
import com.hosea.messagerelayer.utils.db.DataBaseManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


/**
 * Created by heliu on 2018/7/17.
 */

public class SmsActivity extends BaseActivity {

    private ArrayList<SmsBean> mData = new ArrayList<>();
    private ListView lv;
    private SmsAdapter adapter;
    private DataBaseManager dataBaseManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sms_list);

        dataBaseManager = new DataBaseManager(this);
        initView();
        getSmsFromPhone();
        initAdapter();

    }

    private void initAdapter() {
        adapter = new SmsAdapter(mData, this);
        lv.setAdapter(adapter);

    }

    private void initView() {

        lv = (ListView) findViewById(R.id.lv);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
                new AlertDialog.Builder(SmsActivity.this).setTitle("是否拉黑").setMessage("拉黑后将不转发对方短信").setPositiveButton("拉黑", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        SmsBean smsBean = mData.get(position);
                        dataBaseManager.addSMSIntercept(smsBean);
                        Toast.makeText(SmsActivity.this, "添加成功", Toast.LENGTH_SHORT).show();
                    }
                }).setNegativeButton("否", null).show();

            }
        });
    }

    private Uri SMS_INBOX = Uri.parse("content://sms/");

    public void getSmsFromPhone() {
        ContentResolver cr = getContentResolver();
        Map<String, Object> map = new HashMap<String, Object>();
        String[] projection = new String[]{"_id", "address", "person", "body", "date", "type"};
        Cursor cur = cr.query(SMS_INBOX, projection, null, null, "date desc");
        if (null == cur) {
             LogUtils.i("ooc", "************cur == null");
            return;
        }
        while (cur.moveToNext()) {
            String number = cur.getString(cur.getColumnIndex("address"));//手机号
            String name = cur.getString(cur.getColumnIndex("person"));//联系人姓名列表
            String body = cur.getString(cur.getColumnIndex("body"));//短信内容
            //简单的进行判断.
            if (!map.containsKey(number)) {
                map.put(number, number);
                SmsBean smsBean = new SmsBean();
                smsBean.setContent(body);
                smsBean.setName(name);
                smsBean.setPhone(number);
                mData.add(smsBean);
            }
        }
    }
}
