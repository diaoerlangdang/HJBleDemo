package com.hongjia.hjbledemo;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.wise.ble.ConvertData;
import com.wise.ble.WiseBluetoothLe;
import com.wise.ble.WiseCharacteristic;
import com.wise.wisekit.activity.BaseActivity;

import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Pattern;


public class BluetoothControlAcitvity extends BaseActivity {
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
    private TextView testBtn;
    private boolean isTesting = false; // 是否正在测试
    private static final int MSG_DATA_CHANGE = 0x11;
    private LinearLayout bottomLayout; //底部

    // 更新速率
    private static final int MSG_UPDATE_RATE = 0x12;

    private RelativeLayout rateLayout;

    // 发送字节数text view
    private TextView sendByteCountTV;

    // 发送字节数
    private int sendByteCount = 0;

    // 接收字节数text view
    private TextView receiveByteCountTV;

    // 接收字节数
    private int receiveByteCount = 0;

    // 接收速率
    private TextView receiveRateTV;

    private int recCountBySecond = 0;

    // 发送服务
    private WiseCharacteristic mSendCharact = BleConfig.Ble_Data_Send_Service;
    // 接收服务
    private WiseCharacteristic mReceiveCharact = BleConfig.Ble_Data_Receive_Service;

    private Timer timer;

    private TimerTask timerTask;


