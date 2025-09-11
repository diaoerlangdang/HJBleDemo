package com.hongjia.hjbledemo;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.hongjia.hjbledemo.bean.CustomCommand;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 自定义指令管理类
 * 负责自定义指令的本地缓存管理
 */
public class CustomCommandManager {
    
    private static final String PREFS_NAME = "custom_commands";
    private static final String KEY_COMMANDS = "commands";
    
    private static CustomCommandManager instance;
    private Context context;
    private SharedPreferences sharedPreferences;
    
    private CustomCommandManager(Context context) {
        this.context = context.getApplicationContext();
        this.sharedPreferences = this.context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
    
    public static synchronized CustomCommandManager getInstance(Context context) {
        if (instance == null) {
            instance = new CustomCommandManager(context);
        }
        return instance;
    }
    
    /**
     * 获取所有自定义指令
     */
    public List<CustomCommand> getAllCommands() {
        List<CustomCommand> commands = new ArrayList<>();
        String jsonString = sharedPreferences.getString(KEY_COMMANDS, "[]");
        
        try {
            JSONArray jsonArray = new JSONArray(jsonString);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                CustomCommand command = parseCommandFromJson(jsonObject);
                if (command != null) {
                    commands.add(command);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        
        return commands;
    }
    
    /**
     * 添加自定义指令
     */
    public boolean addCommand(CustomCommand command) {
        if (command == null || TextUtils.isEmpty(command.getName()) || TextUtils.isEmpty(command.getCommand())) {
            return false;
        }
        
        List<CustomCommand> commands = getAllCommands();
        commands.add(command);
        return saveCommands(commands);
    }
    
    /**
     * 更新自定义指令
     */
    public boolean updateCommand(CustomCommand command) {
        if (command == null || TextUtils.isEmpty(command.getId())) {
            return false;
        }
        
        List<CustomCommand> commands = getAllCommands();
        for (int i = 0; i < commands.size(); i++) {
            if (command.getId().equals(commands.get(i).getId())) {
                commands.set(i, command);
                return saveCommands(commands);
            }
        }
        
        return false;
    }
    
    /**
     * 删除自定义指令
     */
    public boolean deleteCommand(String commandId) {
        if (TextUtils.isEmpty(commandId)) {
            return false;
        }
        
        List<CustomCommand> commands = getAllCommands();
        for (int i = 0; i < commands.size(); i++) {
            if (commandId.equals(commands.get(i).getId())) {
                commands.remove(i);
                return saveCommands(commands);
            }
        }
        
        return false;
    }
    
    /**
     * 根据ID获取指令
     */
    public CustomCommand getCommandById(String commandId) {
        if (TextUtils.isEmpty(commandId)) {
            return null;
        }
        
        List<CustomCommand> commands = getAllCommands();
        for (CustomCommand command : commands) {
            if (commandId.equals(command.getId())) {
                return command;
            }
        }
        
        return null;
    }
    
    /**
     * 清空所有指令
     */
    public boolean clearAllCommands() {
        return saveCommands(new ArrayList<CustomCommand>());
    }
    
    /**
     * 保存指令列表到本地
     */
    private boolean saveCommands(List<CustomCommand> commands) {
        JSONArray jsonArray = new JSONArray();
        for (CustomCommand command : commands) {
            JSONObject jsonObject = parseCommandToJson(command);
            if (jsonObject != null) {
                jsonArray.put(jsonObject);
            }
        }

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_COMMANDS, jsonArray.toString());
        return editor.commit();

    }
    
    /**
     * 将CustomCommand转换为JSON对象
     */
    private JSONObject parseCommandToJson(CustomCommand command) {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("id", command.getId());
            jsonObject.put("name", command.getName());
            jsonObject.put("command", command.getCommand());
            jsonObject.put("type", command.getType());
            jsonObject.put("createTime", command.getCreateTime());
            return jsonObject;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * 从JSON对象解析CustomCommand
     */
    private CustomCommand parseCommandFromJson(JSONObject jsonObject) {
        try {
            CustomCommand command = new CustomCommand();
            command.setId(jsonObject.getString("id"));
            command.setName(jsonObject.getString("name"));
            command.setCommand(jsonObject.getString("command"));
            command.setType(jsonObject.getInt("type"));
            command.setCreateTime(jsonObject.getLong("createTime"));
            return command;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * 初始化默认指令
     */
    public void initDefaultCommands() {
        List<CustomCommand> existingCommands = getAllCommands();
        if (!existingCommands.isEmpty()) {
            return; // 已有指令，不需要初始化
        }
        
        // 添加一些默认指令示例
        List<CustomCommand> defaultCommands = new ArrayList<>();
        
        defaultCommands.add(new CustomCommand("查询版本", "<RD_SOFT_VERSION>", CustomCommand.TYPE_ASCII));
        defaultCommands.add(new CustomCommand("重启设备", "FF01", CustomCommand.TYPE_HEX));
        defaultCommands.add(new CustomCommand("查询状态", "AT+STATUS?", CustomCommand.TYPE_ASCII));
        
        saveCommands(defaultCommands);
    }
}