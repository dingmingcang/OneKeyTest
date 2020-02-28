package com.hisense.onekeytest.apphost.bean;

/**
 * File Name    ： DataBean
 * Description  ： 通信数据Bean类
 * Author       ： dingmingcang
 * Create Date  ： 2017/02/28
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


    @Override
    public String toString() {
        return "DataBean{" +
                "packType='" + packType + '\'' +
                ", deviceIpLen='" + deviceIpLen + '\'' +
                ", deviceIp='" + deviceIp + '\'' +
                ", KeyType='" + keyType + '\'' +
                ", deviceName='" + deviceName + '\'' +
                ", versionCode='" + versionCode + '\'' +
                ", sendSeq=" + sendSeq +
                ", keyCode=" + keyCode +
                '}';
    }
}
