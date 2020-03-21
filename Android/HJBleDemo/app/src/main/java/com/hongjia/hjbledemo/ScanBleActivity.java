package com.hongjia.hjbledemo;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.githang.statusbar.StatusBarCompat;
import com.wise.ble.ConvertData;
import com.wise.ble.WiseBluetoothLe;
import com.wise.ble.scan.BleParamsOptions;
import com.wise.ble.scan.BluetoothScanManager;
import com.wise.ble.scan.ScanOverListener;
import com.wise.ble.scan.bluetoothcompat.ScanCallbackCompat;
import com.wise.ble.scan.bluetoothcompat.ScanResultCompat;
import com.wise.wisekit.activity.BaseActivity;
import com.wise.wisekit.dialog.LoadingDialog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class ScanBleActivity extends BaseActivity implements EasyPermissions.PermissionCallbacks,
        EasyPermissions.RationaleCallbacks {

    private static final int REQUEST_ENABLE_BT = 1;
    private static final int RC_PERM_CODE = 124;

    private String[] permissionList = {Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION};

    private boolean mScanning=true;

    private ListView mListView = null;

    private LeDeviceListAdapter mLeDeviceListAdapter;

    private WiseBluetoothLe mble = WiseBluetoothLe.getInstance(HJBleApplication.getAppContext());

    private LoadingDialog loadingDialog = null;

    private ProgressBar scanProgress;

    private BluetoothScanManager scanManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        //判断本设备是否支持蓝牙ble，并连接本地蓝牙设备
        if(!mble.isBleSupported() || !mble.connectLocalDevice())
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

        StatusBarCompat.setStatusBarColor(this, getResources().getColor(R.color.colorNavBackground, null), false);

        setTitle("扫描列表");
        topLeftBtn.setImageResource(R.mipmap.info);
        topLeftBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder ab=new AlertDialog.Builder(ScanBleActivity.this);  //(普通消息框)

                ab.setTitle("版本信息");  //设置标题
                ab.setMessage("V " + getVersionName(ScanBleActivity.this));//设置消息内容
                ab.setNegativeButton("确定",null);//设置取消按钮
                ab.show();//显示弹出框
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
                        mble.disconnectDevice();
                    }
                });
        loadingDialog = loadBuilder.create();

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                loadingDialog.show();

                HJBleScanDevice scanDevice = mLeDeviceListAdapter.getScanDeviceInfo(position);

                scanLeDevice(false);

                connectBle(scanDevice.device, scanDevice.sendDataLenMax, scanDevice.isConfig);
            }
        });

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

        //判断本地蓝牙是否已打开
        if(!mble.isOpened())
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
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        scanLeDevice(false);		//停止蓝牙扫描
        mLeDeviceListAdapter.clear();	//清空list
        timer.cancel();
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

    Timer timer = new Timer();
    TimerTask timerTask = new TimerTask() {
        @Override
        public void run() {
            Message message = new Message();
            message.what = 1;
            handler.sendMessage(message);
        }
    };


    // 连接蓝牙
    void connectBle(final BluetoothDevice device, final int sendDataLenMax, final boolean isConfig) {

        new Thread(new Runnable() {
            @Override
            public void run() {

                int i;
                for (i = 0; i < 5; i++)
                {
                    if(mble.connectDevice(device.getAddress()))	//连接蓝牙设备
                        break;

                    mble.disconnectDevice();
                    try {
                        Thread.sleep(500,0);//200ms
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

                try {
                    Thread.sleep(200,0);//200ms
                }
                catch (Exception e){

                }


                // 打开配置通知
                if(isConfig && !mble.openNotify(BleConfig.Ble_Config_Receive_Service))
                {
                    mble.disconnectDevice();

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


                if(mble.openNotify(BleConfig.Ble_Data_Receive_Service))
                {
                    mble.setSendDataLenMax(sendDataLenMax);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            loadingDialog.dismiss();
                            final Intent intent = new Intent(ScanBleActivity.this, BluetoothControlAcitvity.class);
                            intent.putExtra(BluetoothControlAcitvity.EXTRAS_DEVICE_NAME, device.getName());
                            intent.putExtra(BluetoothControlAcitvity.EXTRAS_DEVICE_ADDRESS, device.getAddress().toUpperCase());
                            intent.putExtra(BluetoothControlAcitvity.EXTRAS_DEVICE_IS_CONFIG, isConfig);

                            ScanBleActivity.this.startActivity(intent);
                        }
                    });
                    return ;
                }
                else
                {
                    mble.disconnectDevice();

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



            }
        }).start();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        //打开蓝牙结果
        if(resultCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED)
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
                Toast.makeText(ScanBleActivity.this, "位置已关闭，请先打开位置信息", Toast.LENGTH_LONG).show();
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

            boolean isEasy = false;
            boolean isConfig = false;
            int maxLen = 20;

            //02010603035869
            if(scanRecord.length >= 7 && scanRecord[0] == 0x02 && scanRecord[1] == 0x01 &&
                    scanRecord[2] == 0x06 && scanRecord[3] == 0x03 && scanRecord[4] == 0x03 &&
                    scanRecord[5] == 0x58 && scanRecord[6] == 0x69) {


                isEasy = false;
                maxLen = 20;
                isConfig = false;
            }
            else if(scanRecord.length >= 7 && scanRecord[0] == 0x02 && scanRecord[1] == 0x01 &&
                    scanRecord[2] == 0x06 && scanRecord[3] == 0x09 && scanRecord[4] == 0x03 &&
                    scanRecord[5] == 0x58 && scanRecord[6] == 0x69) {

                isEasy = false;
                maxLen = 160;
                isConfig = true;
            }
            else if(scanRecord.length >= 7 && scanRecord[0] == 0x02 && scanRecord[1] == 0x01 &&
                    scanRecord[2] == 0x06 && scanRecord[3] == 0x09 && scanRecord[4] == 0x03 &&
                    scanRecord[5] == 0x59 && scanRecord[6] == 0x69) {

                isEasy = true;
                maxLen = 160;
                isConfig = true;
            }
            else {
                return;
            }


            Log.d(TAG, ConvertData.bytesToHexString(scanRecord, false));
            HJBleScanDevice scanDevice = new HJBleScanDevice();
            scanDevice.device = result.getDevice();
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

            boolean bResult = isGpsProviderEnabled(this);

            if (!bResult){
                return false;
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
    public static boolean isGpsProviderEnabled(Context context){
        LocationManager service = (LocationManager) context.getSystemService(context.LOCATION_SERVICE);
        return service.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }


    @AfterPermissionGranted(RC_PERM_CODE)
    public void onPermissionsSuccess() {
        if (checkPermissions()) {
            scanLeDevice(true);
            timer.schedule(timerTask,1000,2000);//延时1s，每隔2秒执行一次run方法
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
        BluetoothDevice device;
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

        // 时间
        Time time;

        @Override
        public int compareTo(HJBleScanDevice o) {

            // 降序
            return o.rssi - this.rssi;
        }
    }


    // Adapter for holding devices found through scanning.
    private class LeDeviceListAdapter extends BaseAdapter
    {
        private ArrayList<HJBleScanDevice> mScanDevices;
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
                if (mScanDevices.get(i).device.getAddress().equals(scanDevice.device.getAddress())) {
                    scanDevice.time = new Time();
                    scanDevice.time.setToNow();
                    mScanDevices.set(i, scanDevice);
                    isAdd = false;
                    break;
                }
            }

            if (isAdd) {
                scanDevice.time = new Time();
                scanDevice.time.setToNow();
                mScanDevices.add(scanDevice);
            }

        }

        public void sort() {
            Collections.sort(mScanDevices);
        }

        public BluetoothDevice getDevice(int position)
        {
            return mScanDevices.get(position).device;
        }

        public HJBleScanDevice getScanDeviceInfo(int position)
        {
            return mScanDevices.get(position);
        }


        public void clear() {
            mScanDevices.clear();
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
                view.setTag(viewHolder);
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

            viewHolder.deviceAddress.setText("address:"+scanDevice.device.getAddress() + "     RSSI:"+scanDevice.rssi+"dB");
            viewHolder.deviceRecord.setText("broadcast:"+scanDevice.record);
            String timeStr = "time: " + scanDevice.time.year + "-" + scanDevice.time.month + "-" + scanDevice.time.monthDay + " " +
                    scanDevice.time.hour + ":" + scanDevice.time.minute + ":" + scanDevice.time.second;
            viewHolder.deviceTime.setText(timeStr);

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
    }
}
