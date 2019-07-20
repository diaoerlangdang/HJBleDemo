package com.hongjia.hjbledemo;

import android.app.Activity;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.os.Handler;
import android.view.Window;
import android.view.WindowManager;

import com.githang.statusbar.StatusBarCompat;

public class LaunchActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        StatusBarCompat.setStatusBarColor(this, getResources().getColor(R.color.colorBackground, null), true);

        getWindow().setFormat(PixelFormat.RGBA_8888);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DITHER);

        setContentView(R.layout.activity_launch);

        new Handler(){
            public void handleMessage(android.os.Message msg) {
                Intent i = new Intent(LaunchActivity.this, ScanBleActivity.class);
                startActivity(i);
                finish();
            };
        }.sendEmptyMessageDelayed(0, 2000);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }
}
