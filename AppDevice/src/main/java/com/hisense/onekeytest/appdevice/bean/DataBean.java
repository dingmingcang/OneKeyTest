package com.hisense.onekeytest.appdevice.bean;

import java.util.List;

/**
 * File Name    ： DataBean
 * Description  ： 通信数据Bean类
 * Author       ： dingmingcang
 * Create Date  ： 2017/02/28
 * Version      ： v1
 */

public class DataBean {
    public int packType;
    public String deviceIpLen;
    public String deviceIp;
    public String keyType;
    public String deviceName;
    public String versionCode;
    public int sendSeq;
    public int keyCode;
    public List<DeviceIpBean> deviceIps;

    @Override
    public String toString() {
        return "DataBean{" +
                "packType=" + packType +
                ", deviceIpLen='" + deviceIpLen + '\'' +
                ", deviceIp='" + deviceIp + '\'' +
                ", keyType='" + keyType + '\'' +
                ", deviceName='" + deviceName + '\'' +
                ", versionCode='" + versionCode + '\'' +
                ", sendSeq=" + sendSeq +
                ", keyCode=" + keyCode +
                ", deviceIps=" + deviceIps +
                '}';
    }
}
