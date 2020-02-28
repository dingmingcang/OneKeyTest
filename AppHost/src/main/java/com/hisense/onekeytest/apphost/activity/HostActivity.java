package com.hisense.onekeytest.apphost.activity;

import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;


import com.hisense.onekeytest.apphost.R;
import com.hisense.onekeytest.apphost.adapter.DeviceAdapter;
import com.hisense.onekeytest.apphost.bean.DeviceBean;
import com.hisense.onekeytest.apphost.search.SearcherConst;
import com.hisense.onekeytest.apphost.search.SearcherHost;
import com.hisense.onekeytest.apphost.socket.HostSocket;
import com.hisense.onekeytest.apphost.util.Const;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * File Name    : HostActivity
 * Description  : 手机搜索主界面
 * Author       : dingmingcang
 * Create Date  : 2017/02/24
 */

public class HostActivity extends AppCompatActivity implements View.OnClickListener, AdapterView.OnItemClickListener, HostSocket.HostSocketListener {

    private static final int MESSAGE_SEARCH_START = 40001;
    private static final int MESSAGE_SEARCH_FINISH = 40002;
    private static final int MESSAGE_SEARCH_FINISH_NOT_FOUND = 40003;

    private Button mSearchButton, mConnectButton, mDisConnectButton, mRemoteControl;
    private ListView mListView;
    private LinearLayout mLayout;
    private boolean mIsFirst = true;

    private BaseAdapter mAdapter;

    private List<DeviceBean> mDeviceList;      //搜索到的设备集合
    private List<DeviceBean> mConDeviceList;   //要连接的设备集合
    private List<String> mConnectingList;      //已连接的设备IP集合

    private MyHandler mHandler = new MyHandler(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        if (android.os.Build.VERSION.SDK_INT > 9) {
//            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
//                    .permitAll().build();
//            StrictMode.setThreadPolicy(policy);
//        }

        initView();
        initData();
        setData();
        setListener();
    }

    private void setListener() {
        mSearchButton.setOnClickListener(this);
        mConnectButton.setOnClickListener(this);
        mDisConnectButton.setOnClickListener(this);
        mRemoteControl.setOnClickListener(this);
//        mListView.setOnItemClickListener(this);
    }

    private void setData() {
        mListView.setAdapter(mAdapter);
    }

    private void initData() {

        mDeviceList = new ArrayList<>();
        mConDeviceList = new ArrayList<>();
        mConnectingList = new ArrayList<>();

        mAdapter = new DeviceAdapter(mDeviceList, this);
    }

    private void initView() {
        mSearchButton = $(R.id.btn_search);
        mConnectButton = $(R.id.btn_main_all_connect);
        mDisConnectButton = $(R.id.btn_main_all_disconnect);
        mRemoteControl = $(R.id.btn_remote_control);
        mListView = $(R.id.lv_main_device_ip);
        mLayout = $(R.id.ly_main_button_all);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_search:
                startToSearch();
                if (mIsFirst) {
                    HostSocket.getInstance().start(this);
                    mIsFirst = false;
                }
                break;
            case R.id.btn_remote_control:
                startActivity(new Intent(this, RemoteActivity.class));
                break;
            case R.id.btn_main_all_connect:
                sendConnect();
                break;
            case R.id.btn_main_all_disconnect:
                sendDisConnect("");
                break;
        }
    }

    /**
     * 开始搜索
     */
    private void startToSearch() {
        new SearcherHost() {
            @Override
            public void onSearchStart() {
                pushMessage(MESSAGE_SEARCH_START);
            }

            @Override
            public void onSearchFinish(Set deviceSet) {
                Log.e(Const.CONST_LOG_TAG, "onSearchFinish: ");
                if (deviceSet.size() == 0) {
                    pushMessage(MESSAGE_SEARCH_FINISH_NOT_FOUND);
                    return;
                }
                if (mDeviceList == null) {
                    Log.e(Const.CONST_LOG_TAG, "onSearchFinish: null");
                    mDeviceList = new ArrayList<>();
                }
                if (mDeviceList.size() != 0) {
                    if (!mDeviceList.equals(deviceSet)) {
                        Log.e(Const.CONST_LOG_TAG, "onSearchFinish: !=");
                        mDeviceList.clear();
                        mDeviceList.addAll(deviceSet);
                        pushMessage(MESSAGE_SEARCH_FINISH);
                    }
                } else {
                    Log.e(Const.CONST_LOG_TAG, "onSearchFinish: =0");
                    mDeviceList.addAll(deviceSet);
                    pushMessage(MESSAGE_SEARCH_FINISH);
                }
            }

            @Override
            public void printLog(String log) {
                Log.e(SearcherHost.class.getSimpleName(), "Searching..." + log);
            }
        }.search();
    }

