package com.wise.ble;

import android.bluetooth.BluetoothGattCharacteristic;

public class WiseCharacteristic {

    // 服务id
    private String serviceID;
    // 特征id
    private String characteristicID;


    public WiseCharacteristic() {
    }

    public WiseCharacteristic(String serviceID, String characteristicID) {
        this.serviceID = serviceID;
        this.characteristicID = characteristicID;
    }

    /**
     * serviceID characteristicID是否有值
     *
     * @return 有效 true， 无效false
     */
    boolean isHaveValue() {
        if (serviceID == null || characteristicID == null) {
            return false;
        }

        // 去除所有空格
        String tmp = serviceID.replaceAll(" " , "");

        if (tmp.length() <= 0) {
            return false;
        }

        // 去除所有空格
        tmp = characteristicID.replaceAll(" " , "");

        if (tmp.length() <= 0) {
            return false;
        }

        return true;
    }

    @Override
    public boolean equals(Object obj) {

        if (obj.getClass() == (WiseCharacteristic.class)) {

            WiseCharacteristic tmp = (WiseCharacteristic)obj;
            return (serviceID.equals(tmp.serviceID) && characteristicID.equals(tmp.characteristicID));
        }
        else {
            return false;
        }
    }

    /**
     * 与蓝牙的服务对比是否相等
     * @param characteristic 蓝牙服务
     * @return
     */
    public boolean isEqualBleGattCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (characteristic == null || !isHaveValue()) return false;

        String bleService = characteristic.getService().getUuid().toString();
        String bleCharact = characteristic.getUuid().toString();

        return (bleService.equals(serviceID) && bleCharact.equals(characteristicID));

    }

    public String getServiceID() {
        return serviceID;
    }

    public void setServiceID(String serviceID) {
        this.serviceID = serviceID;
    }

    public String getCharacteristicID() {
        return characteristicID;
    }

    public void setCharacteristicID(String characteristicID) {
        this.characteristicID = characteristicID;
    }
}
