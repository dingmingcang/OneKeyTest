package com.hisense.onekeytest.appdevice.search;

import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import com.hisense.onekeytest.appdevice.application.DeviceApplication;
import com.hisense.onekeytest.appdevice.bean.DataBean;
import com.hisense.onekeytest.appdevice.bean.DeviceIpBean;
import com.hisense.onekeytest.appdevice.util.Const;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.ArrayList;

import static android.content.Context.WIFI_SERVICE;

/**
 * File Name    ：SearcherDevice
 * Description  ：搜索者设备
 * Author       ： dingmingcang
 * Create Date  ： 2017/02/24
 */
public abstract class SearcherDevice extends Thread {
    private volatile boolean mOpenFlag;
    private DatagramSocket mSocket;
    /**
     * 打开
     * 即可以上线
     */
    public boolean open() {
        // 线程只能start()一次，重启必须重新new。因此这里也只能open()一次
        if (this.getState() != State.NEW) {
            return false;}
        mOpenFlag = true;
        this.start();
        return true;}
    /**
     * 关闭
     */
    public void close() {
        mOpenFlag = false;}
    @Override
    public void run() {
        printLog("设备开启");
        DatagramPacket recePack = null;
        try {mSocket = new DatagramSocket(SearcherConst.DEVICE_FIND_PORT);
            // 初始
            mSocket.setSoTimeout(SearcherConst.DEVICE_RECEIVE_DEFAULT_TIME_OUT);
            byte[] buf = new byte[4096];
            recePack = new DatagramPacket(buf, buf.length);
        } catch (SocketException e) {
            e.printStackTrace();}
        if (mSocket == null || mSocket.isClosed() || recePack == null) {
            return;}
        boolean isSearch = false;
        while (mOpenFlag) {
            try {
                // waiting for search from host
                mSocket.receive(recePack);
                // verify the data
                DataBean searchBean = verifySearchData(recePack, "search");
                if (searchBean.packType != SearcherConst.PACKET_TYPE_FIND_DEVICE_REQ_10
                        || searchBean.sendSeq < 1 || searchBean.sendSeq > 3) {
                    printLog("search data type error or seq overstep");
                    if (searchBean.packType != SearcherConst.PACKET_TYPE_FIND_DEVICE_SOCKET || !isSearch){
                        break;}
                    printLog("确认成功");
                    onDeviceSearched((InetSocketAddress) recePack.getSocketAddress());
                    mOpenFlag = false;
                    mSocket.setSoTimeout(SearcherConst.DEVICE_RECEIVE_DEFAULT_TIME_OUT); // 还原连接超时
                }
                isSearch = true;
                byte[] sendData = packData();
                DatagramPacket sendPack = new DatagramPacket(sendData, sendData.length, recePack.getAddress(), recePack.getPort());
                printLog("接收到请求，给主机回复信息");
                mSocket.send(sendPack);
                printLog("等待主机接收确认");
                mSocket.setSoTimeout(SearcherConst.RECEIVE_TIME_OUT_CONNECT);
                mSocket.receive(recePack);
                DataBean okBean = verifySearchData(recePack, "search");
                if (okBean.packType != SearcherConst.PACKET_TYPE_FIND_DEVICE_SOCKET) {
                    break;}
                printLog("确认成功");
                onDeviceSearched((InetSocketAddress) recePack.getSocketAddress());
                mOpenFlag = false;
                mSocket.setSoTimeout(SearcherConst.DEVICE_RECEIVE_DEFAULT_TIME_OUT); // 还原连接超时
            } catch (IOException e) {}}
        mSocket.close();
        printLog("设备已被找到关闭");
//                    while (mOpenFlag) {
//                        try {
//                            mSocket.receive(recePack);
//                            if (verifyCheckData(recePack)) {
//                                printLog("确认成功");
//                                //等待连接指令
//                                mSocket.setSoTimeout(SearcherConst.RECEIVE_TIME_OUT_CONNECT);
//                                while (mOpenFlag) {
//                                    printLog("等待连接");
//                                    mSocket.receive(recePack);
//                                    if (verifySearchData(recePack, "ok")) {
//                                        printLog("接到连接指令，开始连接");
//                                        onDeviceSearched((InetSocketAddress) recePack.getSocketAddress());
//                                        mOpenFlag = false;
//                                    }
//                                }
//                            }
//                        } catch (SocketTimeoutException e) {
//
//                        }
//                        mSocket.setSoTimeout(SearcherConst.RECEIVE_TIME_OUT);
//                    }
    }
    /**
     * 打包响应报文
     * 协议：$ + json
     * json: packType + userData
     */
    private byte[] packData() {
        String json = "$";
        JSONObject jo = new JSONObject();
        try {
            jo.put(Const.CONST_PACK_TYPE, SearcherConst.PACKET_TYPE_FIND_DEVICE_RSP_11);
            packUserData(jo);
            json += jo.toString();
        } catch (JSONException e) {
            e.printStackTrace();}
        printLog(json);
        return json.getBytes();}
    /**
     * 校验搜索数据
     * 协议：$ + json
     * packType - 报文类型
     * sendSeq - 发送序列
     * deviceIpLen - 设备IP长度
     * deviceIp - 设备IP，仅在确认时携带
     * userData - 用户数据
     */
    private DataBean verifySearchData(DatagramPacket pack, String str) {
        byte[] data = new byte[pack.getLength()];
        ByteArrayInputStream bais = new ByteArrayInputStream(pack.getData());
        try {
            bais.read(data);
            bais.close();
        } catch (IOException e) {
            e.printStackTrace();}
        String jstr = new String(data);
        printLog(jstr);
        if (!jstr.startsWith("$")) {
            printLog("search data first not $" + "        " + str);
            return null;}
        int index = jstr.contains("#") ? jstr.indexOf("#") : jstr.length();
        return parseJson(jstr.substring(1, index));}
    /**
     * 校验确认数据
     * 协议：$ + json
     * packType - 报文类型
     * sendSeq - 发送序列
     * deviceIpLen - 设备IP长度
     * deviceIp - 设备IP，仅在确认时携带
     * userData - 用户数据
     */
//    private boolean verifyCheckData(DatagramPacket pack) {
//
//        byte[] data = new byte[pack.getLength()];
//        ByteArrayInputStream bais = new ByteArrayInputStream(pack.getData());
//        try {
//            bais.read(data);
//            bais.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        String jstr = new String(data);
//        printLog(jstr);
//        if (!jstr.startsWith("$")) {
//            printLog("confirm data first not $");
//            return false;
//        }
//        int index = jstr.contains("#") ? jstr.indexOf("#") : jstr.length();
//        DataBean bean = parseJson(jstr.substring(1, index));
//        if (bean.packType != SearcherConst.PACKET_TYPE_FIND_DEVICE_CHK_12
//                || bean.sendSeq < 1 || bean.sendSeq > SearcherConst.RESPONSE_DEVICE_MAX) {
//            printLog("confirm data type error or seq overstep");
//            return false;
//        }
////        String mip = getOwnIp();
////        if (!mip.equals(new String(bean.deviceIp))) {
////            Log.e(Const.CONST_LOG_TAG, "verifyCheckData: IP ERROR"+mip+"   "+new String(bean.deviceIp));
////            return false;
////        }
//        return true;
//    }
    /**
     * 打包用户数据
     * 如果调用者需要，则重写
     *
     * @return
     */
    protected void packUserData(JSONObject jo) {}
    /**
     * 当设备被发现时执行
     */
    public void onDeviceSearched(InetSocketAddress socketAddr) {}
    /**
     * 解析json字符串，手动解析
     *
     * @param json
     * @return DataBean
     */
    private DataBean parseJson(String json) {
        printLog(json);
        DataBean bean = new DataBean();
        try {
            JSONObject jo = new JSONObject(json);
            bean.packType = jo.optInt(Const.CONST_PACK_TYPE);
            switch (bean.packType) {
                case SearcherConst.PACKET_TYPE_FIND_DEVICE_REQ_10:
                    bean.sendSeq = jo.optInt(Const.CONST_SEND_SEQ);
                    break;
                case SearcherConst.PACKET_TYPE_FIND_DEVICE_CHK_12:
                    bean.sendSeq = jo.optInt(Const.CONST_SEND_SEQ);
                    bean.deviceIp = jo.optString(Const.CONST_DEVICE_IP);
                    bean.deviceName = jo.optString(Const.CONST_DEVICE_NAME);
                    bean.versionCode = jo.optString(Const.CONST_VERSION_CODE);
                    break;
                case SearcherConst.PACKET_TYPE_FIND_DEVICE_SOCKET:
                    bean.deviceIps = new ArrayList<>();
                    JSONArray jsa = jo.optJSONArray(Const.CONST_DEVICE_IPS);
                    for (int i = 0; i < jsa.length(); i++) {
                        JSONObject joo = jsa.optJSONObject(i);
                        DeviceIpBean ip = new DeviceIpBean();
                        ip.deviceIp = joo.optString(Const.CONST_DEVICE_IP);
                        bean.deviceIps.add(ip);}
                    break;
                case SearcherConst.SOCKET_TYPE_KEY_CODE:
                    bean.keyCode = jo.optInt(Const.CONST_KEY_CODE);
                    bean.keyType = jo.getString(Const.CONST_KEY_TYPE);
                    break;}
            Log.e(Const.CONST_LOG_TAG, bean.toString());
        } catch (JSONException e) {
            e.printStackTrace();
            printLog("parseJson: ERROR");}
        return bean;}
    /**
     * 获取本机在Wifi中的IP
     *
     * @return 本机地址
     */
    public String getOwnIp() {
        WifiManager wifiManager = (WifiManager) DeviceApplication.getIntence().getSystemService(WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        // 获取32位整型IP地址
        int ipAddress = wifiInfo.getIpAddress();
        String ip = String.format("%d.%d.%d.%d",
                (ipAddress & 0xff), (ipAddress >> 8 & 0xff),
                (ipAddress >> 16 & 0xff), (ipAddress >> 24 & 0xff));
        Log.e(Const.CONST_LOG_TAG, "getOwnIp: " + ip);
        //返回整型地址转换成“*.*.*.*”地址
        return ip;}
    /**
     * 打印日志
     */
    public abstract void printLog(String log);
}
