package com.wise.ble;

import java.io.ByteArrayOutputStream;
import java.util.UUID;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.util.Log;

/*
 * @说明： 蓝牙通信类
 *
 * @作者: 吴睿智
 *
 * @创建时间：2014-9-10
 *
 * @修改时间:
 */

public class WiseBluetoothLe extends BluetoothLe
{
	private final static String TAG = WiseBluetoothLe.class.getSimpleName();

	// 蓝牙状态
	public final static int WISE_BLE_CONNECTED = 1;			//蓝牙已连接
	public final static int WISE_BLE_DISCONNECTED = 3;		//蓝牙断开

//    public final static UUID UUID_WISE_SERVICE = UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb");
//
//    public final static UUID UUID_WISE_DATA_RECEIVE = UUID.fromString("0000fff1-0000-1000-8000-00805f9b34fb");
//    public final static UUID UUID_WISE_DATA_SEND = UUID.fromString("0000fff2-0000-1000-8000-00805f9b34fb");

	public final static int RECV_TIME_OUT_SHORT	= 2000;		//短的接收超时，ms
	public final static int RECV_TIME_OUT_MIDDLE = 5000;	//中长的接收超时，ms
	public final static int RECV_TIME_OUT_LONG	= 10000;	//长的接收超时，ms

	public final static int DEFALUT_BLE_SEND_DATA_LEN_MAX = 20;

	private boolean bHighRate = false; // 高速率模式


	private WiseWaitEvent connectEvent = new WiseWaitEvent();

	private WiseWaitEvent stateEvent = new WiseWaitEvent();

	private WiseWaitEvent readEvent = new WiseWaitEvent();

	private WiseWaitEvent sendEvent = new WiseWaitEvent();

	private WiseWaitEvent recvEvent = new WiseWaitEvent();

	private WiseWaitEvent mtuEvent = new WiseWaitEvent();

	private ByteArrayOutputStream recvBuffer = new ByteArrayOutputStream();

	private byte[] readDataInfo;

	private int mBleState = WISE_BLE_DISCONNECTED;

	private static WiseBluetoothLe mble = null;

	public OnWiseBluetoothCallBack mBleCallBack = null;

	private OnWiseBluetoothManagerData mManagerData = null;

	// 阻塞发送接收时，使用的接收数据通知服务
	private WiseCharacteristic mSendReceiveService;

	// 发送的服务
	private WiseCharacteristic mSendService;

	//每包发送的最大包数
	private int sendDataLenMax = DEFALUT_BLE_SEND_DATA_LEN_MAX;

	// 写方式是否为response
	private boolean bResponse = true;

	public interface OnWiseBluetoothCallBack
	{
		/**
		 * 蓝牙状态更改
		 *
		 * @param state 状态 WISE_BLE_CONNECTED/WISE_BLE_DISCONNECTED
		 */
		void OnWiseBluetoothState(int state);

		/**
		 * 蓝牙状态更改
		 *
		 * @param characteristic 接收数据对应服务
		 * @param recvData 接收的数据
		 */
		void OnReceiveData(WiseCharacteristic characteristic, byte[] recvData);

	}

	public interface OnWiseBluetoothManagerData
	{
		/**
		 * 发送数据预处理
		 *
		 * @param characteristic 发送数据对应服务
		 * @param data 要处理的数据
		 *
		 * @return	处理后的数据
		 */
		byte[] OnPreSend(WiseCharacteristic characteristic, byte[] data);

		/**
		 * 接收数据预处理
		 * @param characteristic 接收数据对应服务
		 * @param data 要处理的数据
		 *
		 * @return	处理后的数据
		 */
		byte[] OnPreReceive(WiseCharacteristic characteristic, byte[] data);
	}

	private WiseBluetoothLe(Context context)
	{
		super(context);
	}

	/**
	 * 获取WiseBluetooth类实例
	 *
	 * @param context context
	 *
	 * @return WiseBluetoothLe，当WiseBluetooth未实例化过，且context为null时，返回null
	 * 		       当WiseBluetooth已实例化过，无论context是否为null，皆返回WiseBluetooth类的实例
	 *
	 */
	public static synchronized WiseBluetoothLe getInstance(Context context)
	{
		if (mble == null)
		{
			if(context == null)
				return null;

			mble = new WiseBluetoothLe(context);
		}
		return mble;
	}

