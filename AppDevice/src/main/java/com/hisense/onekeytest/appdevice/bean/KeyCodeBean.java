package com.hisense.onekeytest.appdevice.bean;

/**
 * Created by Administrator on 2017/3/3.
 */

public class KeyCodeBean {

    public int packType;
    public String keyType;
    public int keyCode;
    public String deviceIp;

    @Override
    public String toString() {
        return "KeyCodeBean{" +
                "packType=" + packType +
                ", keyType='" + keyType + '\'' +
                ", keyCode=" + keyCode +
                ", deviceIp='" + deviceIp + '\'' +
                '}';
    }
}
