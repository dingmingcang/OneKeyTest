package com.hisense.onekeytest.apphost.search;

/**
 * File Name    : SearcherConst
 * Description  : 搜索者常量
 * Author       : dingmingcang
 * Create Date  : 2017/02/24
 */
public interface SearcherConst {

    // UDP 报文类型
    int PACKET_TYPE_FIND_DEVICE_REQ_10   = 10001;   // 搜索请求
    int PACKET_TYPE_FIND_DEVICE_RSP_11   = 10002;   // 搜索响应
    int PACKET_TYPE_FIND_DEVICE_CHK_12   = 10003;   // 搜索确认
    int PACKET_TYPE_FIND_DEVICE_SOCKET   = 10004;   // 建立Socket连接


    // Socket 报文类型
    int SOCKET_TYPE_HEART_BEAT           = 20001;   // 心跳
    int SOCKET_TYPE_DISCONNECT           = 20002;   // 断开连接
    int SOCKET_TYPE_KEY_CODE             = 20003;   // 按键信息


    // 心跳 报文参数
    int HEART_BEAT_INTERVAL              = 2000;    // 心跳间隔
    int HEART_BEAT_TIME_OUT              = 5000;    // 心跳超时时间


    // Socket 报文参数
    int SOCKET_TYPE_KEY_CODE_TYPE_DONE   = 30001;   // 按下
    int SOCKET_TYPE_KEY_CODE_TYPE_UP_    = 30002;   // 抬起
    int SOCKET_TYPE_KEY_CODE_TYPE_LONG   = 30003;   // 长按


    // UDP 报文参数
    int DEVICE_FIND_PORT                 = 9000 ;   // 设备监听端口
    int SOCKET_CONNECT_PORT              = 18888;   // Socket连接端口
    int RECEIVE_TIME_OUT                 = 3000 ;   // 接收超时时间
    int RECEIVE_TIME_OUT_CONNECT         = 10000;   // 接收连接指令超时时间
    int RESPONSE_DEVICE_MAX              = 250  ;   // 响应设备的最大个数，防止UDP广播攻击
    int DEVICE_RECEIVE_DEFAULT_TIME_OUT  = 5000 ;   // 设备默认的接收超时时间


}