	public int getSendDataLenMax() {
		return sendDataLenMax;
	}

	/**
	 * 设置发送数据一包长度
	 * @param sendDataLenMax 发送数据一包长度
	 */
	public void setSendDataLenMax(int sendDataLenMax) {

		this.sendDataLenMax = sendDataLenMax;
	}

	/**
	 * 设置数据处理代理
	 * @param managerData 代理
	 */
	public void setManagerData(OnWiseBluetoothManagerData managerData) {
		mManagerData = managerData;
	}


	/**
	 * 连接远端蓝牙设备
	 *
	 * @param address 远端蓝牙mac地址
	 *
	 * @return 连接成功返回true，否则返回false
	 *
	 */
	public boolean connectDevice(String address)
	{
		return this.connectDevice(address, new OnWiseBluetoothCallBack() {
			@Override
			public void OnWiseBluetoothState(int state) {

			}

			@Override
			public void OnReceiveData(WiseCharacteristic characteristic, byte[] recvData) {

			}
		});
	}


	/**
	 * 连接远端蓝牙设备
	 *
	 * @param address 远端蓝牙mac地址
	 * @param bleCallBack 蓝牙回调函数，当蓝牙非主动断开时，调用
	 *
	 * @return 连接成功返回true，否则返回false
	 *
	 */
	public boolean connectDevice(String address, OnWiseBluetoothCallBack bleCallBack)
	{
		mBleCallBack = bleCallBack;

		connectEvent.init();

		if(!super.connectDevice(address, mGattCallback))
			return false;

		Log.d(TAG, "开始" + System.currentTimeMillis());
		int result = connectEvent.waitSignal(RECV_TIME_OUT_MIDDLE);
		Log.d(TAG, "结束" + System.currentTimeMillis());
		if(WiseWaitEvent.SUCCESS != result)
		{
			disconnectDevice();
			return false;
		}

		return true;
	}

	/**
	 * 开启高速率模式，请在连接成功后调用
	 * @return 成功true，失败false
	 */
	public boolean openHighRate() {

		if (mBluetoothGatt != null && mBluetoothGatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH)) {
			bHighRate = true;
			return true;
		}

