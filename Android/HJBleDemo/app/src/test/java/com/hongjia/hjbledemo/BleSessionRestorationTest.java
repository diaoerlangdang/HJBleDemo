package com.hongjia.hjbledemo;

import android.app.Application;
import com.clj.fastble.BleManager;
import com.clj.fastble.bluetooth.BleBluetooth;
import com.clj.fastble.data.BleDevice;
import com.hongjia.hjbledemo.adapter.LeDeviceListAdapter;
import com.hongjia.hjbledemo.bean.HJBleScanDevice;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, application = Application.class)
public class BleSessionRestorationTest {
    @Test
    public void secondConnectionJobCannotTakeOwnershipOfTheFirst() {
        BleDevice device = mock(BleDevice.class);
        when(device.getMac()).thenReturn("00:11:22:33:44:55");
        BleDeviceSession first = BleDeviceSession.begin(device);
        assertNotNull(first);
        assertNull(BleDeviceSession.begin(device));
        assertSame(first, BleDeviceSession.find(device));
        first.removeIfCurrent(device);
    }

    @Test
    public void flowControlStateSurvivesWithoutAPageListener() {
        BleDevice device = mock(BleDevice.class);
        when(device.getMac()).thenReturn("00:11:22:33:44:55");
        BleDeviceSession session = BleDeviceSession.get(device);
        session.acceptConfigData("<HJ_BLE_BUSY_".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        session.acceptConfigData("STOP_SEND>".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        assertTrue(session.isBusy());
        session.acceptConfigData("<HJ_BLE_IDLE_START_SEND>".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        assertFalse(session.isBusy());
        session.removeIfCurrent(device);
    }

    @Test
    public void newListRestoresReadyMetadataWithoutAdvertising() {
        BleDevice device = mock(BleDevice.class);
        when(device.getMac()).thenReturn("00:11:22:33:44:55");
        BleManager manager = mock(BleManager.class);
        when(manager.getBleBluetooth(device)).thenReturn(mock(BleBluetooth.class));
        BleDeviceSession session = BleDeviceSession.get(device);
        HJBleScanDevice ready = new HJBleScanDevice();
        ready.device = device;
        ready.isConfig = true;
        ready.bFlowControl = true;
        ready.mtuLen = 244;
        session.setPacketLength(244);
        try (MockedStatic<BleManager> singleton = mockStatic(BleManager.class)) {
            singleton.when(BleManager::getInstance).thenReturn(manager);
            LeDeviceListAdapter list = new LeDeviceListAdapter(RuntimeEnvironment.getApplication());
            list.clear();
            assertEquals(0, list.getItemCount());
            assertFalse(BleDeviceSession.isReady(device));
            session.markReady(ready);
            list.clear();
            assertEquals(1, list.getItemCount());
            HJBleScanDevice advertisement = new HJBleScanDevice();
            advertisement.device = device;
            list.addDevice(advertisement);
            assertTrue(list.getScanDeviceInfo(0).bFlowControl);
            assertEquals(244, list.getScanDeviceInfo(0).mtuLen);
            session.disconnect(device);
            assertFalse(BleDeviceSession.isReady(device));
        } finally { BleDeviceSession.remove(device); }
    }

    @Test
    public void oldSessionCleanupCannotRemoveReplacement() {
        BleDevice device = mock(BleDevice.class);
        when(device.getMac()).thenReturn("00:11:22:33:44:55");
        BleDeviceSession old = BleDeviceSession.get(device);
        old.removeIfCurrent(device);
        BleDeviceSession current = BleDeviceSession.get(device);
        old.removeIfCurrent(device);
        assertSame(current, BleDeviceSession.find(device));
        BleDeviceSession.remove(device);
    }
}
