package com.hongjia.hjbledemo;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.PopupMenu;

import com.wise.wisekit.activity.BaseActivity;

public class SetActivity extends BaseActivity {

    // 是否支持配置
    public static final String EXTRAS_SET_IS_CONFIG = "DEVICE_IS_CONFIG";

    // 选择模式
    private LinearLayout selectModeBtn;
    // 选择字符
    private LinearLayout selectCharBtn;

    // 选择模式
    private TextView modeTxt;
    // 选择字符
    private TextView charTxt;

    private boolean isConfig;

    @Override
    protected int getPageLayoutId() {
        return R.layout.activity_set;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void initView() {
        super.initView();

        final Intent intent = getIntent();
        isConfig = intent.getBooleanExtra(EXTRAS_SET_IS_CONFIG, false);

        setTitle("设置");

        modeTxt = findViewById(R.id.mode_txt);
        charTxt = findViewById(R.id.char_txt);

        selectCharBtn = findViewById(R.id.select_char);
        selectCharBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showSelectCharPopupMenu();
            }
        });

        selectModeBtn = findViewById(R.id.select_mode);
        selectModeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showSelectModePopupMenu();
            }
        });

        if (HJBleApplication.shareInstance().isBleConfig()) {
            modeTxt.setText("配置模式");
        }
        else {
            modeTxt.setText("数据模式");
        }

        if (HJBleApplication.shareInstance().isBleHex()) {
            charTxt.setText("Hex");
        }
        else {
            charTxt.setText("ASCII");
        }

        // 不支持配置
        if (!isConfig) {
            findViewById(R.id.config_layout).setVisibility(View.GONE);
        }
        else {
            findViewById(R.id.config_layout).setVisibility(View.VISIBLE);
        }

    }

    private void showSelectModePopupMenu(){
        PopupMenu popupMenu = new PopupMenu(this,selectModeBtn);
        popupMenu.inflate(R.menu.menu_select_mode);

        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                switch (menuItem.getItemId()){
                    case R.id.data_mode:
                        modeTxt.setText("数据模式");
                        HJBleApplication.shareInstance().setBleConfig(false);
                        return true;
                    case R.id.config_mode:
                        modeTxt.setText("配置模式");
                        HJBleApplication.shareInstance().setBleConfig(true);
                        return true;

                    default:
                        //do nothing
                }

                return false;
            }
        });
        popupMenu.show();
    }

    private void showSelectCharPopupMenu(){
        PopupMenu popupMenu = new PopupMenu(this,selectCharBtn);
        popupMenu.inflate(R.menu.menu_select_char);

        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                switch (menuItem.getItemId()){
                    case R.id.ascii:
                        charTxt.setText("ASCII");
                        HJBleApplication.shareInstance().setBleHex(false);
                        return true;
                    case R.id.hex:
                        charTxt.setText("Hex");
                        HJBleApplication.shareInstance().setBleHex(true);
                        return true;

                    default:
                        //do nothing
                }

                return false;
            }
        });
        popupMenu.show();
    }
}
