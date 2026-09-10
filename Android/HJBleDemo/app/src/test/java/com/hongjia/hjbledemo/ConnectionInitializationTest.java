package com.hongjia.hjbledemo;

import android.app.Application;
import com.clj.fastble.BleManager;
import com.clj.fastble.callback.BleNotifyCallback;
import com.clj.fastble.data.BleDevice;
import com.wise.ble.WiseCharacteristic;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, application = HJBleApplication.class)
public class ConnectionInitializationTest {
    private FutureTask<Boolean> open(BleManager manager, ScanBleActivity.ConnectionWork work,
                                     WiseCharacteristic characteristic) {
        FutureTask<Boolean> result = new FutureTask<>(() -> {
            try (MockedStatic<BleManager> singleton = mockStatic(BleManager.class)) {
                singleton.when(BleManager::getInstance).thenReturn(manager);
                return ScanBleActivity.openNotifyBleSynchronization(manager, work.device, characteristic, work);
            }
        });
        work.worker = new Thread(result);
        work.worker.setDaemon(true);
        work.worker.start();
        return result;
    }

    @Test
    public void cancelDuringNotifyEndsWaitAndLateSuccessCannotFinishNextJob() throws Exception {
        BleManager manager = mock(BleManager.class);
        BleDevice device = mock(BleDevice.class);
        when(device.getMac()).thenReturn("00:11:22:33:44:55");
        BlockingQueue<BleNotifyCallback> notifications = new LinkedBlockingQueue<>();
        doAnswer(call -> { notifications.add(call.getArgument(3)); return null; })
                .when(manager).notify(eq(device), anyString(), anyString(), any(BleNotifyCallback.class));
        WiseCharacteristic characteristic = new WiseCharacteristic("service", "characteristic");
        ScanBleActivity.ConnectionWork first = new ScanBleActivity.ConnectionWork(device);
        try (MockedStatic<BleManager> singleton = mockStatic(BleManager.class)) {
            singleton.when(BleManager::getInstance).thenReturn(manager);
            FutureTask<Boolean> firstResult = open(manager, first, characteristic);
            BleNotifyCallback stale = notifications.poll(2, TimeUnit.SECONDS);
            assertNotNull(stale);
            ScanBleActivity activity = new ScanBleActivity();
            ReflectionHelpers.setField(activity, "connectionCancellation", first);
            AtomicBoolean running = ReflectionHelpers.getField(activity, "connectionRunning");
            running.set(true);
            ReflectionHelpers.callInstanceMethod(activity, "cancelConnection");
            assertFalse(firstResult.get(2, TimeUnit.SECONDS));
            verify(manager).disconnect(device);

            ScanBleActivity.ConnectionWork second = new ScanBleActivity.ConnectionWork(device);
            FutureTask<Boolean> secondResult = open(manager, second, characteristic);
            BleNotifyCallback current = notifications.poll(2, TimeUnit.SECONDS);
            assertNotNull(current);
            stale.onNotifySuccess();
            assertFalse(secondResult.isDone());
            current.onNotifySuccess();
            assertTrue(secondResult.get(2, TimeUnit.SECONDS));
        } finally {
            BleDeviceSession.remove(device);
            FastBleListener.getInstance().removeDevice(device);
        }
    }

    @Test
    public void missingGattServicesIsHandledAsUnavailable() {
        BleManager manager = mock(BleManager.class);
        BleDevice device = mock(BleDevice.class);
        when(manager.getBluetoothGattServices(device)).thenReturn(null);
        assertFalse(new ScanBleActivity().isSupportConfigService(manager, device));
    }
}