		return false;
	}

	/**
	 * 关闭高速率模式，请在连接成功后调用
	 * @return 成功true，失败false
	 */
	public boolean closeHighRate() {
		if (mBluetoothGatt != null && mBluetoothGatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_BALANCED)) {
			bHighRate = false;
			return true;
		}

		return false;
	}

	/**
	 * 是否为高速率模式
	 * @return true or false
	 */
	public boolean isbHighRate() {
		return bHighRate;
	}

	/**
	 * 断开设备连接
	 */
	public void disconnectDevice()
	{
		mBleState = WISE_BLE_DISCONNECTED;
		mBleCallBack = null;
		connectEvent.setSignal(WiseWaitEvent.ERROR_FAILED);
		recvEvent.setSignal(WiseWaitEvent.ERROR_FAILED);
		stateEvent.setSignal(WiseWaitEvent.ERROR_FAILED);
		sendEvent.setSignal(WiseWaitEvent.ERROR_FAILED);
		super.disconnectDevice();
		super.disconnectLocalDevice();
	}


	/**
	 * 是否已连接
	 *
	 * @return 已连接返回true，否则返回false
	 *
	 */
	public boolean isConnect()
	{
		return (mBleState == WISE_BLE_CONNECTED);
	}


	/**
	 * 打开通知
	 *
	 * @param charact 需要打开通知的服务
	 *
	 * @return 成功true，否则返回false
	 *
	 */
	public boolean openNotify(WiseCharacteristic charact)
	{
		if(mBluetoothGatt == null)
			return false;

		BluetoothGattCharacteristic characteristic = getBleCharacteristic(charact);

		if (characteristic == null) {
			return false;
		}

		stateEvent.init();
		if(!setCharacteristicNotification(characteristic, true))
			return false;

		if(WiseWaitEvent.SUCCESS != stateEvent.waitSignal(RECV_TIME_OUT_MIDDLE))
			return false;

		return true;
	}

	/**
	 * 读取服务数据
	 *
	 * @param charact 需要读取服务
	 *
	 * @return 成功true，否则返回false
	 *
	 */
	public byte[] readData(WiseCharacteristic charact)
	{
		if(mBluetoothGatt == null)
			return null;

		BluetoothGattCharacteristic characteristic = getBleCharacteristic(charact);

		if (characteristic == null) {
			return null;
		}

		readDataInfo = null;

		readEvent.init();

		if(!mBluetoothGatt.readCharacteristic(characteristic))
			return null;

		if(WiseWaitEvent.SUCCESS != readEvent.waitSignal(RECV_TIME_OUT_MIDDLE))
			return null;


		return readDataInfo;
	}

	public void setWriteTypeResponse(boolean bResponse) {
		this.bResponse = bResponse;
	}


	/**
	 * 发送数据
	 *
	 * @param charact 发送数据的服务
	 * @param data 要发送的数据
	 *
	 * @return 成功或是啊比
	 */
	public boolean sendData(WiseCharacteristic charact, byte[] data)
	{
		if(mBluetoothGatt == null)
			return false;

		byte[] sendTmp = data;
		if (mManagerData != null) {
			sendTmp = mManagerData.OnPreSend(charact, data);
		}

		int MaxLen = this.sendDataLenMax;

		BluetoothGattCharacteristic characteristic = getBleCharacteristic(charact);
		if (characteristic == null) {
			return false;
		}

		// 写类型
		characteristic.setWriteType(bResponse ? BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT : BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);

		mSendService = charact;

		int nCount = (sendTmp.length + MaxLen - 1) / MaxLen;

		byte[] temp;
		for (int i = 0; i < nCount; i++)
		{
			sendEvent.init();

			if( (i+1) != nCount)
			{
				temp = new byte[MaxLen];
			}
			else
			{
				temp = new byte[sendTmp.length-MaxLen*i];
			}

			for (int j = 0; j < temp.length; j++)
			{
				temp[j] = sendTmp[i*(MaxLen)+j];
			}

			characteristic.setValue(temp);
			if(!mBluetoothGatt.writeCharacteristic(characteristic))
				return false;

			if(WiseWaitEvent.SUCCESS != sendEvent.waitSignal(RECV_TIME_OUT_MIDDLE))
				return false;
		}

		return true;
	}

	/**
	 * 发送接收数据，会阻塞等待接收数据
	 *
	 * @param sendCharact 发送数据的服务
	 * @param recvCharact 接收数据的服务
	 * @param data 发送的数据
	 *
	 * @return 接收的数据
	 */
	public byte[] sendReceive(WiseCharacteristic sendCharact, WiseCharacteristic recvCharact, byte[] data)
	{
		return sendReceive(sendCharact, recvCharact, data, RECV_TIME_OUT_MIDDLE);
	}

	/**
	 * 发送接收数据，会阻塞等待接收数据
	 *
	 * @param sendCharact 发送数据的服务
	 * @param recvCharact 接收数据的服务
	 * @param data 发送的数据
	 * @param timeout 等待超时时间，单位ms
	 *
	 * @return 接收的数据
	 */
	public byte[] sendReceive(WiseCharacteristic sendCharact, WiseCharacteristic recvCharact, byte[] data, int timeout)
	{
		recvEvent.init();

		mSendReceiveService = recvCharact;

		if (sendData(sendCharact, data)) {

			if(WiseWaitEvent.SUCCESS == recvEvent.waitSignal(timeout)) {
				byte[] tmp = recvBuffer.toByteArray();
				recvBuffer.reset();
				return tmp;
			}
			else {
				return null;
			}
		}
		else {
			return null;
		}
	}

	/**
	 * 获取有效的特征服务
	 *
	 * @param charact 传入的特征
	 *
	 * @return 蓝牙可使用的特征
	 */
	public BluetoothGattCharacteristic getBleCharacteristic(WiseCharacteristic charact) {

		if (!charact.isHaveValue()) return null;

		UUID serviceUUID = UUID.fromString(charact.getServiceID());
		UUID charactUUID = UUID.fromString(charact.getCharacteristicID());

		BluetoothGattCharacteristic characteristic;
		if (mBluetoothGatt.getService(serviceUUID) == null) {
			return null;
		}

		characteristic = mBluetoothGatt.getService(serviceUUID).getCharacteristic(charactUUID);

		if (characteristic == null) {
			return null;
		}

		return characteristic;
	}

	// Implements callback methods for GATT events that the app cares about.  For example,
	// connection change and services discovered.
	private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback()
	{
		@Override
		public void onConnectionStateChange(BluetoothGatt gatt, int status,
											int newState)
		{
			// String intentAction;
			if (newState == BluetoothProfile.STATE_CONNECTED)
			{
				mBluetoothGatt.discoverServices();
				Log.d(TAG, "连接成功 " + status);
			}
			else if (newState == BluetoothProfile.STATE_DISCONNECTED)
			{
				mBleState = WISE_BLE_DISCONNECTED;
				if(mBleCallBack != null)
					mBleCallBack.OnWiseBluetoothState(mBleState);

				gatt.close();

				Log.d(TAG, "Disconnected from GATT server "+mBleState);
			}
		}

		@Override
		public void onServicesDiscovered(BluetoothGatt gatt, int status)
		{
			if(status == BluetoothGatt.GATT_SUCCESS)
				mBleState = WISE_BLE_CONNECTED;
			int result = (status == BluetoothGatt.GATT_SUCCESS) ? WiseWaitEvent.SUCCESS : WiseWaitEvent.ERROR_FAILED;
			connectEvent.setSignal(result);
		}

		@Override
		public void onCharacteristicRead(BluetoothGatt gatt,
										 BluetoothGattCharacteristic characteristic, int status)
		{
			int result = (status == BluetoothGatt.GATT_SUCCESS) ? WiseWaitEvent.SUCCESS : WiseWaitEvent.ERROR_FAILED;

			if (status == BluetoothGatt.GATT_SUCCESS)
			{
				// broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
				// For all other profiles, writes the data formatted in HEX.
				final byte[] data = characteristic.getValue();
				if (data != null && data.length > 0)
				{
					readDataInfo = data;
				}
			}

			readEvent.setSignal(result);
		}

		@Override
		public void onCharacteristicWrite(BluetoothGatt gatt,
										  BluetoothGattCharacteristic characteristic, int status)
		{
			if (mSendService.isEqualBleGattCharacteristic(characteristic))
			{
				int result = (status == BluetoothGatt.GATT_SUCCESS) ? WiseWaitEvent.SUCCESS : WiseWaitEvent.ERROR_FAILED;
				sendEvent.setSignal(result);
			}

		}

		@Override
		public void onCharacteristicChanged(BluetoothGatt gatt,
											BluetoothGattCharacteristic characteristic)
		{
			// broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
			final byte[] data = characteristic.getValue();
			if (data != null && data.length > 0)
			{
				byte[] recvTmp = data;
				if (mManagerData != null) {

					WiseCharacteristic charact = new WiseCharacteristic();
					charact.setServiceID(characteristic.getService().getUuid().toString());
					charact.setCharacteristicID(characteristic.getUuid().toString());

					recvTmp = mManagerData.OnPreReceive(charact, data);
				}

				if (recvTmp != null) {
					recvBuffer.reset();
					recvBuffer.write(recvTmp, 0, recvTmp.length);
				}

				if ((recvEvent.getWaitStatus() == WiseWaitEvent.Waitting || recvEvent.getWaitStatus() == WiseWaitEvent.WillWaitting) &&
						mSendReceiveService.isEqualBleGattCharacteristic(characteristic)) {
					recvEvent.setSignal(WiseWaitEvent.SUCCESS);
				}
				else if(mBleCallBack != null) {

					WiseCharacteristic charact = new WiseCharacteristic();
					charact.setServiceID(characteristic.getService().getUuid().toString());
					charact.setCharacteristicID(characteristic.getUuid().toString());

					mBleCallBack.OnReceiveData(charact, data);
				}

			}
		}

		@Override
		public void onDescriptorWrite(BluetoothGatt gatt,
									  BluetoothGattDescriptor descriptor, int status)
		{
			if (status == BluetoothGatt.GATT_SUCCESS)
			{
				Log.d(TAG, "Descript success ");
				if(ConvertData.cmpBytes(descriptor.getValue(), BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE) ||
						ConvertData.cmpBytes(descriptor.getValue(), BluetoothGattDescriptor.ENABLE_INDICATION_VALUE))
				{
					stateEvent.setSignal(WiseWaitEvent.SUCCESS);
				}
			}
			else
			{
				stateEvent.setSignal(WiseWaitEvent.ERROR_FAILED);
			}
		}

		@Override
		public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
			super.onMtuChanged(gatt, mtu, status);
		}
	};



}
