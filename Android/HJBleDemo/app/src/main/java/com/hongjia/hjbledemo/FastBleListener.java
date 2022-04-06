package com.hongjia.hjbledemo;

import android.bluetooth.BluetoothGatt;

import com.clj.fastble.BleManager;
import com.clj.fastble.callback.BleGattCallback;
import com.clj.fastble.callback.BleNotifyCallback;
import com.clj.fastble.data.BleDevice;
import com.clj.fastble.exception.BleException;
import com.wise.ble.WiseCharacteristic;

import java.util.HashMap;

// 需要先初始化 BleManager
public class FastBleListener {

   private static FastBleListener mBleListener = null;

   private BleGattCallback connectBleCallBack;

   // 通知回调
   private HashMap<String, BleNotifyCallback> notifyBleCallbackMap = new HashMap<>();

   // 高速
   private HashMap<String, Boolean> highRateMap = new HashMap<>();

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

   public void setConnectBleCallBack(BleGattCallback connectBleCallBack) {
      this.connectBleCallBack = connectBleCallBack;
   }

   public BleGattCallback getConnectBleCallBack() {
      return connectBleCallBack;
   }

   public BleNotifyCallback getNotifyBleCallback(String key) {

      if (notifyBleCallbackMap.containsKey(key)) {
         notifyBleCallbackMap.get(key);
      }
      return null;
   }

   public void setNotifyBleCallback(String characteristicId, BleNotifyCallback notifyBleCallback) {
      if (notifyBleCallbackMap.containsKey(characteristicId)) {
         notifyBleCallbackMap.remove(characteristicId);
      }
      this.notifyBleCallbackMap.put(characteristicId, notifyBleCallback);
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

   public BleGattCallback getBleGattCallBack() {
      BleGattCallback bleGattCallback = new BleGattCallback() {
         @Override
         public void onStartConnect() {
            if (connectBleCallBack != null) {
               connectBleCallBack.onStartConnect();
            }
         }

         @Override
         public void onConnectFail(BleDevice bleDevice, BleException e) {
            if (connectBleCallBack != null) {
               connectBleCallBack.onConnectFail(bleDevice, e);
            }
         }

         @Override
         public void onConnectSuccess(BleDevice bleDevice, BluetoothGatt bluetoothGatt, int i) {
            if (connectBleCallBack != null) {
               connectBleCallBack.onConnectSuccess(bleDevice, bluetoothGatt, i);
            }

            if (highRateMap.containsKey(bleDevice.getMac())) {
               highRateMap.remove(bleDevice.getMac());
            }
            highRateMap.put(bleDevice.getMac(), false);
         }

         @Override
         public void onDisConnected(boolean b, BleDevice bleDevice, BluetoothGatt bluetoothGatt, int i) {
            if (connectBleCallBack != null) {
               connectBleCallBack.onDisConnected(b, bleDevice, bluetoothGatt, i);
            }

            if (highRateMap.containsKey(bleDevice.getMac())) {
               highRateMap.remove(bleDevice.getMac());
            }
         }
      };

      return bleGattCallback;
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
            String id = characteristic.getCharacteristicID();
            if (notifyBleCallbackMap.containsKey(id)) {
               notifyBleCallbackMap.get(id).onNotifySuccess();
            }
         }

         @Override
         public void onNotifyFailure(BleException e) {
            String id = characteristic.getCharacteristicID();
            if (notifyBleCallbackMap.containsKey(id)) {
               notifyBleCallbackMap.get(id).onNotifyFailure(e);
            }
         }

         @Override
         public void onCharacteristicChanged(byte[] bytes) {
            String id = characteristic.getCharacteristicID();
            if (notifyBleCallbackMap.containsKey(id)) {
               notifyBleCallbackMap.get(id).onCharacteristicChanged(bytes);
            }
         }
      });
   }

}
