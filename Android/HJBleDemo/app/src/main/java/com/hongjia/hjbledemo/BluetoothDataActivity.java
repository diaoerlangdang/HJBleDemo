package com.hongjia.hjbledemo;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.icu.text.SimpleDateFormat;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
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
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.wise.ble.ConvertData;
import com.wise.ble.WiseBluetoothLe;
import com.wise.ble.WiseCharacteristic;
import com.wise.wisekit.activity.BaseActivity;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Pattern;

public class BluetoothDataActivity extends BaseActivity {

    private final static String TAG = BluetoothDataActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    // 是否支持配置
    public static final String EXTRAS_DEVICE_IS_CONFIG = "DEVICE_IS_CONFIG";
    // 是否为流控信息
    public static final String EXTRAS_DEVICE_IS_FLOW_CONTROL = "DEVICE_IS_FLOW_CONTROL";

    private List<SendReceiveDataBean> mDataList = new ArrayList<>();
    private String mDeviceName;
    private String mDeviceAddress;
    private boolean isConfig;
    private WiseBluetoothLe mble;
    private Context context;
    private EditText sendEdit;
    private TextView testBtn;
    private Button sendBt;
    private ListView dataListView;
    private SendReceiveDataAdapter mDataAdapter;

    private Handler mHandler;
    private ScrollView scrollView;
    private boolean isTesting = false; // 是否正在测试
    private static final int MSG_DATA_CHANGE = 0x11;
    private LinearLayout bottomLayout; //底部

    // 更新速率
    private static final int MSG_UPDATE_RATE = 0x12;

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
    // 流控
    private TextView tvFlowControl;

    private int recCountBySecond = 0;

    private int sendCountBySecond = 0;

    // 发送服务
    private WiseCharacteristic mSendCharact = BleConfig.Ble_Data_Send_Service;
    // 接收服务
    private WiseCharacteristic mReceiveCharact = BleConfig.Ble_Data_Receive_Service;

    private Timer timer;

    private TimerTask timerTask;

    // 蓝牙是否繁忙
    private boolean bBusyBle = false;

    // 是否为流控模式
    private boolean bFlowControl = false;


