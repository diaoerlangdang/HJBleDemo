package com.hongjia.hjbledemo;

import android.bluetooth.BluetoothGatt;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.InputType;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;

import com.clj.fastble.BleManager;
import com.clj.fastble.data.BleDevice;
import com.wise.ble.WiseBluetoothLe;
import com.wise.wisekit.activity.BaseActivity;

import java.io.File;

public class SetActivity extends BaseActivity {

    // 是否支持配置
    public static final String EXTRAS_SET_IS_CONFIG = "DEVICE_IS_CONFIG";

    public static final String EXTRAS_DEVICE = "DEVICE";

    private static final int SELECT_FILE_REQ = 1001;
    private static final int SELECT_INIT_FILE_REQ = 1002;

    // 高速模式
    private Switch swithHighRate;

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

    private RelativeLayout selectFilePathBtn;

    private Switch switchUseFileTest;

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

    // 文件路径
    private TextView filePathTxt;

    private boolean isConfig;

    private BleDevice mBleDevice;

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
        mBleDevice = intent.getParcelableExtra(EXTRAS_DEVICE);

        setTitle(getResources().getString(R.string.setting_title));

        swithHighRate = findViewById(R.id.switch_high_rate);
        if (Build.VERSION.SDK_INT >= 21) {
            boolean a = FastBleListener.getInstance().getHighRate(mBleDevice.getMac());
            swithHighRate.setChecked(FastBleListener.getInstance().getHighRate(mBleDevice.getMac()));
            swithHighRate.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    if (b) {
                        if (!BleManager.getInstance().requestConnectionPriority(mBleDevice, BluetoothGatt.CONNECTION_PRIORITY_HIGH)) {
                            FastBleListener.getInstance().setHighRate(mBleDevice.getMac(), false);
                            swithHighRate.setChecked(false);
                            Toast.makeText(SetActivity.this, getResources().getString(R.string.failed_to_enable_high_speed_mode), Toast.LENGTH_LONG).show();
                        } else {
                            FastBleListener.getInstance().setHighRate(mBleDevice.getMac(), true);
                        }
                    } else {
                        if (!BleManager.getInstance().requestConnectionPriority(mBleDevice, BluetoothGatt.CONNECTION_PRIORITY_BALANCED)) {
                            FastBleListener.getInstance().setHighRate(mBleDevice.getMac(), true);
                            swithHighRate.setChecked(true);
                            Toast.makeText(SetActivity.this, getResources().getString(R.string.failed_to_disable_high_speed_mode), Toast.LENGTH_LONG).show();
                        } else {
                            FastBleListener.getInstance().setHighRate(mBleDevice.getMac(), false);
                        }
                    }
                }
            });
        } else {
            swithHighRate.setVisibility(View.GONE);
        }
        modeTxt = findViewById(R.id.mode_txt);
        charTxt = findViewById(R.id.char_txt);
        returnTxt = findViewById(R.id.return_txt);
        writeTypeTxt = findViewById(R.id.write_type_txt);

        groupLenTxt = findViewById(R.id.group_len_txt);
        dataLenTxt = findViewById(R.id.data_len_txt);
        gapTimeTxt = findViewById(R.id.gap_time_txt);

        filePathTxt = findViewById(R.id.file_path_txt);
        switchUseFileTest = findViewById(R.id.switch_use_file_test);
        switchUseFileTest.setChecked(HJBleApplication.shareInstance().useFileTest());
        switchUseFileTest.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                HJBleApplication.shareInstance().setUseFileTest(isChecked);
            }
        });

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
                builder.setTitle(getResources().getString(R.string.data_length_of_each_packet_title)).setView(inputServer)
                        .setNegativeButton(getResources().getString(R.string.cancel_btn), null);
                builder.setPositiveButton(getResources().getString(R.string.sure_btn), new DialogInterface.OnClickListener() {

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
                builder.setTitle(getResources().getString(R.string.length_of_test_data_to_be_delivered_each_time)).setView(inputServer)
                        .setNegativeButton(getResources().getString(R.string.cancel_btn), null);
                builder.setPositiveButton(getResources().getString(R.string.sure_btn), new DialogInterface.OnClickListener() {

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
                builder.setTitle(getResources().getString(R.string.data_delivery_interval)).setView(inputServer)
                        .setNegativeButton(getResources().getString(R.string.cancel_btn), null);
                builder.setPositiveButton(getResources().getString(R.string.sure_btn), new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {
                        String info = inputServer.getText().toString();
                        gapTimeTxt.setText(info);
                        HJBleApplication.shareInstance().setTestGapTime(Integer.parseInt(info));
                    }
                });
                builder.show();
            }
        });

        selectFilePathBtn = findViewById(R.id.file_path_layout);
        selectFilePathBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
