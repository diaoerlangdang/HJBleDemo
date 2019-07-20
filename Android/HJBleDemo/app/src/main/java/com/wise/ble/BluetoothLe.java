package com.wise.ble;

import java.util.List;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

public class BluetoothLe
{
    private final static String TAG = BluetoothLe.class.getSimpleName();
    
    private Context mContext;

    protected BluetoothManager mBluetoothManager;
    protected BluetoothAdapter mBluetoothAdapter;
    protected String mBluetoothDeviceAddress;
    protected BluetoothGatt mBluetoothGatt;

    private BluetoothAdapter.LeScanCallback mScanCallback;

    public final static UUID UUID_HEART_RATE_MEASUREMENT =
            UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb");
    public final static UUID UUID_CLIENT_CHARACTERISTIC_CONFIG = 
    		UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    
	public BluetoothLe(Context context)
	{
		mContext = context;
	}
	
	public BluetoothGatt getGatt()
	{
		return mBluetoothGatt;
	}

    public BluetoothAdapter getBluetoothAdapter()
    {
        return mBluetoothAdapter;
    }
	
    /**
     * 判断本地设备是否支持蓝牙ble
     *
     * @param 无
     *
     * @return 支持返回true，否则返回false
     * 
     */
	public boolean isBleSupported()
	{
		if(!mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE))
			return false;
		
		return true;
	}
	
    /**
     * 判断本地蓝牙是否打开
     *
     * @param 无
     *
     * @return 已打开返回true，否则返回false
     * 
     */
	public boolean isOpened()
	{
		if(!mBluetoothAdapter.isEnabled())
			return false;
		
		return true;
	}

    /**
     * 设置扫描回调函数
     *
     * @param scanCallback 扫描回调函数
     *
     * @return 设置成功返回true，否则返回false
     *
     */
	public boolean setScanCallBack(BluetoothAdapter.LeScanCallback scanCallback)
	{
		if(scanCallback == null)
			return false;

		mScanCallback = scanCallback;
		return true;
	}

    /**
     * 开始扫描
     *
     * @param 无
     *
     * @return 开始扫描成功返回true，否则返回false
     *
     */
	public boolean startLeScan()
	{
		if(mScanCallback == null)
			return false;

		return mBluetoothAdapter.startLeScan(mScanCallback);
	}

    /**
     * 停止扫描
     *
     * @param 无
     *
     * @return 停止扫描成功返回true，否则返回false
     *
     */
	public boolean stopLeScan()
	{
		if(mScanCallback == null)
			return false;

		mBluetoothAdapter.stopLeScan(mScanCallback);

		return true;
	}
	
    /**
     * 连接本地蓝牙设备
     *
     * @param 无
     *
     * @return 连接成功返回true，否则返回false
     * 
     */
	public boolean connectLocalDevice()
	{
        if (mBluetoothManager == null) 
        {
            mBluetoothManager = (BluetoothManager)mContext.getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) 
            {
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null)
        {
            return false;
        }

        return true;
	}
	
    /**
     * 断开本地蓝牙设备
     *
     * @param 无
     *
     * @return 断开成功返回true，否则返回false
     * 
     */
    public void disconnectLocalDevice() 
    {
        if (mBluetoothGatt == null) 
        {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }
    
    /**
     * 连接远端蓝牙设备
     *
     * @param address 远端蓝牙mac地址
     * @param gattCallback 蓝牙数据的回调函数
     *
     * @return 连接成功返回true，否则返回false
     * 
     */
    public boolean connectDevice(final String address, BluetoothGattCallback gattCallback) 
    {
        if (mBluetoothAdapter == null || address == null) 
        {
            return false;
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null)
        {
            return false;
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        mBluetoothGatt = device.connectGatt(mContext, false, gattCallback);
        if(mBluetoothGatt == null)
        	return false;
        
        mBluetoothDeviceAddress = address;
        return true;
    }
    
    /**
     * 断开远端蓝牙设备
     *
     * @param 无
     *
     * @return 成功
     * 
     */
    public void disconnectDevice() 
    {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) 
        {
            return ;
        }
        mBluetoothGatt.disconnect();
        Log.d(TAG, "Bluetooth disconnect");
    }
    
    /**
     * 读取服务数据，数据在{@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}中
     * 该操作为异步的
     *	
     * @param characteristic 服务特征值
     *
     * @return 读取请求成功返回true，否则返回false
     * 
     */
    public boolean readCharacteristic(BluetoothGattCharacteristic characteristic) 
    {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) 
        {
            return false;
        }
        return mBluetoothGatt.readCharacteristic(characteristic);
    }
    
    /**
     * 写服务数据，数据在{@code BluetoothGattCallback#onCharacteristicWrite(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}中
     * 该操作为异步的
     *	
     * @param characteristic 服务特征值
     *
     * @return 写请求成功返回true，否则返回false
     * 
     */
    public boolean writeCharacteristic(BluetoothGattCharacteristic characteristic) 
    {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) 
        {
            return false;
        }
        //characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
        return mBluetoothGatt.writeCharacteristic(characteristic);
    }

    /**
     * 设置服务通知，数据在{@code BluetoothGattCallback#onDescriptorWrite(BluetoothGatt gatt,BluetoothGattDescriptor descriptor, int status)}中
     * 该操作为异步的
     *	
     * @param characteristic 服务特征值
     * @param enabled 当为true表示打开通知，否则为关闭通知
     *
     * @return 设置通知请求成功返回true，否则返回false
     * 
     */
    public boolean setCharacteristicNotification(BluetoothGattCharacteristic characteristic,boolean enabled) 
    {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) 
        {
            return false;
        }
        if(!mBluetoothGatt.setCharacteristicNotification(characteristic, enabled))
        	return false;

        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID_CLIENT_CHARACTERISTIC_CONFIG);
        if(descriptor == null)
        	return false;
        
        byte[] data;
        if(enabled)
        	data = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE;
        else 
        	data = BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE;
        
        if(!descriptor.setValue(data))
        	return false;
        return mBluetoothGatt.writeDescriptor(descriptor);
    }

    /**
     * 获取服务列表
     *	
     * @param 无
     *
     * @return 服务列表
     * 
     */
    public List<BluetoothGattService> getSupportedGattServices() 
    {
        if (mBluetoothGatt == null) 
        	return null;

        return mBluetoothGatt.getServices();
    }
}
