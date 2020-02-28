package com.hisense.onekeytest.appdevice.socket;

import android.annotation.SuppressLint;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import com.hisense.onekeytest.appdevice.application.DeviceApplication;
import com.hisense.onekeytest.appdevice.bean.KeyCodeBean;
import com.hisense.onekeytest.appdevice.search.SearcherConst;
import com.hisense.onekeytest.appdevice.util.Const;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;

import static android.content.Context.WIFI_SERVICE;

/**
 * Created by Administrator on 2017/3/3.
 */

public class DeviceSocket {


    /**
     * 处理发送数据的消息Handler
     */
    private Handler mThreadHandler;

    /**
     * mReadThreadRun被置为false时，读取数据的线程将被停止。
     */
    private boolean mReadThreadRun = true;

    /**
     * 读数据的间隔时间
     */
    private final int READ_DATA_INTERVAL = 100;


    /**
     * 最后一次接到心跳数据的时间,单位微妙
     */
    private long mLastHeartBeat = -1;

    /**
     * 心跳定时器
     */
    private Timer mTimerHeartBeat;

    private final int MSG_LOOPER_QUIT = 0;
    private final int MSG_SEND_DATA = 1;
    private final int MSG_HEART_BEAT = 2;

    private String mHostIp;
    private String mIp = getOwnIp();
    private Socket mSocket;
    private BufferedReader mBufferedReader;
    private BufferedWriter mBufferedWriter;
    private boolean mCurDevConnected = false;

    private DeviceSocket.DeviceSocketListener mDeviceSocketListener;

    private static DeviceSocket sInstance;
    private static Object object = new Object();

    private DeviceSocket(String ip) {
        this.mHostIp = ip;
    }

    public static DeviceSocket getInstance(String hostIp) {
        if (sInstance == null) {
            synchronized (object) {
                if (sInstance == null) {
                    sInstance = new DeviceSocket(hostIp);
                }
            }
        }
        return sInstance;
    }

    //开启线程
    public void start(DeviceSocket.DeviceSocketListener listener) {
        mReadThreadRun = true;

        mDeviceSocketListener = listener;
        new Thread(mWaitConnectRunnalbe).start();
        new Thread(mReadRunnable).start();
        new Thread(mWriteRunnable).start();
    }


    /**
     * @return  设备是否连接

     */
    public boolean isCurDeviceConnected() {
        return mCurDevConnected;
    }