//                intent.setType("application/hj");
                intent.setType("*/*");
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(intent, SELECT_FILE_REQ);
            }
        });

        if (HJBleApplication.shareInstance().isBleConfig()) {
            modeTxt.setText(getResources().getString(R.string.config_mode));
        }
        else {
            modeTxt.setText(getResources().getString(R.string.data_mode));
        }

        if (HJBleApplication.shareInstance().isBleHex()) {
            charTxt.setText("Hex");
        }
        else {
            charTxt.setText("ASCII");
        }

        if (HJBleApplication.shareInstance().isAddReturn()) {
            returnTxt.setText(getResources().getString(R.string.yes));
        }
        else {
            returnTxt.setText(getResources().getString(R.string.no));
        }

        if (HJBleApplication.shareInstance().isWriteTypeResponse()) {
            writeTypeTxt.setText(getResources().getString(R.string.yes));
        }
        else {
            writeTypeTxt.setText(getResources().getString(R.string.no));
        }

        groupLenTxt.setText("" + HJBleApplication.shareInstance().getGroupLen());
        dataLenTxt.setText("" + HJBleApplication.shareInstance().testDataLen());
        gapTimeTxt.setText("" + HJBleApplication.shareInstance().testGapTime());
        filePathTxt.setText(getFileName(HJBleApplication.shareInstance().getTestFileUri()));

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
                        modeTxt.setText(getResources().getString(R.string.data_mode));
                        HJBleApplication.shareInstance().setBleConfig(false);
                        return true;
                    case R.id.config_mode:
                        modeTxt.setText(getResources().getString(R.string.config_mode));
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
                        returnTxt.setText(getResources().getString(R.string.yes));
                        HJBleApplication.shareInstance().setAddReturn(true);
                        return true;
                    case R.id.no:
                        returnTxt.setText(getResources().getString(R.string.no));
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
                        writeTypeTxt.setText(getResources().getString(R.string.yes));
                        HJBleApplication.shareInstance().setWriteTypeResponse(true);
                        return true;
                    case R.id.no:
                        writeTypeTxt.setText(getResources().getString(R.string.no));
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

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK)
            return;

        switch (requestCode) {
            case SELECT_FILE_REQ: {
                final Uri uri = data.getData();
//                final String path = FileInfoUtils.getPath(this, uri);
                String fileName = getFileName(uri);
                filePathTxt.setText(fileName);
                // 可使关机后也可以持续使用
                getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
//                HJBleApplication.shareInstance().setTestFilePath(fileName);
                HJBleApplication.shareInstance().setTestFileUri(uri);
                break;
            }
            case SELECT_INIT_FILE_REQ: {
//                mInitFilePath = null;
//                mInitFileStreamUri = null;
//
//                // and read new one
//                final Uri uri = data.getData();
//                /*
//                 * The URI returned from application may be in 'file' or 'content' schema. 'File' schema allows us to create a File object and read details from if
//                 * directly. Data from 'Content' schema must be read by Content Provider. To do that we are using a Loader.
//                 */
//                if (uri.getScheme().equals("file")) {
//                    // the direct path to the file has been returned
//                    mInitFilePath = uri.getPath();
//                    mFileStatusView.setText(R.string.dfu_file_status_ok_with_init);
//                } else if (uri.getScheme().equals("content")) {
//                    // an Uri has been returned
//                    mInitFileStreamUri = uri;
//                    // if application returned Uri for streaming, let's us it. Does it works?
//                    // FIXME both Uris works with Google Drive app. Why both? What's the difference? How about other apps like DropBox?
//                    final Bundle extras = data.getExtras();
//                    if (extras != null && extras.containsKey(Intent.EXTRA_STREAM))
//                        mInitFileStreamUri = extras.getParcelable(Intent.EXTRA_STREAM);
//                    mFileStatusView.setText(R.string.dfu_file_status_ok_with_init);
//                }
                break;
            }
            default:
                break;
        }
    }

    public String getFileName(Uri uri) {
        if (uri == null) return  "-";
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } finally {
                cursor.close();
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result != null ? result : "-";
    }
}
