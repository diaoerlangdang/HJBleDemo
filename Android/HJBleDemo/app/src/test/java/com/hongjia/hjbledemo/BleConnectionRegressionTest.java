package com.hongjia.hjbledemo;

import android.app.Application;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import com.clj.fastble.callback.BleNotifyCallback;
import java.util.UUID;
import android.content.Context;
import java.util.concurrent.TimeUnit;

import com.clj.fastble.BleManager;
import com.clj.fastble.bluetooth.BleBluetooth;
import com.clj.fastble.callback.BleGattCallback;
import com.clj.fastble.data.BleDevice;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.robolectric.shadows.ShadowLooper;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, application = Application.class)
@LooperMode(LooperMode.Mode.PAUSED)
public class BleConnectionRegressionTest {
    private BleManager manager;
    private BluetoothDevice device;
    private BleDevice bleDevice;
    private BluetoothGatt gatt;
    private BluetoothGattCallback platformCallback;

    @Before
    public void setUp() {
        manager = BleManager.getInstance();
        manager.init(RuntimeEnvironment.getApplication());
        manager.destroy();
        manager.setConnectOverTime(15000);
        manager.getBluetoothAdapter().enable();
        manager.setReConnectCount(0);
        device = mock(BluetoothDevice.class);
        when(device.getAddress()).thenReturn("00:11:22:33:44:55");
        when(device.getName()).thenReturn("Test");
        bleDevice = new BleDevice(device);
        gatt = mock(BluetoothGatt.class);
        when(gatt.discoverServices()).thenReturn(true);
        when(device.connectGatt(any(Context.class), anyBoolean(), any(BluetoothGattCallback.class), anyInt()))
                .thenAnswer(call -> {
                    platformCallback = call.getArgument(2);
                    return gatt;
                });
    }

    @Test
    public void cancelPendingConnectionClosesItsGatt() {
        manager.connect(bleDevice, mock(BleGattCallback.class));
        manager.disconnect(bleDevice);
        verify(gatt).disconnect();
        verify(gatt).close();
    }

    @Test
    public void destroyedConnectionCannotRegisterAfterLateServices() {
        BleBluetooth pending = new BleBluetooth(bleDevice);
        pending.connect(bleDevice, false, mock(BleGattCallback.class));
        pending.destroy();
        platformCallback.onConnectionStateChange(gatt, BluetoothGatt.GATT_SUCCESS, BluetoothProfile.STATE_CONNECTED);
        platformCallback.onServicesDiscovered(gatt, BluetoothGatt.GATT_SUCCESS);
        ShadowLooper.idleMainLooper();
        assertNull(manager.getBleBluetooth(bleDevice));
    }

    @Test
    public void duplicateConnectDoesNotReplacePendingGatt() {
        manager.connect(bleDevice, mock(BleGattCallback.class));
        BleGattCallback duplicate = mock(BleGattCallback.class);
        assertNull(manager.connect(bleDevice, duplicate));
        verify(device, times(1)).connectGatt(any(Context.class), anyBoolean(), any(BluetoothGattCallback.class), anyInt());
        verify(duplicate).onConnectFail(eq(bleDevice), any());
        manager.disconnect(bleDevice);
        verify(gatt).close();
    }

    @Test
    public void serviceDiscoveryStillHasAConnectionDeadline() {
        manager.setConnectOverTime(1000);
        BleGattCallback callback = mock(BleGattCallback.class);
        manager.connect(bleDevice, callback);
        platformCallback.onConnectionStateChange(gatt, 0, BluetoothProfile.STATE_CONNECTED);
        ShadowLooper.idleMainLooper(1100, TimeUnit.MILLISECONDS);
        verify(gatt).discoverServices();
        verify(gatt).close();
        verify(callback).onConnectFail(eq(bleDevice), any());
        assertNull(manager.getBleBluetooth(bleDevice));
    }

    private void finishConnection(BleGattCallback callback) {
        manager.connect(bleDevice, callback);
        platformCallback.onConnectionStateChange(gatt, 0, BluetoothProfile.STATE_CONNECTED);
        platformCallback.onServicesDiscovered(gatt, 0);
        ShadowLooper.idleMainLooper();
        assertNotNull(manager.getBleBluetooth(bleDevice));
    }

