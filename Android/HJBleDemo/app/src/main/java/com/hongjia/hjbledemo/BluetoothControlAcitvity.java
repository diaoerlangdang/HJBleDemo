package com.hongjia.hjbledemo;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.wise.ble.ConvertData;
import com.wise.ble.WiseBluetoothLe;
import com.wise.ble.WiseCharacteristic;
import com.wise.wisekit.activity.BaseActivity;

import java.util.regex.Pattern;


public class BluetoothControlAcitvity extends BaseActivity
{
    private final static String TAG = BluetoothControlAcitvity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    // 是否支持配置
	public static final String EXTRAS_DEVICE_IS_CONFIG = "DEVICE_IS_CONFIG";
    
    private String mDeviceName;
    private String mDeviceAddress;
	private boolean isConfig;
    private WiseBluetoothLe mble;
    private Context context;
    private EditText sendEdit; 
    private ScrollView scrollView;
    private Button sendBt;
    private Handler mHandler;  
	private static final int MSG_DATA_CHANGE = 0x11;

	// 发送服务
	private WiseCharacteristic mSendCharact = BleConfig.Ble_Data_Send_Service;
	// 接收服务
	private WiseCharacteristic mReceiveCharact = BleConfig.Ble_Data_Receive_Service;


	@Override
	protected int getPageLayoutId() {
		return R.layout.bluetooth_control;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
		isConfig = intent.getBooleanExtra(EXTRAS_DEVICE_IS_CONFIG, false);

		topLeftBtn.setVisibility(View.VISIBLE);

		setRightText("设置");
		rightTitleTxt.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				final Intent intent = new Intent(BluetoothControlAcitvity.this, SetActivity.class);

				intent.putExtra(SetActivity.EXTRAS_SET_IS_CONFIG, isConfig);

				BluetoothControlAcitvity.this.startActivity(intent);
			}
		});
        
        context = this;

        setButtonsState(true);
        
        mble = WiseBluetoothLe.getInstance(getApplicationContext());	//获取蓝牙实例
		mble.mBleCallBack = bleCallBack;

        mHandler = new Handler(new Handler.Callback() {
			@Override
			public boolean handleMessage(@NonNull Message msg) {
				switch (msg.what) {
					case MSG_DATA_CHANGE:
						int color = msg.arg1;
						String strData = (String)msg.obj;
						SpannableStringBuilder builder = new SpannableStringBuilder(strData);

						//ForegroundColorSpan 为文字前景色，BackgroundColorSpan为文字背景色
						ForegroundColorSpan colorSpan = new ForegroundColorSpan(color);

						switch (color) {
							case Color.BLUE: //send

								builder.setSpan(colorSpan, 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
								break;
							case Color.RED:	//error
								builder.setSpan(colorSpan, 0, strData.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
								break;
							case Color.BLACK: //tips
								builder.setSpan(colorSpan, 0, strData.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
								break;

							default: //receive

								builder.setSpan(colorSpan, 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
								break;
						}
						TextView tView = new TextView(context);
						tView.setText(builder);
						LinearLayout layout = findViewById(R.id.scroll_layout);
						layout.addView(tView);
						scrollView.fullScroll(ScrollView.FOCUS_DOWN);
						break;

					default:
						break;
				}

				return false;
			}
		});
	}


	@Override
	protected void initView() {
		super.initView();

		scrollView = findViewById(R.id.scroll);
		sendBt = findViewById(R.id.send);
		sendEdit = findViewById(R.id.sendData);

		// 清屏
		findViewById(R.id.clear_log).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				LinearLayout layout = findViewById(R.id.scroll_layout);
				layout.removeAllViews();
			}
		});

		sendBt.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{

				String sendString = sendEdit.getText().toString();
				if(sendString == null || sendString.isEmpty())
					return ;
				sendEdit.setText("");

				sendBleData(sendString);

			}
		});

	}

	@Override
	protected void onResume() {
		super.onResume();

		if (HJBleApplication.shareInstance().isBleConfig()) {
			// 发送服务
			mSendCharact = BleConfig.Ble_Config_Send_Service;
			// 接收服务
			mReceiveCharact = BleConfig.Ble_Config_Receive_Service;

			setTitle(mDeviceName+"-配置");
		}
		else {
			// 发送服务
			mSendCharact = BleConfig.Ble_Data_Send_Service;
			// 接收服务
			mReceiveCharact = BleConfig.Ble_Data_Receive_Service;

			setTitle(mDeviceName+"-数据");
		}

		if (HJBleApplication.shareInstance().isBleHex()) {
			sendEdit.setText("");
			sendEdit.setHint("请输入十六进制数据");
		}
		else {
			sendEdit.setText("");
			sendEdit.setHint("请输入字符数据");
		}
	}

	void setButtonsState(boolean enable)
	{
		sendBt.setEnabled(enable);

	}

	//发送数据
	void sendBleData(String sendString)
	{
		byte[] tmpBytes;
		if (HJBleApplication.shareInstance().isBleHex()) {

			if (sendString.length()%2 != 0) {
				Toast.makeText(BluetoothControlAcitvity.this, "Hex长度应为2的倍数", Toast.LENGTH_SHORT).show();
				return;
			}

			String pattern = "^[A-Fa-f0-9]+$";

			if (!Pattern.matches(pattern, sendString)) {
				Toast.makeText(BluetoothControlAcitvity.this, "输入格式错误", Toast.LENGTH_SHORT).show();
				return;
			}

			// 添加回车
			if (HJBleApplication.shareInstance().isAddReturn()) {
				sendString += "0D0A";
			}

			tmpBytes = ConvertData.hexStringToBytes(sendString);
		}
		else {
			// 添加回车
			if (HJBleApplication.shareInstance().isAddReturn()) {
				sendString += "\r\n";
			}
			tmpBytes = ConvertData.utf8ToBytes(sendString);
		}

		if (tmpBytes == null) {
			return;
		}

		addLogText("发送：\r\n      "+sendString,Color.BLUE, sendString.length());

		final byte[] bytes = tmpBytes;
		new Thread(new Runnable() {
			@Override
			public void run() {

				if (bytes == null)
					return;

				if(!mble.sendData(mSendCharact, bytes))
					addLogText("发送失败！",Color.RED,0);

			}
		}).start();

	}



	void addLogText(final String log, final int color, int byteLen)
	{
		Message message = new Message();
        message.what = MSG_DATA_CHANGE;  
        message.arg1 = color;
        message.arg2 = byteLen;
        message.obj = log;
        mHandler.sendMessage(message);
	}

	WiseBluetoothLe.OnWiseBluetoothCallBack bleCallBack = new WiseBluetoothLe.OnWiseBluetoothCallBack() {
		@Override
		public void OnWiseBluetoothState(int state) {

			if (state == WiseBluetoothLe.WISE_BLE_DISCONNECTED) {
				addLogText("蓝牙已断开",Color.BLACK,0);
				finish();
			}

		}

		@Override
		public void OnReceiveData(WiseCharacteristic characteristic,  byte[] recvData) {

			if (characteristic.equals(mReceiveCharact)) {
				String str = "";
				if (HJBleApplication.shareInstance().isBleHex()) {
					str = ConvertData.bytesToHexString(recvData, false);
				}
				else {
					str = ConvertData.bytesToUtf8(recvData);
				}
				addLogText("接收：\r\n      " + str, Color.rgb(139, 0, 255), str.length());
			}

		}
	};


	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
        case android.R.id.home:
            onBackPressed();
            return true;

		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onDestroy()
	{
		super.onDestroy();
		mble.disconnectDevice();
		Log.d(TAG, "destroy");
		mble.disconnectLocalDevice();
		Log.d(TAG, "销毁");
	}
}
