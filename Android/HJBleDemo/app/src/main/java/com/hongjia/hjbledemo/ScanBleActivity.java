package com.hongjia.hjbledemo;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.icu.text.SimpleDateFormat;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.text.TextUtils;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.clj.fastble.BleManager;
import com.clj.fastble.callback.BleGattCallback;
import com.clj.fastble.callback.BleNotifyCallback;
import com.clj.fastble.callback.BleWriteCallback;
import com.clj.fastble.data.BleDevice;
import com.clj.fastble.exception.BleException;
import com.githang.statusbar.StatusBarCompat;
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

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class ScanBleActivity extends BaseActivity implements EasyPermissions.PermissionCallbacks,
        EasyPermissions.RationaleCallbacks {

    // 扫描是否过滤
    private boolean bScanFilter = HJBleApplication.shareInstance().isScanFilter();

    private static final int REQUEST_ENABLE_BT = 1;
    private static final int RC_PERM_CODE = 124;

    private String[] permissionList = {Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE};

    private boolean mScanning=true;

    private ListView mListView = null;

    private LeDeviceListAdapter mLeDeviceListAdapter;

    private LoadingDialog loadingDialog = null;

    private ProgressBar scanProgress;

    private BluetoothScanManager scanManager;

    private Timer timer = new Timer();
    private TimerTask timerTask;

    private WiseWaitEvent connectEvent = new WiseWaitEvent();
    private WiseWaitEvent stateEvent = new WiseWaitEvent();
    private WiseWaitEvent sendEvent = new WiseWaitEvent();
    private WiseWaitEvent recvEvent = new WiseWaitEvent();

    private ByteArrayOutputStream recvBuffer = new ByteArrayOutputStream();

    private BleDevice selectBleDevice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        //判断本设备是否支持蓝牙ble，并连接本地蓝牙设备
        if(!BleManager.getInstance().isSupportBle())
        {
            Toast.makeText(this, "不支持BLE",Toast.LENGTH_SHORT).show();
            finish();
            return ;
        }

        mLeDeviceListAdapter = new LeDeviceListAdapter(this);
        mListView.setAdapter(mLeDeviceListAdapter);
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
            StatusBarCompat.setStatusBarColor(this, getResources().getColor(R.color.colorNavBackground, null), false);
        }

        setTitle("扫描列表");
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

        setRightText("停止扫描");
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

        mListView = findViewById(R.id.list_view);
        scanProgress = findViewById(R.id.progressBar1);

        LoadingDialog.Builder loadBuilder=new LoadingDialog.Builder(this)
                .setCancelable(true)
                .setShowMessage(true)
                .setMessage("正在连接中...")
                .setCancelOutside(false)
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialogInterface) {

                        if (selectBleDevice != null && BleManager.getInstance().isConnected(selectBleDevice)) {
                            BleManager.getInstance().disconnect(selectBleDevice);
                        }
                    }
                });
        loadingDialog = loadBuilder.create();