    @Override
    protected int getPageLayoutId() {
        return R.layout.bluetooth_control;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
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

        timer = new Timer();
        timerTask = new TimerTask() {
            @Override
            public void run() {
                Message message = new Message();
                message.what = MSG_UPDATE_RATE;
                message.arg1 = recCountBySecond;
                recCountBySecond = 0;
                mHandler.sendMessage(message);
            }
        };

        mble = WiseBluetoothLe.getInstance(getApplicationContext());    //获取蓝牙实例
        mble.mBleCallBack = bleCallBack;

        mHandler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(@NonNull Message msg) {
                switch (msg.what) {
                    case MSG_DATA_CHANGE:
                        int color = msg.arg1;
                        String strData = (String) msg.obj;
                        SpannableStringBuilder builder = new SpannableStringBuilder(strData);

                        //ForegroundColorSpan 为文字前景色，BackgroundColorSpan为文字背景色
                        ForegroundColorSpan colorSpan = new ForegroundColorSpan(color);

                        switch (color) {
                            case Color.BLUE: //send

                                builder.setSpan(colorSpan, 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                                break;
                            case Color.RED:    //error
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

                    // 更新速率
                    case MSG_UPDATE_RATE:
                        int rate = msg.arg1;
                        showRecCountBySecond(rate);
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

        rateLayout = findViewById(R.id.rate_layout);
        sendByteCountTV = findViewById(R.id.send_byte_count_tv);
        receiveByteCountTV = findViewById(R.id.receive_byte_count_tv);
        receiveRateTV = findViewById(R.id.receive_rate_tv);

        scrollView = findViewById(R.id.scroll);
        sendBt = findViewById(R.id.send);
        sendEdit = findViewById(R.id.sendData);
        bottomLayout = findViewById(R.id.bottom_layout);

        testBtn = findViewById(R.id.test_btn);
        setIsTesting(false);

        testBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setIsTesting(!isTesting);
                if (isTesting) {
                	startTest();
                    bottomLayout.setVisibility(View.GONE);
                } else {
					stopTest();
                    bottomLayout.setVisibility(View.VISIBLE);
                }
            }
        });

        // 清屏
        findViewById(R.id.clear_log).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LinearLayout layout = findViewById(R.id.scroll_layout);
                layout.removeAllViews();

                setReceiveByteCount(0);
                setSendByteCount(0);
            }
        });

        sendBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String sendString = sendEdit.getText().toString();
                if (TextUtils.isEmpty(sendString))
                    return;
                sendEdit.setText("");

                preSendData(sendString);
            }
        });

    }

    void startTest() {
		setIsTesting(true);

		new Thread(new Runnable() {
			@Override
			public void run() {
				long gapTime = HJBleApplication.shareInstance().testGapTime();
				final int dataLen = HJBleApplication.shareInstance().testDataLen();
				final boolean isBleConfig = HJBleApplication.shareInstance().isBleConfig();

				final byte[] data = new byte[dataLen];

				while (isTesting) {
					final String info = ConvertData.bytesToHexString(data, false);
					addLogText("发送：\r\n      " + info, Color.BLUE, info.length());

					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							if (!isBleConfig) {
								addSendByteCount(dataLen);
							}
						}
					});

					if (!mble.sendData(mSendCharact, data)) {
						addLogText("发送失败！", Color.RED, 0);
					}

					for (int i = 0; i < gapTime / 10 && isTesting; i++) {
						try {
							Thread.sleep(10);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}

				}

			}
		}).start();
	}

	// 停止测试
	void stopTest() {
    	setIsTesting(false);
	}


    private void setSendByteCount(int count) {
        sendByteCount = count;
        sendByteCountTV.setText(String.format("发送字节数：%d Byte", count));
    }

    private void addSendByteCount(int count) {
        setSendByteCount(sendByteCount + count);
    }

    private void setReceiveByteCount(int count) {
        receiveByteCount = count;
        receiveByteCountTV.setText(String.format("接收字节数：%d Byte", count));
    }

    private void addReceiveByteCount(int count) {
        setReceiveByteCount(receiveByteCount + count);
    }

    // 显示速率
    private void showRecCountBySecond(int count) {
        receiveRateTV.setText(String.format("实时速率：%d B/s", count));
    }

    // 开始定时器
    private void startTimer() {
        if (timer == null) {
            timer = new Timer();
        }

        if (timerTask == null) {
            timerTask = new TimerTask() {
                @Override
                public void run() {
                    Message message = new Message();
                    message.what = MSG_UPDATE_RATE;
                    message.arg1 = recCountBySecond;
                    recCountBySecond = 0;
                    mHandler.sendMessage(message);
                }
            };
        }

        if (timer != null && timerTask != null) {
            timer.schedule(timerTask, 10, 1000);
        }
    }

    // 停止定时器
    private void stopTimer() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        if (timerTask != null) {
            timerTask.cancel();
            timerTask = null;
        }
    }


    @Override
    protected void onResume() {
        super.onResume();

        if (HJBleApplication.shareInstance().isBleConfig()) {
            // 发送服务
            mSendCharact = BleConfig.Ble_Config_Send_Service;
            // 接收服务
            mReceiveCharact = BleConfig.Ble_Config_Receive_Service;
            mble.setSendDataLenMax(HJBleApplication.shareInstance().groupLen());

            setTitle(mDeviceName + "-配置");

            rateLayout.setVisibility(View.GONE);

            stopTimer();

            setIsTesting(false);
        } else {
            // 发送服务
            mSendCharact = BleConfig.Ble_Data_Send_Service;
            // 接收服务
            mReceiveCharact = BleConfig.Ble_Data_Receive_Service;

            setTitle(mDeviceName + "-数据");

            rateLayout.setVisibility(View.VISIBLE);

            startTimer();
        }

        if (HJBleApplication.shareInstance().isBleHex()) {
            sendEdit.setText("");
            sendEdit.setHint("请输入十六进制数据");
        } else {
            sendEdit.setText("");
            sendEdit.setHint("请输入字符数据");
        }

        mble.setWriteTypeResponse(HJBleApplication.shareInstance().isWriteTypeResponse());
    }

    @Override
    protected void onPause() {
        super.onPause();

        stopTimer();

        setIsTesting(false);
    }

    void setIsTesting(boolean testing) {
        isTesting = testing;
        if (isTesting) {
            testBtn.setText("停止测试");
        } else {
            testBtn.setText("开始测试");
        }
    }

    void setButtonsState(boolean enable) {
        sendBt.setEnabled(enable);
    }

    // 准备发送数据
    void preSendData(String sendString) {

        byte[] tmpBytes;
        if (HJBleApplication.shareInstance().isBleHex()) {

            if (sendString.length() % 2 != 0) {
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
        } else {
            // 添加回车
            if (HJBleApplication.shareInstance().isAddReturn()) {
                sendString += "\r\n";
            }
            tmpBytes = ConvertData.utf8ToBytes(sendString);
        }

        if (tmpBytes == null) {
            return;
        }

        addLogText("发送：\r\n      " + sendString, Color.BLUE, sendString.length());

        sendBleData(tmpBytes);
    }

    //发送数据
    void sendBleData(byte[] data) {

        if (!HJBleApplication.shareInstance().isBleConfig()) {
            addSendByteCount(data.length);
        }

        final byte[] bytes = data;
        new Thread(new Runnable() {
            @Override
            public void run() {

                if (!mble.sendData(mSendCharact, bytes))
                    addLogText("发送失败！", Color.RED, 0);

            }
        }).start();

    }


    void addLogText(final String log, final int color, int byteLen) {
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
                addLogText("蓝牙已断开", Color.BLACK, 0);
                finish();
            }

        }

        @Override
        public void OnReceiveData(WiseCharacteristic characteristic, byte[] recvData) {

            if (characteristic.equals(mReceiveCharact)) {

                // 不是配置模式
                if (!HJBleApplication.shareInstance().isBleConfig()) {
                    final int len = recvData.length;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            addReceiveByteCount(len);
                            recCountBySecond += len;
                        }
                    });
                }

                String str = "";
                if (HJBleApplication.shareInstance().isBleHex()) {
                    str = ConvertData.bytesToHexString(recvData, false);
                } else {
                    str = ConvertData.bytesToUtf8(recvData);
                }
                addLogText("接收：\r\n      " + str, Color.rgb(139, 0, 255), str.length());
            }

        }
    };


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;

            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mble.disconnectDevice();
        Log.d(TAG, "destroy");
        mble.disconnectLocalDevice();
        Log.d(TAG, "销毁");
    }
}
