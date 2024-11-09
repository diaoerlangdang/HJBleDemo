package com.hongjia.hjbledemo;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.icu.text.SimpleDateFormat;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.clj.fastble.BleManager;
import com.clj.fastble.callback.BleGattCallback;
import com.clj.fastble.callback.BleNotifyCallback;
import com.clj.fastble.callback.BleWriteCallback;
import com.clj.fastble.data.BleDevice;
import com.clj.fastble.exception.BleException;
import com.wise.ble.ConvertData;
import com.wise.ble.WiseBluetoothLe;
import com.wise.ble.WiseCharacteristic;
import com.wise.ble.WiseWaitEvent;
import com.wise.wisekit.activity.BaseActivity;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.regex.Pattern;

public class BluetoothDataActivity extends BaseActivity {

    private final static String TAG = BluetoothDataActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE = "DEVICE";
    // 是否支持配置
    public static final String EXTRAS_DEVICE_IS_CONFIG = "DEVICE_IS_CONFIG";
    // 是否为流控信息
    public static final String EXTRAS_DEVICE_IS_FLOW_CONTROL = "DEVICE_IS_FLOW_CONTROL";

    // 最大的分包数据
    public static final String EXTRAS_DEVICE_GROUP_LEN_MAX = "DEVICE_GROUP_LEN_MAX";

    private List<SendReceiveDataBean> mDataList = new ArrayList<>();
    private String mDeviceName;
    private String mDeviceAddress;
    private BleDevice mBleDevice;
    private boolean isConfig;
    private Context context;
    private EditText sendEdit;
    private TextView testBtn;
    private Button sendBt;
    private RecyclerView dataListView;
    private SendReceiveDataAdapter mDataAdapter;

    private WiseWaitEvent sendEvent = new WiseWaitEvent();

    private Handler mHandler;
    private boolean isTesting = false; // 是否正在测试
    private static final int MSG_DATA_CHANGE = 0x11;
    private LinearLayout bottomLayout; //底部

    // 更新速率
    private static final int MSG_UPDATE_RECEIVE_RATE = 0x12;
    private static final int MSG_UPDATE_SEND_RATE = 0x13;
    private static final int MSG_UPDATE_AVERAGE_SEND_RATE = 0x14;

    // 字节数
    private RelativeLayout dataBytesLayout;

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
    // 发送速率
    private TextView sendRateTV;

    // 最大接收速率
    private TextView maxReceiveRateTV;
    // 最大发送速率
    private TextView maxSendRateTV;
    // 平均发送速率
    private TextView averageSendRateTV;
    // 流控
    private TextView tvFlowControl;

    private TextView readVersionBtn;

    private int recCountBySecond = 0;

//    private int sendCountBySecond = 0;

    // 最大发送速率
    private int maxSendRate = 0;
    // 最大接收速率
    private int maxReceiveRate = 0;

    // 发送服务
    private WiseCharacteristic mSendCharact = BleConfig.Ble_Data_Send_Service();
    // 接收服务
    private WiseCharacteristic mReceiveCharact = BleConfig.Ble_Data_Receive_Service();

    private Timer timer;

    private TimerTask timerTask;

    // 蓝牙是否繁忙
    private boolean bBusyBle = false;

    // 是否为流控模式
    private boolean bFlowControl = false;

    private String fileNameSend = "";
    private String fileNameReceive = "";


    @Override
    protected int getPageLayoutId() {
        return R.layout.activity_bluetooth_data;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        final Intent intent = getIntent();
        mBleDevice = intent.getParcelableExtra(EXTRAS_DEVICE);
        mDeviceName = mBleDevice.getName();
        mDeviceAddress = mBleDevice.getMac().toUpperCase();
        isConfig = intent.getBooleanExtra(EXTRAS_DEVICE_IS_CONFIG, false);
        bFlowControl = intent.getBooleanExtra(EXTRAS_DEVICE_IS_FLOW_CONTROL, false);

        super.onCreate(savedInstanceState);

        fileNameSend = getSendFileName();
        fileNameReceive = getReceiveFileName();

        topLeftBtn.setVisibility(View.VISIBLE);

        setRightText(getResources().getString(R.string.setting_title));
        rightTitleTxt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isTesting) {
                    Toast.makeText(BluetoothDataActivity.this, "正在测试...", Toast.LENGTH_SHORT).show();
                    return;
                }
                final Intent intent = new Intent(BluetoothDataActivity.this, SetActivity.class);

