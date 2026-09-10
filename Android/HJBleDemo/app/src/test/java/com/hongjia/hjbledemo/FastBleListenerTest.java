package com.hongjia.hjbledemo;

import android.app.Application;
import com.clj.fastble.BleManager;
import com.clj.fastble.callback.BleNotifyCallback;
import com.clj.fastble.data.BleDevice;
import com.wise.ble.WiseCharacteristic;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, application = Application.class)
public class FastBleListenerTest {
    @Test
    public void leavingOldPageDoesNotUnregisterNewPage() {
        BleDevice device = mock(BleDevice.class);
        when(device.getMac()).thenReturn("00:11:22:33:44:55");
        WiseCharacteristic characteristic = new WiseCharacteristic("service", "characteristic");
        FastBleListener listener = FastBleListener.getInstance();
        Runnable leaveOld = listener.setNotifyBleCallback(device, characteristic, mock(BleNotifyCallback.class));
        BleNotifyCallback current = mock(BleNotifyCallback.class);
        Runnable leaveCurrent = listener.setNotifyBleCallback(device, characteristic, current);
        leaveOld.run();
        org.junit.Assert.assertSame(current, listener.getNotifyBleCallback(device, characteristic));
        leaveCurrent.run();
        org.junit.Assert.assertNull(listener.getNotifyBleCallback(device, characteristic));
    }

    @Test
    public void routingSeparatesDevicesAndServicesWithTheSameCharacteristic() {
        BleDevice first = mock(BleDevice.class);
        BleDevice second = mock(BleDevice.class);
        when(first.getMac()).thenReturn("00:11:22:33:44:55");
        when(second.getMac()).thenReturn("00:11:22:33:44:66");
        WiseCharacteristic one = new WiseCharacteristic("service-one", "characteristic");
        WiseCharacteristic two = new WiseCharacteristic("service-two", "characteristic");
        FastBleListener listener = FastBleListener.getInstance();
        BleNotifyCallback a = mock(BleNotifyCallback.class);
        BleNotifyCallback b = mock(BleNotifyCallback.class);
        BleNotifyCallback c = mock(BleNotifyCallback.class);
        listener.setNotifyBleCallback(first, one, a);
        listener.setNotifyBleCallback(first, two, b);
        listener.setNotifyBleCallback(second, one, c);
        org.junit.Assert.assertSame(a, listener.getNotifyBleCallback(first, one));
        org.junit.Assert.assertSame(b, listener.getNotifyBleCallback(first, two));
        listener.removeDevice(first);
        org.junit.Assert.assertSame(c, listener.getNotifyBleCallback(second, one));
        listener.removeDevice(second);
    }

    @Test
    public void oldSubscriptionCannotDeliverIntoReconnectedDevice() {
        BleManager manager = mock(BleManager.class);
        BleDevice device = mock(BleDevice.class);
        when(device.getMac()).thenReturn("00:11:22:33:44:55");
        WiseCharacteristic characteristic = new WiseCharacteristic("service", "characteristic");
        FastBleListener listener = FastBleListener.getInstance();
        listener.removeDevice(device);
        try (MockedStatic<BleManager> singleton = mockStatic(BleManager.class)) {
            singleton.when(BleManager::getInstance).thenReturn(manager);
            listener.setNotifyBleCallback(device, characteristic, mock(BleNotifyCallback.class));
            listener.openNotify(device, characteristic);
            listener.removeDevice(device);
            BleNotifyCallback newPage = mock(BleNotifyCallback.class);
            listener.setNotifyBleCallback(device, characteristic, newPage);
            listener.openNotify(device, characteristic);
            ArgumentCaptor<BleNotifyCallback> callbacks = ArgumentCaptor.forClass(BleNotifyCallback.class);
            verify(manager, times(2)).notify(eq(device), anyString(), anyString(), callbacks.capture());
            callbacks.getAllValues().get(0).onCharacteristicChanged(new byte[]{1});
            verifyNoInteractions(newPage);
            callbacks.getAllValues().get(1).onCharacteristicChanged(new byte[]{2});
            verify(newPage).onCharacteristicChanged(new byte[]{2});
        } finally {
            listener.removeDevice(device);
        }
    }
}
