package com.hisense.onekeytest.apphost.socket;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import com.hisense.onekeytest.apphost.search.SearcherConst;
import com.hisense.onekeytest.apphost.util.Const;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Administrator on 2017/3/3.
 */

public class HostSocket {

    /**
     * 处理发送数据的消息Handler
     */
    private Handler mThreadHandler;

    /**
     * mWaitThreadRun被置为false时，wait线程将被停止。
     */
    private boolean mWaitThreadRun = true;

    /**
     * 读数据的间隔时间
     */
    private final int READ_DATA_INTERVAL = 100;


    /**
     * 心跳定时器
     */
//    private Timer mTimerHeartBeat;

    private final int MSG_LOOPER_QUIT = 0;
    private final int MSG_SEND_DATA = 1;
    private final int MSG_HEART_BEAT = 2;

    private Socket mSocket;
    private ServerSocket mServerSocket;
    private List<BufferedReader> mBufferedReaderList;
    private List<BufferedWriter> mBufferedWriterList;

    private boolean mIsDisMsg = false;
    /**
     * 最后一次接到心跳数据的时间集合,单位微妙
     */
//    private List<Long> mLastHeartBeatList;

    private boolean mCurDevConnected = false;

    private boolean mStarted = false;

    private HostSocket.HostSocketListener mHostSocketListener;

    private static HostSocket sInstance;
    private static Object object = new Object();

    private HostSocket() {
    }

    public static HostSocket getInstance() {
        if (sInstance == null) {
            synchronized (object) {
                if (sInstance == null) {
                    sInstance = new HostSocket();
                }
            }
        }
        return sInstance;
    }

    //开启线程
    public void start(HostSocket.HostSocketListener listener) {
        mWaitThreadRun = true;

//        mTimerHeartBeat = new Timer();
        mBufferedReaderList = new ArrayList<>();
        mBufferedWriterList = new ArrayList<>();
//        mLastHeartBeatList = new ArrayList<>();
        mHostSocketListener = listener;
        new Thread(mWaitConnectRunnalbe).start();
//        new Thread(mReadRunnable).start();
        new Thread(mWriteRunnable).start();
        Log.e(Const.CONST_LOG_TAG, "socket start");
        mStarted = true;
    }

    /**
     * @return 设备是否连接
     */
    public boolean isCurDeviceConnected() {
        return mCurDevConnected;
    }


