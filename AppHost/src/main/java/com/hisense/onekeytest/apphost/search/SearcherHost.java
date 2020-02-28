package com.hisense.onekeytest.apphost.search;

import android.util.Log;

import com.hisense.onekeytest.apphost.bean.DataBean;
import com.hisense.onekeytest.apphost.bean.DeviceBean;
import com.hisense.onekeytest.apphost.util.Const;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Set;

import static com.hisense.onekeytest.apphost.search.SearcherConst.PACKET_TYPE_FIND_DEVICE_RSP_11;

/**
 * File Name    ： SearcherHost
 * Description  ： 搜索主机
 * Author       ： dingmingcang
 * Create Date  ： 2017/02/23
 */
public abstract class SearcherHost<T extends DeviceBean> extends Thread {

    private int mUserDataMaxLen;
    private Class<T> mDeviceClazz;

    private DatagramSocket mHostSocket;
    private Set<T> mDeviceSet;
    private DatagramPacket mSendPack;

    private int mPackType;
    private String mDeviceIP;

    public SearcherHost() {
        this(0, DeviceBean.class);
    }

    public SearcherHost(int userDataMaxLen, Class clazz) {
        mDeviceClazz = clazz;
        mUserDataMaxLen = userDataMaxLen;
        mDeviceSet = new HashSet<>();
        try {
            mHostSocket = new DatagramSocket();
            // 设置接收超时时间
            mHostSocket.setSoTimeout(SearcherConst.RECEIVE_TIME_OUT);
            byte[] sendData = new byte[1024];
            InetAddress broadIP = InetAddress.getByName("255.255.255.255");
            mSendPack = new DatagramPacket(sendData, sendData.length, broadIP, SearcherConst.DEVICE_FIND_PORT);
        } catch (SocketException | UnknownHostException e) {
            printLog(e.toString());
            if (mHostSocket != null) {
                mHostSocket.close();
            }
        }
    }

    /**
     * 开始搜索
     *
     * @return true-正常启动，false-已经start()启动过，无法再启动。若要启动需重新new
     */
    public boolean search() {
        if (this.getState() != State.NEW) {
            return false;
        }
        printLog("START");
        this.start();
        return true;
    }

    @Override
    public void run() {
        if (mHostSocket == null || mHostSocket.isClosed() || mSendPack == null) {
            return;
        }

        try {
            // ready
            onSearchStart();
            // start
            boolean isFound = false;
            for (int i = 0; i < 3; i++) {
                // 发送搜索广播
                mPackType = SearcherConst.PACKET_TYPE_FIND_DEVICE_REQ_10;
                mSendPack.setData(packData(i + 1));
                mHostSocket.send(mSendPack);
                // 监听来信
                byte[] receData = new byte[4096];
                DatagramPacket recePack = new DatagramPacket(receData, receData.length);
                try {
                    // 最多接收250个，或超时跳出循环
                    int rspCount = SearcherConst.RESPONSE_DEVICE_MAX;
                    while (rspCount-- > 0) {
                        recePack.setData(receData);
                        mHostSocket.receive(recePack);
                        Log.e(Const.CONST_LOG_TAG, "搜索响应");
                        if (recePack.getLength() > 0) {
                            mDeviceIP = recePack.getAddress().getHostAddress();
                            if (parsePack(recePack)) {
                                printLog("a response from：" + mDeviceIP);
                                isFound = true;
                            }else {
                                break;
                            }
                        }
                    }
                } catch (SocketTimeoutException e) {
                    printLog("udp timeout");
                }
                printLog(String.format("the %dth search finished", i));
                if (isFound){
                    break;
                }
            }
            // finish
            onSearchFinish(mDeviceSet);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (mHostSocket != null) {
                mHostSocket.close();
            }
        }

    }

    /**
     * 搜索开始时执行
     */
    public abstract void onSearchStart();

    /**
     * 打包搜索时的用户数据
     */
    protected void packUserData_Search(JSONObject jo) {

    }

    /**
     * 打包确认时的用户数据
     */
    protected void packUserData_Check(JSONObject jo) {
    }


    /**
     * 解析数据
     * parse if have userData
     *
     * @param type     数据类型
     * @param device   设备
     * @param userData 数据
     * @return return the result of parse, true if parse success, else false
     */
    public boolean parseUserData(byte type, T device, byte[] userData) {
        return true;
    }