    @Override
    protected int getPageLayoutId() {
        return R.layout.activity_bluetooth_data;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
        isConfig = intent.getBooleanExtra(EXTRAS_DEVICE_IS_CONFIG, false);
        bFlowControl = intent.getBooleanExtra(EXTRAS_DEVICE_IS_FLOW_CONTROL, false);

        super.onCreate(savedInstanceState);

        topLeftBtn.setVisibility(View.VISIBLE);

        setRightText("设置");
        rightTitleTxt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Intent intent = new Intent(BluetoothDataActivity.this, SetActivity.class);

                intent.putExtra(SetActivity.EXTRAS_SET_IS_CONFIG, isConfig);

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
                message.what = MSG_UPDATE_RATE;
                message.arg1 = recCountBySecond;
                message.arg2 = sendCountBySecond;
                recCountBySecond = 0;
                sendCountBySecond = 0;
                mHandler.sendMessage(message);
            }
        };

        mble = WiseBluetoothLe.getInstance(getApplicationContext());    //获取蓝牙实例
        mble.mBleCallBack = bleCallBack;

        mHandler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(@NonNull Message msg) {
                switch (msg.what) {
                    // 更新速率
                    case MSG_UPDATE_RATE:
                        int recvRate = msg.arg1;
                        int sendRate = msg.arg2;

                        showRecCountBySecond(recvRate);
                        showSendCountBySecond(sendRate);
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
        receiveByteCountTV = findViewById(R.id.receive_byte_count_tv);
        receiveRateTV = findViewById(R.id.receive_rate_tv);
        tvFlowControl = findViewById(R.id.tv_flow_control);
        tvFlowControl.setText(bFlowControl ? "串口流控：使能" : "串口流控：禁止");

        scrollView = findViewById(R.id.scroll);
        sendBt = findViewById(R.id.send);
        sendEdit = findViewById(R.id.sendData);
        bottomLayout = findViewById(R.id.bottom_layout);

        testBtn = findViewById(R.id.test_btn);
        setIsTesting(false);

        mDataAdapter = new SendReceiveDataAdapter(this);
        dataListView.setAdapter(mDataAdapter);

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
                mDataAdapter.clear();
                mDataAdapter.notifyDataSetChanged();

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

    // 添加数据
    void addDataInfoItem(final SendReceiveDataBean dataBean) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mDataAdapter.addDataItem(dataBean);
                mDataAdapter.notifyDataSetChanged();
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
                // 是否使用文件测试
                final boolean bUseFileTest = HJBleApplication.shareInstance().useFileTest();
                // 文件路径
                final String filePath = HJBleApplication.shareInstance().testFilePath();

                if (bUseFileTest) {
                    FileInputStream fileInputStream = null;
                    BufferedInputStream inputStream = null;
                    try {
                        File file = new File(filePath);// 成文件路径中获取文件
                        if (!file.exists()) {
                            setIsTesting(false);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(BluetoothDataActivity.this, "测试文件不存在", Toast.LENGTH_SHORT).show();
                                }
                            });
                            return;
                        }

                        fileInputStream = new FileInputStream(file);
                        inputStream = new BufferedInputStream(fileInputStream);

                        byte[] inputBuffer = new byte[dataLen];
                        int readCount;
                        while ((readCount = inputStream.read(inputBuffer, 0, dataLen)) != -1) {
                            if (bBusyBle) {
                                setIsTesting(false);
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(BluetoothDataActivity.this, "蓝牙繁忙", Toast.LENGTH_SHORT).show();
                                    }
                                });
                                fileInputStream.close();
                                inputStream.close();
                                return;
                            }

                            byte[] sendBytes = new byte[readCount];
                            System.arraycopy(inputBuffer, 0, sendBytes, 0, readCount);

                            final int sendLen = readCount;
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (!isBleConfig) {
                                        addSendByteCount(sendLen);
                                        sendCountBySecond += sendLen;
                                    }
                                }
                            });

                            if (!mble.sendData(mSendCharact, sendBytes)) {
                                SendReceiveDataBean dataBean = new SendReceiveDataBean(SendReceiveDataBean.DataTypeFailed, "发送失败！");

                                addDataInfoItem(dataBean);

                                setIsTesting(false);
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(BluetoothDataActivity.this, "蓝牙繁忙", Toast.LENGTH_SHORT).show();
                                    }
                                });
                                fileInputStream.close();
                                inputStream.close();
                                return;
                            }

                            for (int i = 0; i < gapTime / 10 && isTesting; i++) {
                                try {
                                    Thread.sleep(10);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        }

                        fileInputStream.close();
                        inputStream.close();
                        setIsTesting(false);

                    } catch (IOException e) {
                        setIsTesting(false);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(BluetoothDataActivity.this, "测试文件不存在", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                } else {
                    final byte[] data = new byte[dataLen];

                    while (isTesting) {

                        // 蓝牙设备不繁忙时，可发送测试数据
                        if (!bBusyBle) {
                            final String info = ConvertData.bytesToHexString(data, false);
//                    SendReceiveDataBean sendDataBean = new SendReceiveDataBean(SendReceiveDataBean.DataTypeSend, info);
//
//                    addDataInfoItem(sendDataBean);

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (!isBleConfig) {
                                        addSendByteCount(dataLen);
                                        sendCountBySecond += dataLen;
                                    }
                                }
                            });

                            if (!mble.sendData(mSendCharact, data)) {
                                SendReceiveDataBean dataBean = new SendReceiveDataBean(SendReceiveDataBean.DataTypeFailed, "发送失败！");

                                addDataInfoItem(dataBean);
                            }
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

    // 显示接收速率
    private void showRecCountBySecond(int count) {
        receiveRateTV.setText(String.format("接收速率：%d B/s", count));
    }

    // 显示发送速率
    private void showSendCountBySecond(int count) {
        sendRateTV.setText(String.format("发送速率：%d B/s", count));
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
                    message.arg2 = sendCountBySecond;
                    recCountBySecond = 0;
                    sendCountBySecond = 0;
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

            dataBytesLayout.setVisibility(View.GONE);

            stopTimer();

            setIsTesting(false);
        } else {
            // 发送服务
            mSendCharact = BleConfig.Ble_Data_Send_Service;
            // 接收服务
            mReceiveCharact = BleConfig.Ble_Data_Receive_Service;

            setTitle(mDeviceName + "-数据");

            dataBytesLayout.setVisibility(View.VISIBLE);

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
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (isTesting) {
                    testBtn.setText("停止测试");
                } else {
                    testBtn.setText("开始测试");
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
                Toast.makeText(BluetoothDataActivity.this, "Hex长度应为2的倍数", Toast.LENGTH_SHORT).show();
                return;
            }

            String pattern = "^[A-Fa-f0-9]+$";

            if (!Pattern.matches(pattern, sendString)) {
                Toast.makeText(BluetoothDataActivity.this, "输入格式错误", Toast.LENGTH_SHORT).show();
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

                if (!mble.sendData(mSendCharact, bytes)) {
                    SendReceiveDataBean dataBean = new SendReceiveDataBean(SendReceiveDataBean.DataTypeFailed, "发送失败！");

                    addDataInfoItem(dataBean);
                }
            }
        }).start();

    }

    WiseBluetoothLe.OnWiseBluetoothCallBack bleCallBack = new WiseBluetoothLe.OnWiseBluetoothCallBack() {
        @Override
        public void OnWiseBluetoothState(int state) {

            if (state == WiseBluetoothLe.WISE_BLE_DISCONNECTED) {
                SendReceiveDataBean dataBean = new SendReceiveDataBean(SendReceiveDataBean.DataTypeOther, "蓝牙已断开");

                addDataInfoItem(dataBean);
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
                SendReceiveDataBean dataBean = new SendReceiveDataBean(SendReceiveDataBean.DataTypeReceive, str);

                addDataInfoItem(dataBean);
            } else if (characteristic.equals(BleConfig.Ble_Config_Receive_Service)) {
                String recvStr = ConvertData.bytesToUtf8(recvData);
                // 蓝牙设备繁忙
                if (recvStr.equals("<HJ_BLE_BUSY_STOP_SEND>")) {
                    SendReceiveDataBean dataBean = new SendReceiveDataBean(SendReceiveDataBean.DataTypeOther, "蓝牙设备繁忙");

                    addDataInfoItem(dataBean);
                    bBusyBle = true;
                } else if (recvStr.equals("<HJ_BLE_IDLE_START_SEND>")) {
                    SendReceiveDataBean dataBean = new SendReceiveDataBean(SendReceiveDataBean.DataTypeOther, "蓝牙设备空闲");

                    addDataInfoItem(dataBean);
                    bBusyBle = false;
                }
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


    // Adapter for holding devices found through scanning.
    private class SendReceiveDataAdapter extends BaseAdapter {
        private ArrayList<SendReceiveDataBean> mDataList;
        private LayoutInflater mInflator;

        public SendReceiveDataAdapter(Context context) {
            super();
            mDataList = new ArrayList<>();
            mInflator = LayoutInflater.from(context);
        }

        public void addDataItem(SendReceiveDataBean dataInfo) {
            mDataList.add(dataInfo);

        }

        public void clear() {
            mDataList.clear();
        }

        @Override
        public int getCount() {
            return mDataList.size();
        }

        @Override
        public SendReceiveDataBean getItem(int i) {
            return mDataList.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            DataViewHolder viewHolder;
            // General ListView optimization code.
            if (view == null) {
                view = mInflator.inflate(R.layout.send_receive_data_item, null);
                viewHolder = new DataViewHolder();
                viewHolder.dataTypeTv = view.findViewById(R.id.tv_data_type);
                viewHolder.dataInfoTv = (TextView) view.findViewById(R.id.tv_data_info);
                viewHolder.dataTimeTv = view.findViewById(R.id.tv_data_time);
                view.setTag(viewHolder);
            } else {
                viewHolder = (DataViewHolder) view.getTag();
            }

            SendReceiveDataBean dataInfo = mDataList.get(i);

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss SSS");
            String timeStr = sdf.format(dataInfo.getTimeStamp());
            viewHolder.dataTimeTv.setText(timeStr);

            switch (dataInfo.getDataType()) {
                case SendReceiveDataBean.DataTypeSend: {
                    viewHolder.dataTypeTv.setTextColor(Color.BLUE);
                    viewHolder.dataTypeTv.setText("发送：");
                    viewHolder.dataInfoTv.setText(dataInfo.getDataInfo());
                }
                break;

                case SendReceiveDataBean.DataTypeReceive: {
                    viewHolder.dataTypeTv.setTextColor(Color.rgb(0xf5, 0x82, 0x20));
                    viewHolder.dataTypeTv.setText("接收：");
                    viewHolder.dataInfoTv.setText(dataInfo.getDataInfo());
                }
                break;

                case SendReceiveDataBean.DataTypeFailed: {
                    viewHolder.dataTypeTv.setTextColor(Color.RED);
                    viewHolder.dataTypeTv.setText("失败：");
                    viewHolder.dataInfoTv.setText(dataInfo.getDataInfo());
                }
                break;

                default: {
                    viewHolder.dataTypeTv.setTextColor(Color.BLACK);
                    viewHolder.dataTypeTv.setText("其他：" + timeStr);
                    viewHolder.dataInfoTv.setText(dataInfo.getDataInfo());
                }
            }

            return view;
        }
    }

    static class DataViewHolder {
        TextView dataTypeTv;
        TextView dataInfoTv;
        TextView dataTimeTv;

    }
}