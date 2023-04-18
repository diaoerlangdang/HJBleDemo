package com.hongjia.hjbledemo;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.wise.wisekit.activity.BaseActivity;

public class AppConfigSetActivity extends BaseActivity {

    TextView tvVersion;
    RelativeLayout rlDataService; // 数据服务
    Switch switchFilter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected int getPageLayoutId() {
        return R.layout.activity_app_config_set;
    }

    @Override
    protected void initView() {
        super.initView();

        setTitle(getResources().getString(R.string.software_setup));

        switchFilter = findViewById(R.id.switch_filter);
        switchFilter.setChecked(HJBleApplication.shareInstance().isScanFilter());
        switchFilter.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                HJBleApplication.shareInstance().setScanFilter(b);
            }
        });

        rlDataService = findViewById(R.id.rl_data_service);
        rlDataService.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final Intent intent = new Intent(AppConfigSetActivity.this, BleServiceConfigActivity.class);
                AppConfigSetActivity.this.startActivity(intent);
            }
        });


        // 版本信息
        tvVersion = findViewById(R.id.tv_version);
        tvVersion.setText("V " + getVersionName(AppConfigSetActivity.this));
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
}
