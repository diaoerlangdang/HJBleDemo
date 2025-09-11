package com.hongjia.hjbledemo;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.hongjia.hjbledemo.adapter.CustomCommandAdapter;
import com.hongjia.hjbledemo.bean.CustomCommand;

import java.util.List;

/**
 * 自定义指令蒙版管理器
 * 提供全屏蒙版效果，右半部分显示指令列表
 */
public class CustomCommandOverlayManager {
    
    private Activity activity;
    private LinearLayout overlayLayout;
    private CustomCommandAdapter adapter;
    private CustomCommandManager commandManager;
    private OnCommandExecuteListener onCommandExecuteListener;
    private float widthPercentage = 0.6f; // 默认右侧区域占屏幕宽度的60%
    
    public interface OnCommandExecuteListener {
        void onCommandExecute(CustomCommand command);
    }
    
    public CustomCommandOverlayManager(Activity activity) {
        this.activity = activity;
        this.commandManager = CustomCommandManager.getInstance(activity);
        initOverlay();
    }
    
    /**
     * 构造函数，支持自定义右侧区域宽度百分比
     * @param activity Activity实例
     * @param widthPercentage 右侧区域占屏幕宽度的百分比 (0.1 - 0.9)
     */
    public CustomCommandOverlayManager(Activity activity, float widthPercentage) {
        this.activity = activity;
        this.commandManager = CustomCommandManager.getInstance(activity);
        this.widthPercentage = Math.max(0.1f, Math.min(0.9f, widthPercentage)); // 限制在10%-90%之间
        initOverlay();
    }
    
    /**
     * 初始化蒙版布局
     */
    private void initOverlay() {
        // 获取屏幕尺寸
        DisplayMetrics displayMetrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int screenWidth = displayMetrics.widthPixels;
        int screenHeight = displayMetrics.heightPixels;
        
        // 创建主蒙版布局
        overlayLayout = new LinearLayout(activity);
        overlayLayout.setLayoutParams(new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 
            ViewGroup.LayoutParams.MATCH_PARENT
        ));
        overlayLayout.setOrientation(LinearLayout.HORIZONTAL);
        overlayLayout.setBackgroundColor(Color.parseColor("#80000000")); // 半透明黑色
        overlayLayout.setVisibility(View.GONE);
        
        // 计算右侧区域宽度
        int rightAreaWidth = (int) (screenWidth * widthPercentage);
        int leftAreaWidth = screenWidth - rightAreaWidth;
        
        // 创建左侧空白区域（点击关闭）
        LinearLayout leftArea = new LinearLayout(activity);
        LinearLayout.LayoutParams leftParams = new LinearLayout.LayoutParams(
            leftAreaWidth, screenHeight
        );
        leftArea.setLayoutParams(leftParams);
        leftArea.setOnClickListener(v -> hideOverlay());
        
        // 创建右侧指令列表区域
        LinearLayout rightArea = createRightArea(rightAreaWidth, screenHeight);
        
