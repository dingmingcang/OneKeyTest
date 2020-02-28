package com.hisense.onekeytest.appdevice.service;

import android.app.Instrumentation;
import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.hisense.onekeytest.appdevice.bean.KeyCodeBean;
import com.hisense.onekeytest.appdevice.search.SearcherDevice;
import com.hisense.onekeytest.appdevice.socket.DeviceSocket;
import com.hisense.onekeytest.appdevice.util.Const;

import java.net.InetSocketAddress;

/**
 * File Name    ： ClientService
 * Description  ： 设备端运行服务，接收keyCode并使设备响应
 * Author       ： dingmingcang
 * Create Date  ： 2017/02/24
 */

public class ClientService extends Service implements DeviceSocket.DeviceSocketListener {
    private static final int MSG_START_CONNECT = 1;
    private static final int MSG_KEY_CODE = 2;
    public static final String COMMAND_SU = "su";
    public static final String COMMAND_EXIT = "exit\n";
    public static final String COMMAND_LINE_END = "\n";
    private DeviceSocket mSocket;
    private SearcherDevice mSearcherDevice;
    public boolean mIsOpen = true;
    private Handler mHandler;
    private Instrumentation mIns = new Instrumentation();
    /**
     * 实例化SearcherDevice
     */
    private void getSearcherDevice() {
        if (mSearcherDevice != null) {
            mSearcherDevice.close();}
        mSearcherDevice = new SearcherDevice() {
            @Override
            public void onDeviceSearched(InetSocketAddress inetSocketAddress) {
                Message msg = Message.obtain();
                msg.obj = inetSocketAddress.getAddress().getHostAddress();
                msg.what = MSG_START_CONNECT;
                mHandler.sendMessage(msg);}
            @Override
            public void printLog(String s) {
                Log.e(SearcherDevice.class.getSimpleName(), s);
            }};}
    public ClientService() {}
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    @Override
    public void onCreate() {
        super.onCreate();
        getSearcherDevice();
        new Thread(mThread).start();
        Toast.makeText(this, "服务已启动", Toast.LENGTH_SHORT).show();}
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e(Const.CONST_LOG_TAG, "onStartCommand: mSearcherDevice.open()  " + mIsOpen);
        //是否为第一次打开
        if (mIsOpen) {
            if (mSearcherDevice != null) {
                mSearcherDevice.open();
                mIsOpen = false;}} else {
            Toast.makeText(this, "服务已经启动过了", Toast.LENGTH_SHORT).show();}
        return START_STICKY;}
    @Override
    public void deviceConnect() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(ClientService.this, "成功建立连接", Toast.LENGTH_SHORT).show();}});}
    @Override
    public void deviceDisconnect() {
        getSearcherDevice();
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mSearcherDevice.open();
                Toast.makeText(ClientService.this, "断开连接", Toast.LENGTH_SHORT).show();}});}
    @Override
    public void receivedData(KeyCodeBean keyCode) {
        Message msg = mHandler.obtainMessage();
        msg.what = MSG_KEY_CODE;
        msg.arg1 = keyCode.keyCode;
        mHandler.sendMessage(msg);}
    private Runnable mThread = new Runnable() {
        @Override
        public void run() {
            Looper.prepare();
            mHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    super.handleMessage(msg);
                    if (msg != null) {
                        switch (msg.what) {
                            case MSG_START_CONNECT:
                                mSocket = DeviceSocket.getInstance(msg.obj.toString());
                                if (mSocket.isCurDeviceConnected()) {
                                    break;}
                                mSocket.start(ClientService.this);
                                break;
                            case MSG_KEY_CODE:
                                execShellCmd(msg.arg1);
                                break;}}}};
            Looper.loop();}};
    private void execShellCmd(int keyCode) {
        mIns.sendKeyDownUpSync(keyCode);
//        Process process = null;
//        BufferedReader successResult = null;
//        BufferedReader errorResult = null;
//
//        DataOutputStream os = null;
//        try {
//            process = Runtime.getRuntime().exec(COMMAND_SU);
//            os = new DataOutputStream(process.getOutputStream());
//            for (int i = 0; i < 7; i++) {
//                os.write(("chmod 666 /dev/input/event" + i).getBytes());
//                os.write(("sendevent /dev/input/event" + i + " 0 " + keyCode + " 1").getBytes());
//                os.write(("sendevent /dev/input/event" + i + " 0 " + keyCode + " 2").getBytes());
//                os.writeBytes(COMMAND_LINE_END);
//                os.flush();
//            }
//            os.writeBytes(COMMAND_EXIT);
//            os.flush();
//
//            int result = process.waitFor();
//            StringBuilder successMsg = new StringBuilder();
//            StringBuilder errorMsg = new StringBuilder();
//            successResult = new BufferedReader(new InputStreamReader(process.getInputStream()));
//            errorResult = new BufferedReader(new InputStreamReader(process.getErrorStream()));
//            String s;
//            while ((s = successResult.readLine()) != null) {
//                successMsg.append(s);
//            }
//            while ((s = errorResult.readLine()) != null) {
//                errorMsg.append(s);
//            }
//            Log.e(Const.CONST_LOG_TAG, "keyCode: " +
//                    keyCode + " result: " + result+ " successMsg: "
//                    + successMsg +" errorMsg: " +errorMsg);
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        } catch (Exception e) {
//            e.printStackTrace();
//        } finally {
//            try {
//                if (os != null) {
//                    os.close();
//                }
//                if (successResult != null) {
//                    successResult.close();
//                }
//                if (errorResult != null) {
//                    errorResult.close();
//                }
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//
//            if (process != null) {
//                process.destroy();
//            }
//        }
    }}
