package com.hongjia.hjbledemo;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import java.text.SimpleDateFormat;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.core.content.ContextCompat;

import com.clj.fastble.BleManager;
import com.clj.fastble.callback.BleGattCallback;
import com.clj.fastble.callback.BleMtuChangedCallback;
import com.clj.fastble.callback.BleNotifyCallback;
import com.clj.fastble.callback.BleWriteCallback;
import com.clj.fastble.data.BleDevice;
import com.clj.fastble.exception.BleException;
import com.hongjia.hjbledemo.adapter.LeDeviceListAdapter;
import com.hongjia.hjbledemo.bean.HJBleScanDevice;
import com.wise.ble.ConvertData;
import com.wise.ble.WiseCharacteristic;
import com.wise.ble.WiseWaitEvent;
import com.wise.ble.scan.BleParamsOptions;
import com.wise.ble.scan.BluetoothScanManager;
import com.wise.ble.scan.ScanOverListener;
import com.wise.ble.scan.bluetoothcompat.ScanCallbackCompat;
import com.wise.ble.scan.bluetoothcompat.ScanResultCompat;
import com.wise.wisekit.activity.BaseActivity;
import com.wise.wisekit.dialog.LoadingDialog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;
import qiu.niorgai.StatusBarCompat;

public class ScanBleActivity extends BaseActivity implements EasyPermissions.PermissionCallbacks,
        EasyPermissions.RationaleCallbacks, LeDeviceListAdapter.OnDeviceListItemClickListener {

    // 扫描是否过滤
    private boolean bScanFilter = HJBleApplication.shareInstance().isScanFilter();

    private static final int REQUEST_ENABLE_BT = 1;
    private static final int RC_PERM_CODE = 124;

    private String[] permissionList = {Manifest.permission.ACCESS_FINE_LOCATION};

    // android 12 以上版本
    @SuppressLint("InlinedApi")
    private String[] permissionListHigher = {Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.ACCESS_FINE_LOCATION};

    private boolean mScanning=true;

    private RecyclerView mRecyclerView;
    private LinearLayoutManager mLayoutManager;

    private LeDeviceListAdapter mLeDeviceListAdapter;

    private LoadingDialog loadingDialog = null;

    private ProgressBar scanProgress;

    private BluetoothScanManager scanManager;

    private Timer timer = new Timer();
    private TimerTask timerTask;

    static final class ConnectionWork {
        final AtomicBoolean cancelled = new AtomicBoolean();
        final BleDevice device;
        final BleDeviceSession session;
        final WiseWaitEvent recvEvent = new WiseWaitEvent();
        final StringBuilder response = new StringBuilder();
        volatile boolean waitingForFlow;
        volatile byte[] flowResponse;
        volatile Thread worker;
        ConnectionWork(BleDevice device) {
            this.device = device;
            session = BleDeviceSession.begin(device);
        }
    }

    private final ExecutorService connectionExecutor = Executors.newSingleThreadExecutor();
    private final AtomicBoolean connectionRunning = new AtomicBoolean(false);
    private volatile ConnectionWork connectionCancellation;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //判断本设备是否支持蓝牙ble，并连接本地蓝牙设备
        if(!BleManager.getInstance().isSupportBle())
        {
            Toast.makeText(this, getResources().getString(R.string.ble_not_supported),Toast.LENGTH_SHORT).show();
            finish();
            return ;
        }

        // 使用LinearLayoutManager来设置布局，也可以使用GridLayoutManager或StaggeredGridLayoutManager
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        mLeDeviceListAdapter = new LeDeviceListAdapter(this);
        mLeDeviceListAdapter.setListener(this);
        mRecyclerView.setAdapter(mLeDeviceListAdapter);

        // 添加分隔线
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(this, mLayoutManager.getOrientation());
        mRecyclerView.addItemDecoration(dividerItemDecoration);
    }

    @Override
    protected int getPageLayoutId() {
        return R.layout.activity_scan_ble;
    }

    /**
     * 获取版本名称
     *
     * @param context 上下文
     *
     * @return 版本名称
     */
    public String getVersionName(Context context) {

        //获取包管理器
        PackageManager pm = context.getPackageManager();
        //获取包信息
        try {
            PackageInfo packageInfo = pm.getPackageInfo(context.getPackageName(), 0);
            //返回版本号
            return packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        return null;

    }


    @Override
    protected void initView() {
        super.initView();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            StatusBarCompat.setStatusBarColor(this, getResources().getColor(com.wise.wisekit.R.color.colorNavBackground, null));
        }

        setTitle(getResources().getString(R.string.scan_list_title));
        topLeftBtn.setImageResource(R.mipmap.info);
        topLeftBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                final Intent intent = new Intent(ScanBleActivity.this, AppConfigSetActivity.class);
                ScanBleActivity.this.startActivity(intent);

//                AlertDialog.Builder ab=new AlertDialog.Builder(ScanBleActivity.this);  //(普通消息框)
//
//                ab.setTitle("版本信息");  //设置标题
//                ab.setMessage("V " + getVersionName(ScanBleActivity.this));//设置消息内容
//                ab.setNegativeButton("确定",null);//设置取消按钮
//                ab.show();//显示弹出框
            }
        });

        setRightText(getResources().getString(R.string.stop_scan));
        rightTitleTxt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (mScanning) {
                    scanLeDevice(false);
                }
                else {
                    mLeDeviceListAdapter.clear();
                    mLeDeviceListAdapter.notifyDataSetChanged();
                    scanLeDevice(true);
                }
            }
        });

        mRecyclerView = findViewById(R.id.recycler_view);
        scanProgress = findViewById(R.id.progressBar1);

        LoadingDialog.Builder loadBuilder=new LoadingDialog.Builder(this)
                .setCancelable(true)
                .setShowMessage(true)
                .setMessage(getResources().getString(R.string.ble_connecting))
                .setCancelOutside(false)
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialogInterface) {
                        cancelConnection();
                    }
                });
        loadingDialog = loadBuilder.create();

