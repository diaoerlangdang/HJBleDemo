package com.hongjia.hjbledemo;

import com.clj.fastble.data.BleDevice;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public final class BleDeviceSession {
    private static final ConcurrentHashMap<String, BleDeviceSession> SESSIONS = new ConcurrentHashMap<>();

    private volatile int packetLength = 20;
    private volatile boolean configMode;
    private volatile boolean busy;
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
        if (session != null) session.sendExecutor.shutdownNow();
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