    /**
     * 断开设备连接
     */
    private boolean disconnectDevice() {
        Log.d("CEXX", "CEXX ---> disconnectDevice");

        mCurDevConnected = false;


        ////// TODO: 2017/3/3  注意
        mReadThreadRun = false;


        if (mTimerHeartBeat != null) {
            mTimerHeartBeat.cancel();
        }

        if (mSocket != null && !mSocket.isClosed()) {
            try {
                mSocket.close();
                mSocket = null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (mBufferedReader != null) {
            try {
                mBufferedReader.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (mBufferedWriter != null) {
            try {
                mBufferedWriter.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return true;
    }


    /**
     * 等待连接
     */
    private Runnable mWaitConnectRunnalbe = new Runnable() {
        @Override
        public void run() {
            try {
                mSocket = new Socket(mHostIp, SearcherConst.SOCKET_CONNECT_PORT);
                mBufferedReader = new BufferedReader(
                        new InputStreamReader(mSocket.getInputStream()));
                mBufferedWriter = new BufferedWriter(
                        new OutputStreamWriter(mSocket.getOutputStream()));
                // 连接成功
                mCurDevConnected = true;
                Log.e(Const.CONST_LOG_TAG, "成功建立连接");
                ////// TODO: 2017/3/3  注意
                mReadThreadRun = true;

                // 开始心跳
                if (mThreadHandler != null) {
                    mThreadHandler.sendEmptyMessage(MSG_HEART_BEAT);
                }
                // 通知设备连接
                if (mDeviceSocketListener != null) {
                    mDeviceSocketListener.deviceConnect();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    };


    /**
     * 读取数据的Runnable
     */
    private Runnable mReadRunnable = new Runnable() {
        @Override
        public void run() {
            while (mReadThreadRun) {
                if (mCurDevConnected && mBufferedReader != null) {
                    try {
                        // 使用readLine方法读取数据
                        String receivedJsonStr = mBufferedReader.readLine();
                        KeyCodeBean bean = parseJsonStr(receivedJsonStr);
                        Log.e(Const.CONST_LOG_TAG, receivedJsonStr);
                        if (bean != null) {
                            // 更新最后时间
                            Log.e(Const.CONST_LOG_TAG, "接收消息");
                            mLastHeartBeat = System.currentTimeMillis();
                            if (bean.packType == SearcherConst.SOCKET_TYPE_DISCONNECT){
                                //断开连接
                                Log.e(Const.CONST_LOG_TAG, "接收断开连接消息");
                                hasDisconnected();
                            } else {
                                if (mDeviceSocketListener != null) {
                                    mDeviceSocketListener.receivedData(bean);
                                }
                            }
                        }
                    } catch (Exception e) {
                        Log.d("CEXX", "CEXX ---> e.toString() : " + e.toString());
                        e.printStackTrace();
                    }
                }

                try {
                    Thread.sleep(READ_DATA_INTERVAL);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    };


    /**
     *  发送心跳连接
     */
    private Runnable mWriteRunnable = new Runnable() {
        @SuppressLint("HandlerLeak")
        @Override
        public void run() {
            Looper.prepare();
            mThreadHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    switch (msg.what) {
                        case MSG_LOOPER_QUIT:
                            Looper.myLooper().quit();
                            break;
                        case MSG_SEND_DATA:
                            /*
                             * 接收发送数据的消息
                             */
                            Bundle bundle = msg.getData();
                            String sendJsonStr = bundle.getString("send_data");

                            if (mCurDevConnected && mBufferedWriter != null) {
                                try {
                                    // 在每条json数据的末尾添加一个'\n'分隔，方便使用readLine方法读取。
                                    mBufferedWriter.write(sendJsonStr + '\n');
                                    mBufferedWriter.flush();
                                    Log.e(Const.CONST_LOG_TAG, "发送心跳  "+sendJsonStr);

                                    // Log.d("CEXX", "CEXX ---> mWriteRunnable
                                    // ---> jsonStr : " + sendJsonStr);
                                } catch (Exception e) {
                                    Log.d("CEXX", "CEXX ---> mWriteRunnable ---> Exception : "
                                            + e.toString());
                                    e.printStackTrace();
                                }
                            }
                            break;
                        case MSG_HEART_BEAT:
                            mTimerHeartBeat = new Timer();

                            // 开始心跳时，首次更新最后心跳时间
                            mLastHeartBeat = System.currentTimeMillis();

//                            mTimerHeartBeat.schedule(new TimerTask() {
//                                @Override
//                                public void run() {
//                                    // 定时发送心跳数据
//                                    Log.e(Const.CONST_LOG_TAG, "发送心跳数据");
//                                    sendHeartBeatData();
//                                }
//                            }, 0, SearcherConst.HEART_BEAT_INTERVAL);

                            mTimerHeartBeat.schedule(new TimerTask() {
                                @Override
                                public void run() {
                                    // 定时判断是否超时
                                    long duration = System.currentTimeMillis() - mLastHeartBeat;
                                    if (duration > SearcherConst.HEART_BEAT_TIME_OUT) {
                                        hasDisconnected();
                                        Log.e(Const.CONST_LOG_TAG, "心跳超时");
                                    }
                                }
                            }, 0, SearcherConst.HEART_BEAT_TIME_OUT);
                            break;
                    }
                }
            };
            Looper.loop();
        }
    };


    /**
     *  打包心跳数据
     */
    private void sendHeartBeatData() {
        try {
            JSONObject jo = new JSONObject();
            jo.put(Const.CONST_PACK_TYPE, SearcherConst.SOCKET_TYPE_HEART_BEAT);
            String jsonStr = jo.toString();
            this.sendJsonData(jsonStr + "\n");

            // Log.d("CEXX", "CEXX ---> sendHeartBeatData ---> jsonStr : " +
            // jsonStr);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void sendJsonData(String jsonStr) {
        if (TextUtils.isEmpty(jsonStr)) {
            return;
        }

        if (mThreadHandler != null) {
            Bundle bundle = new Bundle();
            bundle.putString("send_data", jsonStr);
            Message msg = mThreadHandler.obtainMessage();
            msg.what = MSG_SEND_DATA;
            msg.setData(bundle);
            mThreadHandler.sendMessage(msg);
            // Log.d("CEXX", "CEXX ---> sendJsonData ---> jsonStr : " +
            // jsonStr);
        }
    }
    /**
     * 解析报文
     * @param receivedJsonStr json 字符串
     * @return KeyCodeBean 数据类
     */
    private KeyCodeBean parseJsonStr(String receivedJsonStr) {
        Log.e(Const.CONST_LOG_TAG, "parseJsonStr: "+receivedJsonStr );
        if (receivedJsonStr.equals("")){
            return null;
        }
        KeyCodeBean bean = new KeyCodeBean();
        try {
            JSONObject jo = new JSONObject(receivedJsonStr);
            bean.packType = jo.optInt(Const.CONST_PACK_TYPE);
            if (bean.packType != SearcherConst.SOCKET_TYPE_KEY_CODE){
                bean.deviceIp = jo.optString(Const.CONST_DEVICE_IP);
                Log.e(Const.CONST_LOG_TAG, "DeviceSocket：parseJsonStr: "+bean.toString());
                return bean;
            }
            bean.keyType = jo.optString(Const.CONST_KEY_TYPE);
            bean.keyCode = jo.optInt(Const.CONST_KEY_CODE);
            Log.e(Const.CONST_LOG_TAG, "DeviceSocket：parseJsonStr: "+bean.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return bean;
    }



    private void hasDisconnected() {
        Log.e(Const.CONST_LOG_TAG, "断开连接");
        this.disconnectDevice();
        // 通知设备断开
        if (mDeviceSocketListener != null) {
            mDeviceSocketListener.deviceDisconnect();
        }
    }

    public interface DeviceSocketListener {

        /**
         * 设备连接时调用该方法，非UI线程调用
         */
        void deviceConnect();

        /**
         * 设备断开时调用该方法，非UI线程调用
         */
        void deviceDisconnect();

        /**
         * 接收到数据时调用该方法，非UI线程调用
         *
         */
        void receivedData(KeyCodeBean keyCode);
    }


    /**
     * 获取本机在Wifi中的IP
     * @return 本机地址
     */
    private String getOwnIp() {
        WifiManager wifiManager = (WifiManager) DeviceApplication.getIntence().getSystemService(WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        // 获取32位整型IP地址
        int ipAddress = wifiInfo.getIpAddress();

        String ip = String.format("%d.%d.%d.%d",
                (ipAddress & 0xff), (ipAddress >> 8 & 0xff),
                (ipAddress >> 16 & 0xff), (ipAddress >> 24 & 0xff));
        Log.e(Const.CONST_LOG_TAG, "getOwnIp: " + ip);
        //返回整型地址转换成“*.*.*.*”地址
        return ip;
    }

}