    /**
     * 断开设备连接
     */
    public boolean disconnectDevice(int index) {
        Log.d(Const.CONST_LOG_TAG, "断开连接disconnectDevice " + index);

        if (index >= 0) {
            if (mBufferedReaderList != null && mBufferedReaderList.size() > index) {
                try {
                    mBufferedReaderList.get(index).close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (mBufferedWriterList != null && mBufferedWriterList.size() > index) {
                try {
                    mBufferedWriterList.get(index).close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return true;
        }

        mCurDevConnected = false;

        ////// TODO: 2017/3/3  注意
        mWaitThreadRun = false;


//        if (mTimerHeartBeat != null) {
//            mTimerHeartBeat.cancel();
//        }

        if (mSocket != null && !mSocket.isClosed()) {
            try {
                mSocket.close();
                mSocket = null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (mBufferedReaderList != null && mBufferedReaderList.size() > 0) {
            try {
                for (BufferedReader br : mBufferedReaderList) {
                    if (br != null) {
                        br.close();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (mBufferedWriterList != null && mBufferedWriterList.size() > 0) {
            try {
                for (BufferedWriter bw : mBufferedWriterList) {
                    if (bw != null) {
                        bw.close();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        // 通知设备断开
        if (mHostSocketListener != null) {
            mHostSocketListener.deviceDisconnect();
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
                mServerSocket = new ServerSocket(SearcherConst.SOCKET_CONNECT_PORT);
            } catch (IOException e) {
                Log.e(Const.CONST_LOG_TAG, "端口被占用");
                e.printStackTrace();
            }
            while (mWaitThreadRun) {
                try {
                    mSocket = mServerSocket.accept();
                    BufferedReader br = new BufferedReader(
                            new InputStreamReader(mSocket.getInputStream()));
                    BufferedWriter bw = new BufferedWriter(
                            new OutputStreamWriter(mSocket.getOutputStream()));
                    // 连接成功
                    mCurDevConnected = true;
                    mBufferedReaderList.add(br);
                    mBufferedWriterList.add(bw);
                    Log.e(Const.CONST_LOG_TAG, "连接成功");

                    // 开始心跳
                    if (mThreadHandler != null) {
                        Log.e(Const.CONST_LOG_TAG, "开始心跳");
//                        mThreadHandler.sendEmptyMessage(MSG_HEART_BEAT);
                    }
                    // 通知设备连接
                    if (mHostSocketListener != null) {
                        mHostSocketListener.deviceConnect(mSocket.getInetAddress().getHostAddress());
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    };


    /**
     * 读取数据的Runnable
     */
    private Runnable mReadRunnable = new Runnable() {
        @Override
        public void run() {
            while (mWaitThreadRun) {

                if (mCurDevConnected && mBufferedReaderList != null && mBufferedReaderList.size() > 0) {
                    try {
                        // 使用readLine方法读取数据
                        for (int i = 0; i < mBufferedReaderList.size(); i++) {
                            if (mBufferedReaderList.get(i) != null) {
                                String receivedJsonStr = mBufferedReaderList.get(i).readLine();
                                if (parseJsonStr(receivedJsonStr)) {
                                    // 更新最后心跳时间
//                                    mLastHeartBeatList.set(i,System.currentTimeMillis());
                                } else {
                                    if (mHostSocketListener != null) {
                                        mHostSocketListener.receivedData();
                                    }
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
     * 发送信息
     */
    private Runnable mWriteRunnable = new Runnable() {
        @SuppressLint("HandlerLeak")
        @Override
        public void run() {
            Looper.prepare();
            mThreadHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    if (msg == null) {
                        return;
                    }
                    switch (msg.what) {
                        case MSG_LOOPER_QUIT:
                            Looper.myLooper().quit();
                            break;
                        case MSG_SEND_DATA:
                            /*
                             * 接收发送数据的消息
                             */

                            Bundle bundle = msg.getData();
                            String sendJsonStr;
                            if (mIsDisMsg) {
                                JSONObject jo = new JSONObject();
                                try {
                                    jo.put(Const.CONST_PACK_TYPE, SearcherConst.SOCKET_TYPE_DISCONNECT);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                Log.e(Const.CONST_LOG_TAG, "断开连接: " + jo.toString());
                                sendJsonStr = jo.toString();
                            } else {
                                sendJsonStr = bundle.getString("send_data");
                            }

                            if (mCurDevConnected && mBufferedWriterList != null && mBufferedWriterList.size() > 0) {
                                try {
                                    // 在每条json数据的末尾添加一个'\n'分隔，方便使用readLine方法读取。
                                    for (BufferedWriter bw : mBufferedWriterList) {
                                        if (bw != null) {
                                            bw.write(sendJsonStr + '\n');
                                            bw.flush();
                                        }
                                    }
                                    if (mIsDisMsg) {
                                        disconnectDevice(-1);
                                        mIsDisMsg = false;
                                    }
                                } catch (Exception e) {
                                    Log.d("CEXX", "CEXX ---> mWriteRunnable ---> Exception : "
                                            + e.toString());
                                    e.printStackTrace();
                                }
                            }
                            break;
                        case MSG_HEART_BEAT:
                            // 开始心跳时，首次更新最后心跳时间
//                            long lastTime = System.currentTimeMillis();
//                            mLastHeartBeatList.add(lastTime);
//                            final int index = mLastHeartBeatList.size()-1;
//                            mTimerHeartBeat.schedule(new TimerTask() {
//                                @Override
//                                public void run() {
//                                    // 定时发送心跳数据
//                                    Log.e(Const.CONST_LOG_TAG, "发送心跳数据");
//                                    sendHeartBeatData();
//                                }
//                            }, 0, SearcherConst.HEART_BEAT_INTERVAL);
//
//                            mTimerHeartBeat.schedule(new TimerTask() {
//                                @Override
//                                public void run() {
//                                    // 定时判断心跳是否超时
//                                    long duration = System.currentTimeMillis() - mLastHeartBeatList.get(index);
//                                    if (duration > SearcherConst.HEART_BEAT_TIME_OUT) {
//                                        hasDisconnected(index);
//                                    }
//                                }
//                            }, 0, SearcherConst.HEART_BEAT_TIME_OUT);
                            break;
                    }
                }
            };
            Looper.loop();
        }
    };


    /**
     * 打包心跳数据
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

    /**
     * 发送数据
     *
     * @param jsonStr
     */
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
     *
     * @param receivedJsonStr json 字符串
     * @return false 不是心跳
     */
    private boolean parseJsonStr(String receivedJsonStr) {
        if (receivedJsonStr == "") {
            return false;
        }
        try {
            JSONObject jo = new JSONObject(receivedJsonStr);
            if (jo.optInt(Const.CONST_PACK_TYPE) ==
                    SearcherConst.SOCKET_TYPE_HEART_BEAT) {
                return true;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return false;
    }


    public void hasDisconnected() {
        mIsDisMsg = true;
        mThreadHandler.sendEmptyMessage(MSG_SEND_DATA);
    }

    /**
     * 资源释放
     */
    public void release() {
        if (mStarted) {
            mWaitThreadRun = false;
//            if (mThreadHandler != null) {
//                mThreadHandler.sendEmptyMessage(MSG_LOOPER_QUIT);
//            }
//            this.disconnectDevice(-1);

            if (mServerSocket != null && !mServerSocket.isClosed()) {
                try {
                    mServerSocket.close();
                    mServerSocket = null;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            mStarted = false;
        }
    }

    public interface HostSocketListener {
        /**
         * 设备连接时调用该方法，非UI线程调用
         */
        void deviceConnect(String ip);

        /**
         * 设备断开时调用该方法，非UI线程调用
         */
        void deviceDisconnect();

        /**
         * 接收到数据时调用该方法，非UI线程调用
         */
        void receivedData();
    }
}