        overlayLayout.addView(leftArea);
        overlayLayout.addView(rightArea);
    }
    
    /**
     * 创建右侧指令列表区域
     */
    private LinearLayout createRightArea(int width, int height) {
        LinearLayout rightArea = new LinearLayout(activity);
        LinearLayout.LayoutParams rightParams = new LinearLayout.LayoutParams(width, height);
        rightArea.setLayoutParams(rightParams);
        rightArea.setOrientation(LinearLayout.VERTICAL);
        rightArea.setBackgroundColor(Color.WHITE);
        rightArea.setPadding(20, 20, 20, 20);
        
        // 标题
        TextView titleText = new TextView(activity);
        titleText.setText("自定义指令");
        titleText.setTextSize(18);
        titleText.setTextColor(Color.BLACK);
        titleText.setGravity(Gravity.CENTER);
        titleText.setPadding(0, 0, 0, 20);
        
        // 添加按钮
        Button addButton = new Button(activity);
        addButton.setText("添加指令");
        addButton.setTextColor(Color.WHITE);
        addButton.setBackgroundResource(R.drawable.button_background); // 使用项目统一的按钮背景
        addButton.setPadding(20, 10, 20, 10);
        LinearLayout.LayoutParams addButtonParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        addButtonParams.setMargins(0, 0, 0, 20);
        addButton.setLayoutParams(addButtonParams);
        addButton.setOnClickListener(v -> showAddCommandDialog());
        
        // 指令列表
        RecyclerView recyclerView = new RecyclerView(activity);
        recyclerView.setLayoutManager(new LinearLayoutManager(activity));
        LinearLayout.LayoutParams recyclerParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            0,
            1.0f
        );
        recyclerView.setLayoutParams(recyclerParams);
        
        // 初始化适配器
        adapter = new CustomCommandAdapter(activity, commandManager.getAllCommands());
        adapter.setOnItemClickListener(command -> {
            if (onCommandExecuteListener != null) {
                onCommandExecuteListener.onCommandExecute(command);
            }
            hideOverlay();
        });
        adapter.setOnEditClickListener(this::showEditCommandDialog);
        adapter.setOnDeleteClickListener(this::deleteCommand);
        recyclerView.setAdapter(adapter);
        
        rightArea.addView(titleText);
        rightArea.addView(addButton);
        rightArea.addView(recyclerView);
        
        return rightArea;
    }
    
    /**
     * 显示蒙版
     */
    public void showOverlay() {
        if (overlayLayout.getParent() == null) {
            ViewGroup rootView = (ViewGroup) activity.findViewById(android.R.id.content);
            rootView.addView(overlayLayout);
        }
        
        // 刷新数据
        List<CustomCommand> commands = commandManager.getAllCommands();
        adapter.updateCommands(commands);
        
        overlayLayout.setVisibility(View.VISIBLE);
    }
    
    /**
     * 隐藏蒙版
     */
    public void hideOverlay() {
        if (overlayLayout != null) {
            overlayLayout.setVisibility(View.GONE);
            if (overlayLayout.getParent() != null) {
                ViewGroup parent = (ViewGroup) overlayLayout.getParent();
                parent.removeView(overlayLayout);
            }
        }
    }
    
    /**
     * 显示添加指令对话框
     */
    private void showAddCommandDialog() {
        CustomCommandDialog dialog = new CustomCommandDialog(activity, null);
        dialog.setOnSaveListener(command -> {
            if (commandManager.addCommand(command)) {
                List<CustomCommand> commands = commandManager.getAllCommands();
                adapter.updateCommands(commands);
            }
        });
        dialog.show();
    }
    
    /**
     * 显示编辑指令对话框
     */
    private void showEditCommandDialog(CustomCommand command) {
        CustomCommandDialog dialog = new CustomCommandDialog(activity, command);
        dialog.setOnSaveListener(updatedCommand -> {
            if (commandManager.updateCommand(updatedCommand)) {
                List<CustomCommand> commands = commandManager.getAllCommands();
                adapter.updateCommands(commands);
            }
        });
        dialog.show();
    }
    
    /**
     * 删除指令
     */
    private void deleteCommand(CustomCommand command) {
        if (commandManager.deleteCommand(command.getId())) {
            List<CustomCommand> commands = commandManager.getAllCommands();
            adapter.updateCommands(commands);
        }
    }
    
    /**
     * 设置右侧区域宽度百分比
     * @param widthPercentage 宽度百分比 (0.1 - 0.9)
     */
    public void setWidthPercentage(float widthPercentage) {
        this.widthPercentage = Math.max(0.1f, Math.min(0.9f, widthPercentage));
        // 重新初始化布局
        initOverlay();
    }
    
    /**
     * 获取当前右侧区域宽度百分比
     */
    public float getWidthPercentage() {
        return widthPercentage;
    }
    
    /**
     * 设置指令执行监听器
     */
    public void setOnCommandExecuteListener(OnCommandExecuteListener listener) {
        this.onCommandExecuteListener = listener;
    }
    
    /**
     * 销毁资源
     */
    public void destroy() {
        hideOverlay();
        if (overlayLayout != null && overlayLayout.getParent() != null) {
            ViewGroup parent = (ViewGroup) overlayLayout.getParent();
            parent.removeView(overlayLayout);
        }
        overlayLayout = null;
        adapter = null;
        commandManager = null;
        onCommandExecuteListener = null;
    }
}