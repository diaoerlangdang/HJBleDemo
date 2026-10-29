package com.hongjia.hjbledemo;

import android.content.Intent;
import android.view.View;
import android.widget.Switch;
import android.widget.TextView;
import com.clj.fastble.BleManager;
import com.clj.fastble.bluetooth.BleBluetooth;
import com.clj.fastble.data.BleDevice;
import com.hongjia.hjbledemo.bean.HJBleScanDevice;
import com.wise.wisekit.utils.SPUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, application = HJBleApplication.class)
public class FlowControlDisplayTest {
    @Test
    public void flowCheckDefaultsOffAndSettingsPageRestoresSavedValue() {
        RuntimeEnvironment.getApplication().getSharedPreferences(SPUtils.FILE_NAME, 0)
                .edit().remove("isFlowControl").commit();
        assertFalse(HJBleApplication.shareInstance().isFlowControl());
        ActivityController<AppConfigSetActivity> first = Robolectric.buildActivity(AppConfigSetActivity.class).create();
        Switch toggle = first.get().findViewById(R.id.switch_flow_control);
        assertFalse(toggle.isChecked());
        toggle.setChecked(true);
        assertTrue(RuntimeEnvironment.getApplication().getSharedPreferences(SPUtils.FILE_NAME, 0)
                .getBoolean("isFlowControl", false));
        first.destroy();
        ActivityController<AppConfigSetActivity> restored = Robolectric.buildActivity(AppConfigSetActivity.class).create();
        try {
            Switch savedToggle = restored.get().findViewById(R.id.switch_flow_control);
            assertTrue(savedToggle.isChecked());
            savedToggle.setChecked(false);
            assertFalse(HJBleApplication.shareInstance().isFlowControl());
        } finally { restored.destroy(); }
    }

    @Test
    public void communicationPageUsesConnectionResultAfterGlobalSettingChanges() {
        BleDevice device = mock(BleDevice.class);
        when(device.getMac()).thenReturn("00:11:22:33:44:55");
        when(device.getName()).thenReturn("Test");
        BleManager manager = mock(BleManager.class);
        when(manager.getBleBluetooth(device)).thenReturn(mock(BleBluetooth.class));
        HJBleScanDevice ready = new HJBleScanDevice();
        ready.device = device;
        BleDeviceSession session = BleDeviceSession.get(device);
        session.markReady(ready);
        Intent intent = new Intent(RuntimeEnvironment.getApplication(), BluetoothDataActivity.class);
        intent.putExtra(BluetoothDataActivity.EXTRAS_DEVICE, device);
        // 即使 Intent 中是旧结果，也应使用当前会话保存的状态。
        intent.putExtra(BluetoothDataActivity.EXTRAS_DEVICE_FLOW_CONTROL_CONFIRMED, true);
        intent.putExtra(BluetoothDataActivity.EXTRAS_DEVICE_IS_FLOW_CONTROL, true);
        try (MockedStatic<BleManager> singleton = mockStatic(BleManager.class)) {
            singleton.when(BleManager::getInstance).thenReturn(manager);
            int[] expected = {R.string.flow_control_unknown, R.string.flow_control_enable, R.string.flow_control_disable};
            for (int i = 0; i < expected.length; i++) {
                ready.flowControlConfirmed = i != 0;
                ready.bFlowControl = i == 1;
                HJBleApplication.shareInstance().setFlowControl(i == 0);
                ActivityController<BluetoothDataActivity> page = Robolectric.buildActivity(BluetoothDataActivity.class, intent).create();
                try {
                    TextView status = page.get().findViewById(R.id.tv_flow_control);
                    assertFalse(page.get().isFinishing());
                    assertEquals(View.VISIBLE, status.getVisibility());
                    assertEquals(page.get().getString(expected[i]), status.getText().toString());
                } finally { page.pause().destroy(); }
            }
        } finally { BleDeviceSession.remove(device); }
    }
}
