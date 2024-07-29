package com.hongjia.hjbledemo;

import android.app.Application;
import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;

import com.clj.fastble.BleManager;
import com.wise.wisekit.utils.SPUtils;

public class HJBleApplication extends Application {

    private static final String TAG = HJBleApplication.class.getName();

    private  static HJBleApplication instance;

    // 是否为配置模式
    private boolean isBleConfig = false;

    // 测试文件uri
    private Uri testFileUri;

    //获取应用的context
    public static Context getAppContext() {
        return instance.getApplicationContext();
    }


    @Override
    public void onCreate() {
        super.onCreate();

        instance = this;

        BleManager.getInstance().init(this);

        BleManager.getInstance()
                .enableLog(true)
                .setReConnectCount(1, 5000)
                .setConnectOverTime(5000)
                .setOperateTimeout(5000);

    }

    public static HJBleApplication shareInstance() {
        return instance;
    }

    public boolean isBleConfig() {
        return isBleConfig;
    }

    public void setBleConfig(boolean bleConfig) {
        isBleConfig = bleConfig;
    }

    // 是否存储日志
    public boolean isSaveLog() {
        Boolean isSaveLog = (Boolean)SPUtils.get(getAppContext(), "isSaveLog", false);
        return isSaveLog.booleanValue();
    }

    public void setSaveLog(boolean isSaveLog) {

        SPUtils.put(getAppContext(), "isSaveLog", isSaveLog);
    }

    // 扫描过滤
    public boolean isScanFilter() {
        Boolean bFilter = (Boolean)SPUtils.get(getAppContext(), "isScanFilter", true);
        return bFilter.booleanValue();
    }

    public void setScanFilter(boolean scanFilter) {

        SPUtils.put(getAppContext(), "isScanFilter", scanFilter);
    }

    // 是否支持流控
    public boolean isFlowControl() {
        Boolean flowControl = (Boolean)SPUtils.get(getAppContext(), "isFlowControl", false);
        return flowControl.booleanValue();
    }

    public void setFlowControl(boolean flowControl) {

        SPUtils.put(getAppContext(), "isFlowControl", flowControl);
    }

    // 获取数据通知主服务
    public String getDataNotifyMainService() {
        String defaultId = BleConfig.Ble_Default_Data_Send_Service.getServiceID();

        return (String) SPUtils.get(getAppContext(), "dataNotifyMainService", defaultId);
    }

    // 设置数据通知主服务
    public void setDataNotifyMainService(String serviceId) {
        SPUtils.put(getAppContext(), "dataNotifyMainService", serviceId);
    }

    // 获取数据发送主服务
    public String getDataSendMainService() {
        String defaultId = BleConfig.Ble_Default_Data_Send_Service.getServiceID();

        return (String) SPUtils.get(getAppContext(), "dataSendMainService", defaultId);
    }

    // 设置数据发送主服务
    public void setDataSendMainService(String serviceId) {
        SPUtils.put(getAppContext(), "dataSendMainService", serviceId);
    }

    // 获取数据通知服务
    public String getDataNotifyService() {
        String defaultId = BleConfig.Ble_Default_Data_Receive_Service.getCharacteristicID();

        return (String)SPUtils.get(getAppContext(), "dataNotifyService", defaultId);
    }

    // 设置数据通知服务
    public void setDataNotifyService(String serviceId) {
        SPUtils.put(getAppContext(), "dataNotifyService", serviceId);
    }

    // 获取数据发送服务
    public String getDataSendService() {
        String defaultId = BleConfig.Ble_Default_Data_Send_Service.getCharacteristicID();

        return (String)SPUtils.get(getAppContext(), "dataSendService", defaultId);
    }

    // 设置数据发送服务
    public void setDataSendService(String serviceId) {
        SPUtils.put(getAppContext(), "dataSendService", serviceId);
    }

    // 是否为十六进制模式
    public boolean isBleHex() {

        Boolean hex = (Boolean)SPUtils.get(getAppContext(), "isBleHex", true);
        return hex.booleanValue();
    }

    public void setBleHex(boolean bleHex) {

        SPUtils.put(getAppContext(), "isBleHex", bleHex);
    }

    // 是否在发送数据后面自动加回车
    public boolean isAddReturn() {

        Boolean addReturn = (Boolean)SPUtils.get(getAppContext(), "isAddReturn", false);
        return addReturn.booleanValue();
    }

    public void setAddReturn(boolean addReturn) {

        SPUtils.put(getAppContext(), "isAddReturn", addReturn);
    }

    // 是否为respone
    public boolean isWriteTypeResponse() {

        Boolean isResponse = (Boolean)SPUtils.get(getAppContext(), "isWriteTypeResponse", false);
        return isResponse.booleanValue();
    }

    public void setWriteTypeResponse(boolean isWriteTypeResponse) {

        SPUtils.put(getAppContext(), "isWriteTypeResponse", isWriteTypeResponse);
    }

    // 每次下发测试数据长度
    public int testDataLen() {

        return (Integer)SPUtils.get(getAppContext(), "testDataLen", 20);
    }

    public void setTestDataLen(int testDataLen) {

        SPUtils.put(getAppContext(), "testDataLen", testDataLen);
    }

    // 下发数据时间间隙
    public int testGapTime() {

        return (Integer)SPUtils.get(getAppContext(), "testGapTime", 20);
    }

    public void setTestGapTime(int testGapTime) {

        SPUtils.put(getAppContext(), "testGapTime", testGapTime);
    }

    // 测试文件路径
    public String testFilePath() {

        return (String)SPUtils.get(getAppContext(), "testFilePath", "-");
    }

    public void setTestFilePath(String filePath) {

        SPUtils.put(getAppContext(), "testFilePath", filePath);
    }

    public Uri getTestFileUri() {
        String tmp = (String)SPUtils.get(getAppContext(), "testFilePath", "");
        if (TextUtils.isEmpty(tmp)) return  null;

        return Uri.parse(tmp);
    }

    public void setTestFileUri(Uri testFileUri) {
        SPUtils.put(getAppContext(), "testFilePath", testFileUri.toString());
//        this.testFileUri = testFileUri;
    }

    // 是否使用文件测试
    public boolean useFileTest() {

        return  (Boolean)SPUtils.get(getAppContext(), "useFileTest", false);
    }

    public void setUseFileTest(boolean bUseFileTest) {

        SPUtils.put(getAppContext(), "useFileTest", bUseFileTest);
    }

}
