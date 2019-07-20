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
}
