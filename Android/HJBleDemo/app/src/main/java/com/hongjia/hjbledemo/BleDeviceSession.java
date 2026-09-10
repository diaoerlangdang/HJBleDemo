package com.hongjia.hjbledemo;

import com.clj.fastble.data.BleDevice;
import com.clj.fastble.BleManager;
import com.hongjia.hjbledemo.bean.HJBleScanDevice;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArraySet;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public final class BleDeviceSession {
    private static final ConcurrentHashMap<String, BleDeviceSession> SESSIONS = new ConcurrentHashMap<>();

    private volatile int packetLength = 20;
    private volatile boolean configMode;
    private volatile boolean busy;
    private volatile HJBleScanDevice readyDevice;
    private volatile boolean disconnecting;
    private final StringBuilder configBuffer = new StringBuilder();
    private final CopyOnWriteArraySet<Runnable> disconnectListeners = new CopyOnWriteArraySet<>();
    private final ExecutorService sendExecutor = Executors.newSingleThreadExecutor();

    public static BleDeviceSession get(BleDevice device) {
        BleDeviceSession session = SESSIONS.get(device.getMac());
        if (session != null) return session;
        synchronized (SESSIONS) {
            session = SESSIONS.get(device.getMac());
            if (session == null) {
                session = new BleDeviceSession();
                SESSIONS.put(device.getMac(), session);
            }
            return session;
        }
    }

    public static void remove(BleDevice device) {
        if (device == null) return;
        BleDeviceSession session = SESSIONS.remove(device.getMac());
        if (session != null) session.close();
    }

    public static BleDeviceSession find(BleDevice device) {
        return device == null ? null : SESSIONS.get(device.getMac());
    }

    public boolean isCurrent(BleDevice device) {
        return find(device) == this;
    }

    public boolean isReady() {
        return readyDevice != null && !disconnecting;
    }

    static BleDeviceSession begin(BleDevice device) {
        synchronized (SESSIONS) {
            if (SESSIONS.containsKey(device.getMac())) return null;
            BleDeviceSession session = new BleDeviceSession();
            SESSIONS.put(device.getMac(), session);
            return session;
        }
    }

    HJBleScanDevice getReadyDevice() {
        return readyDevice;
    }

    synchronized void acceptConfigData(byte[] bytes) {
        configBuffer.append(new String(bytes, java.nio.charset.StandardCharsets.UTF_8));
        int end;
        while ((end = configBuffer.indexOf(">")) >= 0) {
            String command = configBuffer.substring(0, end + 1);
            configBuffer.delete(0, end + 1);
            if (command.endsWith("<HJ_BLE_BUSY_STOP_SEND>")) busy = true;
            else if (command.endsWith("<HJ_BLE_IDLE_START_SEND>")) busy = false;
        }
        if (configBuffer.length() > 256) configBuffer.delete(0, configBuffer.length() - 64);
    }

    public void markReady(HJBleScanDevice device) {
        if (isCurrent(device.device) && !disconnecting) readyDevice = device;
    }

    public static List<HJBleScanDevice> readyDevices() {
        List<HJBleScanDevice> devices = new ArrayList<>();
        for (BleDeviceSession session : SESSIONS.values()) {
            HJBleScanDevice device = session.readyDevice;
            if (device != null) devices.add(device);
        }
        return devices;
    }

    public static boolean isReady(BleDevice device) {
        BleDeviceSession session = find(device);
        return session != null && session.isReady() && BleManager.getInstance().getBleBluetooth(device) != null;
    }

    public void disconnect(BleDevice device) {
        if (!isCurrent(device)) return;
        disconnecting = true;
        Runnable disconnect = () -> {
            if (isCurrent(device)) {
                FastBleListener.getInstance().removeDevice(device);
                BleManager.getInstance().disconnect(device);
            }
        };
        if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) disconnect.run();
        else new android.os.Handler(android.os.Looper.getMainLooper()).post(disconnect);
    }

    public void removeIfCurrent(BleDevice device) {
        if (SESSIONS.remove(device.getMac(), this)) {
            FastBleListener.getInstance().removeDevice(device);
            close();
        }
    }

    public Runnable onDisconnected(Runnable listener) {
        disconnectListeners.add(listener);
        return () -> disconnectListeners.remove(listener);
    }

    private void close() {
        readyDevice = null;
        disconnecting = true;
        sendExecutor.shutdownNow();
        for (Runnable listener : disconnectListeners) listener.run();
        disconnectListeners.clear();
    }

    public int getPacketLength() {
        return packetLength;
    }

    public void setPacketLength(int packetLength) {
        this.packetLength = Math.max(1, packetLength);
    }

    public boolean isConfigMode() {
        return configMode;
    }

    public void setConfigMode(boolean configMode) {
        this.configMode = configMode;
    }

    public boolean isBusy() {
        return busy;
    }

    public void setBusy(boolean busy) {
        this.busy = busy;
    }

    public Future<?> submit(Runnable task) {
        return sendExecutor.submit(task);
    }
}