//        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
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

        scanManager = BluetoothScanManager.getInstance(this);
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
    protected void onResume()
    {
        super.onResume();

        bScanFilter = HJBleApplication.shareInstance().isScanFilter();

        //判断本地蓝牙是否已打开
        if(!BleManager.getInstance().isBlueEnable())
        {
            Intent openIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(openIntent, REQUEST_ENABLE_BT);
        }

        mLeDeviceListAdapter.clear();
        mLeDeviceListAdapter.notifyDataSetChanged();

        if (!checkPermissions()) {

            Toast.makeText(this, "请打开位置信息", Toast.LENGTH_SHORT).show();
        }

        EasyPermissions.requestPermissions(
                this,
                "请求位置权限",
                RC_PERM_CODE,
                permissionList);

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

    // 同步连接
    private boolean connectBleSynchronization(BleManager bleManager, BleDevice bleDevice) {

        connectEvent.init();

        FastBleListener.getInstance().setConnectBleCallBack(new BleGattCallback() {
            @Override
            public void onStartConnect() {

            }

            @Override
            public void onConnectFail(BleDevice bleDevice, BleException e) {
                connectEvent.setSignal(WiseWaitEvent.ERROR_FAILED);
            }

            @Override
            public void onConnectSuccess(BleDevice bleDevice, BluetoothGatt bluetoothGatt, int i) {
                connectEvent.setSignal(WiseWaitEvent.SUCCESS);
            }

            @Override
            public void onDisConnected(boolean b, BleDevice bleDevice, BluetoothGatt bluetoothGatt, int i) {
                mLeDeviceListAdapter.notifyDataSetChanged();
            }
        });

        bleManager.connect(bleDevice, FastBleListener.getInstance().getConnectBleCallBack());

        int result = connectEvent.waitSignal(5000);
        if(WiseWaitEvent.SUCCESS != result)
        {
            bleManager.disconnect(bleDevice);
            return false;
        }

        return true;
    }

    // 同步打开通知
    private boolean openNotifyBleSynchronization(BleManager bleManager, BleDevice bleDevice, final WiseCharacteristic characteristic) {

        stateEvent.init();
        FastBleListener.getInstance().setNotifyBleCallback(characteristic.getCharacteristicID(), new BleNotifyCallback() {
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
                if (characteristic.getCharacteristicID().equals(BleConfig.Ble_Config_Receive_Service.getCharacteristicID())) {
                    if (bytes != null && bytes.length > 0) {
                        recvBuffer.reset();
                        recvBuffer.write(bytes, 0, bytes.length);
                        recvEvent.setSignal(WiseWaitEvent.SUCCESS);
                    } else {
                        recvEvent.setSignal(WiseWaitEvent.ERROR_FAILED);
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

    private boolean sendDataSynchronization(BleManager bleManager, BleDevice bleDevice, WiseCharacteristic characteristic, byte[] data) {
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

    private byte[] sendRecvData(BleManager bleManager, BleDevice bleDevice, WiseCharacteristic sendChara, WiseCharacteristic recvChara, byte[] sendData) {
        recvEvent.init();

        if (sendDataSynchronization(bleManager, bleDevice, sendChara, sendData)) {
            if(WiseWaitEvent.SUCCESS == recvEvent.waitSignal(5000)) {
                byte[] tmp = recvBuffer.toByteArray();
                recvBuffer.reset();
                return tmp;
            }
        }
        return null;
    }


    // 连接蓝牙
    void connectBle(final HJBleScanDevice scanDevice) {

        loadingDialog.setMessage("正在连接中...");
        new Thread(new Runnable() {
            @Override
            public void run() {

                BleManager bleManager = BleManager.getInstance();


                int i;
                for (i = 0; i < 5; i++)
                {
                    if(connectBleSynchronization(bleManager, scanDevice.device))	//连接蓝牙设备
                        break;

                    try {
                        Thread.sleep(200,0);//200ms
                    }
                    catch (Exception e){

                    }
                }
                if(i == 5)
                {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(ScanBleActivity.this,"连接失败",Toast.LENGTH_SHORT).show();
                            loadingDialog.dismiss();
                            scanLeDevice(true);
                        }
                    });
                    return ;
                }

//                try {
//                    Thread.sleep(200,0);//200ms
//                }
//                catch (Exception e){
//
//                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        Log.e("'test'", "'正在打开通知1'");
                        loadingDialog.setMessage("正在打开通知");
                    }
                });


                // 打开配置通知
                if(scanDevice.isConfig && !openNotifyBleSynchronization(bleManager, scanDevice.device, BleConfig.Ble_Config_Receive_Service))
                {
                    bleManager.disconnect(scanDevice.device);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(ScanBleActivity.this,"连接失败",Toast.LENGTH_SHORT).show();
                            loadingDialog.dismiss();
                            scanLeDevice(true);
                        }
                    });
                    return ;
                }

//                try {
//                    Thread.sleep(200,0);//200ms
//                }
//                catch (Exception e){
//
//                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        loadingDialog.setMessage("正在读取流控信息");
                    }
                });

                // 是否为流控模式
                boolean bFlowControl = false;

                // 支持配置模式的设备，获取是否为流控模式
                if (scanDevice.isConfig) {
                    byte[] cmd = ConvertData.utf8ToBytes("<RD_UART_FC>");
                    byte[] recv = sendRecvData(bleManager, scanDevice.device, BleConfig.Ble_Config_Send_Service, BleConfig.Ble_Config_Receive_Service, cmd);
                    if (recv != null) {
                        String recvStr = ConvertData.bytesToUtf8(recv);
                        if (recvStr.equals("<rd_uart_fc=1>")) {
                            bFlowControl = true;
                        } else {
                            bFlowControl = false;
                        }
                    }

                    scanDevice.bFlowControl = bFlowControl;

                }


                if(openNotifyBleSynchronization(bleManager, scanDevice.device, BleConfig.Ble_Data_Receive_Service))
                {
                    bleManager.setSplitWriteNum(scanDevice.sendDataLenMax);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            loadingDialog.dismiss();
                            mLeDeviceListAdapter.notifyDataSetChanged();

                        }
                    });
                }
                else
                {
                    BleManager.getInstance().disconnect(scanDevice.device);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(ScanBleActivity.this,"连接失败",Toast.LENGTH_SHORT).show();
                            loadingDialog.dismiss();
                            scanLeDevice(true);
                        }
                    });
                }

            }
        }).start();
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
                Toast.makeText(ScanBleActivity.this, "位置已关闭，请先打开位置信息！！！", Toast.LENGTH_LONG).show();
            }else if(errorCode == SCAN_FAILED_LOCATION_PERMISSION_FORBID){
                Toast.makeText(ScanBleActivity.this, "你没有位置权限", Toast.LENGTH_LONG).show();
            }else{
                Toast.makeText(ScanBleActivity.this, "未知错误", Toast.LENGTH_LONG).show();
            }

        }

        @Override
        public void onScanResult(int callbackType, ScanResultCompat result) {
            super.onScanResult(callbackType, result);

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
            int maxLen = 20;

            // 是否扫描过滤
            if (bScanFilter) {

                if (scanRecordStr.contains("5869") && scanRecord[3] == 0x03) {
                    isEasy = false;
                    maxLen = 20;
                    isConfig = false;
                }
                else if (scanRecordStr.contains("5869")) {
                    isEasy = false;
                    maxLen = 160;
                    isConfig = true;
                }
                else if (scanRecordStr.contains("5969")) {
                    isEasy = true;
                    maxLen = 160;
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
            scanDevice.sendDataLenMax = maxLen;
            mLeDeviceListAdapter.addDevice(scanDevice);
            mLeDeviceListAdapter.notifyDataSetChanged();
        }
    };

    //enable = true表示蓝牙开始扫描，否则表示停止扫描
    private void scanLeDevice(final boolean enable)
    {
        if (enable)
        {
            mScanning = true;
            scanManager.startScanNow();	//开始蓝牙扫描

            setRightText("停止扫描");
            scanProgress.setVisibility(View.VISIBLE);
        }
        else
        {
            //取消停止扫描的线程
            mScanning = false;
            scanManager.stopCycleScan();	//停止蓝牙扫描

            setRightText("开始扫描");
            scanProgress.setVisibility(View.INVISIBLE);
        }
    }

    //检测权限
    private boolean checkPermissions() {

        if (Build.VERSION.SDK_INT >= 23){

            boolean hasPermission = EasyPermissions.hasPermissions(this, permissionList);

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
        String provider = Settings.Secure.getString(getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
        if (TextUtils.isEmpty(provider)) return false;
        return provider.contains("gps");
    }


    @AfterPermissionGranted(RC_PERM_CODE)
    public void onPermissionsSuccess() {
        if (checkPermissions()) {
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

    private class HJBleScanDevice implements Comparable<HJBleScanDevice> {
        // 蓝牙设备
        BleDevice device;
        // 信号强度
        int rssi;
        // 广播数据
        String record;
        // 是否为简易模式
        boolean isEasy;
        // 是否支持配置
        boolean isConfig;
        // 最大发送数据长度
        int sendDataLenMax;
        //
        boolean bFlowControl;

        // 时间
        Long timeStamp;

        @Override
        public int compareTo(HJBleScanDevice o) {

            // 降序
            return o.rssi - this.rssi;
        }
    }


    // Adapter for holding devices found through scanning.
    private class LeDeviceListAdapter extends BaseAdapter
    {
        private List<HJBleScanDevice> mScanDevices;
        private LayoutInflater mInflator;

        public LeDeviceListAdapter(Context context)
        {
            super();
            mScanDevices = new ArrayList<>();
            mInflator = LayoutInflater.from(context);
        }

        public void addDevice(HJBleScanDevice scanDevice)
        {
            boolean isAdd = true;

            for (int i=0; i<mScanDevices.size(); i++) {
                if (mScanDevices.get(i).device.getMac().equals(scanDevice.device.getMac())) {
                    scanDevice.timeStamp = new Date().getTime();
                    mScanDevices.set(i, scanDevice);
                    isAdd = false;
                    break;
                }
            }

            if (isAdd) {
                scanDevice.timeStamp = new Date().getTime();
                mScanDevices.add(scanDevice);
            }

        }

        public void sort() {
            Collections.sort(mScanDevices);
        }

        public BleDevice getDevice(int position)
        {
            return mScanDevices.get(position).device;
        }

        public HJBleScanDevice getScanDeviceInfo(int position)
        {
            return mScanDevices.get(position);
        }


        public void clear() {
            // 保留以连接的
            mScanDevices = mScanDevices.stream().filter(item -> BleManager.getInstance().isConnected(item.device.getMac())).collect(Collectors.toList());
        }

        @Override
        public int getCount()
        {
            return mScanDevices.size();
        }

        @Override
        public Object getItem(int i)
        {
            return mScanDevices.get(i);
        }

        @Override
        public long getItemId(int i)
        {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup)
        {
            ViewHolder viewHolder;
            // General ListView optimization code.
            if (view == null)
            {
//                view = mInflator.inflate(R.layout.activity_main, null);
                view = mInflator.inflate(R.layout.list, null);
                viewHolder = new ViewHolder();
                viewHolder.itemBgLayout = view.findViewById(R.id.item_bg);
                viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
                viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
                viewHolder.deviceRecord = (TextView)view.findViewById(R.id.device_record);
                viewHolder.deviceTime = (TextView)view.findViewById(R.id.device_time);
                viewHolder.imageView = view.findViewById(R.id.icon);
                viewHolder.connectBtn = view.findViewById(R.id.connect_btn);
                viewHolder.detailbtn = view.findViewById(R.id.detail_btn);
                view.setTag(viewHolder);
                viewHolder.connectBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int pos = ((Integer)v.getTag()).intValue();
                        HJBleScanDevice scanDevice = mScanDevices.get(pos);
                        if (BleManager.getInstance().isConnected(scanDevice.device.getMac())) {
                            BleManager.getInstance().disconnect(scanDevice.device);
                        } else {
                            loadingDialog.show();

                            selectBleDevice = scanDevice.device;

                            scanLeDevice(false);

                            connectBle(scanDevice);
                        }

                        mLeDeviceListAdapter.notifyDataSetChanged();
                    }
                });

                viewHolder.detailbtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int pos = ((Integer)v.getTag()).intValue();
                        HJBleScanDevice scanDevice = mScanDevices.get(pos);

                        final Intent intent = new Intent(ScanBleActivity.this, BluetoothDataActivity.class);
                        intent.putExtra(BluetoothDataActivity.EXTRAS_DEVICE_IS_CONFIG, scanDevice.isConfig);
                        intent.putExtra(BluetoothDataActivity.EXTRAS_DEVICE_IS_FLOW_CONTROL, scanDevice.bFlowControl);

                        intent.putExtra(BluetoothDataActivity.EXTRAS_DEVICE, scanDevice.device);

                        ScanBleActivity.this.startActivity(intent);
                    }
                });
            }
            else
            {
                viewHolder = (ViewHolder) view.getTag();
            }

            HJBleScanDevice scanDevice = mScanDevices.get(i);
            final String deviceName = scanDevice.device.getName();
            if (deviceName != null && deviceName.length() > 0)
                viewHolder.deviceName.setText(deviceName);
            else
                viewHolder.deviceName.setText("未知设备");

            viewHolder.connectBtn.setTag(i);
            viewHolder.detailbtn.setTag(i);
            if (BleManager.getInstance().isConnected(scanDevice.device.getMac())) {
                viewHolder.connectBtn.setText("断开");
                viewHolder.detailbtn.setVisibility(View.VISIBLE);
            } else {
                viewHolder.connectBtn.setText("连接");
                viewHolder.detailbtn.setVisibility(View.GONE);
            }

            viewHolder.deviceAddress.setText("address:"+scanDevice.device.getMac() + "     RSSI:"+scanDevice.rssi+"dB");
            viewHolder.deviceRecord.setText("broadcast:"+scanDevice.record);

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            viewHolder.deviceTime.setText(sdf.format(scanDevice.timeStamp));

            if (!scanDevice.isEasy) {
//                viewHolder.imageView.setVisibility(View.GONE);
                viewHolder.itemBgLayout.setBackgroundColor(Color.parseColor("#ffffff"));
                viewHolder.deviceName.setTextColor(Color.parseColor("#333333"));
                viewHolder.deviceAddress.setTextColor(Color.parseColor("#333333"));
                viewHolder.deviceRecord.setTextColor(Color.parseColor("#333333"));
                viewHolder.deviceTime.setTextColor(Color.parseColor("#333333"));
            }
            else {
//                viewHolder.imageView.setVisibility(View.VISIBLE);
                viewHolder.itemBgLayout.setBackgroundColor(Color.parseColor("#1FA7D3"));
                viewHolder.deviceName.setTextColor(Color.WHITE);
                viewHolder.deviceAddress.setTextColor(Color.WHITE);
                viewHolder.deviceRecord.setTextColor(Color.WHITE);
                viewHolder.deviceTime.setTextColor(Color.WHITE);
            }

            return view;
        }
    }

    static class ViewHolder
    {
        LinearLayout itemBgLayout;
        TextView deviceName;
        TextView deviceAddress;
        TextView deviceRecord;
        TextView deviceTime;
        ImageView imageView;

        Button connectBtn;
        Button detailbtn;
    }
}