    /**
     * 搜索结束后执行
     *
     * @param deviceSet 搜索到的设备集合
     */
    public abstract void onSearchFinish(Set deviceSet);

    /**
     * 打印日志
     */
    public void printLog(String log) {
        Log.e(Const.CONST_LOG_TAG, log);
    }


    /**
     * 解析搜索响应报文
     * 协议：$ + json
     * json：json: packType + userData
     *
     * @param pack 数据报
     */
    private boolean parsePack(DatagramPacket pack) {
        printLog("parse search response word");
        if (pack == null || pack.getAddress() == null) {
            return false;
        }
        String ip = pack.getAddress().getHostAddress();
        int port = pack.getPort();

        //去重
        for (T d : mDeviceSet) {
            if (d.getIp().equals(ip)) {
                return false;
            }
        }
        // 解析数据
        byte[] data = new byte[pack.getLength()];
        ByteArrayInputStream bais = new ByteArrayInputStream(pack.getData());
        try {
            bais.read(data);
            bais.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        String json = new String(data);
        Log.e(Const.CONST_LOG_TAG, json );
        if (!json.startsWith("$")) {
            return false;
        }
        DataBean bean = parseJson(json.substring(1, json.length()));
        if (bean.packType != PACKET_TYPE_FIND_DEVICE_RSP_11) {
            return false;
        }
        T device = null;
        try {
            Constructor constructor = mDeviceClazz.getDeclaredConstructor(String.class, int.class);
            device = (T) constructor.newInstance(ip, port);
        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
            Log.e(Const.CONST_LOG_TAG, "device error" );
        }
        if (device == null) {
            Log.e(Const.CONST_LOG_TAG, "parsePack: device == null");
            return false;
        }
        return mDeviceSet.add(device);
    }

    /**
     * 打包搜索报文
     * 协议：$ + json
     * json:
     * packType - 报文类型
     * sendSeq - 发送序列
     * deviceIpLen - 设备IP长度
     * deviceIp - 设备IP，仅在确认时携带
     * userData - 用户数据
     *
     * @param seq 发送序列号
     */
    private byte[] packData(int seq) {
        String json = "$";
        JSONObject jo = new JSONObject();
        try {
            jo.put(Const.CONST_PACK_TYPE, mPackType);
            seq = seq == 3 ? 1 : ++seq;
            jo.put(Const.CONST_SEND_SEQ, seq);
            switch (mPackType) {
                case SearcherConst.PACKET_TYPE_FIND_DEVICE_REQ_10: {
                    // 添加用户数据,添加后需修改设备解析方法
                    packUserData_Search(jo);
                    break;
                }
                case SearcherConst.PACKET_TYPE_FIND_DEVICE_CHK_12: {
                    // deviceIp
                    jo.put(Const.CONST_DEVICE_IP, mDeviceIP.getBytes(Charset.forName("UTF-8")));
                    // 添加用户数据,添加后需修改设备解析方法
                    packUserData_Check(jo);
                    break;
                }
                default:
                    break;
            }
            json += jo.toString();
            for (int i = json.length(); i < 1024; i++) {
                json += "#";
            }
            Log.e(Const.CONST_LOG_TAG, "search word:" + json);
        } catch (JSONException e) {
            Log.e(Const.CONST_LOG_TAG, "add search word error");
            e.printStackTrace();
        }
        return json.getBytes();
    }

    /**
     * 解析json字符串，手动解析
     *
     * @param json
     * @return DataBean
     */
    private DataBean parseJson(String json) {
        //还需要优化
        DataBean bean = new DataBean();
        try {
            JSONObject jo = new JSONObject(json);
            bean.packType = jo.optInt(Const.CONST_PACK_TYPE);
            bean.deviceIp = jo.optString(Const.CONST_DEVICE_IP);
            bean.deviceIpLen = jo.optString(Const.CONST_DEVICE_IP_LEN);
            bean.deviceName = jo.optString(Const.CONST_DEVICE_NAME);
            bean.keyCode = jo.optInt(Const.CONST_KEY_CODE);
            bean.keyType = jo.getString(Const.CONST_KEY_TYPE);
            bean.sendSeq = jo.optInt(Const.CONST_SEND_SEQ);
            bean.versionCode = jo.optString(Const.CONST_VERSION_CODE);
        } catch (JSONException e) {
            e.printStackTrace();
            Log.e("TAG", "parseJson: ERROR");
        }
        return bean;
    }
}
