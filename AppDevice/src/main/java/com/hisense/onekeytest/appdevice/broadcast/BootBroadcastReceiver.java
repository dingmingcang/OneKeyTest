package com.hisense.onekeytest.appdevice.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.hisense.onekeytest.appdevice.service.ClientService;

/**
 * 广播接收者，监听开机和网络变化广播
 * Created by dingmingcang on 2017/2/24.
 */

public class BootBroadcastReceiver extends BroadcastReceiver {


    @Override
    public void onReceive(Context context, Intent intent) {
        context.startService(new Intent(context,ClientService.class));
    }
}