                intent.putExtra(SetActivity.EXTRAS_SET_IS_CONFIG, isConfig);
                intent.putExtra(SetActivity.EXTRAS_DEVICE, mBleDevice);

                BluetoothDataActivity.this.startActivity(intent);
            }
        });

        context = this;

        setButtonsState(true);

        timer = new Timer();
        timerTask = new TimerTask() {
            @Override
            public void run() {
                Message message = new Message();
                message.what = MSG_UPDATE_RECEIVE_RATE;
                message.arg1 = recCountBySecond;
//                message.arg2 = sendCountBySecond;
                recCountBySecond = 0;
//                sendCountBySecond = 0;
                mHandler.sendMessage(message);
            }
        };

        BleManager.getInstance().getBleBluetooth(mBleDevice).addConnectGattCallback(new BleGattCallback() {
            @Override
            public void onStartConnect() {

            }

            @Override
            public void onConnectFail(BleDevice bleDevice, BleException e) {

            }

            @Override
            public void onConnectSuccess(BleDevice bleDevice, BluetoothGatt bluetoothGatt, int i) {

            }

            @Override
            public void onDisConnected(boolean b, BleDevice bleDevice, BluetoothGatt bluetoothGatt, int i) {
                SendReceiveDataBean dataBean = new SendReceiveDataBean(SendReceiveDataBean.DataTypeOther, getResources().getString(R.string.ble_disconnected));

                addDataInfoItem(dataBean);
                finish();
            }
        });

        mHandler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(@NonNull Message msg) {
                switch (msg.what) {
                    // 更新速率
                    case MSG_UPDATE_RECEIVE_RATE: {
                        int recvRate = msg.arg1;
//                        int sendRate = msg.arg2;

                        showRecCountBySecond(recvRate);
//                        showSendCountBySecond(sendRate);
                    }
                        break;
                    case MSG_UPDATE_SEND_RATE: {
                        int sendRate = msg.arg1;
                        showSendCountBySecond(sendRate);
                    }
                        break;
                    case MSG_UPDATE_AVERAGE_SEND_RATE: {
                        int sendRate = msg.arg1;
                        showAverageSendRate(sendRate);
                    }
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

        dataListView = findViewById(R.id.data_list_view);
        dataBytesLayout = findViewById(R.id.data_bytes_layout);
        sendByteCountTV = findViewById(R.id.send_byte_count_tv);
        sendRateTV = findViewById(R.id.send_rate_tv);
        maxSendRateTV = findViewById(R.id.max_send_rate_tv);
        averageSendRateTV = findViewById(R.id.average_send_rate_tv);
        receiveByteCountTV = findViewById(R.id.receive_byte_count_tv);
        receiveRateTV = findViewById(R.id.receive_rate_tv);
        maxReceiveRateTV = findViewById(R.id.max_receive_rate_tv);
        tvFlowControl = findViewById(R.id.tv_flow_control);
        tvFlowControl.setText(bFlowControl ? getResources().getString(R.string.flow_control_enable): getResources().getString(R.string.flow_control_disable));
        boolean supportFlowControl = HJBleApplication.shareInstance().isFlowControl();
        tvFlowControl.setVisibility(supportFlowControl ? View.VISIBLE : View.GONE);
        readVersionBtn = findViewById(R.id.read_version_btn);

        sendBt = findViewById(R.id.send);
        sendEdit = findViewById(R.id.sendData);
        bottomLayout = findViewById(R.id.bottom_layout);

        testBtn = findViewById(R.id.test_btn);
        setIsTesting(false);

        mDataAdapter = new SendReceiveDataAdapter(this);
        dataListView.setLayoutManager(new LinearLayoutManager(this));
        dataListView.addItemDecoration(new DividerItemDecoration(this, LinearLayoutManager.VERTICAL));
        dataListView.setAdapter(mDataAdapter);

        testBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setIsTesting(!isTesting);
                if (isTesting) {
                    startTest();

                } else {
                    stopTest();

                }
            }
        });



        // 清屏
        findViewById(R.id.clear_log).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mDataAdapter.clear();
                mDataAdapter.notifyDataSetChanged();

                maxSendRate = 0;
                maxReceiveRate = 0;
                setReceiveByteCount(0);
                setSendByteCount(0);
                averageSendRateTV.setText(getResources().getString(R.string.average_send_rate_default));
                maxSendRateTV.setText(getResources().getString(R.string.max_send_rate_default));

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

        readVersionBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String sendString = "<RD_SOFT_VERSION>";
                SendReceiveDataBean dataBean = new SendReceiveDataBean(SendReceiveDataBean.DataTypeSend, sendString);

                addDataInfoItem(dataBean);

                byte[] tmpBytes = ConvertData.hexStringToBytes(sendString);
                sendBleData(tmpBytes);
            }
        });

    }

    // 添加数据
    void addDataInfoItem(final SendReceiveDataBean dataBean) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mDataAdapter.addDataItem(dataBean);
                mDataAdapter.notifyDataSetChanged();
                // 滚动到最下面
                int lastPosition = mDataAdapter.getItemCount() - 1;
                dataListView.smoothScrollToPosition(lastPosition);
            }
        });

    }

    BleWriteCallback bleWriteCallback = new BleWriteCallback() {
        @Override
        public void onWriteSuccess(int i, int i1, byte[] bytes) {
            sendEvent.setSignal(WiseWaitEvent.SUCCESS);
        }

        @Override
        public void onWriteFailure(BleException e) {
            sendEvent.setSignal(WiseWaitEvent.ERROR_FAILED);

            SendReceiveDataBean dataBean = new SendReceiveDataBean(SendReceiveDataBean.DataTypeFailed, getResources().getString(R.string.send_failure));

            addDataInfoItem(dataBean);
        }
    };

    private boolean sendDataSynchronization(BleManager bleManager, BleDevice bleDevice, WiseCharacteristic characteristic, byte[] data) {
        sendEvent.init();
        long startTime = System.currentTimeMillis(); // 获取开始时间戳
        bleManager.write(bleDevice, characteristic.getServiceID(), characteristic.getCharacteristicID(), data, bleWriteCallback);

        // respone 才执行
        if (HJBleApplication.shareInstance().isWriteTypeResponse())  {
            int result = sendEvent.waitSignal(5000);
//        bleManager.getBleBluetooth(bleDevice).removeWriteCallback(characteristic.getCharacteristicID());
            if(WiseWaitEvent.SUCCESS != result) {
                return false;
            }
        }

        long endTime = System.currentTimeMillis(); // 获取结束时间戳
        long timeTaken = endTime - startTime; // 计算时间差（以毫秒为单位）
        double timeTakenInSeconds = timeTaken / 1000.0; // 将时间差转换为秒
        int rate = (int) (data.length / timeTakenInSeconds);  // 计算速率（每秒字节数）

        Message message = new Message();
        message.what = MSG_UPDATE_SEND_RATE;
        message.arg1 = rate;
        mHandler.sendMessage(message);

        return true;
    }

    void startTest() {
        setIsTesting(true);
        bottomLayout.setVisibility(View.GONE);

        new Thread(new Runnable() {
            @Override
            public void run() {
                long gapTime = HJBleApplication.shareInstance().testGapTime();
                final int totalDataLen = HJBleApplication.shareInstance().testDataLen();
                final boolean isBleConfig = HJBleApplication.shareInstance().isBleConfig();
                // 是否使用文件测试
                final boolean bUseFileTest = HJBleApplication.shareInstance().useFileTest();
                // 文件路径
//                final String filePath = HJBleApplication.shareInstance().testFilePath();

                Message message2 = new Message();
                message2.what = MSG_UPDATE_AVERAGE_SEND_RATE;
                message2.arg1 = 0;
                mHandler.sendMessage(message2);

                if (bUseFileTest) {
                    try {
                        Uri uri = HJBleApplication.shareInstance().getTestFileUri();
//                        File file = new File(filePath);// 成文件路径中获取文件
                        if (uri == null) {
                            stopTest();
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(BluetoothDataActivity.this, getResources().getString(R.string.test_file_not_exist), Toast.LENGTH_SHORT).show();
                                }
                            });
                            return;
                        }

                        ContentResolver contentResolver = getContentResolver();
                        InputStream fileInputStream = contentResolver.openInputStream(uri);
//                        InputStream fileInputStream = getContentResolver().openInputStream(uri);

//                        fileInputStream = new FileInputStream(file);
                        BufferedInputStream inputStream = new BufferedInputStream(fileInputStream);

                        long startTime = System.currentTimeMillis(); // 获取开始时间戳

                        int groupDataLen = BleManager.getInstance().getSplitWriteNum();
                        byte[] inputBuffer = new byte[groupDataLen];
                        int readCount;
                        long fileTotalLen = 0;
                        while ((readCount = inputStream.read(inputBuffer, 0, groupDataLen)) != -1) {
                            if (bBusyBle) {
                                stopTest();
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(BluetoothDataActivity.this, getResources().getString(R.string.ble_busy), Toast.LENGTH_SHORT).show();
                                    }
                                });
                                fileInputStream.close();
                                inputStream.close();
                                return;
                            }

                            fileTotalLen += readCount;

                            byte[] sendBytes = new byte[readCount];
                            System.arraycopy(inputBuffer, 0, sendBytes, 0, readCount);

                            final int sendLen = readCount;
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (!isBleConfig) {
                                        addSendByteCount(sendLen);
//                                        sendCountBySecond += sendLen;
                                    }
                                }
                            });

                            if (!sendDataSynchronization(BleManager.getInstance(), mBleDevice, mSendCharact, sendBytes)) {
                                SendReceiveDataBean dataBean = new SendReceiveDataBean(SendReceiveDataBean.DataTypeFailed, getResources().getString(R.string.send_failure));

                                addDataInfoItem(dataBean);

                                stopTest();
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(BluetoothDataActivity.this, getResources().getString(R.string.ble_busy), Toast.LENGTH_SHORT).show();
                                    }
                                });
                                fileInputStream.close();
                                inputStream.close();
                            }

                            if (gapTime > 0) {
                                SystemClock.sleep(gapTime);
                            }

                        }

                        fileInputStream.close();
                        inputStream.close();
                        stopTest();

                        long endTime = System.currentTimeMillis(); // 获取结束时间戳
                        long timeTaken = endTime - startTime; // 计算时间差（以毫秒为单位）
                        double timeTakenInSeconds = timeTaken / 1000.0; // 将时间差转换为秒
                        int rate = (int) (fileTotalLen / timeTakenInSeconds); // 计算速率（每秒字节数）
                        Message message = new Message();
                        message.what = MSG_UPDATE_AVERAGE_SEND_RATE;
                        message.arg1 = rate;
                        mHandler.sendMessage(message);

                        Message message1 = new Message();
                        message1.what = MSG_UPDATE_SEND_RATE;
                        message1.arg1 = 0;
                        mHandler.sendMessage(message1);



                    } catch (IOException e) {
                        stopTest();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(BluetoothDataActivity.this, getResources().getString(R.string.test_file_not_exist), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                } else {
                    int groupDataLen = BleManager.getInstance().getSplitWriteNum();

                    long startTime = System.currentTimeMillis(); // 获取开始时间戳

                    // 循环创建字节数组片段
                    for (int i = 0; i < totalDataLen && isTesting; i += groupDataLen) {
                        int len = Math.min(groupDataLen, totalDataLen - i); // 确保最后一个片段不会超出总长度
                        byte[] groupData = new byte[len]; // 创建一个新的字节数组片段
                        // 这里可以填充数据，例如设置为0
                        Arrays.fill(groupData, (byte) 0);


                        // 蓝牙设备不繁忙时，可发送测试数据
                        if (!bBusyBle) {
                            final String info = ConvertData.bytesToHexString(groupData, false);
//                    SendReceiveDataBean sendDataBean = new SendReceiveDataBean(SendReceiveDataBean.DataTypeSend, info);
//
//                    addDataInfoItem(sendDataBean);

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (!isBleConfig) {
                                        addSendByteCount(len);
//                                        sendCountBySecond += len;
                                    }
                                }
                            });

                            if (!sendDataSynchronization(BleManager.getInstance(), mBleDevice, mSendCharact, groupData)) {
                                SendReceiveDataBean dataBean = new SendReceiveDataBean(SendReceiveDataBean.DataTypeFailed, getResources().getString(R.string.send_failure));

                                addDataInfoItem(dataBean);
                            }
                        }

                        if (gapTime > 0) {
                            SystemClock.sleep(gapTime);
                        }
                    }

                    stopTest();
                    long endTime = System.currentTimeMillis(); // 获取结束时间戳
                    long timeTaken = endTime - startTime; // 计算时间差（以毫秒为单位）
                    double timeTakenInSeconds = timeTaken / 1000.0; // 将时间差转换为秒
                    int rate = (int) (totalDataLen / timeTakenInSeconds); // 计算速率（每秒字节数）
                    Message message = new Message();
                    message.what = MSG_UPDATE_AVERAGE_SEND_RATE;
                    message.arg1 = rate;
                    mHandler.sendMessage(message);

                    Message message1 = new Message();
                    message1.what = MSG_UPDATE_SEND_RATE;
                    message1.arg1 = 0;
                    mHandler.sendMessage(message1);
                }

            }
        }).start();
    }

    // 停止测试
    void stopTest() {
        setIsTesting(false);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                bottomLayout.setVisibility(View.VISIBLE);
            }
        });
    }


    private void setSendByteCount(int count) {
        sendByteCount = count;
        sendByteCountTV.setText(String.format(getResources().getString(R.string.send_byte_format), count));
    }

    private void addSendByteCount(int count) {
        setSendByteCount(sendByteCount + count);
    }

    private void setReceiveByteCount(int count) {
        receiveByteCount = count;
        receiveByteCountTV.setText(String.format(getResources().getString(R.string.receive_byte_format), count));
    }

    private void addReceiveByteCount(int count) {
        setReceiveByteCount(receiveByteCount + count);
    }

    // 显示接收速率
    private void showRecCountBySecond(int count) {
        receiveRateTV.setText(String.format(getResources().getString(R.string.receive_rate_format), count));
        if (count >= maxReceiveRate) {
            maxReceiveRate = count;
            maxReceiveRateTV.setText(String.format(getResources().getString(R.string.max_receive_rate_format), maxReceiveRate));
        }

    }

    // 显示发送速率
    private void showSendCountBySecond(int count) {
        sendRateTV.setText(String.format(getResources().getString(R.string.send_rate_format), count));
        if (count >= maxSendRate) {
            maxSendRate = count;
            maxSendRateTV.setText(String.format(getResources().getString(R.string.max_send_rate_format), maxSendRate));
        }
    }

    private void showAverageSendRate(int count) {
        averageSendRateTV.setText(String.format(getResources().getString(R.string.average_send_rate_format), count));
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
                    message.what = MSG_UPDATE_RECEIVE_RATE;
                    message.arg1 = recCountBySecond;
//                    message.arg2 = sendCountBySecond;
                    recCountBySecond = 0;
//                    sendCountBySecond = 0;
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

            setTitle(mDeviceName + getResources().getString(R.string.data_title_config_suffix));

            dataBytesLayout.setVisibility(View.GONE);

            stopTimer();

            setIsTesting(false);

            readVersionBtn.setVisibility(View.VISIBLE);
        } else {
            // 发送服务
            mSendCharact = BleConfig.Ble_Data_Send_Service();
            // 接收服务
            mReceiveCharact = BleConfig.Ble_Data_Receive_Service();

            setTitle(mDeviceName + getResources().getString(R.string.data_title_data_suffix));

            dataBytesLayout.setVisibility(View.VISIBLE);

            readVersionBtn.setVisibility(View.GONE);

            startTimer();
        }

        if (HJBleApplication.shareInstance().isBleHex()) {
            sendEdit.setText("");
            sendEdit.setHint(getResources().getString(R.string.please_input_hex_data));
        } else {
            sendEdit.setText("");
            sendEdit.setHint(getResources().getString(R.string.please_input_character_data));
        }

        writeTypeBle(mSendCharact, HJBleApplication.shareInstance().isWriteTypeResponse());

        FastBleListener.getInstance().setNotifyBleCallback(BleConfig.Ble_Config_Receive_Service.getCharacteristicID(), new BleNotifyCallback() {
            @Override
            public void onNotifySuccess() {

            }

            @Override
            public void onNotifyFailure(BleException e) {

            }

            @Override
            public void onCharacteristicChanged(byte[] bytes) {
                String recvStr = ConvertData.bytesToUtf8(bytes);
                // 蓝牙设备繁忙
                if (recvStr.equals("<HJ_BLE_BUSY_STOP_SEND>")) {
                    SendReceiveDataBean dataBean = new SendReceiveDataBean(SendReceiveDataBean.DataTypeOther, getResources().getString(R.string.ble_busy));

                    addDataInfoItem(dataBean);
                    bBusyBle = true;
                } else if (recvStr.equals("<HJ_BLE_IDLE_START_SEND>")) {
                    SendReceiveDataBean dataBean = new SendReceiveDataBean(SendReceiveDataBean.DataTypeOther, getResources().getString(R.string.ble_idle));

                    addDataInfoItem(dataBean);
                    bBusyBle = false;
                }
            }
        });

        FastBleListener.getInstance().setNotifyBleCallback(mReceiveCharact.getCharacteristicID(), new BleNotifyCallback() {
            @Override
            public void onNotifySuccess() {

            }

            @Override
            public void onNotifyFailure(BleException e) {

            }

            @Override
            public void onCharacteristicChanged(byte[] bytes) {
                // 不是配置模式
                if (!HJBleApplication.shareInstance().isBleConfig()) {
                    final int len = bytes.length;
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
                    str = ConvertData.bytesToHexString(bytes, false);
                } else {
                    str = ConvertData.bytesToUtf8(bytes);
                }

                writeFile(str, false);
                SendReceiveDataBean dataBean = new SendReceiveDataBean(SendReceiveDataBean.DataTypeReceive, str);

                addDataInfoItem(dataBean);
            }
        });
    }

    public void writeTypeBle(WiseCharacteristic chara,  boolean bRespone){
        BluetoothGattService service= BleManager.getInstance().getBluetoothGatt(mBleDevice).getService(UUID.fromString(chara.getServiceID()));
        BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString(chara.getCharacteristicID()));

        if (characteristic == null) {
            SendReceiveDataBean dataBean = new SendReceiveDataBean(SendReceiveDataBean.DataTypeOther, chara.getServiceID() + "  " + chara.getCharacteristicID()+"该服务不存在");

            addDataInfoItem(dataBean);
            return;
        }

        //设置写入类型，WRITE_TYPE_DEFAULT：需要设备回应  WRITE_TYPE_NO_RESPONSE  不需要设备回应
        characteristic.setWriteType(bRespone ? BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT : BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
    }

    @Override
    protected void onPause() {
        super.onPause();

        stopTimer();

        setIsTesting(false);
    }

    void setIsTesting(boolean testing) {
        isTesting = testing;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (isTesting) {
                    testBtn.setText(getResources().getString(R.string.stop_test_btn));
                } else {
                    testBtn.setText(getResources().getString(R.string.start_test_btn));
                }
            }
        });

    }

    void setButtonsState(boolean enable) {
        sendBt.setEnabled(enable);
    }

    // 准备发送数据
    void preSendData(String sendString) {

        byte[] tmpBytes;
        if (HJBleApplication.shareInstance().isBleHex()) {

            if (sendString.length() % 2 != 0) {
                Toast.makeText(BluetoothDataActivity.this, getResources().getString(R.string.hex_len_error), Toast.LENGTH_SHORT).show();
                return;
            }

            String pattern = "^[A-Fa-f0-9]+$";

            if (!Pattern.matches(pattern, sendString)) {
                Toast.makeText(BluetoothDataActivity.this, getResources().getString(R.string.input_format_error), Toast.LENGTH_SHORT).show();
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

        writeFile(sendString + "\r\n", true);

        SendReceiveDataBean dataBean = new SendReceiveDataBean(SendReceiveDataBean.DataTypeSend, sendString);

        addDataInfoItem(dataBean);

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

                Message message2 = new Message();
                message2.what = MSG_UPDATE_AVERAGE_SEND_RATE;
                message2.arg1 = 0;
                mHandler.sendMessage(message2);

                long startTime = System.currentTimeMillis(); // 获取开始时间戳

                int groupLen = BleManager.getInstance().getSplitWriteNum();

                int start = 0;
                while (start < bytes.length) {
                    int end = Math.min(start + groupLen, bytes.length);
                    byte[] group = Arrays.copyOfRange(bytes, start, end);

                    sendDataSynchronization(BleManager.getInstance(), mBleDevice, mSendCharact, group);

                    start += groupLen;
                }

                long endTime = System.currentTimeMillis(); // 获取结束时间戳
                long timeTaken = endTime - startTime; // 计算时间差（以毫秒为单位）
                double timeTakenInSeconds = timeTaken / 1000.0; // 将时间差转换为秒
                int rate = (int) (bytes.length / timeTakenInSeconds); // 计算速率（每秒字节数）并强制转换为整数

                Message message = new Message();
                message.what = MSG_UPDATE_AVERAGE_SEND_RATE;
                message.arg1 = rate;
                mHandler.sendMessage(message);

                Message message1 = new Message();
                message1.what = MSG_UPDATE_SEND_RATE;
                message1.arg1 = 0;
                mHandler.sendMessage(message1);
            }
        }).start();


