package com.clj.fastble.bluetooth;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothProfile;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.clj.fastble.BleManager;
import com.clj.fastble.callback.BleGattCallback;
import com.clj.fastble.callback.BleIndicateCallback;
import com.clj.fastble.callback.BleMtuChangedCallback;
import com.clj.fastble.callback.BleNotifyCallback;
import com.clj.fastble.callback.BleReadCallback;
import com.clj.fastble.callback.BleRssiCallback;
import com.clj.fastble.callback.BleWriteCallback;
import com.clj.fastble.data.BleConnectStateParameter;
import com.clj.fastble.data.BleDevice;
import com.clj.fastble.data.BleMsg;
import com.clj.fastble.exception.ConnectException;
import com.clj.fastble.exception.BleException;
import com.clj.fastble.exception.OtherException;
import com.clj.fastble.exception.TimeoutException;
import com.clj.fastble.utils.BleLog;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static android.bluetooth.BluetoothDevice.TRANSPORT_LE;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class BleBluetooth {

    private BleGattCallback bleGattCallback;
    private BleRssiCallback bleRssiCallback;
    private BleMtuChangedCallback bleMtuChangedCallback;
    private final HashMap<String, BleNotifyCallback> bleNotifyCallbackHashMap = new HashMap<>();
    private final HashMap<String, BleIndicateCallback> bleIndicateCallbackHashMap = new HashMap<>();
    private final HashMap<String, BleWriteCallback> bleWriteCallbackHashMap = new HashMap<>();
    private final HashMap<String, BleReadCallback> bleReadCallbackHashMap = new HashMap<>();

    private LastState lastState;
    private volatile boolean destroyed;
    private volatile boolean disconnecting;
    private boolean isActiveDisconnect = false;
    private final BleDevice bleDevice;
    private BluetoothGatt bluetoothGatt;
    private final MainHandler mainHandler = new MainHandler(Looper.getMainLooper());

    public BleBluetooth(BleDevice bleDevice) {
        this.bleDevice = bleDevice;
    }

    public BleConnector newBleConnector() {
        return new BleConnector(this);
    }

    public synchronized void addConnectGattCallback(BleGattCallback callback) {
        bleGattCallback = callback;
    }

    public synchronized void removeConnectGattCallback() {
        bleGattCallback = null;
    }

    public synchronized void addNotifyCallback(String uuid, BleNotifyCallback bleNotifyCallback) {
        bleNotifyCallbackHashMap.put(uuid, bleNotifyCallback);
    }

    public synchronized void addIndicateCallback(String uuid, BleIndicateCallback bleIndicateCallback) {
        bleIndicateCallbackHashMap.put(uuid, bleIndicateCallback);
    }

    public synchronized void addWriteCallback(String uuid, BleWriteCallback bleWriteCallback) {
        bleWriteCallbackHashMap.put(uuid, bleWriteCallback);
    }

    public synchronized void addReadCallback(String uuid, BleReadCallback bleReadCallback) {
        bleReadCallbackHashMap.put(uuid, bleReadCallback);
    }

    public synchronized void removeNotifyCallback(String uuid) {
        Iterator<String> keys = bleNotifyCallbackHashMap.keySet().iterator();
        while (keys.hasNext()) {
            String key = keys.next();
            if (key.equalsIgnoreCase(uuid) || key.toLowerCase(java.util.Locale.ROOT).endsWith("|" + uuid.toLowerCase(java.util.Locale.ROOT))) keys.remove();
        }
    }

    public synchronized void removeIndicateCallback(String uuid) {
        Iterator<String> keys = bleIndicateCallbackHashMap.keySet().iterator();
        while (keys.hasNext()) {
            String key = keys.next();
            if (key.equalsIgnoreCase(uuid) || key.toLowerCase(java.util.Locale.ROOT).endsWith("|" + uuid.toLowerCase(java.util.Locale.ROOT))) keys.remove();
        }
    }

    public synchronized void removeWriteCallback(String uuid) {
        if (bleWriteCallbackHashMap.containsKey(uuid))
            bleWriteCallbackHashMap.remove(uuid);
    }

    public synchronized void removeReadCallback(String uuid) {
        if (bleReadCallbackHashMap.containsKey(uuid))
            bleReadCallbackHashMap.remove(uuid);
    }

    public synchronized void clearCharacterCallback() {
        bleNotifyCallbackHashMap.clear();
        bleIndicateCallbackHashMap.clear();
        bleWriteCallbackHashMap.clear();
        bleReadCallbackHashMap.clear();
    }

    public synchronized void addRssiCallback(BleRssiCallback callback) {
        bleRssiCallback = callback;
    }

    public synchronized void removeRssiCallback() {
        bleRssiCallback = null;
    }

    public synchronized void addMtuChangedCallback(BleMtuChangedCallback callback) {
        bleMtuChangedCallback = callback;
    }

    public synchronized void removeMtuChangedCallback() {
        bleMtuChangedCallback = null;
    }


    static String notificationKey(BluetoothGattCharacteristic characteristic) {
        return characteristic.getService().getUuid() + "|" + characteristic.getUuid();
    }

    public String getDeviceKey() {
        return bleDevice.getMac();
    }

    public BleDevice getDevice() {
        return bleDevice;
    }

    public BluetoothGatt getBluetoothGatt() {
        return bluetoothGatt;
    }

    public synchronized BluetoothGatt connect(BleDevice bleDevice,
                                              boolean autoConnect,
                                              BleGattCallback callback) {
        return connect(bleDevice, autoConnect, callback, 0);
    }

    public synchronized BluetoothGatt connect(BleDevice bleDevice,
                                              boolean autoConnect,
                                              BleGattCallback callback,
                                              int connectRetryCount) {
        if (destroyed) return null;
        BleLog.i("connect device: " + bleDevice.getName()
                + "\nmac: " + bleDevice.getMac()
                + "\nautoConnect: " + autoConnect
                + "\ncurrentThread: " + Thread.currentThread().getId()
                + "\nconnectCount:" + (connectRetryCount + 1));

        addConnectGattCallback(callback);

        lastState = LastState.CONNECT_CONNECTING;

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                bluetoothGatt = bleDevice.getDevice().connectGatt(BleManager.getInstance().getContext(),
                        autoConnect, coreGattCallback, TRANSPORT_LE);
            } else {
                bluetoothGatt = bleDevice.getDevice().connectGatt(BleManager.getInstance().getContext(),
                        autoConnect, coreGattCallback);
            }
        } catch (SecurityException error) {
            fail(new OtherException("Bluetooth connect permission denied"));
            return null;
        }
        if (bluetoothGatt != null) {
            if (bleGattCallback != null) {
                bleGattCallback.onStartConnect();
            }
            Message message = mainHandler.obtainMessage();
            message.what = BleMsg.MSG_CONNECT_OVER_TIME;
            mainHandler.sendMessageDelayed(message, BleManager.getInstance().getConnectOverTime());

        } else {
            fail(new OtherException("GATT connect exception occurred!"));
        }
        return bluetoothGatt;
    }

    public synchronized void disconnect() {
        if (destroyed || disconnecting) return;
        isActiveDisconnect = true;
        disconnecting = true;
        if (lastState != LastState.CONNECT_CONNECTED) {
            fail(new OtherException("Connection cancelled"));
            return;
        }
        disconnectGatt();
        // Some stacks omit STATE_DISCONNECTED. Keep ownership until close completes.
        mainHandler.postDelayed(() -> finishDisconnect(true, -1), 3000);
    }

    private synchronized void fail(BleException error) {
        if (destroyed) return;
        BleGattCallback callback = bleGattCallback;
        destroy();
        if (callback != null) callback.onConnectFail(bleDevice, error);
    }

    private synchronized void finishDisconnect(boolean active, int status) {
        if (destroyed) return;
        BleGattCallback callback = bleGattCallback;
        BluetoothGatt gatt = bluetoothGatt;
        destroy();
        if (callback != null) callback.onDisConnected(active, bleDevice, gatt, status);
    }

    public synchronized void destroy() {
        if (destroyed) return;
        destroyed = true;
        lastState = LastState.CONNECT_IDLE;
        mainHandler.removeCallbacksAndMessages(null);
        try {
            disconnectGatt();
        } finally {
            closeBluetoothGatt();
            BleManager.getInstance().getMultipleBluetoothController().removeConnectingBle(this);
            BleManager.getInstance().getMultipleBluetoothController().removeBleBluetooth(this);
            removeConnectGattCallback();
            removeRssiCallback();
            removeMtuChangedCallback();
            clearCharacterCallback();
        }
    }

    private synchronized void disconnectGatt() {
        if (bluetoothGatt != null) {
            try { bluetoothGatt.disconnect(); }
            catch (SecurityException error) { BleLog.e("Bluetooth permission revoked during disconnect"); }
        }
    }

    private synchronized void closeBluetoothGatt() {
        if (bluetoothGatt != null) {
            try { bluetoothGatt.close(); }
            catch (SecurityException error) { BleLog.e("Bluetooth permission revoked during close"); }
        }
    }

    private final class MainHandler extends Handler {

        MainHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            synchronized (BleBluetooth.this) {
                if (destroyed || (disconnecting && msg.what != BleMsg.MSG_DISCONNECTED)) return;
                switch (msg.what) {
                    case BleMsg.MSG_CONNECT_FAIL: {
                        BleConnectStateParameter para = (BleConnectStateParameter) msg.obj;
                        fail(new ConnectException(bluetoothGatt, para.getStatus()));
                        break;
                    }
                    case BleMsg.MSG_DISCONNECTED: {
                        BleConnectStateParameter para = (BleConnectStateParameter) msg.obj;
                        finishDisconnect(para.isActive(), para.getStatus());
                        break;
                    }
                    case BleMsg.MSG_CONNECT_OVER_TIME: {
                        fail(new TimeoutException());
                        break;
                    }
                    case BleMsg.MSG_DISCOVER_SERVICES: {
                        if (lastState != LastState.CONNECT_CONNECTING) break;
                        if (bluetoothGatt != null) {
                            boolean discoverServiceResult;
                            try { discoverServiceResult = bluetoothGatt.discoverServices(); }
                            catch (SecurityException error) {
                                fail(new OtherException("Bluetooth service discovery permission denied"));
                                break;
                            }
                            if (!discoverServiceResult) {
                                Message message = mainHandler.obtainMessage();
                                message.what = BleMsg.MSG_DISCOVER_FAIL;
                                mainHandler.sendMessage(message);
                            }
                        } else {
                            Message message = mainHandler.obtainMessage();
                            message.what = BleMsg.MSG_DISCOVER_FAIL;
                            mainHandler.sendMessage(message);
                        }
                    }
                    break;

                    case BleMsg.MSG_DISCOVER_FAIL: {
                        fail(new OtherException("GATT discover services exception occurred!"));
                        break;
                    }

                    case BleMsg.MSG_DISCOVER_SUCCESS: {
                        if (lastState != LastState.CONNECT_CONNECTING) break;
                        mainHandler.removeMessages(BleMsg.MSG_CONNECT_OVER_TIME);
                        mainHandler.removeMessages(BleMsg.MSG_DISCOVER_SERVICES);
                        lastState = LastState.CONNECT_CONNECTED;
                        isActiveDisconnect = false;
                        BleManager.getInstance().getMultipleBluetoothController().removeConnectingBle(BleBluetooth.this);
                        BleManager.getInstance().getMultipleBluetoothController().addBleBluetooth(BleBluetooth.this);

                        BleConnectStateParameter para = (BleConnectStateParameter) msg.obj;
                        int status = para.getStatus();
                        if (bleGattCallback != null)
                            bleGattCallback.onConnectSuccess(bleDevice, bluetoothGatt, status);
                    }
                    break;

                    default:
                        super.handleMessage(msg);
                        break;
                }
            }
        }
    }

    private BluetoothGattCallback coreGattCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            mainHandler.post(() -> {
                synchronized (BleBluetooth.this) {
                    if (destroyed || gatt != bluetoothGatt) return;
                    BleLog.i("BluetoothGattCallback：onConnectionStateChange "
                            + '\n' + "status: " + status
                            + '\n' + "newState: " + newState
                            + '\n' + "currentThread: " + Thread.currentThread().getId());

                    if (newState == BluetoothProfile.STATE_CONNECTED && status == BluetoothGatt.GATT_SUCCESS) {
                        Message message = mainHandler.obtainMessage();
                        message.what = BleMsg.MSG_DISCOVER_SERVICES;
                        mainHandler.sendMessageDelayed(message, 500);

                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED || status != BluetoothGatt.GATT_SUCCESS) {
                        if (lastState == LastState.CONNECT_CONNECTING) {
                            Message message = mainHandler.obtainMessage();
                            message.what = BleMsg.MSG_CONNECT_FAIL;
                            message.obj = new BleConnectStateParameter(status);
                            mainHandler.sendMessage(message);

                        } else if (lastState == LastState.CONNECT_CONNECTED) {
                            Message message = mainHandler.obtainMessage();
                            message.what = BleMsg.MSG_DISCONNECTED;
                            BleConnectStateParameter para = new BleConnectStateParameter(status);
                            para.setActive(isActiveDisconnect);
                            message.obj = para;
                            mainHandler.sendMessage(message);
                        }
                    }

                }
            });
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            mainHandler.post(() -> {
                synchronized (BleBluetooth.this) {
                    if (destroyed || gatt != bluetoothGatt || disconnecting) return;
                    BleLog.i("BluetoothGattCallback：onServicesDiscovered "
                            + '\n' + "status: " + status
                            + '\n' + "currentThread: " + Thread.currentThread().getId());

                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        Message message = mainHandler.obtainMessage();
                        message.what = BleMsg.MSG_DISCOVER_SUCCESS;
                        message.obj = new BleConnectStateParameter(status);
                        mainHandler.sendMessage(message);

                    } else {
                        Message message = mainHandler.obtainMessage();
                        message.what = BleMsg.MSG_DISCOVER_FAIL;
                        mainHandler.sendMessage(message);
                    }

                }
            });
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            byte[] value = characteristic.getValue();
            final byte[] snapshot = value == null ? null : value.clone();
            mainHandler.post(() -> {
                synchronized (BleBluetooth.this) {
                    if (destroyed || gatt != bluetoothGatt || disconnecting) return;

                    Iterator iterator = bleNotifyCallbackHashMap.entrySet().iterator();
                    while (iterator.hasNext()) {
                        Map.Entry entry = (Map.Entry) iterator.next();
                        Object callback = entry.getValue();
                        if (callback instanceof BleNotifyCallback) {
                            BleNotifyCallback bleNotifyCallback = (BleNotifyCallback) callback;
                            if (notificationKey(characteristic).equalsIgnoreCase(bleNotifyCallback.getKey())) {
                                Handler handler = bleNotifyCallback.getHandler();
                                if (handler != null) {
                                    Message message = handler.obtainMessage();
                                    message.what = BleMsg.MSG_CHA_NOTIFY_DATA_CHANGE;
                                    message.obj = bleNotifyCallback;
                                    Bundle bundle = new Bundle();
                                    bundle.putByteArray(BleMsg.KEY_NOTIFY_BUNDLE_VALUE, snapshot);
                                    message.setData(bundle);
                                    handler.sendMessage(message);
                                }
                            }
                        }
                    }

                    iterator = bleIndicateCallbackHashMap.entrySet().iterator();
                    while (iterator.hasNext()) {
                        Map.Entry entry = (Map.Entry) iterator.next();
                        Object callback = entry.getValue();
                        if (callback instanceof BleIndicateCallback) {
                            BleIndicateCallback bleIndicateCallback = (BleIndicateCallback) callback;
                            if (notificationKey(characteristic).equalsIgnoreCase(bleIndicateCallback.getKey())) {
                                Handler handler = bleIndicateCallback.getHandler();
                                if (handler != null) {
                                    Message message = handler.obtainMessage();
                                    message.what = BleMsg.MSG_CHA_INDICATE_DATA_CHANGE;
                                    message.obj = bleIndicateCallback;
                                    Bundle bundle = new Bundle();
                                    bundle.putByteArray(BleMsg.KEY_INDICATE_BUNDLE_VALUE, snapshot);
                                    message.setData(bundle);
                                    handler.sendMessage(message);
                                }
                            }
                        }
                    }

                }
            });
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            mainHandler.post(() -> {
                synchronized (BleBluetooth.this) {
                    if (destroyed || gatt != bluetoothGatt || disconnecting) return;

                    Iterator iterator = bleNotifyCallbackHashMap.entrySet().iterator();
                    while (iterator.hasNext()) {
                        Map.Entry entry = (Map.Entry) iterator.next();
                        Object callback = entry.getValue();
                        if (callback instanceof BleNotifyCallback) {
                            BleNotifyCallback bleNotifyCallback = (BleNotifyCallback) callback;
                            if (notificationKey(descriptor.getCharacteristic()).equalsIgnoreCase(bleNotifyCallback.getKey())) {
                                Handler handler = bleNotifyCallback.getHandler();
                                if (handler != null) {
                                    Message message = handler.obtainMessage();
                                    message.what = BleMsg.MSG_CHA_NOTIFY_RESULT;
                                    message.obj = bleNotifyCallback;
                                    Bundle bundle = new Bundle();
                                    bundle.putInt(BleMsg.KEY_NOTIFY_BUNDLE_STATUS, status);
                                    message.setData(bundle);
                                    handler.sendMessage(message);
                                }
                            }
                        }
                    }

                    iterator = bleIndicateCallbackHashMap.entrySet().iterator();
                    while (iterator.hasNext()) {
                        Map.Entry entry = (Map.Entry) iterator.next();
                        Object callback = entry.getValue();
                        if (callback instanceof BleIndicateCallback) {
                            BleIndicateCallback bleIndicateCallback = (BleIndicateCallback) callback;
                            if (notificationKey(descriptor.getCharacteristic()).equalsIgnoreCase(bleIndicateCallback.getKey())) {
                                Handler handler = bleIndicateCallback.getHandler();
                                if (handler != null) {
                                    Message message = handler.obtainMessage();
                                    message.what = BleMsg.MSG_CHA_INDICATE_RESULT;
                                    message.obj = bleIndicateCallback;
                                    Bundle bundle = new Bundle();
                                    bundle.putInt(BleMsg.KEY_INDICATE_BUNDLE_STATUS, status);
                                    message.setData(bundle);
                                    handler.sendMessage(message);
                                }
                            }
                        }
                    }

                }
            });
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            byte[] value = characteristic.getValue();
            final byte[] snapshot = value == null ? null : value.clone();
            mainHandler.post(() -> {
                synchronized (BleBluetooth.this) {
                    if (destroyed || gatt != bluetoothGatt || disconnecting) return;

                    Iterator iterator = bleWriteCallbackHashMap.entrySet().iterator();
                    while (iterator.hasNext()) {
                        Map.Entry entry = (Map.Entry) iterator.next();
                        Object callback = entry.getValue();
                        if (callback instanceof BleWriteCallback) {
                            BleWriteCallback bleWriteCallback = (BleWriteCallback) callback;
                            if (characteristic.getUuid().toString().equalsIgnoreCase(bleWriteCallback.getKey())) {
                                Handler handler = bleWriteCallback.getHandler();
                                if (handler != null) {
                                    Message message = handler.obtainMessage();
                                    message.what = BleMsg.MSG_CHA_WRITE_RESULT;
                                    message.obj = bleWriteCallback;
                                    Bundle bundle = new Bundle();
                                    bundle.putInt(BleMsg.KEY_WRITE_BUNDLE_STATUS, status);
                                    bundle.putByteArray(BleMsg.KEY_WRITE_BUNDLE_VALUE, snapshot);
                                    message.setData(bundle);
                                    handler.sendMessage(message);
                                }
                            }
                        }
                    }

                }
            });
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            byte[] value = characteristic.getValue();
            final byte[] snapshot = value == null ? null : value.clone();
            mainHandler.post(() -> {
                synchronized (BleBluetooth.this) {
                    if (destroyed || gatt != bluetoothGatt || disconnecting) return;

                    Iterator iterator = bleReadCallbackHashMap.entrySet().iterator();
                    while (iterator.hasNext()) {
                        Map.Entry entry = (Map.Entry) iterator.next();
                        Object callback = entry.getValue();
                        if (callback instanceof BleReadCallback) {
                            BleReadCallback bleReadCallback = (BleReadCallback) callback;
                            if (characteristic.getUuid().toString().equalsIgnoreCase(bleReadCallback.getKey())) {
                                Handler handler = bleReadCallback.getHandler();
                                if (handler != null) {
                                    Message message = handler.obtainMessage();
                                    message.what = BleMsg.MSG_CHA_READ_RESULT;
                                    message.obj = bleReadCallback;
                                    Bundle bundle = new Bundle();
                                    bundle.putInt(BleMsg.KEY_READ_BUNDLE_STATUS, status);
                                    bundle.putByteArray(BleMsg.KEY_READ_BUNDLE_VALUE, snapshot);
                                    message.setData(bundle);
                                    handler.sendMessage(message);
                                }
                            }
                        }
                    }

                }
            });
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            mainHandler.post(() -> {
                synchronized (BleBluetooth.this) {
                    if (destroyed || gatt != bluetoothGatt || disconnecting) return;

                    if (bleRssiCallback != null) {
                        Handler handler = bleRssiCallback.getHandler();
                        if (handler != null) {
                            Message message = handler.obtainMessage();
                            message.what = BleMsg.MSG_READ_RSSI_RESULT;
                            message.obj = bleRssiCallback;
                            Bundle bundle = new Bundle();
                            bundle.putInt(BleMsg.KEY_READ_RSSI_BUNDLE_STATUS, status);
                            bundle.putInt(BleMsg.KEY_READ_RSSI_BUNDLE_VALUE, rssi);
                            message.setData(bundle);
                            handler.sendMessage(message);
                        }
                    }

                }
            });
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            mainHandler.post(() -> {
                synchronized (BleBluetooth.this) {
                    if (destroyed || gatt != bluetoothGatt || disconnecting) return;

                    if (bleMtuChangedCallback != null) {
                        Handler handler = bleMtuChangedCallback.getHandler();
                        if (handler != null) {
                            Message message = handler.obtainMessage();
                            message.what = BleMsg.MSG_SET_MTU_RESULT;
                            message.obj = bleMtuChangedCallback;
                            Bundle bundle = new Bundle();
                            bundle.putInt(BleMsg.KEY_SET_MTU_BUNDLE_STATUS, status);
                            bundle.putInt(BleMsg.KEY_SET_MTU_BUNDLE_VALUE, mtu);
                            message.setData(bundle);
                            handler.sendMessage(message);
                        }
                    }

                }
            });
        }
    };

    enum LastState {
        CONNECT_IDLE,
        CONNECT_CONNECTING,
        CONNECT_CONNECTED,
        CONNECT_FAILURE,
        CONNECT_DISCONNECT
    }

}