//        mRecyclerView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//            @Override
//            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//
//                loadingDialog.show();
//
//                HJBleScanDevice scanDevice = mLeDeviceListAdapter.getScanDeviceInfo(position);
//                selectBleDevice = scanDevice.device;
//
//                scanLeDevice(false);
//
//                connectBle(scanDevice.device, scanDevice.sendDataLenMax, scanDevice.isConfig);
//            }
//        });

        scanManager = BluetoothScanManager.getInstance(getApplicationContext());
        scanManager.setScanOverListener(new ScanOverListener() {
            @Override
            public void onScanOver() {
                if (scanManager.isPauseScanning()){
                }
            }
        });
        scanManager.setScanCallbackCompat(scanCallback);

        BleParamsOptions.Builder builder = new BleParamsOptions.Builder()
                .setBackgroundScanPeriod(10000)
                .setForegroundBetweenScanPeriod(300);

        BluetoothScanManager.setBleParamsOptions(builder.build());

    }

    @Override
    public void onConnectClick(int position) {
        HJBleScanDevice scanDevice = mLeDeviceListAdapter.getScanDeviceInfo(position);
        if (BleManager.getInstance().getBleBluetooth(scanDevice.device) != null) {
            BleDeviceSession session = BleDeviceSession.find(scanDevice.device);
            if (session != null) session.disconnect(scanDevice.device);
            else BleManager.getInstance().disconnect(scanDevice.device);
        } else {
            if (connectionRunning.get()) return;

            loadingDialog.show();

            scanLeDevice(false);

            connectBle(scanDevice);
        }

        mLeDeviceListAdapter.notifyDataSetChanged();
    }

    @Override
    public void onDetailClick(int position) {
        HJBleScanDevice scanDevice = mLeDeviceListAdapter.getScanDeviceInfo(position);
        if (!BleDeviceSession.isReady(scanDevice.device)) return;

        final Intent intent = new Intent(ScanBleActivity.this, BluetoothDataActivity.class);
        intent.putExtra(BluetoothDataActivity.EXTRAS_DEVICE_IS_CONFIG, scanDevice.isConfig);
        intent.putExtra(BluetoothDataActivity.EXTRAS_DEVICE_IS_FLOW_CONTROL, scanDevice.bFlowControl);
        intent.putExtra(BluetoothDataActivity.EXTRAS_DEVICE_GROUP_LEN_MAX, scanDevice.mtuLen);

        intent.putExtra(BluetoothDataActivity.EXTRAS_DEVICE, scanDevice.device);

        ScanBleActivity.this.startActivity(intent);
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        bScanFilter = HJBleApplication.shareInstance().isScanFilter();

        mLeDeviceListAdapter.clear();
        mLeDeviceListAdapter.notifyDataSetChanged();

        if (!checkPermissions()) {

            Toast.makeText(this, getResources().getString(R.string.please_open_location), Toast.LENGTH_SHORT).show();
        }

        EasyPermissions.requestPermissions(
                this,
                getResources().getString(R.string.request_location_permission),
                RC_PERM_CODE,
                android.os.Build.VERSION.SDK_INT < 31 ? permissionList : permissionListHigher);

        startTimer();
    }

    private void startTimer() {
        if (timer == null) {
            timer = new Timer();
        }

        if (timerTask == null) {
            timerTask = new TimerTask() {
                @Override
                public void run() {
                    Message message = new Message();
                    message.what = 1;
                    handler.sendMessage(message);
                }
            };
        }

        if (timer != null) {
            timer.schedule(timerTask,1000,2000);//延时1s，每隔2秒执行一次run方法
        }
    }

    private  void stopTimer() {
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
    protected void onPause()
    {
        super.onPause();
        scanLeDevice(false);		//停止蓝牙扫描
        mLeDeviceListAdapter.clear();	//清空list

        stopTimer();
    }

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 1){
                mLeDeviceListAdapter.sort();
                mLeDeviceListAdapter.notifyDataSetChanged();
            }
            super.handleMessage(msg);
        }
    };

    // Each attempt owns its callback and wait; the callback must not retain an Activity.
    private static final class ConnectionCallback extends BleGattCallback {
        final ConnectionWork work;
        final WiseWaitEvent event;
        final java.lang.ref.WeakReference<ScanBleActivity> activity;
        ConnectionCallback(ConnectionWork work, WiseWaitEvent event, ScanBleActivity activity) {
            this.work = work;
            this.event = event;
            this.activity = new java.lang.ref.WeakReference<>(activity);
        }
        @Override public void onStartConnect() { }
        @Override public void onConnectFail(BleDevice device, BleException error) {
            event.setSignal(WiseWaitEvent.ERROR_FAILED);
        }
        @Override public void onConnectSuccess(BleDevice device, BluetoothGatt gatt, int status) {
            if (work.cancelled.get() || !work.session.isCurrent(device)) {
                work.session.disconnect(device);
                event.setSignal(WiseWaitEvent.ERROR_FAILED);
            } else {
                event.setSignal(WiseWaitEvent.SUCCESS);
            }
        }
        @Override public void onDisConnected(boolean active, BleDevice device, BluetoothGatt gatt, int status) {
            work.cancelled.set(true);
            Thread worker = work.worker;
            if (worker != null) worker.interrupt();
            work.session.removeIfCurrent(device);
            ScanBleActivity owner = activity.get();
            if (owner != null) owner.runIfActive(() -> owner.mLeDeviceListAdapter.notifyDataSetChanged());
        }
    }

    private int connectBleSynchronization(BleManager manager, BleDevice device, ConnectionWork work) {
        WiseWaitEvent event = new WiseWaitEvent();
        event.init();
        ConnectionCallback callback = new ConnectionCallback(work, event, this);
        runOnUiThread(() -> {
            if (!work.cancelled.get() && work.session.isCurrent(device)) manager.connect(device, callback);
            else event.setSignal(WiseWaitEvent.ERROR_FAILED);
        });
        int result = event.waitSignal(16000);
        if (result == WiseWaitEvent.ERROR_TIME_OUT) work.cancelled.set(true);
        return result;
    }

    // 同步打开通知
    static boolean openNotifyBleSynchronization(BleManager bleManager, BleDevice bleDevice, final WiseCharacteristic characteristic, ConnectionWork work) {

        if (isConnectionCancelled(work)) return false;
        WiseWaitEvent stateEvent = new WiseWaitEvent();
        stateEvent.init();
        FastBleListener.getInstance().setNotifyBleCallback(bleDevice, characteristic, new BleNotifyCallback() {
            @Override
            public void onNotifySuccess() {
                stateEvent.setSignal(WiseWaitEvent.SUCCESS);
            }

            @Override
            public void onNotifyFailure(BleException e) {
                stateEvent.setSignal(WiseWaitEvent.ERROR_FAILED);
            }

            @Override
            public void onCharacteristicChanged(byte[] bytes) {
                if (work.cancelled.get() || !work.session.isCurrent(bleDevice)) return;
                if (characteristic.getServiceID().equalsIgnoreCase(BleConfig.Ble_Config_Receive_Service.getServiceID())
                        && characteristic.getCharacteristicID().equalsIgnoreCase(BleConfig.Ble_Config_Receive_Service.getCharacteristicID())
                        && bytes != null) {
                    work.session.acceptConfigData(bytes);
                    if (!work.waitingForFlow) return;
                    synchronized (work.response) {
                        if (!work.waitingForFlow) return;
                        work.response.append(ConvertData.bytesToUtf8(bytes));
                        String response = work.response.toString();
                        String value = response.contains("<rd_uart_fc=1>") ? "<rd_uart_fc=1>"
                                : response.contains("<rd_uart_fc=0>") ? "<rd_uart_fc=0>" : null;
                        if (value != null) {
                            work.flowResponse = ConvertData.utf8ToBytes(value);
                            work.recvEvent.setSignal(WiseWaitEvent.SUCCESS);
                        }
                        if (work.response.length() > 256) work.response.delete(0, work.response.length() - 64);
                    }
                }
            }
        });
        FastBleListener.getInstance().openNotify(bleDevice, characteristic);

        int result = stateEvent.waitSignal(5000);
        if(WiseWaitEvent.SUCCESS != result) {
            return false;
        }

        return true;
    }

    private static boolean sendDataSynchronization(BleManager bleManager, BleDevice bleDevice, WiseCharacteristic characteristic, byte[] data, ConnectionWork work) {
        if (isConnectionCancelled(work)) return false;
        WiseWaitEvent sendEvent = new WiseWaitEvent();
        sendEvent.init();
        bleManager.write(bleDevice, characteristic.getServiceID(), characteristic.getCharacteristicID(), data, new BleWriteCallback() {
            @Override
            public void onWriteSuccess(int i, int i1, byte[] bytes) {
                sendEvent.setSignal(WiseWaitEvent.SUCCESS);
            }

            @Override
            public void onWriteFailure(BleException e) {
                sendEvent.setSignal(WiseWaitEvent.ERROR_FAILED);
            }
        });

        int result = sendEvent.waitSignal(5000);
        if(WiseWaitEvent.SUCCESS != result) {
            return false;
        }

        return true;
    }

    static byte[] sendRecvData(BleManager manager, BleDevice device, WiseCharacteristic send,
                                       byte[] data, ConnectionWork work) {
        if (isConnectionCancelled(work)) return null;
        synchronized (work.response) {
            work.response.setLength(0);
            work.flowResponse = null;
            work.recvEvent.init();
            work.waitingForFlow = true;
        }
        try {
            if (sendDataSynchronization(manager, device, send, data, work)
                    && !isConnectionCancelled(work)
                    && work.recvEvent.waitSignal(5000) == WiseWaitEvent.SUCCESS) return work.flowResponse;
            return null;
        } finally {
            work.waitingForFlow = false;
        }
    }

    boolean isSupportConfigService(BleManager bleManager, BleDevice bleDevice) {
        List<BluetoothGattService> services = bleManager.getBluetoothGattServices(bleDevice);

        if (services == null) return false;
        BluetoothGattService configService = null;
        for (BluetoothGattService service: services) {
            if (service.getUuid().toString().equals(BleConfig.Ble_Config_Receive_Service.getServiceID())) {
                configService = service;
                break;
            }
        }
        if (configService == null) {
            return false;
        }

        BluetoothGattCharacteristic charact = configService.getCharacteristic(UUID.fromString(BleConfig.Ble_Config_Receive_Service.getCharacteristicID()));
        if (charact == null) {
            return false;
        }

        int property = charact.getProperties();
        if ((property & BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0 && ((property & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) > 0 || (property & BluetoothGattCharacteristic.PROPERTY_WRITE) > 0)) {
            return true;
        }

        return false;
    }

    /**
     * 设置mtu
     * @param len 想要设置的mtu
     * @return 真实mtu
     */
    static int requestMtu(BleManager bleManager, BleDevice bleDevice, int len, ConnectionWork work) {
        if (isConnectionCancelled(work)) return 23;
        WiseWaitEvent mtuEvent = new WiseWaitEvent();
        mtuEvent.init();
        java.util.concurrent.atomic.AtomicInteger mtuLength = new java.util.concurrent.atomic.AtomicInteger(23);
        bleManager.setMtu(bleDevice, len, new BleMtuChangedCallback() {
            @Override
            public void onSetMTUFailure(BleException e) {
                mtuEvent.setSignal(WiseWaitEvent.ERROR_FAILED);
            }

            @Override
            public void onMtuChanged(int i) {
                mtuLength.set(Math.max(23, i));
                mtuEvent.setSignal(WiseWaitEvent.SUCCESS);
            }
        });

        mtuEvent.waitSignal(5000);
        return mtuLength.get();
    }


    // 连接蓝牙
    void connectBle(final HJBleScanDevice scanDevice) {
        if (!connectionRunning.compareAndSet(false, true)) return;
        ConnectionWork cancellation = new ConnectionWork(scanDevice.device);
        if (cancellation.session == null) {
            connectionRunning.set(false);
            loadingDialog.dismiss();
            return;
        }
        connectionCancellation = cancellation;
        loadingDialog.setMessage(getResources().getString(R.string.ble_connecting));
        connectionExecutor.submit(new Runnable() {
            @Override
            public void run() {
                cancellation.worker = Thread.currentThread();
                try {
                    BleManager bleManager = BleManager.getInstance();

                    int i;
                    for (i = 0; i < 5; i++)
                    {
                        if (isConnectionCancelled(cancellation)) return;
                        int result = connectBleSynchronization(bleManager, scanDevice.device, cancellation);
                        if (result == WiseWaitEvent.SUCCESS) break;
                        if (result == WiseWaitEvent.ERROR_TIME_OUT) {
                            showConnectionFailure(cancellation);
                            return;
                        }
                        if (isConnectionCancelled(cancellation)) return;

                        SystemClock.sleep(200);//200ms
                    }
                    if(i == 5)
                    {
                        showConnectionFailure(cancellation);
                        return ;
                    }

                    if (isConnectionCancelled(cancellation)) {
                        cancellation.session.disconnect(scanDevice.device);
                        return;
                    }

//                SystemClock.sleep(200);//200ms

                    runConnectionUi(cancellation, new Runnable() {
                        @Override
                        public void run() {

                            Log.e("'test'", "'正在打开通知1'");
                            loadingDialog.setMessage(getResources().getString(R.string.ble_opening_notification));
                        }
                    });

                    scanDevice.bFlowControl = false;
                    scanDevice.isConfig = isSupportConfigService(bleManager, scanDevice.device);


                    // 打开配置通知
                    if(scanDevice.isConfig && !openNotifyBleSynchronization(bleManager, scanDevice.device, BleConfig.Ble_Config_Receive_Service, cancellation))
                    {
                        cancellation.session.disconnect(scanDevice.device);

                        showConnectionFailure(cancellation);
                        return ;
                    }

                    if (isConnectionCancelled(cancellation)) {
                        cancellation.session.disconnect(scanDevice.device);
                        return;
                    }


                    // 设置mtu
                    int mtuLen = requestMtu(bleManager, scanDevice.device, 512, cancellation) - 3;
                    if (isConnectionCancelled(cancellation)) return;
                    scanDevice.mtuLen = mtuLen;
                    cancellation.session.setPacketLength(mtuLen);

                    // 是否为流控模式
                    boolean bFlowControl = false;

                    boolean supportFlowControl = HJBleApplication.shareInstance().isFlowControl();

                    // 支持配置模式的设备，获取是否为流控模式
                    if (scanDevice.isConfig && supportFlowControl) {
                        runConnectionUi(cancellation, new Runnable() {
                            @Override
                            public void run() {

                                loadingDialog.setMessage(getResources().getString(R.string.reading_flow_control));
                            }
                        });

                        byte[] cmd = ConvertData.utf8ToBytes("<RD_UART_FC>");
                        byte[] recv = sendRecvData(bleManager, scanDevice.device, BleConfig.Ble_Config_Send_Service, cmd, cancellation);
                        if (recv == null) {
                            showConnectionFailure(cancellation);
                            return;
                        }
                        bFlowControl = "<rd_uart_fc=1>".equals(ConvertData.bytesToUtf8(recv));

                        scanDevice.bFlowControl = bFlowControl;

                    }

                    if (isConnectionCancelled(cancellation)) {
                        cancellation.session.disconnect(scanDevice.device);
                        return;
                    }


                    if(openNotifyBleSynchronization(bleManager, scanDevice.device, BleConfig.Ble_Data_Receive_Service(), cancellation))
                    {
                        if (isConnectionCancelled(cancellation)) return;
                        cancellation.session.markReady(scanDevice);
                        runConnectionUi(cancellation, new Runnable() {
                            @Override
                            public void run() {

                                loadingDialog.dismiss();
                                mLeDeviceListAdapter.notifyDataSetChanged();

                            }
                        });
                    }
                    else
                    {
                        cancellation.session.disconnect(scanDevice.device);

                        showConnectionFailure(cancellation);
                    }
                } catch (SecurityException error) {
                    showConnectionFailure(cancellation);
                } finally {
                    cancellation.worker = null;
                    Thread.interrupted();
                    runOnUiThread(() -> {
                        if (!cancellation.session.isReady() || cancellation.cancelled.get()) {
                            cancellation.session.disconnect(scanDevice.device);
                            if (BleManager.getInstance().getBleBluetooth(scanDevice.device) == null) {
                                cancellation.session.removeIfCurrent(scanDevice.device);
                            }
                        }
                        if (connectionCancellation == cancellation) {
                            connectionRunning.set(false);
                            if (!isFinishing() && !isDestroyed()) {
                                loadingDialog.dismiss();
                                mLeDeviceListAdapter.notifyDataSetChanged();
                                if (!mScanning) scanLeDevice(true);
                            }
                        }
                    });
                }
            }
        });
    }

    private static boolean isConnectionCancelled(ConnectionWork work) {
        return work.cancelled.get() || Thread.currentThread().isInterrupted() || !work.session.isCurrent(work.device);
    }

    private void showConnectionFailure(ConnectionWork work) {
        runConnectionUi(work, () -> Toast.makeText(this, R.string.ble_connect_failed, Toast.LENGTH_SHORT).show());
    }

    private void runConnectionUi(ConnectionWork work, Runnable action) {
        runIfActive(() -> {
            if (connectionCancellation == work && !work.cancelled.get() && work.session.isCurrent(work.device)) action.run();
        });
    }

    private void cancelConnection() {
        ConnectionWork work = connectionCancellation;
        if (work == null || !connectionRunning.get()) return;
        work.cancelled.set(true);
        Thread worker = work.worker;
        if (worker != null) worker.interrupt();
        work.session.disconnect(work.device);
        if (BleManager.getInstance().getBleBluetooth(work.device) == null) work.session.removeIfCurrent(work.device);
    }

    private void runIfActive(Runnable action) {
        runOnUiThread(() -> {
            if (!isFinishing() && !isDestroyed()) action.run();
        });
    }

    @Override
    protected void onDestroy() {
        cancelConnection();
        connectionExecutor.shutdownNow();
        handler.removeCallbacksAndMessages(null);
        if (scanManager != null) {
            scanManager.setScanCallbackCompat(null);
            scanManager.setScanOverListener(null);
        }
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        //打开蓝牙结果
        if(requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED)
        {
            finish();
            return ;
        }
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_OK) {
            scanLeDevice(true);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    // 扫描回调
    ScanCallbackCompat scanCallback = new ScanCallbackCompat() {
        @Override
        public void onBatchScanResults(List<ScanResultCompat> results) {
            super.onBatchScanResults(results);
        }

        @Override
        public void onScanFailed(final int errorCode) {
            super.onScanFailed(errorCode);

            scanLeDevice(false);

            if (errorCode == SCAN_FAILED_LOCATION_CLOSE){
                Toast.makeText(ScanBleActivity.this, getResources().getString(R.string.location_is_closed), Toast.LENGTH_LONG).show();
            }else if(errorCode == SCAN_FAILED_LOCATION_PERMISSION_FORBID){
                Toast.makeText(ScanBleActivity.this, getResources().getString(R.string.no_location_permission), Toast.LENGTH_LONG).show();
            }else{
                Toast.makeText(ScanBleActivity.this, getResources().getString(R.string.unknown_error), Toast.LENGTH_LONG).show();
            }

        }

        @Override
        public void onScanResult(int callbackType, ScanResultCompat result) {
            super.onScanResult(callbackType, result);

            if (result.getScanRecord() == null) return;
            byte[] scanRecord = result.getScanRecord().getBytes();

            if (scanRecord.length < 9) {
                return;
            }

            // 获取前面9个字节数据
            byte[] tempScan = new byte[9];
            System.arraycopy(scanRecord, 0, tempScan, 0, 9);
            String scanRecordStr = ConvertData.bytesToHexString(tempScan, false);

            boolean isEasy = false;
            boolean isConfig = false;
//            int maxLen = 20;
            boolean bMtu = false;

            // 是否扫描过滤
            if (bScanFilter) {

                if (scanRecordStr.contains("5869") && scanRecord[3] == 0x03) {
                    isEasy = false;
                    bMtu = false;
//                    maxLen = 20;
                    isConfig = false;
                }
                else if (scanRecordStr.contains("5869")) {
                    isEasy = false;
                    bMtu = true;
//                    maxLen = 160;
                    isConfig = true;
                }
                else if (scanRecordStr.contains("5969")) {
                    isEasy = true;
                    bMtu = true;
//                    maxLen = 160;
                    isConfig = true;
                }
                else {
                    return;
                }

            }

            Log.d(TAG, ConvertData.bytesToHexString(scanRecord, false));
            HJBleScanDevice scanDevice = new HJBleScanDevice();
            scanDevice.device = BleManager.getInstance().convertBleDevice(result.getDevice());
            scanDevice.rssi = result.getRssi();
            scanDevice.record = ConvertData.bytesToHexString(scanRecord, false);
            scanDevice.isEasy = isEasy;
            scanDevice.isConfig = isConfig;
            scanDevice.bMtu = bMtu;
//            scanDevice.sendDataLenMax = maxLen;
            mLeDeviceListAdapter.addDevice(scanDevice);
//            mLeDeviceListAdapter.notifyDataSetChanged();
        }
    };

    //enable = true表示蓝牙开始扫描，否则表示停止扫描
    private void scanLeDevice(final boolean enable)
    {
        if (enable && !checkPermissions()) return;
        try {
            if (enable)
            {
                mScanning = true;
                scanManager.startScanNow();	//开始蓝牙扫描

                setRightText(getResources().getString(R.string.stop_scan));
                scanProgress.setVisibility(View.VISIBLE);
            }
            else
            {
                //取消停止扫描的线程
                mScanning = false;
                scanManager.stopCycleScan();	//停止蓝牙扫描

                setRightText(getResources().getString(R.string.start_scan));
                scanProgress.setVisibility(View.INVISIBLE);
            }
        } catch (SecurityException e) {
            mScanning = false;
            Toast.makeText(this, R.string.no_location_permission, Toast.LENGTH_LONG).show();
        }
    }

    //检测权限
    private boolean checkPermissions() {

        if (Build.VERSION.SDK_INT >= 23){

            boolean hasPermission = EasyPermissions.hasPermissions(this, android.os.Build.VERSION.SDK_INT < 31 ? permissionList : permissionListHigher);

            if (Build.VERSION.SDK_INT < 31) {
                boolean bResult = isGpsProviderEnabled();

                if (!bResult){
//                Toast.makeText(ScanBleActivity.this, "部分机型需要打开gps才能扫描到设备", Toast.LENGTH_SHORT).show();
                    return false;
                }
            }

            return hasPermission;
        }
        return true;

    }

    /**
     * is open gps
     * @param context
     * @return
     */
//    public static boolean isGpsProviderEnabled(Context context){
//        LocationManager service = (LocationManager) context.getSystemService(context.LOCATION_SERVICE);
//        return service.isProviderEnabled(LocationManager.GPS_PROVIDER);
//    }

    // 检测gps是否开启，避免部分手机使用isProviderEnabled函数总返回false的问题
    private boolean isGpsProviderEnabled() {
//         String provider = Settings.Secure.getString(getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
//         if (TextUtils.isEmpty(provider)) return false;
//         return provider.contains("gps");
            return true;
    }


    @AfterPermissionGranted(RC_PERM_CODE)
    public void onPermissionsSuccess() {
        if (checkPermissions()) {
            //判断本地蓝牙是否已打开
            if(!BleManager.getInstance().isBlueEnable())
            {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                        && ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                Intent openIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(openIntent, REQUEST_ENABLE_BT);
                return;
            }
            scanLeDevice(true);
        }

    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {

    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {

    }

    @Override
    public void onRationaleAccepted(int requestCode) {

    }

    @Override
    public void onRationaleDenied(int requestCode) {

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }
}
