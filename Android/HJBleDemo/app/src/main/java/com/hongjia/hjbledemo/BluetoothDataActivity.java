package com.hongjia.hjbledemo;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
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
import android.widget.AdapterView;
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

import com.hongjia.hjbledemo.bean.CustomCommand;
import com.wise.ble.ConvertData;
import com.wise.ble.WiseCharacteristic;
import com.wise.ble.WiseWaitEvent;
import com.wise.wisekit.activity.BaseActivity;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.text.SimpleDateFormat;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
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
    private BleDeviceSession deviceSession;
    private boolean isConfig;
    private Context context;
    private EditText sendEdit;
    private TextView testBtn;
    private Button sendBt;
    private RecyclerView dataListView;
    private SendReceiveDataAdapter mDataAdapter;

    private Handler mHandler;
    private volatile boolean isTesting = false; // 是否正在测试
    private Future<?> transferFuture;
    private Future<?> manualSendFuture;
    private final AtomicBoolean manualSendInProgress = new AtomicBoolean(false);
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

    // 自定义指令相关
    private TextView btnCustomCommands;
    private CustomCommandOverlayManager overlayManager;

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
    // 是否为流控模式
    private boolean bFlowControl = false;

    private String fileNameSend = "";
    private String fileNameReceive = "";
    private volatile boolean logFailureReported;
    private final StringBuilder configNotificationBuffer = new StringBuilder();


    @Override
    protected int getPageLayoutId() {
        return R.layout.activity_bluetooth_data;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        final Intent intent = getIntent();
        mBleDevice = intent.getParcelableExtra(EXTRAS_DEVICE);
        if (mBleDevice != null) {
            mDeviceName = mBleDevice.getName();
            mDeviceAddress = mBleDevice.getMac().toUpperCase(Locale.ROOT);
            deviceSession = BleDeviceSession.get(mBleDevice);
        } else {
            mDeviceName = "";
            mDeviceAddress = "";
        }
        isConfig = intent.getBooleanExtra(EXTRAS_DEVICE_IS_CONFIG, false);
        bFlowControl = intent.getBooleanExtra(EXTRAS_DEVICE_IS_FLOW_CONTROL, false);

        super.onCreate(savedInstanceState);

        if (mBleDevice == null) return;

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

        if (mBleDevice != null) {
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
                    BleDeviceSession.remove(bleDevice);
                    FastBleListener.getInstance().removeDevice(bleDevice);
                    SendReceiveDataBean dataBean = new SendReceiveDataBean(SendReceiveDataBean.DataTypeOther, getResources().getString(R.string.ble_disconnected));

                    addDataInfoItem(dataBean);
                    finish();
                }
            });
        }

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
        if (mBleDevice == null) {
            finish();
            return;
        }

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
        btnCustomCommands = findViewById(R.id.btn_custom_commands);

        // 初始化自定义指令蒙版管理器（右侧区域占屏幕70%宽度）
        overlayManager = new CustomCommandOverlayManager(this, 0.85f);
        overlayManager.setOnCommandExecuteListener(new CustomCommandOverlayManager.OnCommandExecuteListener() {
            @Override
            public void onCommandExecute(CustomCommand command) {
                executeCustomCommand(command);
            }
        });

        sendBt = findViewById(R.id.send);
        sendEdit = findViewById(R.id.sendData);
        bottomLayout = findViewById(R.id.bottom_layout);

        testBtn = findViewById(R.id.test_btn);
        setIsTesting(false);

        mDataAdapter = new SendReceiveDataAdapter(this, new OnItemDataClickListener() {
            @Override
            public void onItemClick(int position) {
                SendReceiveDataBean data = mDataAdapter.getItem(position);
                // 获取剪贴板管理器
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                // 设置要复制的文本
                String textToCopy = data.getDataInfo();
                // 将文本复制到剪贴板
                clipboard.setText(textToCopy);
                Toast.makeText(BluetoothDataActivity.this, "复制成功", Toast.LENGTH_SHORT).show();
            }
        });
        dataListView.setLayoutManager(new LinearLayoutManager(this));
        dataListView.addItemDecoration(new DividerItemDecoration(this, LinearLayoutManager.VERTICAL));
        dataListView.setAdapter(mDataAdapter);



        testBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isTesting) {
                    cancelTest();
                } else {
                    startTest();
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
                byte[] tmpBytes = ConvertData.utf8ToBytes(sendString);
                sendBleData(tmpBytes, sendString);
            }
        });

        // 自定义指令按钮点击事件
        btnCustomCommands.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                overlayManager.showOverlay();
            }
        });

    }

    /**
     * 执行自定义指令
     */
    private void executeCustomCommand(CustomCommand command) {
        if (isTesting) {
            Toast.makeText(this, "正在测试中，无法执行指令", Toast.LENGTH_SHORT).show();
            return;
        }

        String commandContent = command.getCommand();
        
        // 根据指令类型处理数据
        if (command.getType() == CustomCommand.TYPE_HEX) {
            // 十六进制指令
            preSendData(commandContent, true);
        } else {
            // ASCII指令
            preSendData(commandContent, false);
        }
    }

    // 添加数据
    void addDataInfoItem(final SendReceiveDataBean dataBean) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (isFinishing() || isDestroyed()) return;
                mDataAdapter.addDataItem(dataBean);
                mDataAdapter.notifyDataSetChanged();
                // 滚动到最下面
                int lastPosition = mDataAdapter.getItemCount() - 1;
                dataListView.smoothScrollToPosition(lastPosition);
            }
        });

    }

    private boolean sendDataSynchronization(BleManager bleManager, BleDevice bleDevice, WiseCharacteristic characteristic, byte[] data) {
        WiseWaitEvent sendEvent = new WiseWaitEvent();
        sendEvent.init();
        long startTime = System.currentTimeMillis(); // 获取开始时间戳
        bleManager.write(bleDevice, characteristic.getServiceID(), characteristic.getCharacteristicID(), data, false, new BleWriteCallback() {
            @Override
            public void onWriteSuccess(int current, int total, byte[] justWrite) {
                sendEvent.setSignal(WiseWaitEvent.SUCCESS);
            }

            @Override
            public void onWriteFailure(BleException e) {
                sendEvent.setSignal(WiseWaitEvent.ERROR_FAILED);
                addDataInfoItem(new SendReceiveDataBean(SendReceiveDataBean.DataTypeFailed,
                        getResources().getString(R.string.send_failure)));
            }
        });

        int result = sendEvent.waitSignal(5000);
//        bleManager.getBleBluetooth(bleDevice).removeWriteCallback(characteristic.getCharacteristicID());
        if(WiseWaitEvent.SUCCESS != result) {
            return false;
        }

        long endTime = System.currentTimeMillis(); // 获取结束时间戳
        long timeTaken = endTime - startTime; // 计算时间差（以毫秒为单位）
        double timeTakenInSeconds = Math.max(timeTaken / 1000.0, 0.001); // 将时间差转换为秒
        int rate = (int) (data.length / timeTakenInSeconds);  // 计算速率（每秒字节数）

        Message message = new Message();
        message.what = MSG_UPDATE_SEND_RATE;
        message.arg1 = rate;
        mHandler.sendMessage(message);

        return true;
    }

    void startTest() {
        if (deviceSession.isBusy()) {
            Toast.makeText(this, R.string.ble_busy, Toast.LENGTH_SHORT).show();
            return;
        }
        setIsTesting(true);
        bottomLayout.setVisibility(View.GONE);

        transferFuture = deviceSession.submit(new Runnable() {
            @Override
            public void run() {

                final boolean isBleConfig = deviceSession.isConfigMode();
                // 是否使用文件测试
                final boolean bUseFileTest = HJBleApplication.shareInstance().useFileTest();
                // 文件路径
//                final String filePath = HJBleApplication.shareInstance().testFilePath();

                Message message2 = new Message();
                message2.what = MSG_UPDATE_AVERAGE_SEND_RATE;
                message2.arg1 = 0;
                mHandler.sendMessage(message2);

                if (bUseFileTest) {
                    int gapTime = HJBleApplication.shareInstance().testFileIntervalPerPacket();
                    int fileGroupLen = HJBleApplication.shareInstance().testFilePerGroupLen();
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

                        try (BufferedInputStream inputStream = openTestFile(uri)) {

                        long startTime = System.currentTimeMillis(); // 获取开始时间戳

                        int groupDataLen = deviceSession.getPacketLength();
                        byte[] fileBuffer = new byte[fileGroupLen];
                        int fileReadCount;
                        long fileTotalLen = 0;
                        while ((fileReadCount = inputStream.read(fileBuffer, 0, fileGroupLen)) != -1 && isTesting) {
                            int offset = 0;
                            while (offset < fileReadCount && isTesting) {
                                if (deviceSession.isBusy()) {
                                    stopTest();
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast.makeText(BluetoothDataActivity.this, getResources().getString(R.string.ble_busy), Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                    return;
                                }

                                int sendLen = Math.min(groupDataLen, fileReadCount - offset);
                                byte[] sendBytes = new byte[sendLen];
                                System.arraycopy(fileBuffer, offset, sendBytes, 0, sendLen);

                                if (!sendDataSynchronization(BleManager.getInstance(), mBleDevice, mSendCharact, sendBytes)) {
                                    boolean cancelled = !isTesting;
                                    stopTest();
                                    if (cancelled) return;
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast.makeText(BluetoothDataActivity.this, getResources().getString(R.string.send_failure), Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                    return;
                                }

                                fileTotalLen += sendLen;
                                final int sendLenFinal = sendLen;
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (!isBleConfig) {
                                            addSendByteCount(sendLenFinal);
                                        }
                                    }
                                });

                                offset += sendLen;
                                if (gapTime > 0) SystemClock.sleep(gapTime);
                            }

                        }

                        stopTest();

                        long endTime = System.currentTimeMillis(); // 获取结束时间戳
                        long timeTaken = endTime - startTime; // 计算时间差（以毫秒为单位）
                        double timeTakenInSeconds = Math.max(timeTaken / 1000.0, 0.001); // 将时间差转换为秒
                        int rate = (int) (fileTotalLen / timeTakenInSeconds); // 计算速率（每秒字节数）
                        Message message = new Message();
                        message.what = MSG_UPDATE_AVERAGE_SEND_RATE;
                        message.arg1 = rate;
                        mHandler.sendMessage(message);

                        Message message1 = new Message();
                        message1.what = MSG_UPDATE_SEND_RATE;
                        message1.arg1 = 0;
                        mHandler.sendMessage(message1);
                        }
                    } catch (IOException | SecurityException e) {
                        stopTest();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(BluetoothDataActivity.this, getResources().getString(R.string.test_file_not_exist), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                } else {
                    long gapTime = HJBleApplication.shareInstance().testGapTime();
                    final int totalDataLen = HJBleApplication.shareInstance().testDataLen();
                    int groupDataLen = deviceSession.getPacketLength();
                    int sentBytes = 0;

                    long startTime = System.currentTimeMillis(); // 获取开始时间戳

                    // 循环创建字节数组片段
                    for (int i = 0; i < totalDataLen && isTesting; i += groupDataLen) {
                        int len = Math.min(groupDataLen, totalDataLen - i); // 确保最后一个片段不会超出总长度
                        byte[] groupData = new byte[len]; // 创建一个新的字节数组片段
                        // 这里可以填充数据，例如设置为0
                        Arrays.fill(groupData, (byte) 0);


                        if (deviceSession.isBusy()) {
                            SendReceiveDataBean dataBean = new SendReceiveDataBean(SendReceiveDataBean.DataTypeOther, getResources().getString(R.string.ble_busy));
                            addDataInfoItem(dataBean);
                            break;
                        }

                        if (!sendDataSynchronization(BleManager.getInstance(), mBleDevice, mSendCharact, groupData)) {
                            break;
                        }

                        sentBytes += len;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (!isBleConfig) {
                                    addSendByteCount(len);
                                }
                            }
                        });

                        if (gapTime > 0) {
                            SystemClock.sleep(gapTime);
                        }
                    }

                    stopTest();
                    long endTime = System.currentTimeMillis(); // 获取结束时间戳
                    long timeTaken = endTime - startTime; // 计算时间差（以毫秒为单位）
                    double timeTakenInSeconds = Math.max(timeTaken / 1000.0, 0.001); // 将时间差转换为秒
                    int rate = (int) (sentBytes / timeTakenInSeconds); // 计算速率（每秒字节数）
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
        });
    }

    // 停止测试
    void stopTest() {
        setIsTesting(false);
        transferFuture = null;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                bottomLayout.setVisibility(View.VISIBLE);
            }
        });
    }

    private void cancelTest() {
        setIsTesting(false);
        Future<?> task = transferFuture;
        if (task != null) task.cancel(true);
        transferFuture = null;
        if (bottomLayout != null) runOnUiThread(() -> bottomLayout.setVisibility(View.VISIBLE));
    }

    private BufferedInputStream openTestFile(Uri uri) throws IOException {
        InputStream stream = getContentResolver().openInputStream(uri);
        if (stream == null) throw new IOException("Unable to open selected file");
        return new BufferedInputStream(stream);
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

        if (deviceSession.isConfigMode()) {
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

        if (mBleDevice != null) {
            writeTypeBle(mSendCharact, HJBleApplication.shareInstance().isWriteTypeResponse());
        }

        FastBleListener.getInstance().setNotifyBleCallback(mBleDevice, BleConfig.Ble_Config_Receive_Service, new BleNotifyCallback() {
            @Override
            public void onNotifySuccess() {

            }

            @Override
            public void onNotifyFailure(BleException e) {

            }

            @Override
            public void onCharacteristicChanged(byte[] bytes) {
                processConfigNotification(bytes);
                if (mReceiveCharact.getCharacteristicID().equals(BleConfig.Ble_Config_Receive_Service.getCharacteristicID())) {
                    handleReceivedData(bytes);
                }
            }
        });

        if (!mReceiveCharact.getCharacteristicID().equals(BleConfig.Ble_Config_Receive_Service.getCharacteristicID())) {
            FastBleListener.getInstance().setNotifyBleCallback(mBleDevice, mReceiveCharact, new BleNotifyCallback() {
            @Override
            public void onNotifySuccess() {

            }

            @Override
            public void onNotifyFailure(BleException e) {

            }

            @Override
            public void onCharacteristicChanged(byte[] bytes) {
                handleReceivedData(bytes);
            }
        });
        }
    }

    private void handleReceivedData(byte[] bytes) {
        if (!deviceSession.isConfigMode()) {
            final int len = bytes.length;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    addReceiveByteCount(len);
                    recCountBySecond += len;
                }
            });
        }

        String str = HJBleApplication.shareInstance().isBleHex()
                ? ConvertData.bytesToHexString(bytes, false)
                : ConvertData.bytesToUtf8(bytes);
        writeFile(str, false);
        addDataInfoItem(new SendReceiveDataBean(SendReceiveDataBean.DataTypeReceive, str));
    }

    private void processConfigNotification(byte[] bytes) {
        synchronized (configNotificationBuffer) {
            configNotificationBuffer.append(ConvertData.bytesToUtf8(bytes));
            while (true) {
                int start = configNotificationBuffer.indexOf("<");
                int end = configNotificationBuffer.indexOf(">", Math.max(start, 0));
                if (start < 0 || end < 0) {
                    if (start < 0) configNotificationBuffer.setLength(0);
                    return;
                }
                String command = configNotificationBuffer.substring(start, end + 1);
                configNotificationBuffer.delete(0, end + 1);
                if ("<HJ_BLE_BUSY_STOP_SEND>".equals(command)) {
                    deviceSession.setBusy(true);
                    cancelTest();
                    addDataInfoItem(new SendReceiveDataBean(SendReceiveDataBean.DataTypeOther,
                            getString(R.string.ble_busy)));
                } else if ("<HJ_BLE_IDLE_START_SEND>".equals(command)) {
                    deviceSession.setBusy(false);
                    addDataInfoItem(new SendReceiveDataBean(SendReceiveDataBean.DataTypeOther,
                            getString(R.string.ble_idle)));
                }
            }
        }
    }

    public void writeTypeBle(WiseCharacteristic chara,  boolean bRespone){
        BluetoothGattService service= BleManager.getInstance().getBluetoothGatt(mBleDevice).getService(UUID.fromString(chara.getServiceID()));
        if (service == null) {
            addDataInfoItem(new SendReceiveDataBean(SendReceiveDataBean.DataTypeOther,
                    chara.getServiceID() + "  " + chara.getCharacteristicID() + "该服务不存在"));
            return;
        }
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
        cancelTest();
        cancelManualSend();
    }

    void setIsTesting(boolean testing) {
        isTesting = testing;
        if (testBtn == null) return;
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
        if (sendBt != null) sendBt.setEnabled(enable);
        if (readVersionBtn != null) readVersionBtn.setEnabled(enable);
        if (btnCustomCommands != null) btnCustomCommands.setEnabled(enable);
    }

    // 准备发送数据
    void preSendData(String sendString) {

        preSendData(sendString, HJBleApplication.shareInstance().isBleHex());
    }

    // 准备发送数据
    void preSendData(String sendString, boolean isHex) {

        byte[] tmpBytes;
        if (isHex) {

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

        sendBleData(tmpBytes, sendString);
    }

    //发送数据
    void sendBleData(byte[] data) {
        String content = HJBleApplication.shareInstance().isBleHex()
                ? ConvertData.bytesToHexString(data, false)
                : ConvertData.bytesToUtf8(data);
        sendBleData(data, content);
    }

    void sendBleData(byte[] data, final String logContent) {
        if (isTesting) {
            Toast.makeText(this, R.string.testing_command_blocked, Toast.LENGTH_SHORT).show();
            return;
        }
        if (deviceSession.isBusy()) {
            Toast.makeText(this, R.string.ble_busy, Toast.LENGTH_SHORT).show();
            return;
        }
        if (!manualSendInProgress.compareAndSet(false, true)) {
            Toast.makeText(this, R.string.testing_command_blocked, Toast.LENGTH_SHORT).show();
            return;
        }

        final byte[] bytes = data;

        setButtonsState(false);
        try {
            manualSendFuture = deviceSession.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        Message message2 = new Message();
                        message2.what = MSG_UPDATE_AVERAGE_SEND_RATE;
                        message2.arg1 = 0;
                        mHandler.sendMessage(message2);

                        long startTime = System.currentTimeMillis(); // 获取开始时间戳

                        int groupLen = deviceSession.getPacketLength();

                        for (byte[] group : BlePacketUtils.split(bytes, groupLen)) {
                            if (!sendDataSynchronization(BleManager.getInstance(), mBleDevice, mSendCharact, group)) {
                                return;
                            }
                        }

                        writeFile(logContent + "\r\n", true);
                        SendReceiveDataBean dataBean = new SendReceiveDataBean(SendReceiveDataBean.DataTypeSend, logContent);
                        addDataInfoItem(dataBean);
                        if (!deviceSession.isConfigMode()) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    addSendByteCount(bytes.length);
                                }
                            });
                        }

                        long endTime = System.currentTimeMillis(); // 获取结束时间戳
                        long timeTaken = endTime - startTime; // 计算时间差（以毫秒为单位）
                        double timeTakenInSeconds = Math.max(timeTaken / 1000.0, 0.001); // 将时间差转换为秒
                        int rate = (int) (bytes.length / timeTakenInSeconds); // 计算速率（每秒字节数）并强制转换为整数

                        Message message = new Message();
                        message.what = MSG_UPDATE_AVERAGE_SEND_RATE;
                        message.arg1 = rate;
                        mHandler.sendMessage(message);

                        Message message1 = new Message();
                        message1.what = MSG_UPDATE_SEND_RATE;
                        message1.arg1 = 0;
                        mHandler.sendMessage(message1);
                    } finally {
                        manualSendInProgress.set(false);
                        manualSendFuture = null;
                        runOnUiThread(() -> {
                            if (!isFinishing() && !isDestroyed()) setButtonsState(true);
                        });
                    }
                }
            });
        } catch (RejectedExecutionException e) {
            manualSendInProgress.set(false);
            manualSendFuture = null;
            setButtonsState(true);
            addDataInfoItem(new SendReceiveDataBean(SendReceiveDataBean.DataTypeFailed,
                    getResources().getString(R.string.send_failure)));
        }


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
        cancelTest();
        cancelManualSend();
        if (mBleDevice != null) {
            FastBleListener listener = FastBleListener.getInstance();
            listener.removeNotifyBleCallback(mBleDevice, BleConfig.Ble_Config_Receive_Service);
            listener.removeNotifyBleCallback(mBleDevice, BleConfig.Ble_Data_Receive_Service());
        }

        // 销毁蒙版管理器
        if (overlayManager != null) {
            overlayManager.destroy();
        }
