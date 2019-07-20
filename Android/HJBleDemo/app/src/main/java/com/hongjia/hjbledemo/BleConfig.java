package com.hongjia.hjbledemo;

import com.wise.ble.WiseCharacteristic;

public class BleConfig {


    // 蓝牙数据发送服务
    public static final WiseCharacteristic Ble_Data_Send_Service = new WiseCharacteristic("0000fff0-0000-1000-8000-00805f9b34fb", "0000fff2-0000-1000-8000-00805f9b34fb");

    // 蓝牙数据接收服务
    public static final WiseCharacteristic Ble_Data_Receive_Service = new WiseCharacteristic("0000fff0-0000-1000-8000-00805f9b34fb", "0000fff1-0000-1000-8000-00805f9b34fb");

    // 蓝牙配置发送服务
    public static final WiseCharacteristic Ble_Config_Send_Service = new WiseCharacteristic("0000fff0-0000-1000-8000-00805f9b34fb", "0000fff3-0000-1000-8000-00805f9b34fb");

    // 蓝牙配置接收服务
    public static final WiseCharacteristic Ble_Config_Receive_Service = new WiseCharacteristic("0000fff0-0000-1000-8000-00805f9b34fb", "0000fff3-0000-1000-8000-00805f9b34fb");
}
