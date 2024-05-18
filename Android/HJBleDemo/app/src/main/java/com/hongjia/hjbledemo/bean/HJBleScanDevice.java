package com.hongjia.hjbledemo.bean;

import com.clj.fastble.data.BleDevice;

public class HJBleScanDevice implements Comparable<HJBleScanDevice> {

    // 蓝牙设备
    public BleDevice device;
    // 信号强度
    public int rssi;
    // 广播数据
    public String record;
    // 是否为简易模式
    public boolean isEasy;
    // 是否支持配置
    public boolean isConfig;
    // 最大发送数据长度
    public int sendDataLenMax;
    // 是否需要获取mtu
    public boolean bMtu;
    // 减过3了
    public int mtuLen;
    //
    public boolean bFlowControl;

    // 时间
    public Long timeStamp;

    @Override
    public int compareTo(HJBleScanDevice o) {

        // 降序
        return o.rssi - this.rssi;
    }
}