//        BleManager.getInstance().getBleBluetooth(mBleDevice).removeWriteCallback(BleConfig.Ble_Config_Send_Service.getCharacteristicID());
//        BleManager.getInstance().getBleBluetooth(mBleDevice).removeWriteCallback(BleConfig.Ble_Data_Send_Service().getCharacteristicID());
        super.onDestroy();
    }

    private void cancelManualSend() {
        manualSendInProgress.set(false);
        Future<?> task = manualSendFuture;
        if (task != null) task.cancel(true);
        manualSendFuture = null;
    }

    private void writeFile(String content, boolean bSend) {

        // 是否保存日志
        if (!HJBleApplication.shareInstance().isSaveLog()) {
            return;
        }

        boolean success = FileInfoUtils.appendDataToDownloadFile(this,
                bSend ? fileNameSend : fileNameReceive, content);
        if (!success && !logFailureReported) {
            logFailureReported = true;
            addDataInfoItem(new SendReceiveDataBean(SendReceiveDataBean.DataTypeFailed,
                    getString(R.string.log_save_failed)));
        }
    }



    private String getSendFileName() {
        // 获取当前时间
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss", Locale.US);
        String currentTime = dateFormat.format(new Date());
        return "hjble_send_" + this.mDeviceAddress.replace(":","") + "_" + currentTime + ".txt";
    }

    private String getReceiveFileName() {
        // 获取当前时间
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss", Locale.US);
        String currentTime = dateFormat.format(new Date());
        return "hjble_receive_" + this.mDeviceAddress.replace(":","") + "_" + currentTime + ".txt";
    }


    // Adapter for holding devices found through scanning.
    private class SendReceiveDataAdapter extends RecyclerView.Adapter<DataViewHolder> {

        private static final int MAX_ITEMS = 800; // 定义最大数据条数 -1 为不限制

        private LinkedList<SendReceiveDataBean> mDataList;
        private LayoutInflater mInflator;
        private OnItemDataClickListener listener;


        public SendReceiveDataAdapter(Context context, OnItemDataClickListener listener) {
            super();
            this.listener = listener;
            mDataList = new LinkedList<>();
            mInflator = LayoutInflater.from(context);
        }

        public SendReceiveDataBean getItem(int position) {
            return mDataList.get(position);
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

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss SSS", Locale.US);
            String timeStr = sdf.format(dataInfo.getTimeStamp());
            viewHolder.dataTimeTv.setText(timeStr);
            viewHolder.bind(position, listener);
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

    // 定义一个接口，用于处理 item 的点击事件
    interface OnItemDataClickListener {
        void onItemClick(int position);
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

        public void bind(final int position, final OnItemDataClickListener listener) {
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.onItemClick(position);
                }
            });
        }
    }
}
