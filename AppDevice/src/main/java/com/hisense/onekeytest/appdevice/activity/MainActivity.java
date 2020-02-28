package com.hisense.onekeytest.appdevice.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.hisense.onekeytest.appdevice.R;
import com.hisense.onekeytest.appdevice.service.ClientService;
import com.hisense.onekeytest.appdevice.util.Const;

/**
 * FileName     :   MainActivity
 * Description  :   主界面
 * Author       :   dingmingcang
 * Create Date  :   2017/02/23
 */
public class MainActivity extends AppCompatActivity {

    private Intent mIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initData();
    }

    private void initData() {
        mIntent = new Intent(this, ClientService.class);
    }

    public void onClick(View view) {
        try {
            stopService(mIntent);
            startService(mIntent);
        } catch (Exception e) {
            Log.e(Const.CONST_LOG_TAG, "MainActivity: service not start");
        }
    }
}
