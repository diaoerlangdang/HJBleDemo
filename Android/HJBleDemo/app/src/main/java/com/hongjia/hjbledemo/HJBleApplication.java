package com.hongjia.hjbledemo;

import android.app.Application;
import android.content.Context;

import com.wise.ble.WiseBluetoothLe;
import com.wise.wisekit.utils.SPUtils;

public class HJBleApplication extends Application {

    private static final String TAG = HJBleApplication.class.getName();

    private  static HJBleApplication instance;

    // 是否为配置模式
    private boolean isBleConfig = false;

    //获取应用的context
    public static Context getAppContext() {
        return instance.getApplicationContext();
    }

    @Override
    public void onCreate() {
        super.onCreate();

        instance = this;

        WiseBluetoothLe.getInstance(getAppContext());

    }

    public static HJBleApplication shareInstance() {
        return instance;
    }

    public boolean isBleConfig() {
        return isBleConfig;
    }

    public void setBleConfig(boolean bleConfig) {
        isBleConfig = bleConfig;
    }

    // 是否为十六进制模式
    public boolean isBleHex() {

        Boolean hex = (Boolean)SPUtils.get(getAppContext(), "isBleHex", true);
        return hex.booleanValue();
    }

    public void setBleHex(boolean bleHex) {

        SPUtils.put(getAppContext(), "isBleHex", bleHex);
    }

    // 是否在发送数据后面自动加回车
    public boolean isAddReturn() {

        Boolean addReturn = (Boolean)SPUtils.get(getAppContext(), "isAddReturn", false);
        return addReturn.booleanValue();
    }

    public void setAddReturn(boolean addReturn) {

        SPUtils.put(getAppContext(), "isAddReturn", addReturn);
    }

    // 是否为respone
    public boolean isWriteTypeResponse() {

        Boolean isResponse = (Boolean)SPUtils.get(getAppContext(), "isWriteTypeResponse", true);
        return isResponse.booleanValue();
    }

    public void setWriteTypeResponse(boolean isWriteTypeResponse) {

        SPUtils.put(getAppContext(), "isWriteTypeResponse", isWriteTypeResponse);
    }

    // 每包数据长度
    public int groupLen() {

        return (Integer)SPUtils.get(getAppContext(), "groupLen", 20);
    }

    public void setGroupLen(int groupLen) {

        SPUtils.put(getAppContext(), "groupLen", groupLen);
    }

    // 每次下发测试数据长度
    public int testDataLen() {

        return (Integer)SPUtils.get(getAppContext(), "testDataLen", 20);
    }

    public void setTestDataLen(int testDataLen) {

        SPUtils.put(getAppContext(), "testDataLen", testDataLen);
    }

    // 下发数据时间间隙
    public int testGapTime() {

        return (Integer)SPUtils.get(getAppContext(), "testGapTime", 20);
    }

    public void setTestGapTime(int testGapTime) {

        SPUtils.put(getAppContext(), "testGapTime", testGapTime);
    }
}