    @Test
    public void missingDisconnectCallbackStillClosesAndNotifiesOnce() {
        BleGattCallback callback = mock(BleGattCallback.class);
        finishConnection(callback);
        manager.disconnect(bleDevice);
        assertNotNull(manager.getBleBluetooth(bleDevice));
        verify(gatt, never()).close();
        ShadowLooper.idleMainLooper(3100, TimeUnit.MILLISECONDS);
        assertNull(manager.getBleBluetooth(bleDevice));
        verify(gatt).close();
        platformCallback.onConnectionStateChange(gatt, 0, BluetoothProfile.STATE_DISCONNECTED);
        ShadowLooper.idleMainLooper();
        verify(callback, times(1)).onDisConnected(eq(true), eq(bleDevice), eq(gatt), anyInt());
    }

    @Test
    public void cancelledGattCallbacksCannotRemoveTheNextConnection() {
        manager.connect(bleDevice, mock(BleGattCallback.class));
        BluetoothGatt oldGatt = gatt;
        BluetoothGattCallback oldCallback = platformCallback;
        manager.disconnect(bleDevice);
        gatt = mock(BluetoothGatt.class);
        when(gatt.discoverServices()).thenReturn(true);
        BleGattCallback current = mock(BleGattCallback.class);
        finishConnection(current);
        oldCallback.onServicesDiscovered(oldGatt, 0);
        oldCallback.onConnectionStateChange(oldGatt, 0, BluetoothProfile.STATE_DISCONNECTED);
        ShadowLooper.idleMainLooper();
        assertSame(gatt, manager.getBluetoothGatt(bleDevice));
        verify(current, never()).onDisConnected(anyBoolean(), any(), any(), anyInt());
        verify(gatt, never()).close();
    }

    @Test
    public void cancellingOneDeviceLeavesOtherPendingGattAlive() {
        BluetoothDevice other = mock(BluetoothDevice.class);
        when(other.getAddress()).thenReturn("00:11:22:33:44:66");
        BluetoothGatt otherGatt = mock(BluetoothGatt.class);
        when(other.connectGatt(any(Context.class), anyBoolean(), any(BluetoothGattCallback.class), anyInt()))
                .thenReturn(otherGatt);
        manager.connect(bleDevice, mock(BleGattCallback.class));
        manager.connect(new BleDevice(other), mock(BleGattCallback.class));
        manager.disconnect(bleDevice);
        verify(gatt).close();
        verifyNoInteractions(otherGatt);
        manager.disconnect(new BleDevice(other));
        verify(otherGatt).close();
    }

    @Test
    public void sameCharacteristicUuidInDifferentServicesHasSeparateSdkCallbacks() {
        finishConnection(mock(BleGattCallback.class));
        BluetoothGattService one = new BluetoothGattService(UUID.randomUUID(), 0);
        BluetoothGattService two = new BluetoothGattService(UUID.randomUUID(), 0);
        UUID uuid = UUID.randomUUID();
        BluetoothGattCharacteristic a = new BluetoothGattCharacteristic(uuid, 16, 0);
        BluetoothGattCharacteristic b = new BluetoothGattCharacteristic(uuid, 16, 0);
        one.addCharacteristic(a);
        two.addCharacteristic(b);
        UUID ccc = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
        BluetoothGattDescriptor da = new BluetoothGattDescriptor(ccc, 0);
        BluetoothGattDescriptor db = new BluetoothGattDescriptor(ccc, 0);
        a.addDescriptor(da);
        b.addDescriptor(db);
        when(gatt.getService(one.getUuid())).thenReturn(one);
        when(gatt.getService(two.getUuid())).thenReturn(two);
        when(gatt.setCharacteristicNotification(any(), eq(true))).thenReturn(true);
        when(gatt.writeDescriptor(any())).thenReturn(true);
        BleNotifyCallback ca = mock(BleNotifyCallback.class, CALLS_REAL_METHODS);
        BleNotifyCallback cb = mock(BleNotifyCallback.class, CALLS_REAL_METHODS);
        manager.notify(bleDevice, one.getUuid().toString(), uuid.toString(), ca);
        manager.notify(bleDevice, two.getUuid().toString(), uuid.toString(), cb);
        platformCallback.onDescriptorWrite(gatt, da, 0);
        platformCallback.onDescriptorWrite(gatt, db, 0);
        a.setValue(new byte[]{1});
        platformCallback.onCharacteristicChanged(gatt, a);
        a.setValue(new byte[]{99});
        b.setValue(new byte[]{2});
        platformCallback.onCharacteristicChanged(gatt, b);
        ShadowLooper.idleMainLooper();
        verify(ca).onNotifySuccess();
        verify(cb).onNotifySuccess();
        verify(ca).onCharacteristicChanged(new byte[]{1});
        verify(cb).onCharacteristicChanged(new byte[]{2});
        verify(ca, times(1)).onCharacteristicChanged(any());
        verify(cb, times(1)).onCharacteristicChanged(any());
    }
}
