package com.hongjia.hjbledemo;


import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.wise.wisekit.activity.BaseActivity;

import java.util.UUID;
import java.util.regex.Pattern;

public class BleServiceConfigActivity extends BaseActivity {

    // 主服务
    EditText etMainService;
    // 通知服务
    EditText etNotifyService;
    // 发送服务
    EditText etSendService;

    // 恢复按钮
    Button btnRecoveryDetault;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected int getPageLayoutId() {
        return R.layout.activity_ble_service_config;
    }

    @Override
    protected void initView() {
        super.initView();

        setTitle(getResources().getString(R.string.service_setup));

        setRightText(getResources().getString(R.string.save_btn));
        rightTitleTxt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String mainService = etMainService.getText().toString();
                if (!verifyServiceUUID(mainService)) {
                    Toast.makeText(BleServiceConfigActivity.this, getResources().getString(R.string.master_service_error), Toast.LENGTH_LONG).show();
                    return;
                }

                String notifyService = etNotifyService.getText().toString();
                if (!verifyServiceUUID(notifyService)) {
                    Toast.makeText(BleServiceConfigActivity.this, getResources().getString(R.string.notify_service_error), Toast.LENGTH_LONG).show();
                    return;
                }

                String sendService = etSendService.getText().toString();
                if (!verifyServiceUUID(sendService)) {
                    Toast.makeText(BleServiceConfigActivity.this, getResources().getString(R.string.send_service_error), Toast.LENGTH_LONG).show();
                    return;
                }

                HJBleApplication.shareInstance().setDataMainService(mainService);
                HJBleApplication.shareInstance().setDataNotifyService(notifyService);
                HJBleApplication.shareInstance().setDataSendService(sendService);

                Toast.makeText(HJBleApplication.shareInstance().getApplicationContext(), getResources().getString(R.string.save_success), Toast.LENGTH_LONG).show();
                finish();

            }
        });

        etMainService = findViewById(R.id.et_main_service);
        etNotifyService = findViewById(R.id.et_notify_service);
        etSendService = findViewById(R.id.et_send_service);

        btnRecoveryDetault = findViewById(R.id.btn_recovery_default);
        btnRecoveryDetault.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                HJBleApplication.shareInstance().setDataMainService(BleConfig.Ble_Default_Data_Send_Service.getServiceID());
                HJBleApplication.shareInstance().setDataNotifyService(BleConfig.Ble_Default_Data_Receive_Service.getCharacteristicID());
                HJBleApplication.shareInstance().setDataSendService(BleConfig.Ble_Default_Data_Send_Service.getCharacteristicID());

                updateData();
            }
        });

        updateData();

    }

    // 校验服务id
    private boolean verifyServiceUUID(String serviceId) {
        String pattern = "^([0-9a-fA-F]{8})-([0-9a-fA-F]{4})-([0-9a-fA-F]{4})-([0-9a-fA-F]{4})-([0-9a-fA-F]{12})$";

        return Pattern.matches(pattern, serviceId);
    }


    // 更新数据
    public void updateData() {
        etMainService.setText(HJBleApplication.shareInstance().getDataMainService());
        etNotifyService.setText(HJBleApplication.shareInstance().getDataNotifyService());
        etSendService.setText(HJBleApplication.shareInstance().getDataSendService());
    }
}