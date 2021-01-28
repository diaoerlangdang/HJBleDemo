package com.hongjia.hjbledemo;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;

import com.wise.wisekit.activity.BaseActivity;

public class SetActivity extends BaseActivity {

    // 是否支持配置
    public static final String EXTRAS_SET_IS_CONFIG = "DEVICE_IS_CONFIG";

    // 选择模式
    private LinearLayout selectModeBtn;
    // 选择字符
    private LinearLayout selectCharBtn;
    // 选择是否添加回车
    private LinearLayout selectAddReturnBtn;
    // 选择写方式
    private LinearLayout selectWriteTypeBtn;

    // 每包数据长度
    private RelativeLayout selecGroupLenBtn;

    // 每次下发测试数据长度
    private RelativeLayout selecDataLenBtn;

    // 下发数据时间间隙
    private RelativeLayout selecGapTimeBtn;

    // 选择模式
    private TextView modeTxt;
    // 选择字符
    private TextView charTxt;
    // 选择是否添加回车
    private TextView returnTxt;
    // 写方式
    private TextView writeTypeTxt;

    // 每包数据长度
    private TextView groupLenTxt;

    // 每次下发测试数据长度
    private TextView dataLenTxt;

    // 下发数据时间间隙
    private TextView gapTimeTxt;

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
        returnTxt = findViewById(R.id.return_txt);
        writeTypeTxt = findViewById(R.id.write_type_txt);

        groupLenTxt = findViewById(R.id.group_len_txt);
        dataLenTxt = findViewById(R.id.data_len_txt);
        gapTimeTxt = findViewById(R.id.gap_time_txt);

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

        selectAddReturnBtn = findViewById(R.id.select_return);
        selectAddReturnBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showSelectReturnPopupMenu();
            }
        });

        selectWriteTypeBtn = findViewById(R.id.select_write_type);
        selectWriteTypeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showSelectWriteTypePopupMenu();
            }
        });

        selecGroupLenBtn = findViewById(R.id.group_len_layout);
        selecGroupLenBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final EditText inputServer = new EditText(SetActivity.this);
                inputServer.setInputType(InputType.TYPE_CLASS_NUMBER);
                inputServer.setText(groupLenTxt.getText());
                AlertDialog.Builder builder = new AlertDialog.Builder(SetActivity.this);
                builder.setTitle("设置每包数据长度").setView(inputServer)
                        .setNegativeButton("取消", null);
                builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {
                        String info = inputServer.getText().toString();
                        groupLenTxt.setText(info);
                        HJBleApplication.shareInstance().setGroupLen(Integer.parseInt(info));
                    }
                });
                builder.show();
            }
        });

        selecDataLenBtn = findViewById(R.id.data_len_layout);
        selecDataLenBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final EditText inputServer = new EditText(SetActivity.this);
                inputServer.setInputType(InputType.TYPE_CLASS_NUMBER);
                inputServer.setText(dataLenTxt.getText());
                AlertDialog.Builder builder = new AlertDialog.Builder(SetActivity.this);
                builder.setTitle("设置每次下发测试数据长度").setView(inputServer)
                        .setNegativeButton("取消", null);
                builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {
                        String info = inputServer.getText().toString();
                        dataLenTxt.setText(info);
                        HJBleApplication.shareInstance().setTestDataLen(Integer.parseInt(info));
                    }
                });
                builder.show();
            }
        });

        selecGapTimeBtn = findViewById(R.id.gap_time_layout);
        selecGapTimeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final EditText inputServer = new EditText(SetActivity.this);
                inputServer.setInputType(InputType.TYPE_CLASS_NUMBER);
                inputServer.setText(gapTimeTxt.getText());
                AlertDialog.Builder builder = new AlertDialog.Builder(SetActivity.this);
                builder.setTitle("设置下发数据时间间隙(ms)").setView(inputServer)
                        .setNegativeButton("取消", null);
                builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {
                        String info = inputServer.getText().toString();
                        gapTimeTxt.setText(info);
                        HJBleApplication.shareInstance().setTestGapTime(Integer.parseInt(info));
                    }
                });
                builder.show();
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

        if (HJBleApplication.shareInstance().isAddReturn()) {
            returnTxt.setText("是");
        }
        else {
            returnTxt.setText("否");
        }

        if (HJBleApplication.shareInstance().isWriteTypeResponse()) {
            writeTypeTxt.setText("是");
        }
        else {
            writeTypeTxt.setText("否");
        }

        groupLenTxt.setText("" + HJBleApplication.shareInstance().groupLen());
        dataLenTxt.setText("" + HJBleApplication.shareInstance().testDataLen());
        gapTimeTxt.setText("" + HJBleApplication.shareInstance().testGapTime());

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

    private void showSelectReturnPopupMenu(){
        PopupMenu popupMenu = new PopupMenu(this,selectAddReturnBtn);
        popupMenu.inflate(R.menu.menu_select_return);

        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                switch (menuItem.getItemId()){
                    case R.id.yes:
                        returnTxt.setText("是");
                        HJBleApplication.shareInstance().setAddReturn(true);
                        return true;
                    case R.id.no:
                        returnTxt.setText("否");
                        HJBleApplication.shareInstance().setAddReturn(false);
                        return true;

                    default:
                        //do nothing
                }

                return false;
            }
        });
        popupMenu.show();
    }

    private void showSelectWriteTypePopupMenu(){
        PopupMenu popupMenu = new PopupMenu(this,selectWriteTypeBtn);
        popupMenu.inflate(R.menu.menu_select_write_type);

        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                switch (menuItem.getItemId()){
                    case R.id.yes:
                        writeTypeTxt.setText("是");
                        HJBleApplication.shareInstance().setWriteTypeResponse(true);
                        return true;
                    case R.id.no:
                        writeTypeTxt.setText("否");
                        HJBleApplication.shareInstance().setWriteTypeResponse(false);
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
