package com.hosea.messagerelayer.activity;

import android.os.Bundle;

import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.hosea.messagerelayer.listener.ICustomCompletedListener;
import com.hosea.messagerelayer.utils.permission.DefaultRationale;
import com.hosea.messagerelayer.utils.permission.PermissionSetting;
import com.yanzhenjie.permission.Action;
import com.yanzhenjie.permission.AndPermission;
import com.yanzhenjie.permission.Rationale;


import java.util.List;

/**
 * Created by heliu on 2018/6/29.
 */

public class BaseActivity extends AppCompatActivity {
    private Rationale mRationale;
    private PermissionSetting mSetting;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mRationale = new DefaultRationale();
        mSetting = new PermissionSetting(this);

    }


    public void requestPermission(final ICustomCompletedListener listener, String... permissions) {
        AndPermission.with(this)
                .permission(permissions)
                .rationale(mRationale)
                .onGranted(new Action() {
                    @Override
                    public void onAction(List<String> permissions) {
//                        toast(R.string.successfully);
                        if (listener != null) {
                            listener.success();
                        }
                    }
                })
                .onDenied(new Action() {
                    @Override
                    public void onAction(@NonNull List<String> permissions) {
                        if (listener != null) {
                            listener.failed(null);
                        }
                        Toast.makeText(BaseActivity.this, "失败", Toast.LENGTH_SHORT);
                        if (AndPermission.hasAlwaysDeniedPermission(BaseActivity.this, permissions)) {
                            mSetting.showSetting(permissions);
                        }
                    }
                })
                .start();
    }

    public void requestPermission(final ICustomCompletedListener listener, String[]... permissions) {
        AndPermission.with(this)
                .permission(permissions)
                .rationale(mRationale)
                .onGranted(new Action() {
                    @Override
                    public void onAction(List<String> permissions) {
//                        toast(R.string.successfully);
                        if (listener != null) {
                            listener.success();
                        }
                    }
                })
                .onDenied(new Action() {
                    @Override
                    public void onAction(@NonNull List<String> permissions) {
                        Toast.makeText(BaseActivity.this, "失败", Toast.LENGTH_SHORT);
                        if (listener != null) {
                            listener.failed(null);
                        }
                        if (AndPermission.hasAlwaysDeniedPermission(BaseActivity.this, permissions)) {
                            mSetting.showSetting(permissions);
                        }
                    }
                })
                .start();
    }
}
