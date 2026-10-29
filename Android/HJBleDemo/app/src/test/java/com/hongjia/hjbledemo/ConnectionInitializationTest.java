package com.hongjia.hjbledemo;

import android.app.Application;
import com.clj.fastble.BleManager;
import com.clj.fastble.callback.BleNotifyCallback;
import com.clj.fastble.callback.BleWriteCallback;
import com.clj.fastble.data.BleDevice;
import com.clj.fastble.exception.OtherException;
import com.hongjia.hjbledemo.bean.HJBleScanDevice;
import com.wise.ble.WiseCharacteristic;
import com.wise.ble.ConvertData;
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
    private HJBleScanDevice flowDevice() {
        HJBleScanDevice scanDevice = new HJBleScanDevice();
        scanDevice.device = mock(BleDevice.class);
        when(scanDevice.device.getMac()).thenReturn("00:11:22:33:44:55");
        scanDevice.isConfig = true;
        return scanDevice;
    }

    private BleNotifyCallback subscribeConfig(BleManager manager, ScanBleActivity.ConnectionWork work) throws Exception {
        BlockingQueue<BleNotifyCallback> callbacks = new LinkedBlockingQueue<>();
        doAnswer(call -> {
            BleNotifyCallback callback = call.getArgument(3);
            callbacks.add(callback);
            callback.onNotifySuccess();
            return null;
        }).when(manager).notify(eq(work.device), anyString(), anyString(), any(BleNotifyCallback.class));
        assertTrue(open(manager, work, BleConfig.Ble_Config_Receive_Service).get(2, TimeUnit.SECONDS));
        return callbacks.poll(2, TimeUnit.SECONDS);
    }

    private FutureTask<Boolean> checkFlow(BleManager manager, HJBleScanDevice device, ScanBleActivity.ConnectionWork work) {
        FutureTask<Boolean> result = new FutureTask<>(() -> ScanBleActivity.checkFlowControl(manager, device, work));
        work.worker = new Thread(result);
        work.worker.setDaemon(true);
        work.worker.start();
        return result;
    }

    @Test
    public void disabledCheckSkipsWriteAndResetsPreviousResult() throws Exception {
        HJBleApplication.shareInstance().setFlowControl(false);
        HJBleScanDevice device = flowDevice();
        device.flowControlConfirmed = true;
        device.bFlowControl = true;
        ScanBleActivity.ConnectionWork work = new ScanBleActivity.ConnectionWork(device.device);
        BleManager manager = mock(BleManager.class);
        try {
            // 连接开始后修改设置，不应改变当前连接的查询决定。
            HJBleApplication.shareInstance().setFlowControl(true);
            assertTrue(checkFlow(manager, device, work).get(2, TimeUnit.SECONDS));
            assertFalse(device.flowControlConfirmed);
            assertFalse(device.bFlowControl);
            verifyNoInteractions(manager);
        } finally { BleDeviceSession.remove(device.device); }
    }

    @Test
    public void missingConfigSkipsQueryEvenWhenEnabled() throws Exception {
        HJBleApplication.shareInstance().setFlowControl(true);
        HJBleScanDevice device = flowDevice();
        device.isConfig = false;
        ScanBleActivity.ConnectionWork work = new ScanBleActivity.ConnectionWork(device.device);
        BleManager manager = mock(BleManager.class);
        try {
            assertTrue(checkFlow(manager, device, work).get(2, TimeUnit.SECONDS));
            assertFalse(device.flowControlConfirmed);
            verifyNoInteractions(manager);
        } finally { BleDeviceSession.remove(device.device); }
    }

    @Test
    public void unsupportedReplyContinuesAndLateReplyCannotChangeResult() throws Exception {
        HJBleApplication.shareInstance().setFlowControl(true);
        HJBleScanDevice device = flowDevice();
        ScanBleActivity.ConnectionWork work = new ScanBleActivity.ConnectionWork(device.device);
        BleManager manager = mock(BleManager.class);
        try {
            BleNotifyCallback notifications = subscribeConfig(manager, work);
            doAnswer(call -> {
                notifications.onCharacteristicChanged(ConvertData.utf8ToBytes("unsupported"));
                BleWriteCallback callback = call.getArgument(4);
                callback.onWriteSuccess(1, 1, call.getArgument(3));
                return null;
            }).when(manager).write(eq(device.device), anyString(), anyString(), any(byte[].class), any(BleWriteCallback.class));
            assertTrue(checkFlow(manager, device, work).get(7, TimeUnit.SECONDS));
            assertFalse(device.flowControlConfirmed);
            assertFalse(work.waitingForFlow);
            assertNull(work.flowResponse);
            notifications.onCharacteristicChanged(ConvertData.utf8ToBytes("<rd_uart_fc=1>"));
            assertFalse(device.flowControlConfirmed);
            assertNull(work.flowResponse);
            // 查询结束后配置通知仍处理设备忙/空闲。
            notifications.onCharacteristicChanged(ConvertData.utf8ToBytes("<HJ_BLE_BUSY_STOP_SEND>"));
            assertTrue(work.session.isBusy());
            notifications.onCharacteristicChanged(ConvertData.utf8ToBytes("<HJ_BLE_IDLE_START_SEND>"));
            assertFalse(work.session.isBusy());
            verify(manager, never()).disconnect(any());
        } finally { BleDeviceSession.remove(device.device); }
    }

    @Test
    public void noReplyContinuesWithoutWaitingState() throws Exception {
        HJBleApplication.shareInstance().setFlowControl(true);
        HJBleScanDevice device = flowDevice();
        ScanBleActivity.ConnectionWork work = new ScanBleActivity.ConnectionWork(device.device);
        BleManager manager = mock(BleManager.class);
        doAnswer(call -> {
            BleWriteCallback callback = call.getArgument(4);
            callback.onWriteSuccess(1, 1, call.getArgument(3));
            return null;
        }).when(manager).write(eq(device.device), anyString(), anyString(), any(byte[].class), any(BleWriteCallback.class));
        try {
            assertTrue(checkFlow(manager, device, work).get(7, TimeUnit.SECONDS));
            assertFalse(device.flowControlConfirmed);
            assertFalse(work.waitingForFlow);
            verify(manager, never()).disconnect(any());
        } finally { BleDeviceSession.remove(device.device); }
    }

    @Test
    public void fragmentedFlowReplyIsRecognizedWithoutConsumingBusyReports() throws Exception {
        for (boolean enabled : new boolean[]{true, false}) {
            HJBleApplication.shareInstance().setFlowControl(true);
            HJBleScanDevice device = flowDevice();
            ScanBleActivity.ConnectionWork work = new ScanBleActivity.ConnectionWork(device.device);
            BleManager manager = mock(BleManager.class);
            try {
                BleNotifyCallback notifications = subscribeConfig(manager, work);
                doAnswer(call -> {
                    notifications.onCharacteristicChanged(ConvertData.utf8ToBytes("<HJ_BLE_BUSY_STOP_SEND>"));
                    notifications.onCharacteristicChanged(ConvertData.utf8ToBytes("<rd_uart_"));
                    notifications.onCharacteristicChanged(ConvertData.utf8ToBytes(enabled ? "fc=1>" : "fc=0>"));
                    BleWriteCallback callback = call.getArgument(4);
                    callback.onWriteSuccess(1, 1, call.getArgument(3));
                    return null;
                }).when(manager).write(eq(device.device), anyString(), anyString(), any(byte[].class), any(BleWriteCallback.class));
                assertTrue(checkFlow(manager, device, work).get(2, TimeUnit.SECONDS));
                assertTrue(device.flowControlConfirmed);
                assertEquals(enabled, device.bFlowControl);
                assertTrue(work.session.isBusy());
                verify(manager).write(eq(device.device), eq(BleConfig.Ble_Config_Send_Service.getServiceID()),
                        eq(BleConfig.Ble_Config_Send_Service.getCharacteristicID()), eq(ConvertData.utf8ToBytes("<RD_UART_FC>")), any());
            } finally { BleDeviceSession.remove(device.device); }
        }
    }

    @Test
    public void writeFailureIsNotTreatedAsUnknownFlowControl() throws Exception {
        HJBleApplication.shareInstance().setFlowControl(true);
        HJBleScanDevice device = flowDevice();
        ScanBleActivity.ConnectionWork work = new ScanBleActivity.ConnectionWork(device.device);
        BleManager manager = mock(BleManager.class);
        doAnswer(call -> {
            BleWriteCallback callback = call.getArgument(4);
            callback.onWriteFailure(new OtherException("write failed"));
            return null;
        }).when(manager).write(eq(device.device), anyString(), anyString(), any(byte[].class), any(BleWriteCallback.class));
        try {
            assertFalse(checkFlow(manager, device, work).get(2, TimeUnit.SECONDS));
            assertFalse(device.flowControlConfirmed);
            assertFalse(work.waitingForFlow);
        } finally { BleDeviceSession.remove(device.device); }
    }

    @Test
    public void writeTimeoutStillStopsInitialization() throws Exception {
        HJBleApplication.shareInstance().setFlowControl(true);
        HJBleScanDevice device = flowDevice();
        ScanBleActivity.ConnectionWork work = new ScanBleActivity.ConnectionWork(device.device);
        BleManager manager = mock(BleManager.class);
        try {
            // 不触发写入完成回调，验证底层写入超时不能降级为业务状态未确认。
            assertFalse(checkFlow(manager, device, work).get(7, TimeUnit.SECONDS));
            assertFalse(device.flowControlConfirmed);
            assertFalse(work.waitingForFlow);
        } finally { BleDeviceSession.remove(device.device); }
    }

    @Test
    public void cancelDuringFlowWaitStopsInitialization() throws Exception {
        HJBleApplication.shareInstance().setFlowControl(true);
        HJBleScanDevice device = flowDevice();
        ScanBleActivity.ConnectionWork work = new ScanBleActivity.ConnectionWork(device.device);
        BleManager manager = mock(BleManager.class);
        BlockingQueue<Boolean> written = new LinkedBlockingQueue<>();
        doAnswer(call -> {
            BleWriteCallback callback = call.getArgument(4);
            callback.onWriteSuccess(1, 1, call.getArgument(3));
            written.add(true);
            return null;
        }).when(manager).write(eq(device.device), anyString(), anyString(), any(byte[].class), any(BleWriteCallback.class));
        try {
            FutureTask<Boolean> result = checkFlow(manager, device, work);
            assertNotNull(written.poll(2, TimeUnit.SECONDS));
            work.cancelled.set(true);
            work.worker.interrupt();
            assertFalse(result.get(2, TimeUnit.SECONDS));
            assertFalse(work.waitingForFlow);
        } finally { BleDeviceSession.remove(device.device); }
    }

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
