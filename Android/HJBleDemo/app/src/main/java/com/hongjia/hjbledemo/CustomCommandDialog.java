package com.hongjia.hjbledemo;

import android.app.Dialog;
import android.content.Context;
import android.text.TextUtils;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.hongjia.hjbledemo.bean.CustomCommand;

import java.util.UUID;

/**
 * 自定义指令对话框
 */
public class CustomCommandDialog extends Dialog {
    
    private EditText etCommandName;
    private EditText etCommand;
    private Spinner spinnerType;
    private Button btnSave;
    private Button btnCancel;
    
    private CustomCommand currentCommand;
    private OnSaveListener onSaveListener;
    
    public interface OnSaveListener {
        void onSave(CustomCommand command);
    }
    
    public CustomCommandDialog(Context context, CustomCommand command) {
        super(context);
        this.currentCommand = command;
        initView();
        initData();
        
        // 设置对话框尺寸
        getWindow().setLayout(
            (int) (context.getResources().getDisplayMetrics().widthPixels * 0.85), // 宽度为屏幕的85%
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        );
    }
    
    private void initView() {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_command, null);
        setContentView(view);
        
        etCommandName = view.findViewById(R.id.et_command_name);
        etCommand = view.findViewById(R.id.et_command);
        spinnerType = view.findViewById(R.id.spinner_type);
        btnSave = view.findViewById(R.id.btn_save);
        btnCancel = view.findViewById(R.id.btn_cancel);
        
        // 设置类型选择器 - 注意：数组索引要与CustomCommand中的类型常量对应
        // CustomCommand.TYPE_HEX = 0, CustomCommand.TYPE_ASCII = 1
        String[] types = {"HEX", "ASCII"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), 
            android.R.layout.simple_spinner_item, types);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerType.setAdapter(adapter);
        
        // 设置按钮事件
        btnSave.setOnClickListener(v -> saveCommand());
        btnCancel.setOnClickListener(v -> dismiss());
        
        // 为类型选择器添加监听，根据类型动态调整输入提示
        spinnerType.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                updateInputHints(position);
            }
            
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
        
        setTitle(currentCommand == null ? getContext().getString(R.string.add_command_title) : getContext().getString(R.string.edit_command_title));
    }
    
    private void initData() {
        if (currentCommand != null) {
            etCommandName.setText(currentCommand.getName());
            etCommand.setText(currentCommand.getCommand());
            spinnerType.setSelection(currentCommand.getType());
        }
        // 初始化时更新输入提示
        updateInputHints(spinnerType.getSelectedItemPosition());
    }
    
    /**
     * 根据选中的指令类型更新输入提示
     * 注意：Spinner的position和CustomCommand的type常量对应
     * position 0 = TYPE_HEX, position 1 = TYPE_ASCII
     */
    private void updateInputHints(int spinnerPosition) {
        if (spinnerPosition == CustomCommand.TYPE_HEX) { // position 0 = HEX
            etCommand.setHint(getContext().getString(R.string.hex_input_hint));
            // 为HEX类型添加输入过滤器
            etCommand.setFilters(new InputFilter[]{new HexInputFilter(), new InputFilter.LengthFilter(1000)});
        } else { // position 1 = ASCII
            etCommand.setHint(getContext().getString(R.string.ascii_input_hint));
            // 移除输入过滤器
            etCommand.setFilters(new InputFilter[]{new InputFilter.LengthFilter(1000)});
        }
    }
    
    private void saveCommand() {
        String name = etCommandName.getText().toString().trim();
        String command = etCommand.getText().toString().trim();
        int spinnerPosition = spinnerType.getSelectedItemPosition();
        // Spinner的position就是CustomCommand的type值
        int type = spinnerPosition;
        
        if (TextUtils.isEmpty(name)) {
            Toast.makeText(getContext(), R.string.error_empty_command_name, Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (TextUtils.isEmpty(command)) {
            Toast.makeText(getContext(), R.string.error_empty_command_content, Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 如果是HEX类型，进行严格验证
        if (type == CustomCommand.TYPE_HEX) {
            String validationResult = validateHexCommand(command);
            if (validationResult != null) {
                Toast.makeText(getContext(), validationResult, Toast.LENGTH_SHORT).show();
                return;
            }
        }
        
        CustomCommand saveCommand;
        if (currentCommand == null) {
            // 新建指令
            saveCommand = new CustomCommand(name, command, type);
        } else {
            // 编辑指令
            saveCommand = currentCommand;
            saveCommand.setName(name);
            saveCommand.setCommand(command);
            saveCommand.setType(type);
        }
        
        if (onSaveListener != null) {
            onSaveListener.onSave(saveCommand);
        }
        
        dismiss();
    }
    
    /**
     * 验证HEX格式指令
     * @param hex HEX字符串
     * @return 如果验证通过返回null，否则返回错误信息
     */
    private String validateHexCommand(String hex) {
        // 移除所有空格和分隔符
        hex = hex.replaceAll("[\\s\\-:]", "");
        
        // 检查是否为空
        if (hex.isEmpty()) {
            return getContext().getString(R.string.error_hex_empty);
        }
        
        // 检查长度是否为偶数
        if (hex.length() % 2 != 0) {
            return getContext().getString(R.string.error_hex_length);
        }
        
        // 检查是否只包含0-9和a-f（大小写不敏感）
        if (!hex.matches("^[0-9A-Fa-f]+$")) {
            return getContext().getString(R.string.error_hex_invalid_chars);
        }
        
        // 检查长度限制（避免过长的指令）
        if (hex.length() > 1000) {
            return getContext().getString(R.string.error_hex_too_long);
        }
        
        return null; // 验证通过
    }
    
    public void setOnSaveListener(OnSaveListener listener) {
        this.onSaveListener = listener;
    }
    
    /**
     * HEX输入过滤器，只允许0-9、A-F、a-f的字符
     */
    private static class HexInputFilter implements InputFilter {
        @Override
        public CharSequence filter(CharSequence source, int start, int end,
                                   Spanned dest, int dstart, int dend) {
            StringBuilder builder = new StringBuilder();
            for (int i = start; i < end; i++) {
                char c = source.charAt(i);
                // 允许0-9、A-F、a-f和空格、分隔符
                if ((c >= '0' && c <= '9') || 
                    (c >= 'A' && c <= 'F') || 
                    (c >= 'a' && c <= 'f') ||
                    c == ' ' || c == '-' || c == ':') {
                    builder.append(c);
                }
            }
            // 如果没有有效字符，返回空字符串
            boolean allInvalid = builder.length() == 0 && (end - start) > 0;
            if (allInvalid) {
                return "";
            }
            // 如果全部字符都有效，返回null表示使用原始输入
            if (builder.length() == (end - start)) {
                return null;
            }
            return builder.toString();
        }
    }
}