    private void pushMessage(int what) {
        mHandler.sendEmptyMessage(what);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        TextView text = (TextView) view.findViewById(R.id.tv_item_text);
        if (!mDeviceList.get(i).getIsConnect()) {
            text.setText(getResources().getString(R.string.str_item_disconnect));
            mDeviceList.get(i).setIsConnect(true);
            mConDeviceList.clear();
            mConDeviceList.add(mDeviceList.get(i));
            sendConnect();
        } else {
            text.setText(getResources().getString(R.string.str_item_connect));
            mDeviceList.get(i).setIsConnect(false);
            sendDisConnect(mDeviceList.get(i).getIp());
        }
    }

    /**
     * 发送断开命令
     *
     * @param ip
     */
    private void sendDisConnect(String ip) {
        Log.e(Const.CONST_LOG_TAG, "sendDisConnect: " + ip);
        if (!HostSocket.getInstance().isCurDeviceConnected()) {
            return;
        }
        HostSocket.getInstance().hasDisconnected();

        if (mConnectingList != null && mConnectingList.size() > 0) {
            mConnectingList.clear();
            mDeviceList.clear();
            mAdapter.notifyDataSetChanged();
            mListView.setVisibility(View.GONE);
            mRemoteControl.setVisibility(View.GONE);
            mLayout.setVisibility(View.GONE);
            mSearchButton.setText(getResources().getString(R.string.str_btn_search));
        }

    }

    /**
     * 发送连接命令
     */
    private void sendConnect() {
        Log.e(Const.CONST_LOG_TAG, "connect: start");
        if (mDeviceList == null) {
            Toast.makeText(this, getResources().getString(R.string.str_toast_no_search), Toast.LENGTH_SHORT).show();
            return;
        }
        sendConnectUDP();
    }


    /**
     * Socket状态回调
     */
    @Override
    public void deviceConnect(final String ip) {
        mConnectingList.add(ip);
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mRemoteControl.setVisibility(View.VISIBLE);
                Toast.makeText(HostActivity.this, ip + " 已连接", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void deviceDisconnect() {
        HostSocket.getInstance().release();
        mIsFirst = true;
    }

    @Override
    public void receivedData() {

    }

    /**
     * Handler
     */
    private static class MyHandler extends Handler {
        private WeakReference<HostActivity> ref;

        MyHandler(HostActivity activity) {
            ref = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            HostActivity activity = ref.get();
            switch (msg.what) {
                case MESSAGE_SEARCH_START:
                    activity.mSearchButton.setText(activity.getResources().getString(R.string.str_btn_searching));
                    break;
                case MESSAGE_SEARCH_FINISH:
                    activity.mSearchButton.setText(activity.getResources().getString(R.string.str_btn_search_over));
                    activity.mListView.setVisibility(View.VISIBLE);
                    activity.mLayout.setVisibility(View.VISIBLE);
                    activity.mAdapter.notifyDataSetChanged();
                    break;
                case MESSAGE_SEARCH_FINISH_NOT_FOUND:
                    activity.mSearchButton.setText(activity.getResources().getString(R.string.str_toast_no_search));
                    activity.mListView.setVisibility(View.GONE);
                    activity.mLayout.setVisibility(View.GONE);
                    break;
                default:
                    break;
            }
        }
    }


    /**
     * 发送建立连接广播
     */
    public void sendConnectUDP() {
        new Thread() {
            @Override
            public void run() {
                super.run();
                try {
                    byte[] data = packConnect();
                    InetSocketAddress addr = new InetSocketAddress(InetAddress.getByName("255.255.255.255"), SearcherConst.DEVICE_FIND_PORT);
                    DatagramPacket datagramPacket = new DatagramPacket(data, data.length, addr);
                    final DatagramSocket sendSocket = new DatagramSocket();
                    sendSocket.send(datagramPacket);
                } catch (SocketException | UnknownHostException e) {
                    e.printStackTrace();
                    Log.e(Const.CONST_LOG_TAG, "初始化建立连接广播DatagramPacket失败");
                } catch (IOException e) {
                    Log.e(Const.CONST_LOG_TAG, "发送建立连接广播失败");
                    e.printStackTrace();
                }
            }
        }.start();
    }


    /**
     * 打包建立连接报文
     *
     * @return
     */
    private byte[] packConnect() {
        String json = "$";
        JSONObject jo = new JSONObject();
        try {
            jo.put(Const.CONST_PACK_TYPE, SearcherConst.PACKET_TYPE_FIND_DEVICE_SOCKET);
            JSONArray jsa = new JSONArray();
            for (int i = 0; i < mDeviceList.size(); i++) {
                JSONObject jso = new JSONObject();
                jso.put(Const.CONST_DEVICE_IP, mDeviceList.get(i).getIp());
                jsa.put(jso);
            }
            jo.put(Const.CONST_DEVICE_IPS, jsa);
            json += jo.toString();
            Log.e(Const.CONST_LOG_TAG, "connect data:" + json);
        } catch (JSONException e) {
            Log.e(Const.CONST_LOG_TAG, "add search data error");
            e.printStackTrace();
        }
        for (int i = json.length(); i < 1024; i++) {
            json += "#";
        }
        return json.getBytes();
    }


    private <V extends View> V $(int id) {
        return (V) findViewById(id);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        sendDisConnect("out app");
        finish();
    }
}
