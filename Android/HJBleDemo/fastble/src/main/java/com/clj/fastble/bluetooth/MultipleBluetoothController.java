package com.clj.fastble.bluetooth;


import android.bluetooth.BluetoothDevice;
import android.os.Build;

import com.clj.fastble.BleManager;
import com.clj.fastble.data.BleDevice;
import com.clj.fastble.utils.BleLruHashMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MultipleBluetoothController {

    private final BleLruHashMap<String, BleBluetooth> bleLruHashMap;
    private final HashMap<String, BleBluetooth> bleTempHashMap;

    public MultipleBluetoothController() {
        bleLruHashMap = new BleLruHashMap<>(BleManager.getInstance().getMaxConnectCount());
        bleTempHashMap = new HashMap<>();
    }

    public synchronized BleBluetooth buildConnectingBle(BleDevice bleDevice) {
        String key = bleDevice.getMac();
        if (bleTempHashMap.containsKey(key) || bleLruHashMap.containsKey(key)
                || bleTempHashMap.size() + bleLruHashMap.size() >= BleManager.getInstance().getMaxConnectCount()) return null;
        BleBluetooth bleBluetooth = new BleBluetooth(bleDevice);
        bleTempHashMap.put(key, bleBluetooth);
        return bleBluetooth;
    }

    public synchronized void removeConnectingBle(BleBluetooth bleBluetooth) {
        if (bleBluetooth == null) {
            return;
        }
        if (bleTempHashMap.get(bleBluetooth.getDeviceKey()) == bleBluetooth) {
            bleTempHashMap.remove(bleBluetooth.getDeviceKey());
        }
    }

    public synchronized void addBleBluetooth(BleBluetooth bleBluetooth) {
        if (bleBluetooth == null) {
            return;
        }
        if (!bleLruHashMap.containsKey(bleBluetooth.getDeviceKey())) {
            bleLruHashMap.put(bleBluetooth.getDeviceKey(), bleBluetooth);
        }
    }

    public synchronized void removeBleBluetooth(BleBluetooth bleBluetooth) {
        if (bleBluetooth == null) {
            return;
        }
        if (bleLruHashMap.get(bleBluetooth.getDeviceKey()) == bleBluetooth) {
            bleLruHashMap.remove(bleBluetooth.getDeviceKey());
        }
    }

    public synchronized boolean isContainDevice(BleDevice bleDevice) {
        return bleDevice != null && bleLruHashMap.containsKey(bleDevice.getMac());
    }

    public synchronized boolean isContainDevice(BluetoothDevice bluetoothDevice) {
        return bluetoothDevice != null && bleLruHashMap.containsKey(bluetoothDevice.getAddress());
    }

    public synchronized BleBluetooth getBleBluetooth(BleDevice bleDevice) {
        if (bleDevice != null) {
            if (bleLruHashMap.containsKey(bleDevice.getMac())) {
                return bleLruHashMap.get(bleDevice.getMac());
            }
        }
        return null;
    }

    public void disconnect(BleDevice bleDevice) {
        if (bleDevice == null) return;
        BleBluetooth connection;
        synchronized (this) {
            connection = bleLruHashMap.get(bleDevice.getMac());
            if (connection == null) connection = bleTempHashMap.get(bleDevice.getMac());
        }
        if (connection != null) connection.disconnect();
    }

    public void disconnectAllDevice() {
        List<BleBluetooth> connections;
        synchronized (this) {
            connections = new ArrayList<>(bleLruHashMap.values());
            connections.addAll(bleTempHashMap.values());
        }
        for (BleBluetooth connection : connections) connection.disconnect();
    }

    public void destroy() {
        List<BleBluetooth> connections;
        synchronized (this) {
            connections = new ArrayList<>(bleLruHashMap.values());
            connections.addAll(bleTempHashMap.values());
        }
        for (BleBluetooth connection : connections) connection.destroy();

    }

    public synchronized List<BleBluetooth> getBleBluetoothList() {
        List<BleBluetooth> bleBluetoothList = new ArrayList<>(bleLruHashMap.values());
        Collections.sort(bleBluetoothList, new Comparator<BleBluetooth>() {
            @Override
            public int compare(BleBluetooth lhs, BleBluetooth rhs) {
                return lhs.getDeviceKey().compareToIgnoreCase(rhs.getDeviceKey());
            }
        });
        return bleBluetoothList;
    }

    public List<BleDevice> getDeviceList() {
        refreshConnectedDevice();
        List<BleDevice> deviceList = new ArrayList<>();
        for (BleBluetooth BleBluetooth : getBleBluetoothList()) {
            if (BleBluetooth != null) {
                deviceList.add(BleBluetooth.getDevice());
            }
        }
        return deviceList;
    }

    public void refreshConnectedDevice() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            List<BleBluetooth> bluetoothList = getBleBluetoothList();
            for (int i = 0; bluetoothList != null && i < bluetoothList.size(); i++) {
                BleBluetooth bleBluetooth = bluetoothList.get(i);
                if (!BleManager.getInstance().isConnected(bleBluetooth.getDevice())) {
                    bleBluetooth.disconnect();
                }
            }
        }
    }


}
