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


    // 每次下发测试数据长度
    private RelativeLayout selecDataLenBtn;

    // 下发数据时间间隙
    private RelativeLayout selecGapTimeBtn;

    // 测试文件每包下发大小
    private RelativeLayout selecFilePerGroupBtn;

    // 测试文件每包下发大小
    private RelativeLayout selecFileIntervalPerPacketBtn;

    private RelativeLayout selectFilePathBtn;

    private Switch switchUseFileTest;
    private Switch switchSaveLog;

    // 选择模式
    private TextView modeTxt;
    // 选择字符
    private TextView charTxt;
    // 选择是否添加回车
    private TextView returnTxt;
    // 写方式
    private TextView writeTypeTxt;

    // 每次下发测试数据长度
    private TextView dataLenTxt;

    // 下发数据时间间隙
    private TextView gapTimeTxt;

    // 测试文件每包下发数
    private TextView filePerGroupTxt;
    private TextView fileIntervalPerPacketTxt;

    // 文件路径
    private TextView filePathTxt;

    private boolean isConfig;

    private BleDevice mBleDevice;
    private BleDeviceSession deviceSession;
    private Runnable removeDisconnectListener;

    @Override
    protected int getPageLayoutId() {
        return R.layout.activity_set;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (deviceSession == null || !deviceSession.isCurrent(mBleDevice) || !BleDeviceSession.isReady(mBleDevice)) {
            finish();
            return;
        }
        removeDisconnectListener = deviceSession.onDisconnected(() -> runOnUiThread(this::finish));
    }

    @Override
    protected void onPause() {
        if (removeDisconnectListener != null) removeDisconnectListener.run();
        removeDisconnectListener = null;
        super.onPause();
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
        if (mBleDevice == null) {
            finish();
            return;
        }
        deviceSession = BleDeviceSession.find(mBleDevice);
        if (deviceSession == null || !BleDeviceSession.isReady(mBleDevice)) { finish(); return; }

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

        dataLenTxt = findViewById(R.id.data_len_txt);
        gapTimeTxt = findViewById(R.id.gap_time_txt);

        filePerGroupTxt = findViewById(R.id.file_per_group_txt);
        fileIntervalPerPacketTxt = findViewById(R.id.file_interval_per_packet_txt);

        filePathTxt = findViewById(R.id.file_path_txt);
        switchUseFileTest = findViewById(R.id.switch_use_file_test);
        switchUseFileTest.setChecked(HJBleApplication.shareInstance().useFileTest());
        switchUseFileTest.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                HJBleApplication.shareInstance().setUseFileTest(isChecked);
            }
        });
        switchSaveLog = findViewById(R.id.switch_save_log);
        switchSaveLog.setChecked(HJBleApplication.shareInstance().isSaveLog());
        switchSaveLog.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                HJBleApplication.shareInstance().setSaveLog(isChecked);
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

        selecDataLenBtn = findViewById(R.id.data_len_layout);
        selecDataLenBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final EditText inputServer = new EditText(SetActivity.this);
                inputServer.setInputType(InputType.TYPE_CLASS_NUMBER);
                inputServer.setText(dataLenTxt.getText());
                AlertDialog.Builder builder = new AlertDialog.Builder(SetActivity.this);
                builder.setTitle(getResources().getString(R.string.total_length_of_test_data)).setView(inputServer)
                        .setNegativeButton(getResources().getString(R.string.cancel_btn), null);
                builder.setPositiveButton(getResources().getString(R.string.sure_btn), new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {
                        Integer value = parseNumber(inputServer, 1);
                        if (value == null) return;
                        dataLenTxt.setText(String.valueOf(value));
                        HJBleApplication.shareInstance().setTestDataLen(value);
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
                        Integer value = parseNumber(inputServer, 0);
                        if (value == null) return;
                        gapTimeTxt.setText(String.valueOf(value));
                        HJBleApplication.shareInstance().setTestGapTime(value);
                    }
                });
                builder.show();
            }
        });

        selecFilePerGroupBtn = findViewById(R.id.file_per_group_layout);
        selecFilePerGroupBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final EditText inputServer = new EditText(SetActivity.this);
                inputServer.setInputType(InputType.TYPE_CLASS_NUMBER);
                inputServer.setText(filePerGroupTxt.getText());
                AlertDialog.Builder builder = new AlertDialog.Builder(SetActivity.this);
                builder.setTitle(getResources().getString(R.string.test_file_send_size_per_packet)).setView(inputServer)
                        .setNegativeButton(getResources().getString(R.string.cancel_btn), null);
                builder.setPositiveButton(getResources().getString(R.string.sure_btn), new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {
                        Integer value = parseNumber(inputServer, 1);
                        if (value == null) return;
                        filePerGroupTxt.setText(String.valueOf(value));
                        HJBleApplication.shareInstance().setTestFilePerGroupLen(value);
                    }
                });
                builder.show();
            }
        });

        selecFileIntervalPerPacketBtn = findViewById(R.id.file_interval_per_packet_layout);
        selecFileIntervalPerPacketBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final EditText inputServer = new EditText(SetActivity.this);
                inputServer.setInputType(InputType.TYPE_CLASS_NUMBER);
                inputServer.setText(fileIntervalPerPacketTxt.getText());
                AlertDialog.Builder builder = new AlertDialog.Builder(SetActivity.this);
                builder.setTitle(getResources().getString(R.string.test_file_interval_per_packet)).setView(inputServer)
                        .setNegativeButton(getResources().getString(R.string.cancel_btn), null);
                builder.setPositiveButton(getResources().getString(R.string.sure_btn), new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {
                        Integer value = parseNumber(inputServer, 0);
                        if (value == null) return;
                        fileIntervalPerPacketTxt.setText(String.valueOf(value));
                        HJBleApplication.shareInstance().setTestFileIntervalPerPacket(value);
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
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
                startActivityForResult(intent, SELECT_FILE_REQ);
            }
        });

        if (deviceSession.isConfigMode()) {
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

        dataLenTxt.setText("" + HJBleApplication.shareInstance().testDataLen());
        filePerGroupTxt.setText("" + HJBleApplication.shareInstance().testFilePerGroupLen());
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
                        deviceSession.setConfigMode(false);
                        return true;
                    case R.id.config_mode:
                        modeTxt.setText(getResources().getString(R.string.config_mode));
                        deviceSession.setConfigMode(true);
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
                if (data == null || data.getData() == null) return;
                final Uri uri = data.getData();
//                final String path = FileInfoUtils.getPath(this, uri);
                String fileName = getFileName(uri);
                filePathTxt.setText(fileName);
                // 可使关机后也可以持续使用
                try {
                    if ((data.getFlags() & Intent.FLAG_GRANT_READ_URI_PERMISSION) == 0) {
                        throw new SecurityException("Read permission was not granted");
                    }
                    getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                } catch (SecurityException e) {
                    Toast.makeText(this, R.string.file_permission_failed, Toast.LENGTH_LONG).show();
                    return;
                }
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
                    int nameColumn = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameColumn >= 0) result = cursor.getString(nameColumn);
                }
            } finally {
                if (cursor != null) cursor.close();
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

    private Integer parseNumber(EditText input, int minValue) {
        try {
            int value = Integer.parseInt(input.getText().toString().trim());
            if (value < minValue) throw new NumberFormatException();
            return value;
        } catch (NumberFormatException e) {
            Toast.makeText(this, getString(R.string.invalid_number, minValue), Toast.LENGTH_SHORT).show();
            return null;
        }
    }
}
