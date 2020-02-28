package com.hisense.onekeytest.apphost.bean;

/**
 * 设备Bean
 * 只要IP一样，则认为是同一个设备
 */

public class DeviceBean {

    String ip;      // IP地址
    int port;       // 端口
    boolean isConnect = false; //连接状态：true 已连接；false 未连接

    public DeviceBean() {
    }

    public DeviceBean(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    public DeviceBean(String ip, int port, boolean connect) {
        this.ip = ip;
        this.port = port;
        this.isConnect = connect;

    }

    @Override
    public int hashCode() {
        return ip.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof DeviceBean) {
            return this.ip.equals(((DeviceBean) o).getIp());
        }
        return super.equals(o);
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public boolean getIsConnect() {
        return this.isConnect;
    }

    public void setIsConnect(boolean isConnect) {
        this.isConnect = isConnect;
    }
}

