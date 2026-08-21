package com.hongjia.hjbledemo;

import com.clj.fastble.BleManager;
import com.clj.fastble.callback.BleNotifyCallback;
import com.clj.fastble.data.BleDevice;
import com.clj.fastble.exception.BleException;
import com.wise.ble.WiseCharacteristic;

import java.util.concurrent.ConcurrentHashMap;

// 需要先初始化 BleManager
public class FastBleListener {

   private static FastBleListener mBleListener = null;

   // 通知回调
   private final ConcurrentHashMap<String, BleNotifyCallback> notifyBleCallbackMap = new ConcurrentHashMap<>();

   // 高速
   private final ConcurrentHashMap<String, Boolean> highRateMap = new ConcurrentHashMap<>();

   private FastBleListener()
   {
      super();
   }

   public static synchronized FastBleListener getInstance()
   {
      if (mBleListener == null)
      {
         mBleListener = new FastBleListener();
      }
      return mBleListener;
   }

   public BleNotifyCallback getNotifyBleCallback(BleDevice device, WiseCharacteristic characteristic) {
      return notifyBleCallbackMap.get(notifyKey(device, characteristic));
   }

   public void setNotifyBleCallback(BleDevice device, WiseCharacteristic characteristic, BleNotifyCallback notifyBleCallback) {
      notifyBleCallbackMap.put(notifyKey(device, characteristic), notifyBleCallback);
   }

   public void removeNotifyBleCallback(BleDevice device, WiseCharacteristic characteristic) {
      notifyBleCallbackMap.remove(notifyKey(device, characteristic));
   }

   // 设置高速模式
   public void setHighRate(String mac, boolean isOpen) {
      if (highRateMap.containsKey(mac)) {
         highRateMap.remove(mac);
      }

      highRateMap.put(mac, isOpen);
   }

   public boolean getHighRate(String mac) {
      if (highRateMap.containsKey(mac)) {
         return highRateMap.get(mac);
      } else {
         return false;
      }
   }

   /**
    * 打开通知
    * @param bleDevice  设备
    * @param characteristic 特征
    */
   public void openNotify(BleDevice bleDevice, final WiseCharacteristic characteristic) {
      BleManager.getInstance().notify(bleDevice, characteristic.getServiceID(), characteristic.getCharacteristicID(), new BleNotifyCallback() {
         @Override
         public void onNotifySuccess() {
            BleNotifyCallback callback = getNotifyBleCallback(bleDevice, characteristic);
            if (callback != null) callback.onNotifySuccess();
         }

         @Override
         public void onNotifyFailure(BleException e) {
            BleNotifyCallback callback = getNotifyBleCallback(bleDevice, characteristic);
            if (callback != null) callback.onNotifyFailure(e);
         }

         @Override
         public void onCharacteristicChanged(byte[] bytes) {
            BleNotifyCallback callback = getNotifyBleCallback(bleDevice, characteristic);
            if (callback != null) callback.onCharacteristicChanged(bytes);
         }
      });
   }

   private String notifyKey(BleDevice device, WiseCharacteristic characteristic) {
      return device.getMac() + "|" + characteristic.getServiceID() + "|" + characteristic.getCharacteristicID();
   }

   public void removeDevice(BleDevice device) {
      if (device == null) return;
      String prefix = device.getMac() + "|";
      for (String key : notifyBleCallbackMap.keySet()) {
         if (key.startsWith(prefix)) notifyBleCallbackMap.remove(key);
      }
      highRateMap.remove(device.getMac());
   }

}