//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//
//                BleManager.getInstance().write(mBleDevice, mSendCharact.getServiceID(), mSendCharact.getCharacteristicID(), bytes, new BleWriteCallback() {
//                    @Override
//                    public void onWriteSuccess(int i, int i1, byte[] bytes) {
////                        SendReceiveDataBean dataBean = new SendReceiveDataBean(SendReceiveDataBean.DataTypeOther, "发送成功");
////
////                        addDataInfoItem(dataBean);
//                    }
//
//                    @Override
//                    public void onWriteFailure(BleException e) {
//                        SendReceiveDataBean dataBean = new SendReceiveDataBean(SendReceiveDataBean.DataTypeFailed, getResources().getString(R.string.send_failure));
//
//                        addDataInfoItem(dataBean);
//                    }
//                });
//
//            }
//        }).start();

    }


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
//        BleManager.getInstance().getBleBluetooth(mBleDevice).removeWriteCallback(BleConfig.Ble_Config_Send_Service.getCharacteristicID());
//        BleManager.getInstance().getBleBluetooth(mBleDevice).removeWriteCallback(BleConfig.Ble_Data_Send_Service().getCharacteristicID());
//        BleManager.getInstance().disconnect(mBleDevice);
    }

    private void writeFile(String content, boolean bSend) {

        // 是否保存日志
        if (!HJBleApplication.shareInstance().isSaveLog()) {
            return;
        }

        if (bSend) {
            // 写文件
            FileInfoUtils.appendDataToDownloadFile(fileNameSend, content);
        }
        else {
            // 写文件
            FileInfoUtils.appendDataToDownloadFile(fileNameReceive, content);
        }
    }



    private String getSendFileName() {
        // 获取当前时间
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        String currentTime = dateFormat.format(new Date());
        return "hjble_send_" + this.mDeviceAddress.replace(":","") + "_" + currentTime + ".txt";
    }

    private String getReceiveFileName() {
        // 获取当前时间
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        String currentTime = dateFormat.format(new Date());
        return "hjble_receive_" + this.mDeviceAddress.replace(":","") + "_" + currentTime + ".txt";
    }


    // Adapter for holding devices found through scanning.
    private class SendReceiveDataAdapter extends RecyclerView.Adapter<DataViewHolder> {

        private static final int MAX_ITEMS = 800; // 定义最大数据条数 -1 为不限制

        private LinkedList<SendReceiveDataBean> mDataList;
        private LayoutInflater mInflator;


        public SendReceiveDataAdapter(Context context) {
            super();
            mDataList = new LinkedList<>();
            mInflator = LayoutInflater.from(context);
        }

        public void addDataItem(SendReceiveDataBean dataInfo) {

            // 倒序

            // 如果数据列表已满，先移除最旧的数据
            if (mDataList.size() >= MAX_ITEMS && MAX_ITEMS > 0) {
                mDataList.removeFirst();
            }

            mDataList.addLast(dataInfo);
        }

        public void clear() {
            mDataList.clear();
        }

        @Override
        public DataViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.send_receive_data_item, parent, false);
            return new DataViewHolder(view);
        }

        @Override
        public void onBindViewHolder(DataViewHolder viewHolder, int position) {
            SendReceiveDataBean dataInfo = mDataList.get(position);

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss SSS");
            String timeStr = sdf.format(dataInfo.getTimeStamp());
            viewHolder.dataTimeTv.setText(timeStr);
            switch (dataInfo.getDataType()) {
                case SendReceiveDataBean.DataTypeSend: {
                    viewHolder.dataTypeTv.setTextColor(Color.BLUE);
                    viewHolder.dataTypeTv.setText(getResources().getString(R.string.data_type_title_send));
                    viewHolder.dataInfoTv.setText(dataInfo.getDataInfo());
                }
                break;

                case SendReceiveDataBean.DataTypeReceive: {
                    viewHolder.dataTypeTv.setTextColor(Color.rgb(0xf5, 0x82, 0x20));
                    viewHolder.dataTypeTv.setText(getResources().getString(R.string.data_type_title_receive));
                    viewHolder.dataInfoTv.setText(dataInfo.getDataInfo());
                }
                break;

                case SendReceiveDataBean.DataTypeFailed: {
                    viewHolder.dataTypeTv.setTextColor(Color.RED);
                    viewHolder.dataTypeTv.setText(getResources().getString(R.string.data_type_title_failed));
                    viewHolder.dataInfoTv.setText(dataInfo.getDataInfo());
                }
                break;

                default: {
                    viewHolder.dataTypeTv.setTextColor(Color.BLACK);
                    viewHolder.dataTypeTv.setText(getResources().getString(R.string.data_type_title_other));
                    viewHolder.dataInfoTv.setText(dataInfo.getDataInfo());
                }
            }
        }

        @Override
        public int getItemCount() {
            return mDataList.size();
        }

    }

    static class DataViewHolder extends RecyclerView.ViewHolder {
        TextView dataTypeTv;
        TextView dataInfoTv;
        TextView dataTimeTv;

        public DataViewHolder(View itemView) {
            super(itemView);
            dataTypeTv = itemView.findViewById(R.id.tv_data_type);
            dataInfoTv = itemView.findViewById(R.id.tv_data_info);
            dataTimeTv = itemView.findViewById(R.id.tv_data_time);
        }
    }
}